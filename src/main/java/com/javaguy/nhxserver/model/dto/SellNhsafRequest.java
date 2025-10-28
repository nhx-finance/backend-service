package com.javaguy.nhxserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for selling nhSAF for USDC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellNhsafRequest {
    private String sellerAccountId;
    private String sellerPrivateKey; // In production, use secure key management
    private String nhsafAmount; // Amount in nhSAF (decimal format)
}
