package com.javaguy.nhxserver.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when tokens are received by treasury
 */
@Getter
public class TokensReceivedEvent extends TransactionEvent {
    private final String hederaTransactionId;
    private final String fromAccountId;
    private final BigDecimal tokenAmount;

    public TokensReceivedEvent(
            Object source,
            Long transactionId,
            String hederaTransactionId,
            String fromAccountId,
            BigDecimal tokenAmount,
            LocalDateTime eventTimestamp) {
        super(source, transactionId, eventTimestamp);
        this.hederaTransactionId = hederaTransactionId;
        this.fromAccountId = fromAccountId;
        this.tokenAmount = tokenAmount;
    }
}
