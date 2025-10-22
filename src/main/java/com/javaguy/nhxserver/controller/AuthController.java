package com.javaguy.nhxserver.controller;

import com.javaguy.nhxserver.model.dto.*;
import com.javaguy.nhxserver.service.security.api.AuthUseCase;
import com.javaguy.nhxserver.service.security.PasswordResetService;
import com.javaguy.nhxserver.exception.PasswordsMismatchException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import com.javaguy.nhxserver.service.security.jwt.JwtUtils;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthUseCase authService;
    private final PasswordResetService passwordResetService;
    private final JwtUtils jwtUtils;

    @Operation(summary = "Authenticate user", description = "Authenticates a user with email and password, returning JWT and refresh tokens.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User authenticated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials or bad request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - user not found or disabled",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.authenticateUser(request);

        // Generate JWT cookie
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(loginResponse.jwtToken());

        // Generate Refresh Token cookie
        ResponseCookie refreshCookie = jwtUtils.generateJwtRefreshCookie(loginResponse.refreshToken());

        log.info("Login successful for user with ID: {}", loginResponse.userId());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(loginResponse);
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
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authService.registerUser(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Logout user", description = "Logs out the currently authenticated user by invalidating their refresh token and clearing JWT cookies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User logged out successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        authService.logout();

        Cookie cookie = new Cookie(jwtUtils.getJwtRefreshCookieName(), null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);

        return new ResponseEntity<>(new MessageResponse("Successfully logged out", "success"), HttpStatus.OK);
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
        MessageResponse messageResponse = authService.verifyEmail(token);
        Object data = messageResponse.data();
        if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            if (dataMap.containsKey("verified") && (Boolean) dataMap.get("verified")) {
                return ResponseEntity.ok(messageResponse);
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(messageResponse);
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
        return new ResponseEntity<>(authService.resendVerificationEmail(request.email()), HttpStatus.OK);
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

    @Operation(summary = "Refresh access token", description = "Rotates refresh token and issues a new JWT using the refresh token stored in httpOnly cookie.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]);
        String cookieName = jwtUtils.getJwtRefreshCookieName();
        String refreshToken = Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401)
                    .body(MessageResponse.error("Missing refresh token", new MessageResponse.ErrorDetails(
                            "MISSING_REFRESH_TOKEN", null, null
                    )));
        }
        return new ResponseEntity<>(authService.refreshToken(refreshToken), HttpStatus.OK);
    }
}