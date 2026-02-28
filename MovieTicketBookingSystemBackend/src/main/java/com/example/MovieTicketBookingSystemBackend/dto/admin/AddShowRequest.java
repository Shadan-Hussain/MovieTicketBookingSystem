package com.example.MovieTicketBookingSystemBackend.dto.admin;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class AddShowRequest {
    @NotNull(message = "movieId is required")
    private Long movieId;
    @NotNull(message = "hallId is required")
    private Long hallId;
    @NotNull(message = "startTime is required")
    private OffsetDateTime startTime;
    @NotNull(message = "endTime is required")
    private OffsetDateTime endTime;

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public Long getHallId() { return hallId; }
    public void setHallId(Long hallId) { this.hallId = hallId; }
    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
}
