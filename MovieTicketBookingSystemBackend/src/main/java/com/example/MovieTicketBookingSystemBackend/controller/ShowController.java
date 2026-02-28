package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.MessageResponse;
import com.example.MovieTicketBookingSystemBackend.dto.ShowResponse;
import com.example.MovieTicketBookingSystemBackend.dto.ShowSeatResponse;
import com.example.MovieTicketBookingSystemBackend.dto.StripeSessionResponse;
import com.example.MovieTicketBookingSystemBackend.model.ShowSeat;
import com.example.MovieTicketBookingSystemBackend.model.Transaction;
import com.example.MovieTicketBookingSystemBackend.repository.ShowSeatRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TransactionRepository;
import com.example.MovieTicketBookingSystemBackend.service.SeatLockService;
import com.example.MovieTicketBookingSystemBackend.service.ShowService;
import com.example.MovieTicketBookingSystemBackend.service.StripeService;
import com.stripe.exception.StripeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;
    private final ShowSeatRepository showSeatRepository;
    private final TransactionRepository transactionRepository;
    private final SeatLockService seatLockService;
    private final StripeService stripeService;

    public ShowController(ShowService showService, ShowSeatRepository showSeatRepository,
                          TransactionRepository transactionRepository,
                          SeatLockService seatLockService, StripeService stripeService) {
        this.showService = showService;
        this.showSeatRepository = showSeatRepository;
        this.transactionRepository = transactionRepository;
        this.seatLockService = seatLockService;
        this.stripeService = stripeService;
    }

    @GetMapping
    public ResponseEntity<List<ShowResponse>> getShows(
            @RequestParam Long city_id,
            @RequestParam Long movie_id) {
        return ResponseEntity.ok(showService.getShowsByCityAndMovie(city_id, movie_id));
    }

    @GetMapping("/{showId}/seats")
    public ResponseEntity<List<ShowSeatResponse>> getSeatsForShow(@PathVariable Long showId) {
        return ResponseEntity.ok(showService.getSeatsForShow(showId));
    }

    @PostMapping("/{showId}/seats/{seatId}/lock")
    public ResponseEntity<MessageResponse> lockSeat(
            @PathVariable Long showId,
            @PathVariable Long seatId,
            @RequestAttribute("userId") Long userId) {
        var showSeat = showSeatRepository.findByShow_ShowIdAndSeat_SeatId(showId, seatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ShowSeat not found"));
        if (!ShowSeat.STATUS_AVAILABLE.equals(showSeat.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is not available");
        }
        if (!seatLockService.setLockIfAbsent(showId, seatId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat already locked");
        }
        return ResponseEntity.ok(new MessageResponse("Seat successfully locked"));
    }

    @PostMapping("/{showId}/seats/{seatId}/payment-session")
    public ResponseEntity<StripeSessionResponse> createPaymentSession(
            @PathVariable Long showId,
            @PathVariable Long seatId,
            @RequestAttribute("userId") Long userId) {
        if (!seatLockService.isLockedBy(showId, seatId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lock expired or held by another user; please lock the seat again");
        }
        transactionRepository.findFirstByShow_ShowIdAndSeat_SeatIdOrderByCreatedAtDesc(showId, seatId)
                .ifPresent(txn -> {
                    if (Transaction.STATUS_SUCCESS.equals(txn.getStatus())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "A successful transaction already exists for this show and seat");
                    }
                    if (Transaction.STATUS_PENDING.equals(txn.getStatus())) {
                        Instant created = txn.getCreatedAt();
                        Instant now = Instant.now();
                        if (created != null && Duration.between(created, now).toMinutes() < 10) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                    "A recent pending transaction exists for this show and seat; please wait a few minutes");
                        }
                        // Pending but older than 10 minutes: mark as FAILED so we can proceed
                        txn.setStatus(Transaction.STATUS_FAILED);
                        txn.setUpdatedAt(now);
                        transactionRepository.save(txn);
                    }
                });
        var showSeat = showSeatRepository.findByShow_ShowIdAndSeat_SeatId(showId, seatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ShowSeat not found"));
        if (!ShowSeat.STATUS_AVAILABLE.equals(showSeat.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is not available");
        }
        try {
            StripeSessionResponse response = stripeService.createCheckoutSession(showId, seatId, userId);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment session creation failed", e);
        }
    }
}
