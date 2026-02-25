package com.example.MovieTicketBookingSystemBackend.dto;

import java.time.Instant;

public class TicketResponse {

    private Long ticketId;
    private Long showId;
    private Long seatId;
    private Long transactionId;
    private Instant createdAt;

    public TicketResponse(Long ticketId, Long showId, Long seatId, Long transactionId, Instant createdAt) {
        this.ticketId = ticketId;
        this.showId = showId;
        this.seatId = seatId;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }

    public Long getTicketId() { return ticketId; }
    public Long getShowId() { return showId; }
    public Long getSeatId() { return seatId; }
    public Long getTransactionId() { return transactionId; }
    public Instant getCreatedAt() { return createdAt; }
}
