package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.MessageResponse;
import com.example.MovieTicketBookingSystemBackend.dto.ShowResponse;
import com.example.MovieTicketBookingSystemBackend.dto.ShowSeatResponse;
import com.example.MovieTicketBookingSystemBackend.dto.StripeSessionResponse;
import com.example.MovieTicketBookingSystemBackend.model.Seat;
import com.example.MovieTicketBookingSystemBackend.model.Show;
import com.example.MovieTicketBookingSystemBackend.model.ShowSeat;
import com.example.MovieTicketBookingSystemBackend.repository.SeatRepository;
import com.example.MovieTicketBookingSystemBackend.repository.ShowRepository;
import com.example.MovieTicketBookingSystemBackend.repository.ShowSeatRepository;
import com.stripe.exception.StripeException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatRepository seatRepository;
    private final SeatLockService seatLockService;
    private final StripeService stripeService;

    public ShowService(ShowRepository showRepository, ShowSeatRepository showSeatRepository,
                       SeatRepository seatRepository, SeatLockService seatLockService,
                       StripeService stripeService) {
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.seatRepository = seatRepository;
        this.seatLockService = seatLockService;
        this.stripeService = stripeService;
    }

    public List<ShowResponse> getShowsByCityAndMovie(Long cityId, Long movieId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Show> shows = showRepository.findByCityIdAndMovieId(cityId, movieId);
        return shows.stream()
                .filter(s -> s.getStartTime() != null && s.getStartTime().isAfter(now))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns show details by id (for seat map page). Validates show is in the future. Includes movie name.
     */
    public ShowResponse getShowById(Long showId) {
        validateShowStartInFuture(showId);
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));
        ShowResponse response = toResponse(show);
        if (show.getMovie() != null) {
            response.setMovieName(show.getMovie().getName());
        }
        return response;
    }

    /**
     * Throws if the show is not found or its start time is not after the current time.
     */
    public void validateShowStartInFuture(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));
        if (show.getStartTime() == null || !show.getStartTime().isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Show ended or already started");
        }
    }

    public List<ShowSeatResponse> getSeatsForShow(Long showId) {
        validateShowStartInFuture(showId);
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

    /**
     * Locks the seat for the user. Validates show is in future and seat is available.
     * @return message for response body
     * @throws ResponseStatusException NOT_FOUND, CONFLICT
     */
    public MessageResponse lockSeat(Long showId, Long seatId, Long userId) {
        validateShowStartInFuture(showId);
        ShowSeat showSeat = showSeatRepository.findByShow_ShowIdAndSeat_SeatId(showId, seatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ShowSeat not found"));
        if (!ShowSeat.STATUS_AVAILABLE.equals(showSeat.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is not available");
        }
        if (!seatLockService.setLockIfAbsent(showId, seatId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat not available at the moment");
        }
        return new MessageResponse("Seat successfully locked");
    }

    /**
     * Creates a Stripe checkout session for the seat. Caller must hold the lock for this user.
     * @throws ResponseStatusException NOT_FOUND, FORBIDDEN, CONFLICT, INTERNAL_SERVER_ERROR
     */
    public StripeSessionResponse createPaymentSession(Long showId, Long seatId, Long userId) {
        validateShowStartInFuture(showId);
        if (!seatLockService.isLockedBy(showId, seatId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lock expired or held by another user; please lock the seat again");
        }
        ShowSeat showSeat = showSeatRepository.findByShow_ShowIdAndSeat_SeatId(showId, seatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ShowSeat not found"));
        if (!ShowSeat.STATUS_AVAILABLE.equals(showSeat.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is not available");
        }
        try {
            return stripeService.createCheckoutSession(showId, seatId, userId);
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment session creation failed", e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    private ShowResponse toResponse(Show s) {
        String theatreName = s.getHall() != null && s.getHall().getTheatre() != null ? s.getHall().getTheatre().getName() : null;
        String hallName = s.getHall() != null ? s.getHall().getName() : null;
        return new ShowResponse(s.getShowId(), s.getMovieId(), s.getHallId(), theatreName, hallName,
                s.getStartTime(), s.getEndTime());
    }
}
