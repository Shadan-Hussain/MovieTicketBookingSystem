package com.example.MovieTicketBookingSystemBackend.repository;

import com.example.MovieTicketBookingSystemBackend.model.Hall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HallRepository extends JpaRepository<Hall, Long> {

    List<Hall> findByTheatre_TheatreId(Long theatreId);

    boolean existsByTheatre_TheatreIdAndName(Long theatreId, String name);
}
