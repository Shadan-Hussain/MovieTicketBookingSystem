package com.example.MovieTicketBookingSystemBackend.webhook;

import com.stripe.model.Event;

/**
 * Handles a specific Stripe webhook event type. Dispatcher routes events to the appropriate handler by type.
 */
public interface StripeWebhookEventHandler {

    /**
     * The Stripe event type this handler handles (e.g. "checkout.session.completed").
     */
    String supportedEventType();

    /**
     * Process the event. Extract and validate payload, then delegate to business logic.
     *
     * @param event the verified Stripe event
     * @return error message if processing failed, null on success
     */
    String handle(Event event);
}
