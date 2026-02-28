package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.AuthResponse;
import com.example.MovieTicketBookingSystemBackend.dto.SignupResponse;
import com.example.MovieTicketBookingSystemBackend.model.User;
import com.example.MovieTicketBookingSystemBackend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public SignupResponse signup(String username, String password, String email, String name) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        String trimmedEmail = email.trim();
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username already taken");
        }
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email already registered");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(trimmedEmail);
        user.setName(name.trim());
        user.setRole(User.ROLE_USER);
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);
        return new SignupResponse(user.getUserId());
    }

    public AuthResponse login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
        }
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        String token = jwtService.generate(user.getUserId(), user.getUsername());
        return new AuthResponse(token, user.getUserId(), user.getRole());
    }
}
