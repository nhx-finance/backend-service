package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WalletRequestDto(
    @NotBlank(message = "Action is required")
    @Pattern(regexp = "create|link", message = "Action must be 'create' or 'link'")
    String action,
    String accountId
) {}
