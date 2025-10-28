package com.javaguy.nhxserver.service.user;

import com.javaguy.nhxserver.exception.ResourceNotFoundException;
import com.javaguy.nhxserver.model.dto.*;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.service.PaymentMethodService;
import com.javaguy.nhxserver.service.PaymentTransactionService;
import com.javaguy.nhxserver.service.PortfolioService;
import com.javaguy.nhxserver.service.ProductAccessService;
import com.javaguy.nhxserver.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.javaguy.nhxserver.model.enums.TransactionType;

@Service
@RequiredArgsConstructor
public class UserProfileQueryService {

    private final com.javaguy.nhxserver.service.user.UserService userService;
    private final PortfolioService portfolioService;
    private final TransactionService transactionService;
    private final ProductAccessService productAccessService;
    private final PaymentMethodService paymentMethodService;
    private final PaymentTransactionService paymentTransactionService;

    @Transactional(readOnly = true)
    public UserResponse buildUserProfile(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        List<AssetDto> assets = userService.getAssetsByUserId(userId).stream()
                .map(AssetDto::fromEntity)
                .collect(Collectors.toList());
        List<TransactionDto> transactions = transactionService.getUserTransactions(userId, null, null, null).stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
        List<ProductAccessDto> productAccesses = productAccessService.getProductAccessByUserId(userId).stream()
                .map(ProductAccessDto::fromEntity)
                .collect(Collectors.toList());
        List<PaymentMethodDto> paymentMethods = paymentMethodService.getPaymentMethodsByUserId(userId).stream()
                .map(PaymentMethodDto::fromEntity)
                .collect(Collectors.toList());
        List<PaymentTransactionDto> paymentTransactions = paymentTransactionService.getPaymentTransactions(userId, null, null, null).stream()
                .map(PaymentTransactionDto::fromEntity)
                .collect(Collectors.toList());

        List<PortfolioDto> currentPortfolio = portfolioService.getCurrentPortfolio(userId).stream()
                .map(PortfolioDto::fromEntity)
                .collect(Collectors.toList());

        List<PortfolioSnapshotDto> portfolioHistory = portfolioService.getPortfolioHistory(userId, null, null).stream()
                .map(PortfolioSnapshotDto::fromEntity)
                .collect(Collectors.toList());

        return UserResponse.fromUser(user, assets, currentPortfolio, portfolioHistory, transactions, productAccesses, paymentMethods, paymentTransactions);
    }
}
