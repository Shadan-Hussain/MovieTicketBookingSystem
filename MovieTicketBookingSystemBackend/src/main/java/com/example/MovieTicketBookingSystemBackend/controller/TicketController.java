package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.TicketResponse;
import com.example.MovieTicketBookingSystemBackend.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fetch ticket by seat (seat_id = seat number).
 */
@RestController
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    private TicketRepository ticketRepository;

    @GetMapping("/{seatId}")
    public ResponseEntity<TicketResponse> getBySeatId(@PathVariable Long seatId) {
        return ticketRepository.findBySeatId(seatId)
                .map(t -> ResponseEntity.ok(new TicketResponse(t.getTicketNumber(), t.getSeatId())))
                .orElse(ResponseEntity.notFound().build());
    }
}
