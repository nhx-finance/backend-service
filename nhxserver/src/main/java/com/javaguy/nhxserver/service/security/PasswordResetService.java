package com.javaguy.nhxserver.service.security;

import com.javaguy.nhxserver.exception.TokenExpiredException;
import com.javaguy.nhxserver.model.entity.PasswordResetToken;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.repository.PasswordResetTokenRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import com.javaguy.nhxserver.service.EmailService;
import com.javaguy.nhxserver.service.security.RefreshTokenService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Value("${nhx.app.passwordResetExpirationMs}")
    private Long passwordResetExpirationMs;

    @Transactional
    public void createPasswordResetTokenForUser(String email) throws MessagingException {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Invalidate any existing tokens
            tokenRepository.findByUser(user).ifPresent(token -> {
                token.setUsed(true);
                tokenRepository.save(token);
            });

            // Create new token
            String token = UUID.randomUUID().toString();
            PasswordResetToken myToken = new PasswordResetToken();
            myToken.setToken(token);
            myToken.setUser(user);
            myToken.setExpiryDate(Instant.now().plusMillis(passwordResetExpirationMs));
            tokenRepository.save(myToken);

            // Send email with reset link
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        }
    }

    @Transactional
    public boolean validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        if (tokenOptional.isEmpty()) {
            return false;
        }

        PasswordResetToken passToken = tokenOptional.get();
        if (passToken.isUsed() || passToken.getExpiryDate().isBefore(Instant.now())) {
            return false;
        }

        return true;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenExpiredException("Invalid or expired password reset token."));

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new TokenExpiredException("Token has expired or already been used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        // Invalidate all existing refresh tokens for the user, forcing re-login
        refreshTokenService.deleteByUserId(user.getId());
    }
}
