package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.StripeSessionResponse;
import com.example.MovieTicketBookingSystemBackend.model.Seat;
import com.example.MovieTicketBookingSystemBackend.model.Ticket;
import com.example.MovieTicketBookingSystemBackend.repository.SeatRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TicketRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stripe Checkout sessions, refunds, and webhook handling (payment success only).
 */
@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatLockService seatLockService;

    @Value("${stripe.successUrl:http://localhost:8081/seats/success}")
    private String successUrl;

    @Value("${stripe.cancelUrl:http://localhost:8081/seats/cancel}")
    private String cancelUrl;

    /**
     * Creates a Stripe Checkout Session for the given seat.
     * Amount is taken from seat.getPrice() (expected in cents).
     *
     * @param seat the locked seat to pay for
     * @return session id and url for redirecting the customer to Stripe Checkout
     */
    public StripeSessionResponse createCheckoutSession(Seat seat) throws StripeException {
        Stripe.apiKey = secretKey;

        Long amountCents = seat.getPrice() != null ? seat.getPrice() : 100L;
        String productName = "Seat #" + seat.getSeatId();

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
                        .putMetadata("seatId", String.valueOf(seat.getSeatId()))
                        .build();

        Session session = Session.create(params);

        return new StripeSessionResponse(session.getId(), session.getUrl());
    }

    /**
     * Refunds the payment for a Checkout Session (e.g. when payment succeeded but Redis lock expired).
     */
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
     * Webhook: payment success (e.g. checkout.session.completed). Idempotent by session ID.
     * If Redis lock expired or any failure, initiates refund.
     */
    @Transactional
    public void handlePaymentSuccess(String sessionId, Long seatId) {
        if (!seatLockService.setTicketLockIfAbsent(seatId)) {
            return;
        }

        if (!seatLockService.isLocked(seatId)) {
            log.warn("Payment success but Redis lock expired for seatId={}, sessionId={}; initiating refund", seatId, sessionId);
            try {
                createRefundForCheckoutSession(sessionId);
            } catch (Exception ex) {
                log.error("Refund failed for sessionId={}", sessionId, ex);
            }
            return;
        }

        try {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new IllegalStateException("Seat not found: " + seatId));
            seat.setBookingStatus(Seat.STATUS_BOOKED);
            seatRepository.save(seat);

            Ticket ticket = new Ticket();
            ticket.setSeatId(seatId);
            ticket.setStripeSessionId(sessionId);
            ticket.setTicketNumber(Ticket.generateTicketNumber());
            ticket.setCreatedAt(java.time.Instant.now());
            ticketRepository.save(ticket);
        } catch (DataIntegrityViolationException e) {
            // Duplicate seat_id (late webhook after Redis TTL) — idempotent, skip
            return;
        } catch (Exception e) {
            log.error("handlePaymentSuccess failed for sessionId={}, seatId={}; initiating refund", sessionId, seatId, e);
            try {
                createRefundForCheckoutSession(sessionId);
            } catch (Exception ex) {
                log.error("Refund failed for sessionId={}", sessionId, ex);
            }
            throw e;
        }
    }
}
