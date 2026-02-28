package com.example.MovieTicketBookingSystemBackend.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class SeatLockService {

    private static final String TICKET_LOCK_VALUE = "locked";
    private static final Duration SEAT_LOCK_TTL = Duration.ofMinutes(10);
    private static final Duration TICKET_LOCK_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;

    public SeatLockService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Lock key for (showId, seatId) — used for single-seat booking flow. Value is user_id (plain). */
    private static String seatLockKey(Long showId, Long seatId) {
        return "seat:lock:" + showId + ":" + seatId;
    }

    /** Idempotency key for webhook: one ticket per Stripe session. */
    private static String ticketLockKey(String stripeSessionId) {
        return "ticket:lock:" + stripeSessionId;
    }

    /** Returns true if the seat for this show is currently locked. */
    public boolean isLocked(Long showId, Long seatId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(seatLockKey(showId, seatId)));
    }

    /**
     * Returns the user_id that holds the lock, or empty if not locked.
     */
    public Optional<Long> getLockOwner(Long showId, Long seatId) {
        String key = seatLockKey(showId, seatId);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return Optional.empty();
        }
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns true if the seat is locked by the given user.
     */
    public boolean isLockedBy(Long showId, Long seatId, Long userId) {
        return getLockOwner(showId, seatId).filter(owner -> owner.equals(userId)).isPresent();
    }

    /**
     * Sets seat lock only if key does not exist (atomic). Value stored is user_id (plain).
     * @return true if lock was set, false if key already existed
     */
    public boolean setLockIfAbsent(Long showId, Long seatId, Long userId) {
        String value = String.valueOf(userId);
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(seatLockKey(showId, seatId), value, SEAT_LOCK_TTL));
    }

    /** Removes the seat lock (e.g. after payment success or failure). */
    public void removeLock(Long showId, Long seatId) {
        redisTemplate.delete(seatLockKey(showId, seatId));
    }

    /**
     * Sets ticket lock by session ID for idempotent webhook handling.
     * @return true if this request should create the ticket, false if duplicate webhook
     */
    public boolean setTicketLockIfAbsent(String stripeSessionId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(ticketLockKey(stripeSessionId), TICKET_LOCK_VALUE, TICKET_LOCK_TTL));
    }
}
