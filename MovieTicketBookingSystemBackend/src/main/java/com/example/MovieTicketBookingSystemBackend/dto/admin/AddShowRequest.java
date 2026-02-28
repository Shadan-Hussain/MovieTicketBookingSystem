package com.example.MovieTicketBookingSystemBackend.dto.admin;

import java.time.OffsetDateTime;

public class AddShowRequest {
    private Long movieId;
    private Long hallId;
    private OffsetDateTime startTime;
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
