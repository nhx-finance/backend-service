package com.javaguy.nhxserver.model.dto;

import java.util.Set;

public record AuthResponse(
        String message,
        String tokenType,
        Set<String> roles,
        UserInfo userInfo) {
}
