package com.example.MovieTicketBookingSystemBackend.dto.admin;

/** Hall capacity is derived from the number of seats added to the hall. */
public class AddHallRequest {
    private Long theatreId;
    private String name;

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
