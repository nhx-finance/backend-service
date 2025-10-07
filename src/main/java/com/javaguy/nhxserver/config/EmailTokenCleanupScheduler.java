package com.javaguy.nhxserver.config;

import com.javaguy.nhxserver.service.email.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailTokenCleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailTokenCleanupScheduler.class);
    
    private final EmailVerificationService emailVerificationService;
    
    /**
     * Clean up expired email verification tokens every 6 hours
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6 hours in milliseconds
    public void cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired email verification tokens");
        try {
            emailVerificationService.cleanupExpiredTokens();
            logger.info("Cleanup of expired email verification tokens completed");
        } catch (Exception e) {
            logger.error("Error during cleanup of expired email verification tokens", e);
        }
    }
}


