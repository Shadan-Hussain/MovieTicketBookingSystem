package com.example.MovieTicketBookingSystemBackend.repository;

import com.example.MovieTicketBookingSystemBackend.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTransaction_TransactionId(Long transactionId);

    List<Ticket> findByTransaction_User_UserIdOrderByCreatedAtDesc(Long userId);
}
