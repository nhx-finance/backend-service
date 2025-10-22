package com.javaguy.nhxserver.service.security.api;

import com.javaguy.nhxserver.model.dto.*;


public interface AuthUseCase {

    LoginResponse authenticateUser(LoginRequest loginRequest);

    RegistrationResponse registerUser(RegisterRequest request);

    MessageResponse verifyEmail(String token);

    MessageResponse resendVerificationEmail(String email);

    void logout();

    LoginResponse refreshToken(String refreshToken);
}
