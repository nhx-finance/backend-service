package com.javaguy.nhxserver.service.security;

import com.javaguy.nhxserver.exception.TokenRefreshException;
import com.javaguy.nhxserver.model.entity.RefreshToken;
import com.javaguy.nhxserver.repository.RefreshTokenRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.javaguy.nhxserver.model.entity.User;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    @Value("${nhx.app.jwtRefreshExpirationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        // Check if a refresh token already exists for this user
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(userId);
        existingToken.ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found for refresh token creation")));
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }

        return token;
    }

    @Transactional
    public Long deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found for refresh token deletion"));
        return refreshTokenRepository.deleteByUser(user);
    }
}
