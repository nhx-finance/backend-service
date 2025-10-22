package com.javaguy.nhxserver.service.security;

import com.javaguy.nhxserver.exception.AccountDisabledException;
import com.javaguy.nhxserver.exception.EmailNotVerifiedException;
import com.javaguy.nhxserver.exception.EmailSendingException;
import com.javaguy.nhxserver.exception.UserNotFound;
import com.javaguy.nhxserver.model.dto.*;
import com.javaguy.nhxserver.model.entity.RefreshToken;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.repository.UserRepository;
import com.javaguy.nhxserver.service.email.EmailVerificationService;
import com.javaguy.nhxserver.service.security.api.AuthUseCase;
import com.javaguy.nhxserver.service.security.jwt.JwtUtils;
import com.javaguy.nhxserver.service.user.UserDetailsImpl;
import com.javaguy.nhxserver.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.javaguy.nhxserver.model.dto.MessageResponse;
import com.javaguy.nhxserver.model.dto.RegistrationResponse;
import com.javaguy.nhxserver.model.dto.LoginResponse;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse authenticateUser(@Valid LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.email());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new UserNotFound("User not found with email: " + loginRequest.email()));
            if (!userRepository.existsByEmail(loginRequest.email())) {
                throw new UserNotFound("User not found with email: " + loginRequest.email());
            }
            if (!user.isEnabled() || !user.isEmailVerified()) {
                log.warn("Login attempt with unverified or disabled account for user: {}", user.getEmail());
                throw new EmailNotVerifiedException("Please verify your email address before logging in. Check your email for the verification link.");
            }

            // Generate JWT and refresh token
            String jwt = jwtUtils.generateToken(userDetails);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

            Set<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            long now = System.currentTimeMillis();

            log.info("Authentication successful for user: {}", loginRequest.email());

            return new LoginResponse(
                    "Authentication successful",
                    user.getEmail(),
                    userDetails.getId(),
                    jwt,
                    refreshToken.getToken(),
                    roles,
                    now + jwtUtils.getJwtExpirationTimeMs(),
                    now + jwtUtils.getRefreshExpirationTimeMs()
            );

        } catch (DisabledException e) {
            // Check if the user exists and needs email verification
            User user = userRepository.findByEmail(loginRequest.email())
                    .orElse(null);

            if (user != null && !user.isEmailVerified()) {
                log.warn("Login attempt with unverified email for user: {}", loginRequest.email());
                throw new EmailNotVerifiedException("Please verify your email address before logging in. Check your email for the verification link.");
            } else {
                log.error("Authentication failed - account disabled for user: {}", loginRequest.email());
                throw new AccountDisabledException("Your account has been disabled. Please contact support.");
            }
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", loginRequest.email());
            throw new RuntimeException("Invalid email or password", e);
        }
    }

    @Transactional
    public RegistrationResponse registerUser(@Valid RegisterRequest request) {
        log.info("Registering user with email: {}", request.email());

        User registeredUser = userService.registerUser(request);
        try {
            emailVerificationService.generateAndSendVerificationToken(registeredUser);
            return new RegistrationResponse(
                    registeredUser.getUserId(),
                    registeredUser.getEmail(),
                    registeredUser.getCreatedAt()
            );
        } catch (Exception e) {
            throw new EmailSendingException("User registered, but failed to send verification email.", e);
        }
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        boolean verified = emailVerificationService.verifyEmail(token);
        if (verified) {
            return new MessageResponse("Email verified successfully!", Map.of("verified", true));
        } else {
            return MessageResponse.error(
                    "Email verification failed",
                    new MessageResponse.ErrorDetails("VERIFICATION_FAILED", "The verification link is invalid or expired.", null)
            );
        }
    }

    @Transactional
    public MessageResponse resendVerificationEmail(String email) {
        boolean sent = emailVerificationService.resendVerificationEmail(email);
        if (sent) {
            return new MessageResponse("Verification email sent successfully!", "success");
        } else {
            return MessageResponse.error(
                    "Failed to resend verification email",
                    new MessageResponse.ErrorDetails("RESEND_FAILED", "Email not found or already verified.", null)
            );
        }
    }

    @Transactional
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            refreshTokenService.deleteByUserId(userDetails.getId());
            log.info("Logout successful for user with id: {}", userDetails.getId());
        }
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public LoginResponse refreshToken(String oldRefreshToken) {
        RefreshToken token = refreshTokenService.findByToken(oldRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        User user = token.getUser();
        if (user == null) throw new RuntimeException("Token not associated with a user");

        if (!user.isEnabled() || !user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Account not verified or disabled");
        }

        RefreshToken newToken = refreshTokenService.createRefreshToken(user.getUserId());
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        String jwt = jwtUtils.generateToken(userDetails);
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        long now = System.currentTimeMillis();

        return new LoginResponse(
                "Token refreshed",
                user.getEmail(),
                user.getUserId(),
                jwt,
                newToken.getToken(),
                roles,
                now + jwtUtils.getJwtExpirationTimeMs(),
                now + jwtUtils.getRefreshExpirationTimeMs()
        );
    }
}
