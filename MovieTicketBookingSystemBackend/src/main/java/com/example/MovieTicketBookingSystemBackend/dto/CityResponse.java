package com.example.MovieTicketBookingSystemBackend.dto;

public class CityResponse {

    private Long cityId;
    private String name;

    public CityResponse(Long cityId, String name) {
        this.cityId = cityId;
        this.name = name;
    }

    public Long getCityId() { return cityId; }
    public String getName() { return name; }
}
