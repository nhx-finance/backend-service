package com.javaguy.nhxserver.model.dto;

import java.time.LocalDateTime;

public record RegistrationResponse(
        Long userId,
        String email,
        LocalDateTime createdAt
) {
}
