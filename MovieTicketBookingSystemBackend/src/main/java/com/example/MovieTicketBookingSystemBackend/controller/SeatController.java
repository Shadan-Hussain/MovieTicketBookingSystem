package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.StripeSessionResponse;
import com.example.MovieTicketBookingSystemBackend.model.Seat;
import com.example.MovieTicketBookingSystemBackend.service.SeatService;
import com.example.MovieTicketBookingSystemBackend.service.StripeService;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seats")
public class SeatController {

    @Autowired
    private SeatService seatService;

    @Autowired
    private StripeService stripeService;

    @GetMapping("/{id}")
    public ResponseEntity<Seat> getSeat(@PathVariable Long id) {
        Seat seat = seatService.getSeat(id);
        return ResponseEntity.ok(seat);
    }

    /** Locks the seat (AVAILABLE -> LOCKED, lock_expiry = now + 10 min). Then call pay to get Stripe Checkout URL. */
    @PutMapping("/{id}/book")
    public ResponseEntity<Seat> bookSeat(@PathVariable Long id) {
        Seat seat = seatService.bookSeat(id);
        return ResponseEntity.ok(seat);
    }

    /**
     * Creates a Stripe Checkout session for a LOCKED seat. Returns sessionId and sessionUrl;
     * client redirects the customer to sessionUrl to complete payment.
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<StripeSessionResponse> pay(@PathVariable Long id) {
        seatService.validateSeatLockedForPayment(id);
        Seat seat = seatService.getSeat(id);
        try {
            StripeSessionResponse response = stripeService.createCheckoutSession(seat);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
