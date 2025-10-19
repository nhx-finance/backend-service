package com.javaguy.nhxserver.model.dto;

public record UserInfo(
            Long userId,
            String username,
            String email,
            String phoneNumber,
            String fullName) {}
