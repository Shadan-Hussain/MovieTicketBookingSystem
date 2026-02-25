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

    /** Find ticket by show and seat (successful transaction). Returns empty if not found. */
    public Optional<TicketResponse> getTicket(Long showId, Long seatId) {
        return transactionRepository.findByShowIdAndSeatIdAndStatus(showId, seatId, Transaction.STATUS_SUCCESS)
                .flatMap(txn -> ticketRepository.findByTransactionId(txn.getTransactionId())
                        .map(this::toResponse));
    }

    private TicketResponse toResponse(Ticket t) {
        var txn = transactionRepository.findById(t.getTransactionId()).orElse(null);
        Long showId = txn != null ? txn.getShowId() : null;
        Long seatId = txn != null ? txn.getSeatId() : null;
        return new TicketResponse(t.getTicketId(), showId, seatId, t.getTransactionId(), t.getCreatedAt());
    }
}
