package com.javaguy.nhxserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for token balances
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBalanceResponse {
    private String accountId;
    private long usdcBalance;
    private long nhsafBalance;
    private long hbarBalance;
    
    // Human-readable formatted values
    private String usdcBalanceFormatted;
    private String nhsafBalanceFormatted;
    private String hbarBalanceFormatted;
}
