package com.javaguy.nhxserver.model.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Builder
public record PortfolioPerformanceDto(
        BigDecimal currentValue,
        BigDecimal initialValue,
        BigDecimal totalGainLoss,
        BigDecimal percentageChange,
        int snapshotCount,
        LocalDateTime firstSnapshotDate,
        LocalDateTime latestSnapshotDate
) {
}
