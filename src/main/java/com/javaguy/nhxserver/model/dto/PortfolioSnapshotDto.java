package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.entity.PortfolioSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

public record PortfolioSnapshotDto(
        Long id,
        LocalDateTime snapshotTime,
        Map<String, BigDecimal> holdings,
        BigDecimal totalValueUsdc, // Changed from totalValueKes
        BigDecimal exchangeRate
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static PortfolioSnapshotDto fromEntity(PortfolioSnapshot snapshot) {
        Map<String, BigDecimal> holdingsMap = Collections.emptyMap();
        if (snapshot.getHoldingsJson() != null && !snapshot.getHoldingsJson().isEmpty()) {
            try {
                holdingsMap = objectMapper.readValue(snapshot.getHoldingsJson(), new TypeReference<Map<String, BigDecimal>>() {});
            } catch (IOException e) {
                // Log error or handle gracefully
                e.printStackTrace();
            }
        }
        return new PortfolioSnapshotDto(
                snapshot.getId(),
                snapshot.getSnapshotTime(),
                holdingsMap,
                snapshot.getTotalValueUsdc(), // Changed from getTotalValueKes()
                snapshot.getExchangeRate()
        );
    }
}
