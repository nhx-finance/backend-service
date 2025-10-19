package com.javaguy.nhxserver.controller;

import com.javaguy.nhxserver.model.dto.*;
import com.javaguy.nhxserver.service.security.AuthService;
import com.javaguy.nhxserver.service.security.PasswordResetService;
import com.javaguy.nhxserver.exception.PasswordsMismatchException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and authorization management APIs")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Authenticate user", description = "Authenticates a user with username/email and password, returning JWT and refresh tokens.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User authenticated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials or bad request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - user not found or disabled",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return authService.authenticateUser(request);
    }

    @Operation(summary = "Register a new user", description = "Registers a new user with provided details, assigns a default role, and sends an email verification link.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "409", description = "User with username, email, or phone number already exists",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error or email sending failed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return authService.registerUser(request);
    }

    @Operation(summary = "Logout user", description = "Logs out the currently authenticated user by invalidating their refresh token and clearing JWT cookies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User logged out successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return authService.logout();
    }

    @Operation(summary = "Request password reset", description = "Initiates a password reset process by sending a password reset email to the provided email address.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset instructions sent (if email exists)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Error processing password reset request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)))
    })
    @PostMapping("/password/reset-request")
    public ResponseEntity<?> resetPasswordRequest(@Valid @RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.createPasswordResetTokenForUser(request.email());
            return ResponseEntity.ok()
                    .body(new AuthResponse("Password reset instructions sent if email exists", null, null, null));
        } catch (Exception e) {
            log.error("Error: {}", e);
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("Error processing password reset request", null, null, null));
        }
    }

    @Operation(summary = "Validate password reset token", description = "Validates a password reset token to ensure it is not expired or already used.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired password reset token",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)))
    })
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

    @Operation(summary = "Reset user password", description = "Resets the user's password using a valid password reset token and new password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password has been reset successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Passwords do not match or invalid/expired token",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)))
    })
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetConfirmation request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordsMismatchException("Passwords do not match");
        }

        try {
            passwordResetService.resetPassword(request.token(), request.password());
            return ResponseEntity.ok()
                    .body(new AuthResponse("Password has been reset successfully", null, null, null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(e.getMessage(), null, null, null));
        }
    }

    @Operation(summary = "Verify user email", description = "Verifies a user's email address using a valid verification token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Email verification failed (invalid, expired, or used token)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)))
    })
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        return authService.verifyEmail(token);
    }

    @Operation(summary = "Resend email verification link", description = "Resends the email verification link to a user if their account is not yet verified.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification email sent successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Failed to resend verification email (e.g., email not found or already verified)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)))
    })
    @PostMapping("/resend-verification-email")
    public ResponseEntity<?> resendVerificationEmail(@Valid @RequestBody PasswordResetRequest request) {
        return authService.resendVerificationEmail(request.email());
    }

    @Operation(summary = "Test authentication access", description = "A simple endpoint to test if authentication endpoints are accessible. Requires no authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Auth endpoints are accessible",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok()
                .body(Map.of("message", "Auth endpoints are accessible"));
    }
}