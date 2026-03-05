package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.MessageResponse;
import com.example.MovieTicketBookingSystemBackend.dto.ShowResponse;
import com.example.MovieTicketBookingSystemBackend.dto.ShowSeatResponse;
import com.example.MovieTicketBookingSystemBackend.dto.StripeSessionResponse;
import com.example.MovieTicketBookingSystemBackend.service.ShowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping
    public ResponseEntity<List<ShowResponse>> getShows(
            @RequestParam Long city_id,
            @RequestParam Long movie_id) {
        return ResponseEntity.ok(showService.getShowsByCityAndMovie(city_id, movie_id));
    }

    @GetMapping("/{showId}")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable Long showId) {
        return ResponseEntity.ok(showService.getShowById(showId));
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
        return ResponseEntity.ok(showService.lockSeat(showId, seatId, userId));
    }

    @PostMapping("/{showId}/seats/{seatId}/payment-session")
    public ResponseEntity<StripeSessionResponse> createPaymentSession(
            @PathVariable Long showId,
            @PathVariable Long seatId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(showService.createPaymentSession(showId, seatId, userId));
    }
}
