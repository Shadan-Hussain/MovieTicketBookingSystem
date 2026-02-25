package com.example.MovieTicketBookingSystemBackend.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Processes Stripe webhook payloads: signature verification, event deserialization, and dispatch.
 * Delegates payment success handling to StripeService.
 */
@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);
    private static final String EVENT_CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    private final StripeService stripeService;

    public StripeWebhookService(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    /**
     * Verifies the webhook signature, constructs the event, and processes it.
     * Uses the raw request body so no whitespace or encoding changes break Stripe's signature.
     *
     * @param rawPayload     raw request body (exactly as received)
     * @param stripeSignature Stripe-Signature header
     * @return error message if processing failed (signature invalid, missing metadata, etc.), null if success
     */
    public String processWebhook(byte[] rawPayload, String stripeSignature) {
        if (stripeSignature == null || webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Webhook missing signature or secret");
            return "Missing signature or config";
        }

        // Single conversion from raw bytes to string — same as Stripe's recommendation (e.g. getContent() in PHP).
        // Any prior decode/re-encode can change whitespace and invalidate the signature.
        String payload = new String(rawPayload, StandardCharsets.UTF_8);

        Event event;
        try {
            event = Webhook.constructEvent(payload, stripeSignature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook signature verification failed: {} (payload length={}). For local dev use the secret from 'stripe listen', not Dashboard.",
                    e.getMessage(), rawPayload.length);
            return "Invalid signature";
        }

        if (EVENT_CHECKOUT_SESSION_COMPLETED.equals(event.getType())) {
            String err = processCheckoutSessionCompleted(event);
            if (err != null) {
                return err;
            }
        } else {
            log.debug("Ignoring event type: {}", event.getType());
        }

        return null;
    }

    /**
     * Deserializes the checkout session from the event and delegates to StripeService.
     * @return error message if session invalid or metadata missing, null on success
     */
    private String processCheckoutSessionCompleted(Event event) {
        Session session = deserializeSession(event);
        if (session == null) {
            return "Invalid checkout session in event";
        }

        String showId = session.getMetadata() != null ? session.getMetadata().get("show_id") : null;
        String seatId = session.getMetadata() != null ? session.getMetadata().get("seat_id") : null;
        if (showId == null || seatId == null || showId.isEmpty() || seatId.isEmpty()) {
            log.error("checkout.session missing metadata show_id/seat_id for session={}", session.getId());
            return "Missing metadata show_id/seat_id";
        }

        stripeService.handlePaymentSuccess(session.getId());
        return null;
    }

    private Session deserializeSession(Event event) {
        var deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            if (obj instanceof Session) {
                return (Session) obj;
            }
        }
        try {
            StripeObject obj = deserializer.deserializeUnsafe();
            if (obj instanceof Session) {
                return (Session) obj;
            }
        } catch (Exception e) {
            log.error("Failed to deserialize checkout session from event: {}", e.getMessage());
        }
        log.error("Event data object is not a checkout session");
        return null;
    }
}
