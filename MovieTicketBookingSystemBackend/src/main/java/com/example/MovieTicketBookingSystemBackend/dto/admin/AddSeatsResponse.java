package com.example.MovieTicketBookingSystemBackend.dto.admin;

public class AddSeatsResponse {
    private final int count;
    private final int capacity;

    public AddSeatsResponse(int count, int capacity) {
        this.count = count;
        this.capacity = capacity;
    }

    public int getCount() { return count; }
    public int getCapacity() { return capacity; }
}
