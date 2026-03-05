package com.example.MovieTicketBookingSystemBackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis: seat_lock:{show_id}:{seat_id} → user_id or user_id:session_id.
 * TTL 10 min. Lock: if key exists do not proceed. Payment session: only if value is auth user and session_id not already set.
 */
@Service
public class SeatLockService {

    private static final Logger log = LoggerFactory.getLogger(SeatLockService.class);
    private static final Duration SEAT_LOCK_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;

    public SeatLockService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static String key(Long showId, Long seatId) {
        return "seat_lock:" + showId + ":" + seatId;
    }

    private String getValue(Long showId, Long seatId) {
        return redisTemplate.opsForValue().get(key(showId, seatId));
    }

    public boolean isLocked(Long showId, Long seatId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(showId, seatId)));
    }

    /** Owner of the lock (user_id from value). */
    public Optional<Long> getLockOwner(Long showId, Long seatId) {
        String value = getValue(showId, seatId);
        if (value == null || value.isEmpty()) return Optional.empty();
        String userIdStr = value.contains(":") ? value.substring(0, value.indexOf(':')) : value;
        try {
            return Optional.of(Long.parseLong(userIdStr));
        } catch (NumberFormatException e) {
            log.warn("Invalid lock value: key={}, value={}", key(showId, seatId), value, e);
            return Optional.empty();
        }
    }

    public boolean isLockedBy(Long showId, Long seatId, Long userId) {
        return getLockOwner(showId, seatId).filter(owner -> owner.equals(userId)).isPresent();
    }

    /**
     * Set lock only if key does not exist. Value = user_id. TTL 10 min.
     * If key exists do not proceed (return false).
     */
    public boolean setLockIfAbsent(Long showId, Long seatId, Long userId) {
        String value = String.valueOf(userId);
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key(showId, seatId), value, SEAT_LOCK_TTL));
    }

    /**
     * Set session_id on the lock. Call only when value is auth user and session_id not already there.
     * Checks: (1) value must be this user, (2) value must not already contain session_id (no colon).
     * If either fails returns false (do not proceed). Otherwise sets value to user_id:session_id and resets TTL to 10 min.
     */
    public boolean setPaymentSession(Long showId, Long seatId, Long userId, String sessionId) {
        String k = key(showId, seatId);
        String current = redisTemplate.opsForValue().get(k);
        if (current == null || current.isEmpty()) {
            log.warn("setPaymentSession: no lock for showId={}, seatId={}", showId, seatId);
            return false;
        }
        String currentUserId = current.contains(":") ? current.substring(0, current.indexOf(':')) : current;
        if (!String.valueOf(userId).equals(currentUserId)) {
            log.warn("setPaymentSession: lock held by another user for showId={}, seatId={}", showId, seatId);
            return false;
        }
        if (current.contains(":")) {
            log.warn("setPaymentSession: session_id already set for showId={}, seatId={}", showId, seatId);
            return false;
        }
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        String newValue = userId + ":" + sessionId;
        redisTemplate.opsForValue().set(k, newValue, SEAT_LOCK_TTL);
        return true;
    }

    public static final class PaymentSessionData {
        private final Long userId;
        private final String sessionId;

        public PaymentSessionData(Long userId, String sessionId) {
            this.userId = userId;
            this.sessionId = sessionId;
        }

        public Long getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
    }

    /** Get stored user_id and session_id for (show_id, seat_id). Empty if key missing. */
    public Optional<PaymentSessionData> getPaymentSessionData(Long showId, Long seatId) {
        String value = getValue(showId, seatId);
        if (value == null || value.isEmpty()) return Optional.empty();
        int colon = value.indexOf(':');
        try {
            Long uid = Long.parseLong(colon >= 0 ? value.substring(0, colon) : value);
            String sid = (colon >= 0 && colon < value.length() - 1) ? value.substring(colon + 1) : null;
            return Optional.of(new PaymentSessionData(uid, sid));
        } catch (NumberFormatException e) {
            log.warn("Invalid payment session value: key={}, value={}", key(showId, seatId), value, e);
            return Optional.empty();
        }
    }

    public void removeLock(Long showId, Long seatId) {
        redisTemplate.delete(key(showId, seatId));
    }
}
