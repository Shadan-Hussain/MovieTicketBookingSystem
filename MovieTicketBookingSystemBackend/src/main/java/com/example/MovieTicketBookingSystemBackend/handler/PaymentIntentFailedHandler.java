package com.example.MovieTicketBookingSystemBackend.handler;

import com.example.MovieTicketBookingSystemBackend.service.StripeService;
import com.example.MovieTicketBookingSystemBackend.util.StripeWebhookUtils;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentIntentFailedHandler implements StripeWebhookEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentIntentFailedHandler.class);
    private static final String EVENT_TYPE = "payment_intent.payment_failed";

    private final StripeService stripeService;

    public PaymentIntentFailedHandler(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @Override
    public String supportedEventType() {
        return EVENT_TYPE;
    }

    @Override
    public String handle(Event event) {
        PaymentIntent paymentIntent = StripeWebhookUtils.deserializePaymentIntent(event);
        if (paymentIntent == null) return "Invalid payment intent in event";

        String paymentIntentId = paymentIntent.getId();
        if (paymentIntentId == null || paymentIntentId.isEmpty()) {
            log.error("payment_intent.payment_failed event without id");
            return "Missing payment_intent id";
        }

        Session session = stripeService.getSessionByPaymentIntentId(paymentIntentId);
        if (session == null) {
            log.warn("Payment failure: no checkout session found for paymentIntentId={}", paymentIntentId);
            return "No session for payment intent";
        }

        String sessionId = session.getId();
        if (sessionId == null || sessionId.isEmpty()) return "Invalid session in payment failure";

        StripeWebhookUtils.CheckoutMetadata meta = StripeWebhookUtils.getRequiredCheckoutMetadata(session);
        if (meta == null) {
            log.error("payment_intent.payment_failed: session missing required metadata (show_id, seat_id, user_id) for session={}", sessionId);
            return "Missing metadata show_id, seat_id or user_id";
        }

        stripeService.handlePaymentFailure(sessionId, meta.showId(), meta.seatId(), meta.userId());
        return null;
    }
}
