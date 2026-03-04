package com.example.MovieTicketBookingSystemBackend.dto;

public class AuthResponse {
    private String token;
    private Long userId;
    private String role;
    private String name;

    public AuthResponse(String token, Long userId, String role, String name) {
        this.token = token;
        this.userId = userId;
        this.role = role;
        this.name = name;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public String getName() { return name; }
}
