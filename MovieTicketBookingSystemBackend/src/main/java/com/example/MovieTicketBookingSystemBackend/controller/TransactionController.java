package com.example.MovieTicketBookingSystemBackend.controller;

import com.example.MovieTicketBookingSystemBackend.dto.TransactionResponse;
import com.example.MovieTicketBookingSystemBackend.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> listMyTransactions(
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(transactionService.listTransactionsForUser(userId));
    }
}
