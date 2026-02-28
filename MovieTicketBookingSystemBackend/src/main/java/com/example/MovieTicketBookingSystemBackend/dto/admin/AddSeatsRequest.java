package com.example.MovieTicketBookingSystemBackend.dto.admin;

/**
 * Create a grid of seats for a hall.
 * Rows and cols define the grid size. Row indices are 0-based.
 * Rows from premiumRowStart to premiumRowEnd (inclusive) are PREMIUM; the rest are NORMAL.
 */
public class AddSeatsRequest {
    private Integer rows;
    private Integer cols;
    /** 0-based inclusive start row for premium seats (e.g. 0 = first row). */
    private Integer premiumRowStart;
    /** 0-based inclusive end row for premium seats (e.g. 1 = first two rows premium). */
    private Integer premiumRowEnd;
    private Long pricePremium;
    private Long priceNormal;

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }
    public Integer getCols() { return cols; }
    public void setCols(Integer cols) { this.cols = cols; }
    public Integer getPremiumRowStart() { return premiumRowStart; }
    public void setPremiumRowStart(Integer premiumRowStart) { this.premiumRowStart = premiumRowStart; }
    public Integer getPremiumRowEnd() { return premiumRowEnd; }
    public void setPremiumRowEnd(Integer premiumRowEnd) { this.premiumRowEnd = premiumRowEnd; }
    public Long getPricePremium() { return pricePremium; }
    public void setPricePremium(Long pricePremium) { this.pricePremium = pricePremium; }
    public Long getPriceNormal() { return priceNormal; }
    public void setPriceNormal(Long priceNormal) { this.priceNormal = priceNormal; }
}
