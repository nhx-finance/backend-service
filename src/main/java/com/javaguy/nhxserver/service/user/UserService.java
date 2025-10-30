package com.javaguy.nhxserver.service.user;

import com.javaguy.nhxserver.exception.UserNotFound;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.repository.UserRepository;
import com.javaguy.nhxserver.service.user.api.UserUseCase;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.stereotype.Service;
import com.javaguy.nhxserver.model.dto.UpdateUserRequest;
import com.javaguy.nhxserver.exception.ResourceNotFoundException;
import com.javaguy.nhxserver.exception.UserAlreadyExistsException;
import com.javaguy.nhxserver.model.dto.RegisterRequest;
import com.javaguy.nhxserver.repository.RoleRepository;
import com.javaguy.nhxserver.model.enums.ERole;
import com.javaguy.nhxserver.model.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.javaguy.nhxserver.model.dto.MessageResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import com.javaguy.nhxserver.exception.ApiException;
import org.springframework.http.HttpStatus;
import com.javaguy.nhxserver.model.dto.WalletResponse;
import com.javaguy.nhxserver.model.dto.KycSubmissionDto;
import com.javaguy.nhxserver.model.entity.Asset;
import com.javaguy.nhxserver.repository.AssetRepository;
import com.javaguy.nhxserver.model.enums.KycStatus;

import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AssetRepository assetRepository;

    @Transactional(readOnly = true)
        public Optional<User> findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFound("User not found with username: " + username));
        return Optional.of(user);
    }

    @Transactional(readOnly = true)
        public Optional<User> findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFound("User not found with email: " + email));
        return Optional.of(user);
    }
    @Transactional(readOnly = true)
        public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Transactional(readOnly = true)
        public List<Asset> getAssetsByUserId(Long userId) {
        return assetRepository.findByUserUserId(userId);
    }

    @Transactional
    public MessageResponse updateKycStatusToCompleted(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        user.setKycStatus(KycStatus.COMPLETED);
        userRepository.save(user);

        return new MessageResponse("KYC status updated to COMPLETED successfully");
    }

    @Transactional(readOnly = true)
    public KycStatus getKycStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        return user.getKycStatus(); 
    }

    @Transactional
    public MessageResponse submitKyc(Long userId, KycSubmissionDto kycDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        user.setFullName(kycDto.fullName());
        user.setPhoneNumber(kycDto.phoneNumber());
        user.setKycStatus(KycStatus.PENDING); // Set to pending on submission
        userRepository.save(user);

        return new MessageResponse("KYC submitted successfully", Map.of("status", KycStatus.PENDING.toString()));
    }

    private static final Pattern ETHEREUM_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    @Transactional
    public MessageResponse setWalletAddress(Long userId, String walletAddress, String walletName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (!ETHEREUM_ADDRESS_PATTERN.matcher(walletAddress).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "InvalidWalletAddress", "Invalid Ethereum wallet address format.");
        }

        if (user.getWalletAddress() != null && !user.getWalletAddress().isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "WalletAddressAlreadySet", "Wallet address has already been set and cannot be changed.");
        }

        user.setWalletAddress(walletAddress);
        user.setWalletName(walletName); // Set the wallet name
        userRepository.save(user);
        return new MessageResponse("Wallet address set successfully", user.getUpdatedAt());
    }

    @Transactional(readOnly = true)
        public WalletResponse getWalletAddress(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (user.getWalletAddress() == null || user.getWalletAddress().isEmpty()) {
            throw new ResourceNotFoundException("Wallet address not set for user with id " + userId);
        }
        return new WalletResponse(user.getWalletAddress(), user.getWalletName());
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        User user = User.builder()
                .email(request.email())
                .username(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName("")
                .phoneNumber("")
                .profileImageUrl(null)
                .walletAddress(null)
                .walletName(null)
                .build();

        // Assign default role
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role is not found."));
        roles.add(userRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Transactional
    public MessageResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new UserAlreadyExistsException("Username is already taken");
            }
            user.setUsername(request.username());
        }

        userRepository.save(user);
        return new MessageResponse("Profile updated successfully", user.getUpdatedAt());
    }

    @Transactional
    public void updateProfileImage(Long userId, String newImageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        user.setProfileImageUrl(newImageUrl);
        userRepository.save(user);
    }
}
