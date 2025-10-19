package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.RefreshToken;
import com.javaguy.nhxserver.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserUserId(Long userId);
    @Modifying
    void deleteByUser(User user);
}
