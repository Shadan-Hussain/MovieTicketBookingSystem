package com.example.MovieTicketBookingSystemBackend.dto;

public class RedirectResponse {

    private String message;
    private String sessionId;

    public RedirectResponse(String message) {
        this.message = message;
    }

    public RedirectResponse(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public String getSessionId() {
        return sessionId;
    }
}
