package com.example.MovieTicketBookingSystemBackend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AddTheatreRequest {
    @NotNull(message = "cityId is required")
    private Long cityId;
    @NotBlank(message = "name is required")
    private String name;
    @NotBlank(message = "address is required")
    private String address;

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
