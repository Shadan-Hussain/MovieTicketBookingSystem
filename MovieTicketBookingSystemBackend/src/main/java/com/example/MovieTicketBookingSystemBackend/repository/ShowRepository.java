package com.example.MovieTicketBookingSystemBackend.repository;

import com.example.MovieTicketBookingSystemBackend.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query(value = "SELECT s.* FROM show s JOIN hall h ON s.hall_id = h.hall_id JOIN theatre t ON h.theatre_id = t.theatre_id WHERE t.city_id = :cityId AND s.movie_id = :movieId ORDER BY s.start_time", nativeQuery = true)
    List<Show> findByCityIdAndMovieId(@Param("cityId") Long cityId, @Param("movieId") Long movieId);

    @Query("SELECT DISTINCT s.movie.movieId FROM Show s WHERE s.hall.hallId IN :hallIds")
    List<Long> findDistinctMovieIdsByHallIdIn(@Param("hallIds") List<Long> hallIds);

    /**
     * Counts shows in this hall whose time range overlaps the given [startTime, endTime].
     * Two ranges overlap iff existing.start < new.end AND existing.end > new.start (catches all cases:
     * new inside existing, existing inside new, or partial overlap).
     */
    @Query("SELECT COUNT(s) FROM Show s WHERE s.hall.hallId = :hallId AND s.startTime < :endTime AND s.endTime > :startTime")
    long countOverlappingInHall(@Param("hallId") Long hallId, @Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);
}
