package com.example.MovieTicketBookingSystemBackend.dto;

public class TicketResponse {

    private String ticketNumber;
    private Long seatId;

    public TicketResponse(String ticketNumber, Long seatId) {
        this.ticketNumber = ticketNumber;
        this.seatId = seatId;
    }

    public String getTicketNumber() { return ticketNumber; }
    public Long getSeatId() { return seatId; }
}
