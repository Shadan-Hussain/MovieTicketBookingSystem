package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.MovieResponse;
import com.example.MovieTicketBookingSystemBackend.service.MovieService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getMovies(@RequestParam Long city_id) {
        return ResponseEntity.ok(movieService.getMoviesByCity(city_id));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponse> getMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(movieService.getMovieById(movieId));
    }

    /** Serves the poster image stored in DB (BYTEA). No auth so <img src> works. */
    @GetMapping("/{movieId}/poster")
    public ResponseEntity<byte[]> getPoster(@PathVariable Long movieId) {
        return movieService.getPoster(movieId)
                .map(p -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(p.contentType()))
                        .body(p.bytes()))
                .orElse(ResponseEntity.notFound().build());
    }
}
