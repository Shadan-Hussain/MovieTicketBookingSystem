package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.StripeSessionResponse;
import com.example.MovieTicketBookingSystemBackend.model.Seat;
import com.example.MovieTicketBookingSystemBackend.model.ShowSeat;
import com.example.MovieTicketBookingSystemBackend.model.Ticket;
import com.example.MovieTicketBookingSystemBackend.model.Transaction;
import com.example.MovieTicketBookingSystemBackend.repository.SeatRepository;
import com.example.MovieTicketBookingSystemBackend.repository.ShowSeatRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TicketRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
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

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${stripe.successUrl:http://localhost:8081/seats/success}")
    private String successUrl;

    @Value("${stripe.cancelUrl:http://localhost:8081/seats/cancel}")
    private String cancelUrl;

    private final SeatRepository seatRepository;
    private final TransactionRepository transactionRepository;
    private final ShowSeatRepository showSeatRepository;
    private final TicketRepository ticketRepository;
    private final SeatLockService seatLockService;

    public StripeService(SeatRepository seatRepository,
                         TransactionRepository transactionRepository,
                         ShowSeatRepository showSeatRepository,
                         TicketRepository ticketRepository,
                         SeatLockService seatLockService) {
        this.seatRepository = seatRepository;
        this.transactionRepository = transactionRepository;
        this.showSeatRepository = showSeatRepository;
        this.ticketRepository = ticketRepository;
        this.seatLockService = seatLockService;
    }

    /**
     * Creates a Stripe Checkout Session for (showId, seatId). Creates Transaction PENDING with metadata.
     * Caller must hold Redis lock for (showId, seatId).
     */
    public StripeSessionResponse createCheckoutSession(Long showId, Long seatId) throws StripeException {
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
                        .build();

        Session session = Session.create(params);

        Transaction txn = new Transaction();
        txn.setShowId(showId);
        txn.setSeatId(seatId);
        txn.setStripeSessionId(session.getId());
        txn.setAmount(amountCents);
        txn.setCurrency("usd");
        txn.setStatus(Transaction.STATUS_PENDING);
        txn.setCreatedAt(Instant.now());
        txn.setUpdatedAt(Instant.now());
        transactionRepository.save(txn);

        return new StripeSessionResponse(session.getId(), session.getUrl());
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
            ShowSeat showSeat = showSeatRepository.findByShowIdAndSeatId(showId, seatId)
                    .orElseThrow(() -> new IllegalStateException("ShowSeat not found: " + showId + "," + seatId));
            showSeat.setStatus(ShowSeat.STATUS_BOOKED);
            showSeatRepository.save(showSeat);

            txn.setStatus(Transaction.STATUS_SUCCESS);
            txn.setUpdatedAt(Instant.now());
            transactionRepository.save(txn);
            log.info("Payment success: transactionId={} showId={} seatId={} sessionId={}", txn.getTransactionId(), showId, seatId, sessionId);

            if (ticketRepository.findByTransactionId(txn.getTransactionId()).isEmpty()) {
                Ticket ticket = new Ticket();
                ticket.setTransactionId(txn.getTransactionId());
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
