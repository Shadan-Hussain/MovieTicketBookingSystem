package com.example.MovieTicketBookingSystemBackend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Single request to add a hall with its seat grid. All fields are required.
 * premiumRowEnd is 1-based inclusive (0 = no premium rows; 2 = rows 1 and 2 premium).
 */
public class AddHallRequest {
    @NotNull(message = "theatreId is required")
    private Long theatreId;
    @NotBlank(message = "name is required")
    private String name;
    @NotNull(message = "rows is required")
    private Integer rows;
    @NotNull(message = "cols is required")
    private Integer cols;
    @NotNull(message = "premiumRowEnd is required")
    private Integer premiumRowEnd;
    @NotNull(message = "priceNormal is required")
    private Long priceNormal;
    @NotNull(message = "pricePremium is required")
    private Long pricePremium;

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }
    public Integer getCols() { return cols; }
    public void setCols(Integer cols) { this.cols = cols; }
    public Integer getPremiumRowEnd() { return premiumRowEnd; }
    public void setPremiumRowEnd(Integer premiumRowEnd) { this.premiumRowEnd = premiumRowEnd; }
    public Long getPriceNormal() { return priceNormal; }
    public void setPriceNormal(Long priceNormal) { this.priceNormal = priceNormal; }
    public Long getPricePremium() { return pricePremium; }
    public void setPricePremium(Long pricePremium) { this.pricePremium = pricePremium; }
}
