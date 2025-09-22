package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.model.entity.KycStatus;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Transactional
    public void updateKycStatus(Long userId, KycStatus status) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setKycStatus(status);
            if (status == KycStatus.APPROVED) {
                user.setEnabled(true);
            }
            userRepository.save(user);
        });
    }
}
