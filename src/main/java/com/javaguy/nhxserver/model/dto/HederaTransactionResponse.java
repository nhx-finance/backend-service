package com.javaguy.nhxserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Hedera transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HederaTransactionResponse {
    private boolean success;
    private String transactionId;
    private String status;
    private String message;
    private String errorCode;
}
