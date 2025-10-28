package com.javaguy.nhxserver.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a transaction fails
 */
@Getter
public class TransactionFailedEvent extends TransactionEvent {
    private final String reason;
    private final String failureStage;

    public TransactionFailedEvent(
            Object source,
            Long transactionId,
            String reason,
            String failureStage,
            LocalDateTime eventTimestamp) {
        super(source, transactionId, eventTimestamp);
        this.reason = reason;
        this.failureStage = failureStage;
    }
}
