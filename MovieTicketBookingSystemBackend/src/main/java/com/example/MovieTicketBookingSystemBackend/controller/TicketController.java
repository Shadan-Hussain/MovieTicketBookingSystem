package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.TicketResponse;
import com.example.MovieTicketBookingSystemBackend.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Fetch ticket by show and seat (successful payment).
 */
@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<?> getTicketsOrOne(
            @RequestParam(required = false) Long show_id,
            @RequestParam(required = false) Long seat_id,
            @RequestAttribute("userId") Long userId) {
        if (show_id != null && seat_id != null) {
            return ticketService.getTicket(show_id, seat_id, userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.ok(ticketService.listTicketsForUser(userId));
    }
}
