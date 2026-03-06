package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.admin.*;
import com.example.MovieTicketBookingSystemBackend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addCity(request));
    }

    @PostMapping("/theatres")
    public ResponseEntity<CreatedResponse> addTheatre(@Valid @RequestBody AddTheatreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addTheatre(request));
    }

    @PostMapping("/halls")
    public ResponseEntity<CreatedResponse> addHall(@Valid @RequestBody AddHallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addHall(request));
    }

    @PostMapping(value = "/movies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreatedResponse> addMovie(
            @RequestPart("movie") @Valid AddMovieRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addMovie(request, file));
    }

    @DeleteMapping("/halls/{hallId}")
    public ResponseEntity<Void> deleteHall(@PathVariable Long hallId) {
        adminService.deleteHall(hallId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/shows")
    public ResponseEntity<CreatedResponse> addShow(@Valid @RequestBody AddShowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addShow(request));
    }
}
