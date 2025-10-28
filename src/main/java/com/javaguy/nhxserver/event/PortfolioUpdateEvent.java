package com.javaguy.nhxserver.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
// import java.util.UUID; // Removed unused import

/**
 * Event published when portfolio should be updated
 */
@Getter
public class PortfolioUpdateEvent extends ApplicationEvent {
    private final Long userId; // Changed from UUID to Long
    private final String tokenId;
    private final BigDecimal amount;
    private final String operation; // "ADD" or "SUBTRACT"
    private final LocalDateTime eventTimestamp;

    public PortfolioUpdateEvent(
            Object source,
            Long userId, // Changed to Long
            String tokenId,
            BigDecimal amount,
            String operation,
            LocalDateTime eventTimestamp) {
        super(source);
        this.userId = userId;
        this.tokenId = tokenId;
        this.amount = amount;
        this.operation = operation;
        this.eventTimestamp = eventTimestamp;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }
}
