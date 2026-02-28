package com.example.MovieTicketBookingSystemBackend.dto;

import java.time.Instant;

public class TransactionResponse {

    private Long transactionId;
    private Long showId;
    private Long seatId;
    private Long amount;
    private String currency;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public TransactionResponse(Long transactionId, Long showId, Long seatId, Long amount,
                               String currency, String status, Instant createdAt, Instant updatedAt) {
        this.transactionId = transactionId;
        this.showId = showId;
        this.seatId = seatId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getTransactionId() { return transactionId; }
    public Long getShowId() { return showId; }
    public Long getSeatId() { return seatId; }
    public Long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
