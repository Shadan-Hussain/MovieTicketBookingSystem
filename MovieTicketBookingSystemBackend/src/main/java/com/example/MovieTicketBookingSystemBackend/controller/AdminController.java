package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.admin.*;
import com.example.MovieTicketBookingSystemBackend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

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

    @GetMapping("/options/theatres")
    public ResponseEntity<List<OptionDto>> listTheatreOptions() {
        return ResponseEntity.ok(adminService.listTheatreOptions());
    }

    @GetMapping("/options/halls")
    public ResponseEntity<List<OptionDto>> listHallOptions() {
        return ResponseEntity.ok(adminService.listHallOptions());
    }

    @GetMapping("/options/movies")
    public ResponseEntity<List<OptionDto>> listMovieOptions() {
        return ResponseEntity.ok(adminService.listMovieOptions());
    }

    @PostMapping("/cities")
    public ResponseEntity<CreatedResponse> addCity(@Valid @RequestBody AddCityRequest request) {
        CreatedResponse created = adminService.addCity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/theatres")
    public ResponseEntity<CreatedResponse> addTheatre(@Valid @RequestBody AddTheatreRequest request) {
        CreatedResponse created = adminService.addTheatre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/halls")
    public ResponseEntity<CreatedResponse> addHall(@Valid @RequestBody AddHallRequest request) {
        CreatedResponse created = adminService.addHall(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/movies")
    public ResponseEntity<CreatedResponse> addMovie(@Valid @RequestBody AddMovieRequest request) {
        CreatedResponse created = adminService.addMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private static final long MAX_POSTER_SIZE_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final Set<String> ALLOWED_POSTER_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @PostMapping("/movies/{movieId}/poster")
    public ResponseEntity<Void> uploadMoviePoster(
            @PathVariable Long movieId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poster file is required");
        }
        if (file.getSize() > MAX_POSTER_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Poster size must not exceed 2 MB");
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            contentType = contentType.split(";")[0].trim();
        }
        if (contentType == null || !ALLOWED_POSTER_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Poster must be image/jpeg, image/png, or image/webp");
        }
        try {
            adminService.setMoviePoster(movieId, file.getBytes(), contentType);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to store poster: " + e.getMessage());
        }
    }

    /**
     * Add a grid of seats to a hall. Body: rows, cols, premiumRowStart, premiumRowEnd (0-based inclusive),
     * pricePremium, priceNormal. Capacity is updated from seat count.
     */
    @PostMapping("/halls/{hallId}/seats")
    public ResponseEntity<AddSeatsResponse> addSeats(
            @PathVariable Long hallId,
            @Valid @RequestBody AddSeatsRequest request) {
        if (request.getRows() <= 0 || request.getCols() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rows and cols must be positive");
        }
        AddSeatsResponse created = adminService.addSeats(hallId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/shows")
    public ResponseEntity<CreatedResponse> addShow(@Valid @RequestBody AddShowRequest request) {
        CreatedResponse created = adminService.addShow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
