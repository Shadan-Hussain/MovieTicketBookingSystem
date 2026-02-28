package com.example.MovieTicketBookingSystemBackend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "show_seat", uniqueConstraints = @UniqueConstraint(columnNames = { "show_id", "seat_id" }))
public class ShowSeat {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_LOCKED = "LOCKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_seat_id")
    private Long showSeatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "status", nullable = false)
    private String status = STATUS_AVAILABLE;

    @Column(name = "created_at")
    private Instant createdAt;

    public ShowSeat() {
    }

    public Long getShowSeatId() { return showSeatId; }
    public void setShowSeatId(Long showSeatId) { this.showSeatId = showSeatId; }
    public Show getShow() { return show; }
    public void setShow(Show show) { this.show = show; }
    public Long getShowId() { return show != null ? show.getShowId() : null; }
    public Seat getSeat() { return seat; }
    public void setSeat(Seat seat) { this.seat = seat; }
    public Long getSeatId() { return seat != null ? seat.getSeatId() : null; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
