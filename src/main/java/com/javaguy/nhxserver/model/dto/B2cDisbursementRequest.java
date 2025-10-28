package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record B2cDisbursementRequest(
        @NotBlank(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Amount must be positive")
        BigDecimal amount,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^2547[0-9]{8}$", message = "Invalid M-Pesa phone number format (e.g., 2547XXXXXXXX)")
        String phoneNumber,
        @NotBlank(message = "Transaction reference is required")
        @Size(max = 20, message = "Transaction reference cannot exceed 20 characters")
        String transactionRef,
        @NotBlank(message = "Transaction description is required")
        @Size(max = 100, message = "Transaction description cannot exceed 100 characters")
        String transactionDesc
) {}
