package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.TicketResponse;
import com.example.MovieTicketBookingSystemBackend.model.Ticket;
import com.example.MovieTicketBookingSystemBackend.model.Transaction;
import com.example.MovieTicketBookingSystemBackend.repository.TicketRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TicketService {

    private final TransactionRepository transactionRepository;
    private final TicketRepository ticketRepository;

    public TicketService(TransactionRepository transactionRepository, TicketRepository ticketRepository) {
        this.transactionRepository = transactionRepository;
        this.ticketRepository = ticketRepository;
    }

    /** Find ticket by show and seat (successful transaction). Returns ticket only if it belongs to the given user. */
    public Optional<TicketResponse> getTicket(Long showId, Long seatId, Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return transactionRepository.findByShow_ShowIdAndSeat_SeatIdAndStatus(showId, seatId, Transaction.STATUS_SUCCESS)
                .filter(txn -> txn.getUserId() != null && txn.getUserId().equals(userId))
                .flatMap(txn -> ticketRepository.findByTransaction_TransactionId(txn.getTransactionId())
                        .map(this::toResponse));
    }

    private TicketResponse toResponse(Ticket t) {
        var txn = transactionRepository.findById(t.getTransactionId()).orElse(null);
        Long showId = txn != null ? txn.getShowId() : null;
        Long seatId = txn != null ? txn.getSeatId() : null;
        return new TicketResponse(t.getTicketId(), showId, seatId, t.getTransactionId(), t.getCreatedAt());
    }
}
