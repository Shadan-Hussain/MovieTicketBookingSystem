package com.example.MovieTicketBookingSystemBackend.dto.admin;

public class CreatedResponse {
    private final Long id;

    public CreatedResponse(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
