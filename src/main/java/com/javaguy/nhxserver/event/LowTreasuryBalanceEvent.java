package com.javaguy.nhxserver.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when treasury balance is low
 */
@Getter
public class LowTreasuryBalanceEvent extends ApplicationEvent {
    private final String tokenId;
    private final BigDecimal currentBalance;
    private final BigDecimal threshold;
    private final LocalDateTime eventTimestamp;

    public LowTreasuryBalanceEvent(
            Object source,
            String tokenId,
            BigDecimal currentBalance,
            BigDecimal threshold,
            LocalDateTime eventTimestamp) {
        super(source);
        this.tokenId = tokenId;
        this.currentBalance = currentBalance;
        this.threshold = threshold;
        this.eventTimestamp = eventTimestamp;
    }
}
