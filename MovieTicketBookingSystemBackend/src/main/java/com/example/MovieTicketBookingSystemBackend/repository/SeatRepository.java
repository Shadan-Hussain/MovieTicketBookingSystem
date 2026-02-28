package com.example.MovieTicketBookingSystemBackend.repository;

import com.example.MovieTicketBookingSystemBackend.model.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByHallId(Long hallId);

    /** Loads seat with pessimistic write lock (SELECT ... FOR UPDATE). Use in booking/update flows. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.seatId = :id")
    Optional<Seat> findByIdWithLock(@Param("id") Long id);
}
