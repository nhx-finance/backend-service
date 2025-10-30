package com.javaguy.nhxserver.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when portfolio should be updated
 */
@Getter
public class PortfolioUpdateEvent extends ApplicationEvent {
    private final Long userId;
    private final String tokenId;
    private final BigDecimal amount;
    private final String operation; // "ADD" or "SUBTRACT"
    private final LocalDateTime eventTimestamp;

    public PortfolioUpdateEvent(
            Object source,
            Long userId,
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
}
