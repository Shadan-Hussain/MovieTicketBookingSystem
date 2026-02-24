package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.model.Seat;
import com.example.MovieTicketBookingSystemBackend.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SeatService {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatLockService seatLockService;

    public Seat getSeat(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seat not found"));
    }

    /**
     * Redis-first: try to acquire lock in Redis (SET NX). Only if we get it, touch the DB.
     * Rejected requests never hit the DB, so the DB is not a concurrency bottleneck.
     */
    public Seat bookSeat(Long id) {
        if (!seatLockService.setLockIfAbsent(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seat is locked; complete payment or retry after lock expiry");
        }

        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> {
                    seatLockService.removeLock(id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Seat not found");
                });

        if (Seat.STATUS_BOOKED.equals(seat.getBookingStatus())) {
            seatLockService.removeLock(id);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat already booked");
        }

        return seat;
    }

    /** Validates seat is locked in Redis (lock not expired). Use before starting payment. */
    public void validateSeatLockedForPayment(Long seatId) {
        if (!seatLockService.isLocked(seatId)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Lock expired; please book again");
        }
    }

    /** Called by payment webhook: remove Redis lock and set seat to BOOKED or AVAILABLE in DB. */
    @Transactional
    public Seat completePayment(Long seatId, boolean success) {
        if (!seatLockService.isLocked(seatId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is not locked; cannot complete payment");
        }

        seatLockService.removeLock(seatId);

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seat not found"));

        seat.setBookingStatus(success ? Seat.STATUS_BOOKED : Seat.STATUS_AVAILABLE);
        return seatRepository.saveAndFlush(seat);
    }
}
