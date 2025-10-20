package com.javaguy.nhxserver.model.dto;

import java.util.Set;

public record LoginResponse(
        String message,
        String email,
        String jwtToken,
        String refreshToken,
        Set<String> roles
) {}
