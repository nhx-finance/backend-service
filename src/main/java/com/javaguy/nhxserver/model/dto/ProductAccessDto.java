package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.entity.ProductAccess;

import java.math.BigDecimal;

public record ProductAccessDto(
        Long id,
        String name,
        String description,
        Boolean access,
        BigDecimal minimumDeposit,
        BigDecimal fees,
        String custodian
) {
    public static ProductAccessDto fromEntity(ProductAccess productAccess) {
        return new ProductAccessDto(
                productAccess.getId(),
                productAccess.getName(),
                productAccess.getDescription(),
                productAccess.getAccess(),
                productAccess.getMinimumDeposit(),
                productAccess.getFees(),
                productAccess.getCustodian()
        );
    }
}
