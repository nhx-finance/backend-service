package com.javaguy.nhxserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for exchange rate quotes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateQuote {
    private String usdcAmount;
    private String nhsafAmount;
    private String exchangeRate; // 1 nhSAF = X USDC
    private String feeAmount; // If applicable
    private String totalUsdcRequired;
    private long timestamp;
}
