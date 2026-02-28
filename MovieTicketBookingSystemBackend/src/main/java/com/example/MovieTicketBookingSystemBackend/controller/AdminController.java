package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.admin.*;
import com.example.MovieTicketBookingSystemBackend.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin-only endpoints for adding cities, theatres, halls, movies, seats, and shows.
 * JWT auth can be added later to ensure only admin users can call these.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/cities")
    public ResponseEntity<CreatedResponse> addCity(@RequestBody AddCityRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        CreatedResponse created = adminService.addCity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/theatres")
    public ResponseEntity<CreatedResponse> addTheatre(@RequestBody AddTheatreRequest request) {
        if (request.getCityId() == null || request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cityId and name are required");
        }
        CreatedResponse created = adminService.addTheatre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/halls")
    public ResponseEntity<CreatedResponse> addHall(@RequestBody AddHallRequest request) {
        if (request.getTheatreId() == null || request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "theatreId and name are required");
        }
        CreatedResponse created = adminService.addHall(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/movies")
    public ResponseEntity<CreatedResponse> addMovie(@RequestBody AddMovieRequest request) {
        if (request.getName() == null || request.getName().isBlank() || request.getDurationMins() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name and durationMins are required");
        }
        CreatedResponse created = adminService.addMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Add a grid of seats to a hall. Body: rows, cols, premiumRowStart, premiumRowEnd (0-based inclusive),
     * pricePremium, priceNormal. Capacity is updated from seat count.
     */
    @PostMapping("/halls/{hallId}/seats")
    public ResponseEntity<AddSeatsResponse> addSeats(
            @PathVariable Long hallId,
            @RequestBody AddSeatsRequest request) {
        if (request == null || request.getRows() == null || request.getCols() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rows and cols are required");
        }
        if (request.getRows() <= 0 || request.getCols() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rows and cols must be positive");
        }
        AddSeatsResponse created = adminService.addSeats(hallId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/shows")
    public ResponseEntity<CreatedResponse> addShow(@RequestBody AddShowRequest request) {
        if (request.getMovieId() == null || request.getHallId() == null
                || request.getStartTime() == null || request.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "movieId, hallId, startTime and endTime are required");
        }
        CreatedResponse created = adminService.addShow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
