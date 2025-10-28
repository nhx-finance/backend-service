package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.enums.TransactionStatus;
import lombok.Builder;

import java.math.BigDecimal;
// import java.util.UUID; // Removed unused import

@Builder
public record TransactionInitiationResponse(
        String message, // Added message field
        Long transactionId, // Changed from UUID to Long
        TransactionStatus status,
        BigDecimal estimatedTokens, // For purchase (nhSAF)
        BigDecimal estimatedUsdc,    // For sell (USDC)
        String hederaTransactionId      // Hedera transaction ID (if immediately available)
) {
}
