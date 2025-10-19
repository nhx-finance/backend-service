package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
        Long id,
        String transactionId,
        String stock,
        BigDecimal amount,
        BigDecimal quantity,
        String type,
        LocalDateTime date
) {
    public static TransactionDto fromEntity(Transaction transaction) {
        return new TransactionDto(
                transaction.getId(),
                transaction.getTransactionId(),
                transaction.getStock(),
                transaction.getAmount(),
                transaction.getQuantity(),
                transaction.getType(),
                transaction.getDate()
        );
    }
}
