package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.model.entity.EmailVerificationToken;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.repository.EmailVerificationTokenRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);
    
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    
    @Value("${nhx.app.baseUrl}")
    private String baseUrl;
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${nhx.app.emailVerificationExpirationMs:86400000}")
    private long tokenExpirationMs;
    
    /**
     * Generate and send email verification token
     */
    public void generateAndSendVerificationToken(User user) {
        logger.info("Generating email verification token for user: {}", user.getUsername());
        tokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        int expiryMinutes = (int) (tokenExpirationMs / 60000); // Convert ms to minutes
        
        EmailVerificationToken verificationToken = new EmailVerificationToken(token, user, expiryMinutes);
        tokenRepository.save(verificationToken);
        sendVerificationEmail(user, token);
        
        logger.info("Email verification token generated and sent for user: {}", user.getUsername());
    }
    
    /**
     * Verify email using token
     */
    public boolean verifyEmail(String token) {
        logger.info("Attempting to verify email with token: {}", token.substring(0, 8) + "...");
        
        Optional<EmailVerificationToken> optionalToken = tokenRepository.findByToken(token);
        
        if (optionalToken.isEmpty()) {
            logger.warn("Email verification failed - token not found: {}", token.substring(0, 8) + "...");
            return false;
        }
        
        EmailVerificationToken verificationToken = optionalToken.get();
        
        if (verificationToken.isUsed()) {
            logger.warn("Email verification failed - token already used: {}", token.substring(0, 8) + "...");
            return false;
        }
        
        if (verificationToken.isExpired()) {
            logger.warn("Email verification failed - token expired: {}", token.substring(0, 8) + "...");
            return false;
        }
        
        // Mark token as used
        verificationToken.markAsUsed();
        tokenRepository.save(verificationToken);
        
        // Enable the user account
        User user = verificationToken.getUser();
        user.setEnabled(true);
        user.setEmailVerified(true);
        userRepository.save(user);
        
        logger.info("Email verification successful for user: {}", user.getUsername());
        return true;
    }
    
    /**
     * Resend verification email
     */
    public boolean resendVerificationEmail(String email) {
        logger.info("Resending verification email to: {}", email);
        
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            logger.warn("Resend verification failed - user not found: {}", email);
            return false;
        }
        
        User user = optionalUser.get();
        
        if (user.isEnabled()) {
            logger.warn("Resend verification failed - user already verified: {}", email);
            return false;
        }
        
        generateAndSendVerificationToken(user);
        return true;
    }
    
    /**
     * Send verification email
     */
    @Async
    protected void sendVerificationEmail(User user, String token) {
        try {
            String verificationUrl = baseUrl + "/api/v1/auth/verify-email?token=" + token;
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Verify Your Email Address - NHX Platform");
            message.setText(buildEmailContent(user.getFirstName(), verificationUrl));
            message.setFrom(fromEmail);
            
            mailSender.send(message);
            logger.info("Verification email sent successfully to: {}", user.getEmail());
            
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {}", user.getEmail(), e);
        }
    }
    
    /**
     * Build email content
     */
    private String buildEmailContent(String firstName, String verificationUrl) {
        return String.format("""
            Dear %s,
            
            Welcome to NHX Platform!
            
            To complete your registration and activate your account, please click the link below to verify your email address:
            
            %s
            
            This link will expire in 24 hours.
            
            If you didn't create an account with us, please ignore this email.
            
            Best regards,
            The NHX Team
            
            ---
            If the link above doesn't work, copy and paste the following URL into your browser:
            %s
            """, firstName, verificationUrl, verificationUrl);
    }
    
    /**
     * Check if user has pending verification
     */
    public boolean hasPendingVerification(User user) {
        return tokenRepository.existsByUserAndUsedFalse(user) && !user.isEnabled();
    }
    
    /**
     * Clean up expired tokens
     */
    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("Cleaning up expired email verification tokens");
        tokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
    }
}

