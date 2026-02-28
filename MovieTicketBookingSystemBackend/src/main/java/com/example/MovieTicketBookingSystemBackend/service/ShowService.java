package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.ShowResponse;
import com.example.MovieTicketBookingSystemBackend.dto.ShowSeatResponse;
import com.example.MovieTicketBookingSystemBackend.model.Seat;
import com.example.MovieTicketBookingSystemBackend.model.Show;
import com.example.MovieTicketBookingSystemBackend.model.ShowSeat;
import com.example.MovieTicketBookingSystemBackend.repository.SeatRepository;
import com.example.MovieTicketBookingSystemBackend.repository.ShowRepository;
import com.example.MovieTicketBookingSystemBackend.repository.ShowSeatRepository;
import com.example.MovieTicketBookingSystemBackend.service.SeatLockService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatRepository seatRepository;
    private final SeatLockService seatLockService;

    public ShowService(ShowRepository showRepository, ShowSeatRepository showSeatRepository,
                       SeatRepository seatRepository, SeatLockService seatLockService) {
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.seatRepository = seatRepository;
        this.seatLockService = seatLockService;
    }

    public List<ShowResponse> getShowsByCityAndMovie(Long cityId, Long movieId) {
        List<Show> shows = showRepository.findByCityIdAndMovieId(cityId, movieId);
        return shows.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ShowSeatResponse> getSeatsForShow(Long showId) {
        List<ShowSeat> showSeats = showSeatRepository.findByShow_ShowIdOrderBySeat_SeatId(showId);
        if (showSeats.isEmpty()) {
            return List.of();
        }
        List<Long> seatIds = showSeats.stream().map(ShowSeat::getSeatId).distinct().collect(Collectors.toList());
        Map<Long, Seat> seatMap = seatRepository.findAllById(seatIds).stream().collect(Collectors.toMap(Seat::getSeatId, s -> s));
        return showSeats.stream()
                .map(ss -> {
                    Seat seat = seatMap.get(ss.getSeatId());
                    if (seat == null) return null;
                    String status = seatLockService.isLocked(showId, ss.getSeatId())
                            ? ShowSeat.STATUS_LOCKED
                            : ss.getStatus();
                    return new ShowSeatResponse(seat.getSeatId(), seat.getRowNum(), seat.getColNum(), seat.getNumber(), seat.getPrice(), seat.getType(), status);
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    private ShowResponse toResponse(Show s) {
        return new ShowResponse(s.getShowId(), s.getMovieId(), s.getHallId(), s.getStartTime(), s.getEndTime());
    }
}
