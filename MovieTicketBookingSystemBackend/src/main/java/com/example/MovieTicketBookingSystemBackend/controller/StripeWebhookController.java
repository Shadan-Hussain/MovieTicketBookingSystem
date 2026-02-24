package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Receives Stripe webhook events. Configure Stripe to POST to /webhook/stripe
 * (e.g. use stripe listen --forward-to localhost:8081/webhook/stripe).
 */
@RestController
@RequestMapping("/webhook")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    private final StripeService stripeService;

    public StripeWebhookController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) throws IOException {
        String payload = request.getReader().lines().collect(Collectors.joining("\n"));
        String sigHeader = request.getHeader("Stripe-Signature");

        if (sigHeader == null || webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Webhook missing signature or secret");
            return ResponseEntity.badRequest().body("Missing signature or config");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            handleCheckoutSessionCompleted(event);
        } else {
            log.debug("Ignoring event type: {}", event.getType());
        }

        return ResponseEntity.ok().build();
    }

    /** Handles checkout.session.completed (payment success) only. Other events are ignored; locks expire after TTL. */
    private void handleCheckoutSessionCompleted(Event event) {
        Session session = null;
        var deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            if (obj instanceof Session) {
                session = (Session) obj;
            }
        }
        if (session == null) {
            try {
                StripeObject obj = deserializer.deserializeUnsafe();
                if (obj instanceof Session) {
                    session = (Session) obj;
                }
            } catch (Exception e) {
                log.error("Failed to deserialize checkout session from event: {} (API version mismatch? use deserializeUnsafe)", e.getMessage());
                return;
            }
        }
        if (session == null) {
            log.error("Event data object is not a checkout session");
            return;
        }
        String seatIdStr = session.getMetadata() != null ? session.getMetadata().get("seatId") : null;
        if (seatIdStr == null || seatIdStr.isEmpty()) {
            log.error("checkout.session missing metadata.seatId for session={}", session.getId());
            return;
        }
        try {
            Long seatId = Long.parseLong(seatIdStr);
            stripeService.handlePaymentSuccess(session.getId(), seatId);
        } catch (NumberFormatException e) {
            log.error("Invalid seatId in metadata: {}", seatIdStr, e);
        }
    }
}
