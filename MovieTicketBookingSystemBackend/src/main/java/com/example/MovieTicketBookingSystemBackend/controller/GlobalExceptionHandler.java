package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {
        int statusCode = ex.getStatusCode().value();
        String reasonPhrase = HttpStatus.valueOf(statusCode).getReasonPhrase();
        String message = ex.getReason() != null ? ex.getReason() : reasonPhrase;
        ErrorResponse body = new ErrorResponse(
                statusCode,
                reasonPhrase,
                request.getRequestURI(),
                message
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}
