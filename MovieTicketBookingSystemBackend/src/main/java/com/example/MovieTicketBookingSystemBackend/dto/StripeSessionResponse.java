package com.example.MovieTicketBookingSystemBackend.dto;

public class StripeSessionResponse {

    private String sessionId;
    private String sessionUrl;

    public StripeSessionResponse(String sessionId, String sessionUrl) {
        this.sessionId = sessionId;
        this.sessionUrl = sessionUrl;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSessionUrl() {
        return sessionUrl;
    }
}
