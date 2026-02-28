package com.example.MovieTicketBookingSystemBackend.dto.admin;

import jakarta.validation.constraints.NotBlank;

public class AddCityRequest {
    @NotBlank(message = "name is required")
    private String name;
    @NotBlank(message = "stateCode is required")
    private String stateCode;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
}
