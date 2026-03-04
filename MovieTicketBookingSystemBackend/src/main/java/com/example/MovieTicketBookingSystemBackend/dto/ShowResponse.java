package com.example.MovieTicketBookingSystemBackend.dto;

import java.time.OffsetDateTime;

public class ShowResponse {

    private Long showId;
    private Long movieId;
    private Long hallId;
    private String theatreName;
    private String hallName;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    public ShowResponse(Long showId, Long movieId, Long hallId, String theatreName, String hallName,
                        OffsetDateTime startTime, OffsetDateTime endTime) {
        this.showId = showId;
        this.movieId = movieId;
        this.hallId = hallId;
        this.theatreName = theatreName;
        this.hallName = hallName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getShowId() { return showId; }
    public Long getMovieId() { return movieId; }
    public Long getHallId() { return hallId; }
    public String getTheatreName() { return theatreName; }
    public String getHallName() { return hallName; }
    public OffsetDateTime getStartTime() { return startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
}
