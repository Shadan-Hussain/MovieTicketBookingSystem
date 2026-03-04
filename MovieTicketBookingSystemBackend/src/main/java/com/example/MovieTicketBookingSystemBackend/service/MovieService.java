package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.MovieResponse;
import com.example.MovieTicketBookingSystemBackend.model.Hall;
import com.example.MovieTicketBookingSystemBackend.model.Movie;
import com.example.MovieTicketBookingSystemBackend.model.Theatre;
import com.example.MovieTicketBookingSystemBackend.repository.HallRepository;
import com.example.MovieTicketBookingSystemBackend.repository.MovieRepository;
import com.example.MovieTicketBookingSystemBackend.repository.ShowRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TheatreRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class MovieService {

    private final TheatreRepository theatreRepository;
    private final HallRepository hallRepository;
    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;

    public MovieService(TheatreRepository theatreRepository, HallRepository hallRepository,
                        ShowRepository showRepository, MovieRepository movieRepository) {
        this.theatreRepository = theatreRepository;
        this.hallRepository = hallRepository;
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
    }

    /** Movies that have at least one show in the given city. */
    public List<MovieResponse> getMoviesByCity(Long cityId) {
        List<Long> theatreIds = theatreRepository.findByCity_CityId(cityId).stream()
                .map(Theatre::getTheatreId)
                .toList();
        if (theatreIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> hallIds = theatreIds.stream()
                .flatMap(tid -> hallRepository.findByTheatre_TheatreId(tid).stream())
                .map(Hall::getHallId)
                .collect(Collectors.toList());
        if (hallIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> movieIds = showRepository.findDistinctMovieIdsByHallIdIn(hallIds);
        if (movieIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Movie> movies = movieRepository.findAllById(movieIds);
        return movies.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Returns a single movie by id, or 404 if not found. */
    public MovieResponse getMovieById(Long movieId) {
        Movie m = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));
        return toResponse(m);
    }

    /** Returns poster image bytes and content type if the movie has a stored poster. */
    public Optional<PosterData> getPoster(Long movieId) {
        return movieRepository.findById(movieId)
                .filter(m -> m.getPosterImage() != null && m.getPosterImage().length > 0)
                .map(m -> new PosterData(m.getPosterImage(),
                        m.getPosterContentType() != null ? m.getPosterContentType() : "image/jpeg"));
    }

    public record PosterData(byte[] bytes, String contentType) {}

    private MovieResponse toResponse(Movie m) {
        boolean hasPoster = m.getPosterImage() != null && m.getPosterImage().length > 0;
        return new MovieResponse(m.getMovieId(), m.getName(), m.getDurationMins(), m.getDescription(),
                hasPoster, m.getLanguage());
    }
}
