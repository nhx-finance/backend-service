package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.entity.PaymentTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionDto(
        Long id,
        String transactionId,
        String type,
        String method,
        BigDecimal amount,
        String currency,
        String status,
        LocalDateTime date,
        String description
) {
    public static PaymentTransactionDto fromEntity(PaymentTransaction paymentTransaction) {
        return new PaymentTransactionDto(
                paymentTransaction.getId(),
                paymentTransaction.getTransactionId(),
                paymentTransaction.getType(),
                paymentTransaction.getMethod(),
                paymentTransaction.getAmount(),
                paymentTransaction.getCurrency(),
                paymentTransaction.getStatus(),
                paymentTransaction.getDate(),
                paymentTransaction.getDescription()
        );
    }
}
