package com.example.MovieTicketBookingSystemBackend.dto;

public class ShowSeatResponse {

    private Long seatId;
    private Integer rowNum;
    private Integer colNum;
    private String number;
    private Long price;
    private String type;
    private String status; // AVAILABLE | BOOKED

    public ShowSeatResponse(Long seatId, Integer rowNum, Integer colNum, String number, Long price, String type, String status) {
        this.seatId = seatId;
        this.rowNum = rowNum;
        this.colNum = colNum;
        this.number = number;
        this.price = price;
        this.type = type;
        this.status = status;
    }

    public Long getSeatId() { return seatId; }
    public Integer getRowNum() { return rowNum; }
    public Integer getColNum() { return colNum; }
    public String getNumber() { return number; }
    public Long getPrice() { return price; }
    public String getType() { return type; }
    public String getStatus() { return status; }
}
