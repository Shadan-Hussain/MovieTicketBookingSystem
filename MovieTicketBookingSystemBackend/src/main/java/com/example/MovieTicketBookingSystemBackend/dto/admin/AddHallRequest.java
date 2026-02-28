package com.example.MovieTicketBookingSystemBackend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Hall capacity is derived from the number of seats added to the hall. */
public class AddHallRequest {
    @NotNull(message = "theatreId is required")
    private Long theatreId;
    @NotBlank(message = "name is required")
    private String name;

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
