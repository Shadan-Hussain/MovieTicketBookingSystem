package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.TicketResponse;
import com.example.MovieTicketBookingSystemBackend.model.Hall;
import com.example.MovieTicketBookingSystemBackend.model.Seat;
import com.example.MovieTicketBookingSystemBackend.model.Show;
import com.example.MovieTicketBookingSystemBackend.model.Theatre;
import com.example.MovieTicketBookingSystemBackend.model.Ticket;
import com.example.MovieTicketBookingSystemBackend.model.Transaction;
import com.example.MovieTicketBookingSystemBackend.repository.TicketRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final TransactionRepository transactionRepository;
    private final TicketRepository ticketRepository;

    public TicketService(TransactionRepository transactionRepository, TicketRepository ticketRepository) {
        this.transactionRepository = transactionRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTicketsForUser(Long userId) {
        if (userId == null) return List.of();
        return ticketRepository.findByTransaction_User_UserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Find ticket by show and seat (successful transaction). Returns ticket only if it belongs to the given user.
     * If the user has a transaction in FAILED or REFUND_INITIATED state for this show+seat, throws with appropriate message. */
    @Transactional(readOnly = true)
    public Optional<TicketResponse> getTicket(Long showId, Long seatId, Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        transactionRepository.findFirstByShow_ShowIdAndSeat_SeatIdOrderByCreatedAtDesc(showId, seatId)
                .filter(txn -> txn.getUserId() != null && txn.getUserId().equals(userId))
                .ifPresent(txn -> {
                    if (Transaction.STATUS_FAILED.equals(txn.getStatus())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payment failed");
                    }
                    if (Transaction.STATUS_REFUND_INITIATED.equals(txn.getStatus())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket creation failed, refund initiated");
                    }
                });
        return transactionRepository.findByShow_ShowIdAndSeat_SeatIdAndStatus(showId, seatId, Transaction.STATUS_SUCCESS)
                .filter(txn -> txn.getUserId() != null && txn.getUserId().equals(userId))
                .flatMap(txn -> ticketRepository.findByTransaction_TransactionId(txn.getTransactionId())
                        .map(this::toResponse));
    }

    private TicketResponse toResponse(Ticket t) {
        var txn = t.getTransaction();
        Long showId = txn != null ? txn.getShowId() : null;
        Long seatId = txn != null ? txn.getSeatId() : null;
        Show show = txn != null ? txn.getShow() : null;
        Seat seat = txn != null ? txn.getSeat() : null;
        String seatNumber = seat != null ? seat.getNumber() : null;
        Hall hall = show != null ? show.getHall() : null;
        Theatre theatre = hall != null ? hall.getTheatre() : null;
        String movieName = show != null && show.getMovie() != null ? show.getMovie().getName() : null;
        String theatreName = theatre != null ? theatre.getName() : null;
        String theatreAddress = theatre != null ? theatre.getAddress() : null;
        String hallName = hall != null ? hall.getName() : null;
        String showStartTime = show != null && show.getStartTime() != null ? show.getStartTime().toString() : null;
        String showEndTime = show != null && show.getEndTime() != null ? show.getEndTime().toString() : null;
        return new TicketResponse(t.getTicketId(), showId, seatId, seatNumber, t.getTransactionId(),
                movieName, theatreName, theatreAddress, hallName, showStartTime, showEndTime, t.getCreatedAt());
    }
}
