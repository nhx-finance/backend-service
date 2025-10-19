package com.javaguy.nhxserver.model.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email")
        String email)
       {}
