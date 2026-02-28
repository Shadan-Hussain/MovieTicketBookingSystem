package com.example.MovieTicketBookingSystemBackend.dto.admin;

public class AddCityRequest {
    private String name;
    private String stateCode;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
}
