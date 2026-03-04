package com.example.MovieTicketBookingSystemBackend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "duration_mins", nullable = false)
    private Integer durationMins;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "poster_image", columnDefinition = "BYTEA")
    private byte[] posterImage;

    @Column(name = "poster_content_type", length = 128)
    private String posterContentType;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "created_at")
    private Instant createdAt;

    public Movie() {
    }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDurationMins() { return durationMins; }
    public void setDurationMins(Integer durationMins) { this.durationMins = durationMins; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public byte[] getPosterImage() { return posterImage; }
    public void setPosterImage(byte[] posterImage) { this.posterImage = posterImage; }
    public String getPosterContentType() { return posterContentType; }
    public void setPosterContentType(String posterContentType) { this.posterContentType = posterContentType; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
