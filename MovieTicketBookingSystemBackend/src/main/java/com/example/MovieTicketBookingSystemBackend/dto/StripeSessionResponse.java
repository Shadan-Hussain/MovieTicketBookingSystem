package com.example.MovieTicketBookingSystemBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StripeSessionResponse {

    @JsonProperty("sessionId")
    private String sessionId;
    @JsonProperty("sessionUrl")
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
