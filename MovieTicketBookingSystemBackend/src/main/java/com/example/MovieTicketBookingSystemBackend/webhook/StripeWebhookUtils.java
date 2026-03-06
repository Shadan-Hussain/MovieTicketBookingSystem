package com.example.MovieTicketBookingSystemBackend.webhook;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Pure helpers for parsing Stripe webhook events and extracting session metadata.
 * No Stripe API calls; no dependency on application services.
 */
public final class StripeWebhookUtils {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookUtils.class);
    private static final String METADATA_SHOW_ID = "show_id";
    private static final String METADATA_SEAT_ID = "seat_id";
    private static final String METADATA_USER_ID = "user_id";

    private StripeWebhookUtils() {}

    /** Checkout session metadata (show_id, seat_id, user_id) required for both success and failure handling. */
    public record CheckoutMetadata(long showId, long seatId, long userId) {}

    public static Session deserializeSession(Event event) {
        return deserializeEventData(event, Session.class);
    }

    public static PaymentIntent deserializePaymentIntent(Event event) {
        return deserializeEventData(event, PaymentIntent.class);
    }

    /**
     * Extracts and validates required checkout metadata: show_id, seat_id, user_id. All are mandatory.
     */
    public static CheckoutMetadata getRequiredCheckoutMetadata(Session session) {
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null) return null;
        String showIdStr = metadata.get(METADATA_SHOW_ID);
        String seatIdStr = metadata.get(METADATA_SEAT_ID);
        String userIdStr = metadata.get(METADATA_USER_ID);
        if (isBlank(showIdStr) || isBlank(seatIdStr) || isBlank(userIdStr)) return null;
        try {
            return new CheckoutMetadata(
                    Long.parseLong(showIdStr),
                    Long.parseLong(seatIdStr),
                    Long.parseLong(userIdStr));
        } catch (NumberFormatException e) {
            log.warn("Invalid numeric metadata for session {}: show_id={}, seat_id={}, user_id={}",
                    session.getId(), showIdStr, seatIdStr, userIdStr, e);
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static <T extends StripeObject> T deserializeEventData(Event event, Class<T> type) {
        var deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            if (type.isInstance(obj)) return (T) obj;
        }
        try {
            StripeObject obj = deserializer.deserializeUnsafe();
            if (type.isInstance(obj)) return (T) obj;
        } catch (Exception e) {
            log.error("Failed to deserialize {} from event", type.getSimpleName(), e);
        }
        return null;
    }
}
