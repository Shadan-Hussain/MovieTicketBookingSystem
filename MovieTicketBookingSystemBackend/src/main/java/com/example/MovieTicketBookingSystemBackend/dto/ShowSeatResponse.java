package com.example.MovieTicketBookingSystemBackend.dto;

public class ShowSeatResponse {

    private Long seatId;
    private String number;
    private Long price;
    private String type;
    private String status; // AVAILABLE | BOOKED

    public ShowSeatResponse(Long seatId, String number, Long price, String type, String status) {
        this.seatId = seatId;
        this.number = number;
        this.price = price;
        this.type = type;
        this.status = status;
    }

    public Long getSeatId() { return seatId; }
    public String getNumber() { return number; }
    public Long getPrice() { return price; }
    public String getType() { return type; }
    public String getStatus() { return status; }
}
