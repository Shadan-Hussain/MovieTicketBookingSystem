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
import java.util.stream.Collectors;

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

    private MovieResponse toResponse(Movie m) {
        return new MovieResponse(m.getMovieId(), m.getName(), m.getDurationMins(), m.getDescription(),
                m.getPosterUrl(), m.getLanguage());
    }
}
