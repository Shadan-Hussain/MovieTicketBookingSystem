package com.example.MovieTicketBookingSystemBackend.repository;

import com.example.MovieTicketBookingSystemBackend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByStripeSessionId(String stripeSessionId);

    Optional<Transaction> findByShowIdAndSeatIdAndStatus(Long showId, Long seatId, String status);

    /** True if there is a transaction for this show+seat with status in the given list (e.g. PENDING, SUCCESS). */
    boolean existsByShowIdAndSeatIdAndStatusIn(Long showId, Long seatId, List<String> statuses);
}
