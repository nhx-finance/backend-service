package com.javaguy.nhxserver.service.security.api;

import com.javaguy.nhxserver.model.dto.LoginRequest;
import com.javaguy.nhxserver.model.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;

public interface AuthUseCase {
    ResponseEntity<?> authenticateUser(LoginRequest loginRequest);
    ResponseEntity<?> registerUser(RegisterRequest request);
    ResponseEntity<?> verifyEmail(String token);
    ResponseEntity<?> resendVerificationEmail(String email);
    ResponseEntity<?> logout();
    ResponseEntity<?> refreshToken(String refreshToken);
}
