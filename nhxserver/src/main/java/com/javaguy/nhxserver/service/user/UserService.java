package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.model.entity.KycStatus;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.stereotype.Service;
import com.javaguy.nhxserver.model.dto.UpdateUserRequest;
import com.javaguy.nhxserver.exception.ResourceNotFoundException;
import com.javaguy.nhxserver.exception.UserAlreadyExistsException;
import com.javaguy.nhxserver.model.dto.RegisterRequest;
import com.javaguy.nhxserver.repository.RoleRepository;
import com.javaguy.nhxserver.model.entity.ERole;
import com.javaguy.nhxserver.model.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
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
    public User createUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException("Phone number is already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        user.setEnabled(false);
        user.setEmailVerified(false);

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default user role not found."));
        roles.add(userRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new UserAlreadyExistsException("Username is already taken");
            }
            user.setUsername(request.username());
        }

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new UserAlreadyExistsException("Email is already registered");
            }
            user.setEmail(request.email());
            user.setEmailVerified(false);
            user.setEnabled(false);
        }

        if (request.phoneNumber() != null && !request.phoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
                throw new UserAlreadyExistsException("Phone number is already registered");
            }
            user.setPhoneNumber(request.phoneNumber());
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }

        return userRepository.save(user);
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
    
    @Transactional
    public User updateProfileImage(Long userId, String newImageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        user.setProfileImageUrl(newImageUrl);
        return userRepository.save(user);
    }
}
