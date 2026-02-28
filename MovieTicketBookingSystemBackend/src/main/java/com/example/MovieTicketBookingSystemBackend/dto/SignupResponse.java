package com.example.MovieTicketBookingSystemBackend.dto;

public class SignupResponse {
    private Long id;

    public SignupResponse(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
