package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.EmailVerificationToken;
import com.javaguy.nhxserver.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    Optional<EmailVerificationToken> findByToken(String token);
    
    Optional<EmailVerificationToken> findByUser(User user);
    
    Optional<EmailVerificationToken> findByUserAndUsedFalse(User user);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiryDate <= ?1")
    void deleteAllExpiredTokens(LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user = ?1")
    void deleteByUser(User user);
    
    boolean existsByUserAndUsedFalse(User user);
}

