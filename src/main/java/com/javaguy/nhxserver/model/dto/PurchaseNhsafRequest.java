package com.javaguy.nhxserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for purchasing nhSAF with USDC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseNhsafRequest {
    private String buyerAccountId;
    private String buyerPrivateKey; // In production, use secure key management
    private String usdcAmount; // Amount in USDC (decimal format, e.g., "100.50")
}
