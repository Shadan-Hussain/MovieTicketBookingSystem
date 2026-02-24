package com.example.MovieTicketBookingSystemBackend.dto;

public class MessageResponse {

    private String message;
    private String sessionId;

    public MessageResponse(String message) {
        this.message = message;
    }

    public MessageResponse(String message, String sessionId) {
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
