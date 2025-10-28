package com.javaguy.nhxserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token info response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfoResponse {
    private String usdcTokenId;
    private String nhsafTokenId;
    private String treasuryAccountId;
    private String exchangeRate;
    private int usdcDecimals;
    private int nhsafDecimals;
}
