package com.javaguy.nhxserver.controller;

import com.javaguy.nhxserver.model.dto.*;
import com.javaguy.nhxserver.service.security.AuthService;
import com.javaguy.nhxserver.service.security.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return authService.authenticateUser(request);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return authService.registerUser(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return authService.logout();
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<?> resetPasswordRequest(@Valid @RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.createPasswordResetTokenForUser(request.getEmail());
            return ResponseEntity.ok()
                    .body(new AuthResponse("Password reset instructions sent if email exists", null, null, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("Error processing password reset request", null, null, null));
        }
    }

    @GetMapping("/password/reset/validate")
    public ResponseEntity<?> validatePasswordResetToken(@RequestParam("token") String token) {
        boolean valid = passwordResetService.validatePasswordResetToken(token);
        if (!valid) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("Invalid or expired password reset token", null, null, null));
        }
        return ResponseEntity.ok()
                .body(new AuthResponse("Token is valid", null, null, null));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetConfirmation request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("Passwords do not match", null, null, null));
        }

        try {
            passwordResetService.resetPassword(request.getToken(), request.getPassword());
            return ResponseEntity.ok()
                    .body(new AuthResponse("Password has been reset successfully", null, null, null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(e.getMessage(), null, null, null));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok()
                .body(Map.of("message", "Auth endpoints are accessible"));
    }
}