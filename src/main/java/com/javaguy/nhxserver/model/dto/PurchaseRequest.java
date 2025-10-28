package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record PurchaseRequest(
        @DecimalMin(value = "0.01", message = "USDC amount must be positive")
        String usdcAmount,
        @NotBlank(message = "Hedera account ID is required")
//        @Pattern(regexp = "^(0|([1-9]\d*))\\.(0|([1-9]\d*))\\.(0|([1-9]\d*))$", message = "Invalid Hedera account ID format (e.g., 0.0.12345)")
        String hederaAccountId,
        @NotBlank(message = "Buyer private key is required")
        String buyerPrivateKey
) {}
