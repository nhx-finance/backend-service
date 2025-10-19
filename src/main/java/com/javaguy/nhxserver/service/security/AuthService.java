package com.javaguy.nhxserver.service.security;

import com.javaguy.nhxserver.model.dto.LoginRequest;
import com.javaguy.nhxserver.model.dto.RegisterRequest;
import com.javaguy.nhxserver.model.entity.RefreshToken;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.repository.UserRepository;
import com.javaguy.nhxserver.model.dto.AuthResponse;
import com.javaguy.nhxserver.service.email.EmailVerificationService;
import com.javaguy.nhxserver.service.user.UserDetailsImpl;
import com.javaguy.nhxserver.service.security.jwt.JwtUtils;
import com.javaguy.nhxserver.exception.EmailNotVerifiedException;
import com.javaguy.nhxserver.exception.AccountDisabledException;
import com.javaguy.nhxserver.exception.EmailSendingException;
import com.javaguy.nhxserver.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.javaguy.nhxserver.model.dto.MessageResponse;
import com.javaguy.nhxserver.model.dto.RegistrationResponse;
import com.javaguy.nhxserver.model.dto.UserInfo;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ResponseEntity<?> authenticateUser(@Valid LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.email());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isEnabled() || !user.isEmailVerified()) {
                log.warn("Login attempt with unverified or disabled email for user: {}", user.getEmail());
                throw new EmailNotVerifiedException("Please verify your email address before logging in. Check your email for the verification link.");
            }

            ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());
            ResponseCookie jwtRefreshCookie = jwtUtils.generateJwtRefreshCookie(refreshToken.getToken());

            Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

            UserInfo userInfo = new UserInfo(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getFullName()
            );

            log.info("Authentication successful for user: {}", loginRequest.email());
            
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                .body(new AuthResponse("Authentication successful", jwtCookie.getValue(), roles, userInfo));
                
        } catch (DisabledException e) {
            // Check if the user exists and needs email verification
            String email = loginRequest.email();
            User user = userRepository.findByEmail(email)
                    .or(() -> userRepository.findByEmail(email))
                    .orElse(null);
            
            if (user != null && !user.isEmailVerified()) {
                log.warn("Login attempt with unverified email for user: {}", email);
                throw new EmailNotVerifiedException("Please verify your email address before logging in. Check your email for the verification link.");
            } else {
                log.error("Authentication failed - account disabled for user: {}", email, e);
                throw new AccountDisabledException("Your account has been disabled. Please contact support.");
            }
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", loginRequest.email(), e);
            throw new RuntimeException("Invalid username/email or password", e);
        } catch (Exception e) {
            log.error("Unexpected authentication error for user: {}", loginRequest.email(), e);
            throw new RuntimeException("An unexpected error occurred during authentication", e);
        }
    }

    @Transactional
    public ResponseEntity<?> registerUser(@Valid RegisterRequest request) {
        log.info("Registering user with email: {}", request.email());

        User registeredUser = userService.registerUser(request);

        try {
            emailVerificationService.generateAndSendVerificationToken(registeredUser);
            log.info("Email verification token sent to: {}", registeredUser.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(new RegistrationResponse(
                    registeredUser.getUserId(),
                    registeredUser.getEmail(),
                    registeredUser.getCreatedAt()
            ));

        } catch (Exception e) {
            log.error("Failed to send verification email for user: {}", registeredUser.getEmail(), e);
            throw new EmailSendingException("User registered, but failed to send verification email. Please contact support.", e);
        }
    }
    
    @Transactional
    public ResponseEntity<?> verifyEmail(String token) {
        log.info("Verifying email with token: {}", token.substring(0, 8) + "...");
        
        boolean verified = emailVerificationService.verifyEmail(token);
        
        if (verified) {
            return ResponseEntity.ok(new MessageResponse("Email verified successfully! You can now log in to your account.", Map.of("verified", true)));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(MessageResponse.error("Email verification failed", new MessageResponse.ErrorDetails("VERIFICATION_FAILED", "The verification link is invalid, expired, or has already been used.", null)));
        }
    }
    
    @Transactional
    public ResponseEntity<?> resendVerificationEmail(String email) {
        log.info("Resending verification email to: {}", email);
        
        boolean sent = emailVerificationService.resendVerificationEmail(email);
        if (sent) {
            return ResponseEntity.ok(new MessageResponse("Verification email sent successfully! Please check your email.", "success"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(MessageResponse.error("Failed to resend verification email", new MessageResponse.ErrorDetails("RESEND_FAILED", "Email not found or account already verified.", null)));
        }
    }

    @Transactional
    public ResponseEntity<?> logout() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            if (principal instanceof UserDetailsImpl) {
                Long userId = ((UserDetailsImpl) principal).getId();
                log.info("Logging out user ID: {}", userId);

                try {
                    refreshTokenService.deleteByUserId(userId);
                    log.debug("Successfully deleted refresh tokens for user ID: {}", userId);
                } catch (Exception e) {
                    log.warn("Failed to delete refresh tokens for user ID: {}, but continuing logout", userId, e);
                }
            } else {
                log.debug("No authenticated user found during logout");
            }

            SecurityContextHolder.clearContext();

            ResponseCookie jwtCookie = jwtUtils.getClean1JwtCookie();
            ResponseCookie jwtRefreshCookie = jwtUtils.getCleanJwtRefreshCookie();

            log.info("Logout successful");
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                    .body(new AuthResponse("Logout successful", null, null, null));

        } catch (Exception e) {
            log.error("Error during logout", e);

            ResponseCookie jwtCookie = jwtUtils.getClean1JwtCookie();
            ResponseCookie jwtRefreshCookie = jwtUtils.getCleanJwtRefreshCookie();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                    .body(new AuthResponse("Logout completed with warnings", null, null, null));
        }
    }
}
