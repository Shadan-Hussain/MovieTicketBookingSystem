package com.example.MovieTicketBookingSystemBackend.dto;

public class MovieResponse {

    private Long movieId;
    private String name;
    private Integer durationMins;
    private String description;
    private String posterUrl;
    private String language;

    public MovieResponse(Long movieId, String name, Integer durationMins, String description,
                         String posterUrl, String language) {
        this.movieId = movieId;
        this.name = name;
        this.durationMins = durationMins;
        this.description = description;
        this.posterUrl = posterUrl;
        this.language = language;
    }

    public Long getMovieId() { return movieId; }
    public String getName() { return name; }
    public Integer getDurationMins() { return durationMins; }
    public String getDescription() { return description; }
    public String getPosterUrl() { return posterUrl; }
    public String getLanguage() { return language; }
}
