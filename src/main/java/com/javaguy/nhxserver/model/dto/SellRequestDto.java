package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SellRequestDto(
    @NotBlank(message = "Token symbol is required")
    String tokenSymbol,

    @NotBlank(message = "Amount to burn is required")
    String amountToBurn,

    @NotBlank(message = "Amount of USDC to send is required")
    String amountUsdcToSend,

    @NotBlank(message = "Recipient Hedera account ID is required")
    String recipientAccountIdStr
) {}
