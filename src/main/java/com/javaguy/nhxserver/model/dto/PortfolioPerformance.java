package com.javaguy.nhxserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for portfolio performance metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPerformance {
    private BigDecimal currentValue;
    private BigDecimal initialValue;
    private BigDecimal totalGainLoss;
    private BigDecimal percentageChange;
    private int snapshotCount;
    private LocalDateTime firstSnapshotDate;
    private LocalDateTime latestSnapshotDate;
}
