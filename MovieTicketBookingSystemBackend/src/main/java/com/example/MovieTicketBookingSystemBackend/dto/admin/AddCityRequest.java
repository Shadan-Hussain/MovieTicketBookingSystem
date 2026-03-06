package com.example.MovieTicketBookingSystemBackend.dto.admin;

import jakarta.validation.constraints.NotBlank;

public class AddCityRequest {
    @NotBlank(message = "name is required")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
