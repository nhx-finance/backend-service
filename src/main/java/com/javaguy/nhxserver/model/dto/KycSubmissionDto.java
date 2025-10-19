package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.NotBlank;

public record KycSubmissionDto(
        @NotBlank(message = "Full name is required")
        String fullName,
        @NotBlank(message = "Phone number is required")
        String phoneNumber,
        @NotBlank(message = "ID type is required")
        String idType,
        @NotBlank(message = "ID number is required")
        String idNumber
) {
}
