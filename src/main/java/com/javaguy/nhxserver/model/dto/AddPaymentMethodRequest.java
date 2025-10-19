package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddPaymentMethodRequest(
        @NotBlank(message = "Payment method name is required")
        String name,
        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\+(?:[0-9] ?){6,14}[0-9]$", message = "Invalid mobile number format.")
        String mobileNumber
) {
}
