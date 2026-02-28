package com.example.MovieTicketBookingSystemBackend.dto.admin;

import java.time.LocalDate;

public class AddMovieRequest {
    private String name;
    private Integer durationMins;
    private String description;
    private String posterUrl;
    private String language;
    private LocalDate releaseDate;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDurationMins() { return durationMins; }
    public void setDurationMins(Integer durationMins) { this.durationMins = durationMins; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
}
