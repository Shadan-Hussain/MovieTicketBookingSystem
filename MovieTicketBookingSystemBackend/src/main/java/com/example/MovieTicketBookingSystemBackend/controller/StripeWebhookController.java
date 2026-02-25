package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.service.StripeWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Receives Stripe webhook events. Configure Stripe to POST to /webhook/stripe
 * (e.g. use stripe listen --forward-to localhost:8081/webhook/stripe).
 * Passes the raw request body to the service so Stripe's signature verification gets the exact bytes Stripe signed.
 */
@RestController
@RequestMapping("/webhook")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) throws IOException {
        // Pass raw body only — no decode/re-encode. Service converts to string once for constructEvent.
        byte[] rawPayload = request.getInputStream().readAllBytes();
        String sigHeader = request.getHeader("Stripe-Signature");

        String error = stripeWebhookService.processWebhook(rawPayload, sigHeader);
        if (error != null) {
            log.warn("Stripe webhook rejected: {}", error);
            return ResponseEntity.badRequest().body(error);
        }
        return ResponseEntity.ok().build();
    }
}
