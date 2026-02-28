package com.example.MovieTicketBookingSystemBackend.repository;

import com.example.MovieTicketBookingSystemBackend.model.ShowSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShow_ShowIdOrderBySeat_SeatId(Long showId);

    Optional<ShowSeat> findByShow_ShowIdAndSeat_SeatId(Long showId, Long seatId);
}
