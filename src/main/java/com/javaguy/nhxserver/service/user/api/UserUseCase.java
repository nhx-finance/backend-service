package com.javaguy.nhxserver.service.user.api;

import com.javaguy.nhxserver.model.dto.*;
import com.javaguy.nhxserver.model.entity.Asset;
import com.javaguy.nhxserver.model.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserUseCase {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long userId);
    List<Asset> getAssetsByUserId(Long userId);

    MessageResponse submitKyc(Long userId, KycSubmissionDto kycDto);
    MessageResponse setWalletAddress(Long userId, String walletAddress, String walletName);
    WalletResponse getWalletAddress(Long userId);

    User registerUser(RegisterRequest request);
    MessageResponse updateUser(Long userId, UpdateUserRequest request);
    void updateProfileImage(Long userId, String newImageUrl);
}
