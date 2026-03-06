package com.example.MovieTicketBookingSystemBackend.dispatcher;

import com.example.MovieTicketBookingSystemBackend.handler.StripeWebhookEventHandler;
import com.stripe.model.Event;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Routes Stripe webhook events to the correct handler by event type. No switch-case; handlers register by type.
 */
@Component
public class StripeWebhookEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookEventDispatcher.class);

    private final List<StripeWebhookEventHandler> handlers;
    private Map<String, StripeWebhookEventHandler> handlerByEventType;

    public StripeWebhookEventDispatcher(List<StripeWebhookEventHandler> handlers) {
        this.handlers = handlers;
    }

    @PostConstruct
    void init() {
        this.handlerByEventType = handlers.stream()
                .collect(Collectors.toMap(StripeWebhookEventHandler::supportedEventType, h -> h, (a, b) -> a));
    }

    /**
     * Dispatch the event to the handler for its type. Unknown event types are ignored.
     *
     * @param event the verified Stripe event
     * @return error message if the handler returned one, null on success or if event type is ignored
     */
    public String dispatch(Event event) {
        StripeWebhookEventHandler handler = handlerByEventType.get(event.getType());
        if (handler == null) {
            log.debug("Ignoring event type: {}", event.getType());
            return null;
        }
        return handler.handle(event);
    }
}
