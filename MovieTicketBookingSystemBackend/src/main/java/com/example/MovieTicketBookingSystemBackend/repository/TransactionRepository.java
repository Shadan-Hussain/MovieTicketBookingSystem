package com.example.MovieTicketBookingSystemBackend.repository;

import com.example.MovieTicketBookingSystemBackend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    Optional<Transaction> findByStripeSessionId(String stripeSessionId);

    Optional<Transaction> findByShow_ShowIdAndSeat_SeatIdAndStatus(Long showId, Long seatId, String status);

    /**
     * Latest transaction for a show+seat ordered by creation time.
     */
    Optional<Transaction> findFirstByShow_ShowIdAndSeat_SeatIdOrderByCreatedAtDesc(Long showId, Long seatId);
}
