package com.example.MovieTicketBookingSystemBackend.dto.admin;

public class AddSeatsResponse {
    private final String message;

    public AddSeatsResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
