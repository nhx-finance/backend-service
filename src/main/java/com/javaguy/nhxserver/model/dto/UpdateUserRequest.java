package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        @Email(message = "Please provide a valid email")
        String email,
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Please provide a valid phone number in international format (e.g., +254712345678)")
        String phoneNumber,
        @Size(max = 50, message = "Name must not exceed 50 characters")
        String fullName)
        {
}
