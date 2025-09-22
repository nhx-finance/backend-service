package com.javaguy.nhxserver.model.dto;

public record RegistrationSuccessInfo(
        boolean requiresEmailVerification,
        String email,
        Long userId) {}
