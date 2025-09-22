package com.javaguy.nhxserver.service.security;

import com.javaguy.nhxserver.model.entity.PasswordResetToken;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.repository.PasswordResetTokenRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import com.javaguy.nhxserver.service.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        // We don't want to reveal if the email exists in our system
        // So we'll return successfully even if the email wasn't found
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
        PasswordResetToken passToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (passToken.isUsed() || passToken.getExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token has expired or already been used");
        }

        User user = passToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        passToken.setUsed(true);
        tokenRepository.save(passToken);
    }
}
