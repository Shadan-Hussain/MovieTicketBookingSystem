package com.example.MovieTicketBookingSystemBackend.dto;

public class CityResponse {

    private Long cityId;
    private String name;
    private String stateCode;

    public CityResponse(Long cityId, String name, String stateCode) {
        this.cityId = cityId;
        this.name = name;
        this.stateCode = stateCode;
    }

    public Long getCityId() { return cityId; }
    public String getName() { return name; }
    public String getStateCode() { return stateCode; }
}
