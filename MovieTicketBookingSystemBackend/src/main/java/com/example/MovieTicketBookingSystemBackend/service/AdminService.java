package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.admin.*;
import com.example.MovieTicketBookingSystemBackend.model.*;
import com.example.MovieTicketBookingSystemBackend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Admin-only operations. JWT auth can be added later to restrict access.
 */
@Service
public class AdminService {

    private final CityRepository cityRepository;
    private final TheatreRepository theatreRepository;
    private final HallRepository hallRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

    public AdminService(CityRepository cityRepository,
                        TheatreRepository theatreRepository,
                        HallRepository hallRepository,
                        MovieRepository movieRepository,
                        SeatRepository seatRepository,
                        ShowRepository showRepository,
                        ShowSeatRepository showSeatRepository) {
        this.cityRepository = cityRepository;
        this.theatreRepository = theatreRepository;
        this.hallRepository = hallRepository;
        this.movieRepository = movieRepository;
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
    }

    @Transactional
    public CreatedResponse addCity(AddCityRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (req.getStateCode() == null || req.getStateCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State code is required");
        }
        if (cityRepository.existsByName(req.getName().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "City name already exists");
        }
        City city = new City();
        city.setName(req.getName());
        city.setStateCode(req.getStateCode());
        city.setCreatedAt(Instant.now());
        return new CreatedResponse(cityRepository.save(city).getCityId());
    }

    @Transactional
    public CreatedResponse addTheatre(AddTheatreRequest req) {
        City city = cityRepository.findById(req.getCityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "City not found"));
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (req.getAddress() == null || req.getAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address is required");
        }
        if (theatreRepository.existsByName(req.getName().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Theatre name already exists");
        }
        Theatre theatre = new Theatre();
        theatre.setCity(city);
        theatre.setName(req.getName());
        theatre.setAddress(req.getAddress());
        theatre.setCreatedAt(Instant.now());
        return new CreatedResponse(theatreRepository.save(theatre).getTheatreId());
    }

    @Transactional
    public CreatedResponse addHall(AddHallRequest req) {
        Theatre theatre = theatreRepository.findById(req.getTheatreId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Theatre not found"));
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        Hall hall = new Hall();
        hall.setTheatre(theatre);
        hall.setName(req.getName());
        hall.setCapacity(null);
        hall.setCreatedAt(Instant.now());
        return new CreatedResponse(hallRepository.save(hall).getHallId());
    }

    @Transactional
    public CreatedResponse addMovie(AddMovieRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (req.getDurationMins() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration is required");
        }
        if (req.getDescription() == null || req.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required");
        }
        if (req.getLanguage() == null || req.getLanguage().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Language is required");
        }
        if (movieRepository.existsByName(req.getName().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movie name already exists");
        }
        Movie movie = new Movie();
        movie.setName(req.getName());
        movie.setDurationMins(req.getDurationMins());
        movie.setDescription(req.getDescription());
        movie.setPosterUrl(req.getPosterUrl());
        movie.setLanguage(req.getLanguage());
        movie.setCreatedAt(Instant.now());
        return new CreatedResponse(movieRepository.save(movie).getMovieId());
    }

    /**
     * Create a grid of seats for the hall. Rows premiumRowStart..premiumRowEnd (inclusive, 0-based) are PREMIUM.
     * Seat number: row 0-25 -> A1, A2, ... B1, ...; row 26+ -> R27C1, etc.
     * Updates hall capacity to the new total seat count.
     */
    @Transactional
    public AddSeatsResponse addSeats(Long hallId, AddSeatsRequest req) {
        if (!hallRepository.existsById(hallId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall not found");
        }
        if (req.getRows() == null || req.getCols() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rows and cols are required");
        }
        int rows = req.getRows();
        int cols = req.getCols();
        if (rows <= 0 || cols <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rows and cols must be positive");
        }
        int premiumStart = req.getPremiumRowStart() != null ? req.getPremiumRowStart() : -1;
        int premiumEnd = req.getPremiumRowEnd() != null ? req.getPremiumRowEnd() : -1;
        long pricePremium = req.getPricePremium() != null ? req.getPricePremium() : 200L;
        long priceNormal = req.getPriceNormal() != null ? req.getPriceNormal() : 100L;

        Instant now = Instant.now();
        int count = 0;
        Hall hallForSeats = hallRepository.findById(hallId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall not found"));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Seat seat = new Seat();
                seat.setHall(hallForSeats);
                seat.setRowNum(r);
                seat.setColNum(c);
                seat.setNumber(seatNumberFromRowCol(r, c));
                boolean isPremium = (r >= premiumStart && r <= premiumEnd);
                seat.setType(isPremium ? Seat.TYPE_PREMIUM : Seat.TYPE_NORMAL);
                seat.setPrice(isPremium ? pricePremium : priceNormal);
                seat.setCreatedAt(now);
                seatRepository.save(seat);
                count++;
            }
        }

        long existing = seatRepository.findByHall_HallId(hallId).size();
        hallForSeats.setCapacity((int) existing);
        hallRepository.save(hallForSeats);

        return new AddSeatsResponse("Seats added");
    }

    private static String seatNumberFromRowCol(int row, int col) {
        if (row < 26) {
            return String.valueOf((char) ('A' + row)) + (col + 1);
        }
        return "R" + (row + 1) + "C" + (col + 1);
    }

    @Transactional
    public CreatedResponse addShow(AddShowRequest req) {
        Hall hall = hallRepository.findById(req.getHallId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall not found"));
        Movie movie = movieRepository.findById(req.getMovieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movie not found"));
        if (req.getStartTime() == null || req.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time and end time are required");
        }
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        if (showRepository.countOverlappingInHall(hall.getHallId(), req.getStartTime(), req.getEndTime()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Show time overlaps with another show in this hall");
        }
        Show show = new Show();
        show.setHall(hall);
        show.setMovie(movie);
        show.setStartTime(req.getStartTime());
        show.setEndTime(req.getEndTime());
        show.setCreatedAt(Instant.now());
        show = showRepository.save(show);

        List<Seat> seats = seatRepository.findByHall_HallId(hall.getHallId());
        Instant now = Instant.now();
        for (Seat seat : seats) {
            ShowSeat showSeat = new ShowSeat();
            showSeat.setShow(show);
            showSeat.setSeat(seat);
            showSeat.setStatus(ShowSeat.STATUS_AVAILABLE);
            showSeat.setCreatedAt(now);
            showSeatRepository.save(showSeat);
        }

        return new CreatedResponse(show.getShowId());
    }
}
