package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.entity.Portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PortfolioDto(
        Long id,
        String tokenId,
        BigDecimal balance,
        LocalDateTime lastUpdated
) {
    public static PortfolioDto fromEntity(Portfolio portfolio) {
        return new PortfolioDto(
                portfolio.getId(),
                portfolio.getTokenId(),
                portfolio.getBalance(),
                portfolio.getLastUpdated()
        );
    }
}
