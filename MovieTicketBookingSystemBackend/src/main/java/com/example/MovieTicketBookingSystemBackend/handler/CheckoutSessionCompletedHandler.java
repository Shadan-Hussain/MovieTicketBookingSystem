package com.example.MovieTicketBookingSystemBackend.handler;

import com.example.MovieTicketBookingSystemBackend.service.StripeService;
import com.example.MovieTicketBookingSystemBackend.util.StripeWebhookUtils;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CheckoutSessionCompletedHandler implements StripeWebhookEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CheckoutSessionCompletedHandler.class);
    private static final String EVENT_TYPE = "checkout.session.completed";

    private final StripeService stripeService;

    public CheckoutSessionCompletedHandler(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @Override
    public String supportedEventType() {
        return EVENT_TYPE;
    }

    @Override
    public String handle(Event event) {
        Session session = StripeWebhookUtils.deserializeSession(event);
        if (session == null) return "Invalid checkout session in event";

        StripeWebhookUtils.CheckoutMetadata meta = StripeWebhookUtils.getRequiredCheckoutMetadata(session);
        if (meta == null) {
            log.error("checkout.session missing required metadata (show_id, seat_id, user_id) for session={}", session.getId());
            return "Missing metadata show_id, seat_id or user_id";
        }

        stripeService.handlePaymentSuccess(session.getId(), meta.showId(), meta.seatId(), meta.userId());
        return null;
    }
}
