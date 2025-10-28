package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.entity.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import com.javaguy.nhxserver.model.dto.AssetDto;
import com.javaguy.nhxserver.model.dto.PortfolioDto;
import com.javaguy.nhxserver.model.dto.PortfolioSnapshotDto;
import com.javaguy.nhxserver.model.dto.TransactionDto;
import com.javaguy.nhxserver.model.dto.ProductAccessDto;
import com.javaguy.nhxserver.model.dto.PaymentMethodDto;
import com.javaguy.nhxserver.model.dto.PaymentTransactionDto;

public record UserResponse(
        Long userId,
        String email,
        String username,
        String fullName,
        String phoneNumber,
        String profileImageUrl,
        String walletAddress,
        List<AssetDto> assets,
        List<PortfolioDto> currentPortfolio,
        List<PortfolioSnapshotDto> portfolioHistory,
        List<TransactionDto> transactions,
        List<ProductAccessDto> productAccess,
        List<PaymentMethodDto> paymentMethods,
        List<PaymentTransactionDto> paymentTransactions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public UserResponse(Long userId, String email, String username, String fullName, String phoneNumber, String profileImageUrl, String walletAddress, List<AssetDto> assets, List<PortfolioDto> currentPortfolio, List<PortfolioSnapshotDto> portfolioHistory, List<TransactionDto> transactions, List<ProductAccessDto> productAccess, List<PaymentMethodDto> paymentMethods, List<PaymentTransactionDto> paymentTransactions, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.profileImageUrl = profileImageUrl;
        this.walletAddress = walletAddress;
        this.assets = assets;
        this.currentPortfolio = currentPortfolio;
        this.portfolioHistory = portfolioHistory;
        this.transactions = transactions;
        this.productAccess = productAccess;
        this.paymentMethods = paymentMethods;
        this.paymentTransactions = paymentTransactions;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponse fromUser(User user, List<AssetDto> assets, List<PortfolioDto> currentPortfolio, List<PortfolioSnapshotDto> portfolioHistory, List<TransactionDto> transactions, List<ProductAccessDto> productAccesses, List<PaymentMethodDto> paymentMethods, List<PaymentTransactionDto> paymentTransactions) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getProfileImageUrl(),
                user.getWalletAddress(),
                assets,
                currentPortfolio,
                portfolioHistory,
                transactions,
                productAccesses,
                paymentMethods,
                paymentTransactions,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
