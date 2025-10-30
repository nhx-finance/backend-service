package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SellRequestDto(
    @NotBlank(message = "Token symbol is required")
    String tokenSymbol,

    @Positive(message = "Amount to burn must be positive")
    long amountToBurn,

    @Positive(message = "Amount of USDC to send must be positive")
    long amountUsdcToSend,

    @NotBlank(message = "Recipient Hedera account ID is required")
    String recipientAccountIdStr
) {}
