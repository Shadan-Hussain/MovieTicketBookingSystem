package com.example.MovieTicketBookingSystemBackend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AddMovieRequest {
    @NotBlank(message = "name is required")
    private String name;
    @NotNull(message = "durationMins is required")
    private Integer durationMins;
    @NotBlank(message = "description is required")
    private String description;
    @NotBlank(message = "language is required")
    private String language;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDurationMins() { return durationMins; }
    public void setDurationMins(Integer durationMins) { this.durationMins = durationMins; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
