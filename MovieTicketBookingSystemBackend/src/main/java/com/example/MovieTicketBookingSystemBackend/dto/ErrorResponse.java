package com.example.MovieTicketBookingSystemBackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String path;
    private String message;

    public ErrorResponse(int status, String error, String path, String message) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.path = path;
        this.message = message;
    }

    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getPath() { return path; }
    public String getMessage() { return message; }
}
