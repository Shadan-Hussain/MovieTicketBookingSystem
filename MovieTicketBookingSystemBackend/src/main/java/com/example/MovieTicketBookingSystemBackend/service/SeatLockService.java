package com.example.MovieTicketBookingSystemBackend.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SeatLockService {

    private static final String LOCK_VALUE = "locked";
    private static final Duration SEAT_LOCK_TTL = Duration.ofMinutes(10);
    private static final Duration TICKET_LOCK_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;

    public SeatLockService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Lock key for (showId, seatId) — used for single-seat booking flow. */
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
     * Sets seat lock only if key does not exist (atomic).
     * @return true if lock was set, false if key already existed
     */
    public boolean setLockIfAbsent(Long showId, Long seatId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(seatLockKey(showId, seatId), LOCK_VALUE, SEAT_LOCK_TTL));
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
                redisTemplate.opsForValue().setIfAbsent(ticketLockKey(stripeSessionId), LOCK_VALUE, TICKET_LOCK_TTL));
    }
}
