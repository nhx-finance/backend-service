package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.NotBlank;

public record WalletRequestDto(
    @NotBlank(message = "Wallet address is required")
    String walletAddress
) {}
