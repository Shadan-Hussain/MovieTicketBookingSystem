package com.example.MovieTicketBookingSystemBackend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "seat", uniqueConstraints = @UniqueConstraint(columnNames = { "hall_id", "number" }))
public class Seat {

    public static final String TYPE_NORMAL = "NORMAL";
    public static final String TYPE_PREMIUM = "PREMIUM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "hall_id", nullable = false)
    private Long hallId;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "price", nullable = false, columnDefinition = "bigint default 100")
    private Long price = 100L;

    @Column(name = "type", nullable = false)
    private String type = TYPE_NORMAL;

    @Column(name = "created_at")
    private Instant createdAt;

    public Seat() {
    }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }
    public Long getHallId() { return hallId; }
    public void setHallId(Long hallId) { this.hallId = hallId; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
