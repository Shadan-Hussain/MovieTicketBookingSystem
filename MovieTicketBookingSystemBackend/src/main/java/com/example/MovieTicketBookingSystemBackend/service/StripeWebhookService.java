package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dispatcher.StripeWebhookEventDispatcher;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    @Value("${STRIPE_WEBHOOK_SECRET:}")
    private String webhookSecret;

    private final StripeWebhookEventDispatcher eventDispatcher;

    public StripeWebhookService(StripeWebhookEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    public String processWebhook(byte[] rawPayload, String stripeSignature) {
        if (stripeSignature == null || webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Webhook missing signature or secret");
            return "Missing signature or config";
        }

        String payload = new String(rawPayload, StandardCharsets.UTF_8);
        Event event;
        try {
            event = Webhook.constructEvent(payload, stripeSignature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook signature verification failed: {} (payload length={}). For local dev use the secret from 'stripe listen', not Dashboard.",
                    e.getMessage(), rawPayload.length, e);
            return "Invalid signature";
        }

        return eventDispatcher.dispatch(event);
    }
}

