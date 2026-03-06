package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.StripeSessionResponse;
import com.example.MovieTicketBookingSystemBackend.model.Seat;
import com.example.MovieTicketBookingSystemBackend.model.ShowSeat;
import com.example.MovieTicketBookingSystemBackend.model.Ticket;
import com.example.MovieTicketBookingSystemBackend.model.Transaction;
import com.example.MovieTicketBookingSystemBackend.repository.SeatRepository;
import com.example.MovieTicketBookingSystemBackend.repository.ShowRepository;
import com.example.MovieTicketBookingSystemBackend.repository.ShowSeatRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TicketRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TransactionRepository;
import com.example.MovieTicketBookingSystemBackend.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.model.checkout.SessionCollection;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionListParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Stripe Checkout sessions and webhook handling. Single service: verify webhook, validate metadata, then handle payment success/failure.
 */
@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${STRIPE_SECRET_KEY:}")
    private String secretKey;

    @Value("${stripe.successUrl}")
    private String successUrl;

    @Value("${stripe.cancelUrl}")
    private String cancelUrl;

    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final ShowSeatRepository showSeatRepository;
    private final TicketRepository ticketRepository;
    private final SeatLockService seatLockService;

    public StripeService(SeatRepository seatRepository,
                         ShowRepository showRepository,
                         UserRepository userRepository,
                         TransactionRepository transactionRepository,
                         ShowSeatRepository showSeatRepository,
                         TicketRepository ticketRepository,
                         SeatLockService seatLockService) {
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.showSeatRepository = showSeatRepository;
        this.ticketRepository = ticketRepository;
        this.seatLockService = seatLockService;
    }

    /** Look up Checkout Session by payment intent id (for payment_intent.payment_failed). Used by PaymentIntentFailedHandler. */
    public Session getSessionByPaymentIntentId(String paymentIntentId) {
        try {
            Stripe.apiKey = secretKey;
            SessionListParams params = SessionListParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setLimit(1L)
                    .build();
            SessionCollection sessions = Session.list(params);
            if (sessions == null || sessions.getData() == null || sessions.getData().isEmpty()) {
                return null;
            }
            return sessions.getData().get(0);
        } catch (StripeException e) {
            log.warn("Failed to look up session by payment intent {}", paymentIntentId, e);
            return null;
        }
    }

    // ---------- Checkout session creation ----------

    /**
     * Creates a Stripe Checkout Session for (showId, seatId, userId). Does not create or update Transaction.
     * Caller must hold Redis lock for this user; session_id is stored in Redis only after this call.
     */
    public StripeSessionResponse createCheckoutSession(Long showId, Long seatId, Long userId) throws StripeException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        Stripe.apiKey = secretKey;

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));
        long amountRupees = seat.getPrice() != null ? seat.getPrice() : 100L;
        long amountPaise = amountRupees * 100L;
        String productName = "Seat #" + seat.getNumber() + " (Show " + showId + ")";

        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(productName)
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("inr")
                        .setUnitAmount(amountPaise)
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(priceData)
                        .build();

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .addLineItem(lineItem)
                        .putMetadata("show_id", String.valueOf(showId))
                        .putMetadata("seat_id", String.valueOf(seatId))
                        .putMetadata("user_id", String.valueOf(userId))
                        .build();

        Session session = Session.create(params);

        String sessionId = session.getId();
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalStateException("Stripe session has no id");
        }
        if (!seatLockService.setPaymentSession(showId, seatId, userId, sessionId)) {
            throw new IllegalStateException("Payment session already set or lock invalid");
        }

        return new StripeSessionResponse(sessionId, session.getUrl());
    }

    /**
     * Webhook: payment_intent.payment_failed. Session and metadata (show_id, seat_id, user_id) already resolved in processWebhook.
     */
    @Transactional
    public void handlePaymentFailure(String sessionId, Long showId, Long seatId, Long userId) {
        try {
            Optional<Transaction> existing = transactionRepository.findByStripeSessionId(sessionId);
            if (existing.isPresent()) {
                Transaction txn = existing.get();
                if (Transaction.STATUS_SUCCESS.equals(txn.getStatus()) || Transaction.STATUS_REFUND_INITIATED.equals(txn.getStatus())) {
                    return;
                }
                txn.setStatus(Transaction.STATUS_FAILED);
                txn.setUpdatedAt(Instant.now());
                transactionRepository.save(txn);
                log.info("Payment failed: transactionId={} marked FAILED", txn.getTransactionId());
                return;
            }
            Seat seat = seatRepository.findById(seatId).orElse(null);
            long amountRupees = seat != null && seat.getPrice() != null ? seat.getPrice() : 100L;
            Transaction txn = createAndSaveTransaction(showId, seatId, userId, sessionId, amountRupees, Transaction.STATUS_FAILED);
            log.info("Payment failed: created FAILED transactionId={} for sessionId={}", txn.getTransactionId(), sessionId);
        } catch (Exception e) {
            log.error("handlePaymentFailure failed for sessionId={}", sessionId, e);
        }
    }

    public void createRefundForCheckoutSession(String sessionId) throws StripeException {
        Stripe.apiKey = secretKey;
        Session session = Session.retrieve(sessionId);
        String paymentIntentId = session.getPaymentIntent();
        if (paymentIntentId == null || paymentIntentId.isEmpty()) {
            return;
        }
        Refund.create(RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .build());
    }

    /**
     * After Redis validation: create Transaction (SUCCESS), update ShowSeat, create Ticket. Does not remove Redis key.
     */
    private Transaction createAndSaveTransaction(Long showId, Long seatId, Long userId, String sessionId, long amountRupees, String status) {
        Transaction txn = new Transaction();
        txn.setShow(showRepository.getReferenceById(showId));
        txn.setSeat(seatRepository.getReferenceById(seatId));
        txn.setUser(userRepository.getReferenceById(userId));
        txn.setStripeSessionId(sessionId);
        txn.setAmount(amountRupees);
        txn.setCurrency("inr");
        txn.setStatus(status);
        txn.setCreatedAt(Instant.now());
        txn.setUpdatedAt(Instant.now());
        return transactionRepository.save(txn);
    }

    private void completeBooking(Long showId, Long seatId, Long userId, String sessionId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalStateException("Seat not found: " + seatId));
        long amountRupees = seat.getPrice() != null ? seat.getPrice() : 100L;
        Transaction txn = createAndSaveTransaction(showId, seatId, userId, sessionId, amountRupees, Transaction.STATUS_SUCCESS);

        ShowSeat showSeat = showSeatRepository.findByShow_ShowIdAndSeat_SeatId(showId, seatId)
                .orElseThrow(() -> new IllegalStateException("ShowSeat not found: " + showId + "," + seatId));
        showSeat.setStatus(ShowSeat.STATUS_BOOKED);
        showSeatRepository.save(showSeat);

        Ticket ticket = new Ticket();
        ticket.setTransaction(txn);
        ticket.setCreatedAt(Instant.now());
        ticketRepository.save(ticket);

        log.info("Payment success: transactionId={} showId={} seatId={} sessionId={}", txn.getTransactionId(), showId, seatId, sessionId);
    }

    private static class RefundRequiredException extends RuntimeException {
        RefundRequiredException(String message) { super(message); }
        RefundRequiredException(String message, Throwable cause) { super(message, cause); }
    }

    /**
     * Webhook: checkout.session.completed. show_id, seat_id, user_id are already validated by the webhook layer.
     * 1) Idempotency: if transaction already SUCCESS for this sessionId, return.
     * 2) Redis: key must exist, stored session_id must match; optionally userId must match.
     * 3) Create Transaction, update ShowSeat, create Ticket.
     * Any validation failure or booking failure triggers refund.
     */
    @Transactional
    public void handlePaymentSuccess(String sessionId, Long showId, Long seatId, Long userId) {
        try {
            Stripe.apiKey = secretKey;

            Optional<Transaction> existingTxn = transactionRepository.findByStripeSessionId(sessionId);
            if (existingTxn.isPresent() && Transaction.STATUS_SUCCESS.equals(existingTxn.get().getStatus())) {
                return;
            }

            var sessionData = seatLockService.getPaymentSessionData(showId, seatId);
            if (sessionData.isEmpty()) {
                throw new RefundRequiredException("Redis key missing for showId=" + showId + ", seatId=" + seatId + ", sessionId=" + sessionId);
            }
            String storedSessionId = sessionData.get().getSessionId();
            if (storedSessionId == null || !storedSessionId.equals(sessionId)) {
                throw new RefundRequiredException("Session id mismatch for showId=" + showId + ", seatId=" + seatId + ", sessionId=" + sessionId);
            }
            if (!userId.equals(sessionData.get().getUserId())) {
                throw new RefundRequiredException("User id mismatch for showId=" + showId + ", seatId=" + seatId + ", sessionId=" + sessionId);
            }

            completeBooking(showId, seatId, userId, sessionId);

        } catch (DataIntegrityViolationException e) {
            log.error("Duplicate booking (idempotent), sessionId={}", sessionId, e);
        } catch (Exception e) {
            log.error("handlePaymentSuccess failed for sessionId={}, initiating refund", sessionId, e);
            try {
                createRefundForCheckoutSession(sessionId);
            } catch (Exception ex) {
                log.error("Refund failed for sessionId={}", sessionId, ex);
            }
            throw e;
        }
    }
}
