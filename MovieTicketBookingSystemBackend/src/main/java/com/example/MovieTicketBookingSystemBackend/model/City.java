package com.example.MovieTicketBookingSystemBackend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "city")
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "created_at")
    private Instant createdAt;

    public City() {
    }

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
