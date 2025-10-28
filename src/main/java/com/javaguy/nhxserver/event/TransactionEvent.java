package com.javaguy.nhxserver.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Base class for transaction events
 */
@Getter
public abstract class TransactionEvent extends ApplicationEvent {
    private final Long transactionId;
    private final LocalDateTime eventTimestamp;

    public TransactionEvent(Object source, Long transactionId, LocalDateTime eventTimestamp) {
        super(source);
        this.transactionId = transactionId;
        this.eventTimestamp = eventTimestamp;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }
}
