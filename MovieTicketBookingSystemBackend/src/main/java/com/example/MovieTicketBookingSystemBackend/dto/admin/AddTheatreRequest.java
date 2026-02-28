package com.example.MovieTicketBookingSystemBackend.dto.admin;

public class AddTheatreRequest {
    private Long cityId;
    private String name;
    private String address;

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
