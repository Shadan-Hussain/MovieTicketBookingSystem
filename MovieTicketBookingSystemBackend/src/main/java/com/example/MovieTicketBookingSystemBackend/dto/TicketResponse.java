package com.example.MovieTicketBookingSystemBackend.dto;

import java.time.Instant;

public class TicketResponse {

    private Long ticketId;
    private Long showId;
    private Long seatId;
    private String seatNumber;
    private Long transactionId;
    private String movieName;
    private String theatreName;
    private String theatreAddress;
    private String hallName;
    private String showStartTime;
    private String showEndTime;
    private Instant createdAt;

    public TicketResponse(Long ticketId, Long showId, Long seatId, String seatNumber, Long transactionId,
                          String movieName, String theatreName, String theatreAddress, String hallName,
                          String showStartTime, String showEndTime, Instant createdAt) {
        this.ticketId = ticketId;
        this.showId = showId;
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.transactionId = transactionId;
        this.movieName = movieName;
        this.theatreName = theatreName;
        this.theatreAddress = theatreAddress;
        this.hallName = hallName;
        this.showStartTime = showStartTime;
        this.showEndTime = showEndTime;
        this.createdAt = createdAt;
    }

    public Long getTicketId() { return ticketId; }
    public String getMovieName() { return movieName; }
    public Long getShowId() { return showId; }
    public Long getSeatId() { return seatId; }
    public String getSeatNumber() { return seatNumber; }
    public Long getTransactionId() { return transactionId; }
    public String getTheatreName() { return theatreName; }
    public String getTheatreAddress() { return theatreAddress; }
    public String getHallName() { return hallName; }
    public String getShowStartTime() { return showStartTime; }
    public String getShowEndTime() { return showEndTime; }
    public Instant getCreatedAt() { return createdAt; }
}
