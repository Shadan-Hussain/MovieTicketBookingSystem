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

    private static String seatLockKey(Long seatId) {
        return "seat:lock:" + seatId;
    }

    private static String ticketLockKey(Long seatId) {
        return "ticket:lock:" + seatId;
    }

    /** Returns true if the seat is currently locked (key exists and TTL not expired). */
    public boolean isLocked(Long seatId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(seatLockKey(seatId)));
    }

    /**
     * Sets seat lock only if key does not exist (atomic). Value is "locked".
     * @return true if lock was set, false if key already existed (someone else locked it)
     */
    public boolean setLockIfAbsent(Long seatId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(seatLockKey(seatId), LOCK_VALUE, SEAT_LOCK_TTL));
    }

    /** Removes the seat lock (e.g. after payment success or failure). */
    public void removeLock(Long seatId) {
        redisTemplate.delete(seatLockKey(seatId));
    }

    /**
     * Sets ticket lock only if key does not exist (atomic). Used for idempotent payment-success handling.
     * @return true if lock was set (this request should create the ticket), false if already set (duplicate webhook → skip)
     */
    public boolean setTicketLockIfAbsent(Long seatId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(ticketLockKey(seatId), LOCK_VALUE, TICKET_LOCK_TTL));
    }
}
