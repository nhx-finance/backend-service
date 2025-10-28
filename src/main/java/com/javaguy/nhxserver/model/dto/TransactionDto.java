package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.entity.Transaction;
import com.javaguy.nhxserver.model.enums.TransactionStatus;
import com.javaguy.nhxserver.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
// import java.util.UUID; // Removed unused import

public record TransactionDto(
        Long id, // Changed from UUID to Long
        Long userId, // Added userId
        TransactionType type,
        TransactionStatus status,
        BigDecimal amountUsdc, // Changed from amountKes
        BigDecimal tokenAmount,
        BigDecimal exchangeRate,
        String hederaAccountId,
        // Removed phoneNumber and darajaTransactionId
        String hederaTransactionId,
        BigDecimal fees,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        String memo
) {
    public static TransactionDto fromEntity(Transaction transaction) {
        return new TransactionDto(
                transaction.getId(),
                transaction.getUser().getUserId(), // Get userId from User entity
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmountUsdc(), // Changed from getAmountKes()
                transaction.getTokenAmount(),
                transaction.getExchangeRate(),
                transaction.getHederaAccountId(),
                // Removed phoneNumber and darajaTransactionId
                transaction.getHederaTransactionId(),
                transaction.getFees(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getCompletedAt(),
                transaction.getMemo()
        );
    }
}
