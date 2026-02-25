package com.example.MovieTicketBookingSystemBackend.dto;

import java.time.OffsetDateTime;

public class ShowResponse {

    private Long showId;
    private Long movieId;
    private Long hallId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    public ShowResponse(Long showId, Long movieId, Long hallId, OffsetDateTime startTime, OffsetDateTime endTime) {
        this.showId = showId;
        this.movieId = movieId;
        this.hallId = hallId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getShowId() { return showId; }
    public Long getMovieId() { return movieId; }
    public Long getHallId() { return hallId; }
    public OffsetDateTime getStartTime() { return startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
}
