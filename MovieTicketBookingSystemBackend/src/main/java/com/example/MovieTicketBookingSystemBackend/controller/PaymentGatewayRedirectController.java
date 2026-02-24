package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles redirects from the payment gateway (Stripe) after the customer completes or cancels checkout.
 * Stripe sends the user to our success/cancel URLs; we respond with a message and (on success) the session_id.
 */
@RestController
@RequestMapping("/redirect")
public class PaymentGatewayRedirectController {

    /**
     * Stripe redirects the customer's browser to this URL after successful payment.
     * We set successUrl in StripeService as .../redirect/success?session_id={CHECKOUT_SESSION_ID}.
     * Stripe replaces {CHECKOUT_SESSION_ID} with the real session id when redirecting, so the request
     * we receive is GET /redirect/success?session_id=cs_xxxxx — i.e. session_id comes from the query string.
     */
    @GetMapping("/success")
    public ResponseEntity<MessageResponse> paymentSuccess(
            @RequestParam(value = "session_id", required = false) String sessionId) {
        return ResponseEntity.ok(new MessageResponse("Payment succeeded", sessionId));
    }

    /** Stripe redirects here when the customer cancels or leaves Checkout. */
    @GetMapping("/cancel")
    public ResponseEntity<MessageResponse> paymentCancel() {
        return ResponseEntity.ok(new MessageResponse("Payment cancelled"));
    }
}
