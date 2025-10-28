package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

/**
 * Generic sell request. Frontend must provide the tokenId being sold,
 * the token amount (decimal string), the amount of USDC expected (decimal
 * string),
 * and the recipient Hedera account for USDC.
 */
public record SellRequest(
                @NotBlank(message = "Token ID is required") String tokenId,
                @DecimalMin(value = "0.00000001", message = "Token amount must be positive") String tokenAmount,
                @DecimalMin(value = "0.000001", message = "USDC amount must be positive") String usdcAmount,
                @NotBlank(message = "Hedera account ID is required") String hederaAccountId) {
}
