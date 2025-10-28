package com.javaguy.nhxserver.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when token transfer is completed
 */
@Getter
public class TokenTransferCompletedEvent extends TransactionEvent {
    private final String hederaTransactionId;
    private final String accountId;
    private final BigDecimal tokenAmount;

    public TokenTransferCompletedEvent(
            Object source,
            Long transactionId,
            String hederaTransactionId,
            String accountId,
            BigDecimal tokenAmount,
            LocalDateTime eventTimestamp) {
        super(source, transactionId, eventTimestamp);
        this.hederaTransactionId = hederaTransactionId;
        this.accountId = accountId;
        this.tokenAmount = tokenAmount;
    }
}
