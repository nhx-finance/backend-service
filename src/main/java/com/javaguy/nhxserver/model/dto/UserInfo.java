package com.javaguy.nhxserver.model.dto;

public record UserInfo(
            Long id,
            String username,
            String email,
            String phoneNumber,
            String firstName,
            String lastName,
            String kycStatus) {}
