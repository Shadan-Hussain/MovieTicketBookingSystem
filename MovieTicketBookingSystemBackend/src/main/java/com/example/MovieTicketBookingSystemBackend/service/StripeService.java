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
 * Stripe Checkout sessions and webhook handling for (show_id, seat_id) flow.
 * Creates Transaction PENDING when creating session; on success updates ShowSeat, Transaction, creates Ticket.
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

    /**
     * Creates a Stripe Checkout Session for (showId, seatId, userId). Creates Transaction PENDING with metadata.
     * Caller must hold Redis lock for (showId, seatId) for this user.
     */
    public StripeSessionResponse createCheckoutSession(Long showId, Long seatId, Long userId) throws StripeException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        Stripe.apiKey = secretKey;

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));
        long amountCents = seat.getPrice() != null ? seat.getPrice() : 100L;
        String productName = "Seat #" + seat.getNumber() + " (Show " + showId + ")";

        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(productName)
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmount(amountCents)
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

        Transaction txn = new Transaction();
        txn.setShow(showRepository.getReferenceById(showId));
        txn.setSeat(seatRepository.getReferenceById(seatId));
        txn.setUser(userRepository.getReferenceById(userId));
        txn.setStripeSessionId(session.getId());
        txn.setAmount(amountCents);
        txn.setCurrency("usd");
        txn.setStatus(Transaction.STATUS_PENDING);
        txn.setCreatedAt(Instant.now());
        txn.setUpdatedAt(Instant.now());
        transactionRepository.save(txn);

        return new StripeSessionResponse(session.getId(), session.getUrl());
    }

    /**
     * Webhook: payment_intent.payment_failed. Marks the corresponding transaction as FAILED if found.
     */
    @Transactional
    public void handlePaymentFailure(String paymentIntentId) {
        try {
            Stripe.apiKey = secretKey;
            SessionListParams params = SessionListParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setLimit(1L)
                    .build();
            SessionCollection sessions = Session.list(params);
            if (sessions == null || sessions.getData() == null || sessions.getData().isEmpty()) {
                log.warn("Payment failure but no checkout session found for paymentIntentId={}", paymentIntentId);
                return;
            }
            Session session = sessions.getData().get(0);
            String sessionId = session.getId();
            Optional<Transaction> optTxn = transactionRepository.findByStripeSessionId(sessionId);
            if (optTxn.isEmpty()) {
                log.warn("Payment failure but no transaction for sessionId={}, paymentIntentId={}", sessionId, paymentIntentId);
                return;
            }
            Transaction txn = optTxn.get();
            if (Transaction.STATUS_FAILED.equals(txn.getStatus())
                    || Transaction.STATUS_SUCCESS.equals(txn.getStatus())
                    || Transaction.STATUS_REFUND_INITIATED.equals(txn.getStatus())) {
                return;
            }
            txn.setStatus(Transaction.STATUS_FAILED);
            txn.setUpdatedAt(Instant.now());
            transactionRepository.save(txn);
            log.info("Payment failed: transactionId={} marked FAILED (paymentIntentId={}, sessionId={})",
                    txn.getTransactionId(), paymentIntentId, sessionId);
        } catch (Exception e) {
            log.error("handlePaymentFailure failed for paymentIntentId={}", paymentIntentId, e);
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
     * Webhook: checkout.session.completed. Idempotent by stripe_session_id.
     * Sets ShowSeat BOOKED, Transaction SUCCESS, creates Ticket. If lock expired, initiates refund.
     */
    @Transactional
    public void handlePaymentSuccess(String sessionId) {
        Optional<Transaction> optTxn = transactionRepository.findByStripeSessionId(sessionId);
        if (optTxn.isEmpty()) {
            log.warn("Payment success but no transaction for sessionId={}", sessionId);
            return;
        }
        Transaction txn = optTxn.get();
        if (Transaction.STATUS_SUCCESS.equals(txn.getStatus())) {
            return; // idempotent, already done
        }

        boolean lockSet = seatLockService.setTicketLockIfAbsent(sessionId);
        if (!lockSet) {
            // Duplicate webhook or retry: re-fetch and return if already SUCCESS; otherwise fall through and complete
            optTxn = transactionRepository.findByStripeSessionId(sessionId);
            if (optTxn.isEmpty()) {
                return;
            }
            if (Transaction.STATUS_SUCCESS.equals(optTxn.get().getStatus())) {
                return;
            }
            txn = optTxn.get();
        }

        Long showId = txn.getShowId();
        Long seatId = txn.getSeatId();

        if (!seatLockService.isLocked(showId, seatId)) {
            log.warn("Payment success but Redis lock expired for showId={}, seatId={}, sessionId={}; initiating refund", showId, seatId, sessionId);
            try {
                createRefundForCheckoutSession(sessionId);
                txn.setStatus(Transaction.STATUS_REFUND_INITIATED);
                txn.setUpdatedAt(Instant.now());
                transactionRepository.save(txn);
            } catch (Exception ex) {
                log.error("Refund failed for sessionId={}", sessionId, ex);
            }
            return;
        }

        try {
            ShowSeat showSeat = showSeatRepository.findByShow_ShowIdAndSeat_SeatId(showId, seatId)
                    .orElseThrow(() -> new IllegalStateException("ShowSeat not found: " + showId + "," + seatId));
            showSeat.setStatus(ShowSeat.STATUS_BOOKED);
            showSeatRepository.save(showSeat);

            txn.setStatus(Transaction.STATUS_SUCCESS);
            txn.setUpdatedAt(Instant.now());
            transactionRepository.save(txn);
            log.info("Payment success: transactionId={} showId={} seatId={} sessionId={}", txn.getTransactionId(), showId, seatId, sessionId);

            if (ticketRepository.findByTransaction_TransactionId(txn.getTransactionId()).isEmpty()) {
                Ticket ticket = new Ticket();
                ticket.setTransaction(txn);
                ticket.setCreatedAt(Instant.now());
                ticketRepository.save(ticket);
            }

            seatLockService.removeLock(showId, seatId);
        } catch (DataIntegrityViolationException e) {
            // Duplicate ticket: ensure transaction is SUCCESS
            txn.setStatus(Transaction.STATUS_SUCCESS);
            txn.setUpdatedAt(Instant.now());
            transactionRepository.save(txn);
        } catch (Exception e) {
            log.error("handlePaymentSuccess failed for sessionId={}, showId={}, seatId={}; initiating refund", sessionId, showId, seatId, e);
            try {
                createRefundForCheckoutSession(sessionId);
                txn.setStatus(Transaction.STATUS_REFUND_INITIATED);
                txn.setUpdatedAt(Instant.now());
                transactionRepository.save(txn);
            } catch (Exception ex) {
                log.error("Refund failed for sessionId={}", sessionId, ex);
            }
            throw e;
        }
    }
}
