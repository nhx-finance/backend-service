package com.javaguy.nhxserver.model.dto;

import java.util.Set;

public record LoginResponse(
        String message,
        String userEmail,
        Long userId,
        String jwtToken,
        String refreshToken,
        Set<String> roles,
        Long jwtExpiresAt,
        Long refreshExpiresAt
) {}
