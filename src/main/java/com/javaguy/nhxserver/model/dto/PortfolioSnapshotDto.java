package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.entity.PortfolioSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PortfolioSnapshotDto(
        Long id,
        LocalDate date,
        BigDecimal balance,
        LocalDateTime createdAt
) {
    public static PortfolioSnapshotDto fromEntity(PortfolioSnapshot snapshot) {
        return new PortfolioSnapshotDto(
                snapshot.getId(),
                snapshot.getDate(),
                snapshot.getBalance(),
                snapshot.getCreatedAt()
        );
    }
}
