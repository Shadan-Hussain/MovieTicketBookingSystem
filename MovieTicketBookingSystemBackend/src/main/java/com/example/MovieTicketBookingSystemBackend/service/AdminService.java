package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.admin.*;
import com.example.MovieTicketBookingSystemBackend.model.*;
import com.example.MovieTicketBookingSystemBackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        City city = new City();
        city.setName(req.getName());
        city.setStateCode(req.getStateCode());
        city.setCreatedAt(Instant.now());
        return new CreatedResponse(cityRepository.save(city).getCityId());
    }

    @Transactional
    public CreatedResponse addTheatre(AddTheatreRequest req) {
        Theatre theatre = new Theatre();
        theatre.setCityId(req.getCityId());
        theatre.setName(req.getName());
        theatre.setAddress(req.getAddress());
        theatre.setCreatedAt(Instant.now());
        return new CreatedResponse(theatreRepository.save(theatre).getTheatreId());
    }

    @Transactional
    public CreatedResponse addHall(AddHallRequest req) {
        Hall hall = new Hall();
        hall.setTheatreId(req.getTheatreId());
        hall.setName(req.getName());
        hall.setCapacity(null);
        hall.setCreatedAt(Instant.now());
        return new CreatedResponse(hallRepository.save(hall).getHallId());
    }

    @Transactional
    public CreatedResponse addMovie(AddMovieRequest req) {
        Movie movie = new Movie();
        movie.setName(req.getName());
        movie.setDurationMins(req.getDurationMins());
        movie.setDescription(req.getDescription());
        movie.setPosterUrl(req.getPosterUrl());
        movie.setLanguage(req.getLanguage());
        movie.setReleaseDate(req.getReleaseDate());
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
        int rows = req.getRows() != null ? req.getRows() : 0;
        int cols = req.getCols() != null ? req.getCols() : 0;
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("rows and cols must be positive");
        }
        int premiumStart = req.getPremiumRowStart() != null ? req.getPremiumRowStart() : -1;
        int premiumEnd = req.getPremiumRowEnd() != null ? req.getPremiumRowEnd() : -1;
        long pricePremium = req.getPricePremium() != null ? req.getPricePremium() : 200L;
        long priceNormal = req.getPriceNormal() != null ? req.getPriceNormal() : 100L;

        Instant now = Instant.now();
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Seat seat = new Seat();
                seat.setHallId(hallId);
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

        Hall hall = hallRepository.findById(hallId).orElseThrow();
        long existing = seatRepository.findByHallId(hallId).size();
        hall.setCapacity((int) existing);
        hallRepository.save(hall);

        return new AddSeatsResponse(count, (int) existing);
    }

    private static String seatNumberFromRowCol(int row, int col) {
        if (row < 26) {
            return String.valueOf((char) ('A' + row)) + (col + 1);
        }
        return "R" + (row + 1) + "C" + (col + 1);
    }

    @Transactional
    public CreatedResponse addShow(AddShowRequest req) {
        Show show = new Show();
        show.setMovieId(req.getMovieId());
        show.setHallId(req.getHallId());
        show.setStartTime(req.getStartTime());
        show.setEndTime(req.getEndTime());
        show.setCreatedAt(Instant.now());
        show = showRepository.save(show);

        List<Seat> seats = seatRepository.findByHallId(req.getHallId());
        Instant now = Instant.now();
        for (Seat seat : seats) {
            ShowSeat showSeat = new ShowSeat();
            showSeat.setShowId(show.getShowId());
            showSeat.setSeatId(seat.getSeatId());
            showSeat.setStatus(ShowSeat.STATUS_AVAILABLE);
            showSeat.setCreatedAt(now);
            showSeatRepository.save(showSeat);
        }

        return new CreatedResponse(show.getShowId());
    }
}
