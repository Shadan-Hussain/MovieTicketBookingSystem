package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.TransactionResponse;
import com.example.MovieTicketBookingSystemBackend.model.Transaction;
import com.example.MovieTicketBookingSystemBackend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactionsForUser(Long userId) {
        if (userId == null) return List.of();
        return transactionRepository.findByUser_UserIdOrderByCreatedAtDesc(userId).stream()
                .filter(t -> !Transaction.STATUS_PENDING.equals(t.getStatus()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getTransactionId(),
                t.getShowId(),
                t.getSeatId(),
                t.getAmount(),
                t.getCurrency(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
