package com.javaguy.nhxserver.service;

import com.hedera.hashgraph.sdk.AccountId;
import com.javaguy.nhxserver.event.*;
import com.javaguy.nhxserver.exception.ApiException;
import com.javaguy.nhxserver.model.dto.HederaTransactionResponse;
import com.javaguy.nhxserver.model.entity.Transaction;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.model.enums.TransactionStatus;
import com.javaguy.nhxserver.model.enums.TransactionType;
import com.javaguy.nhxserver.repository.TransactionRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import com.javaguy.nhxserver.service.hedera.HederaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final HederaService hederaService;
    private final ApplicationEventPublisher eventPublisher;
    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    private static final int USDC_DECIMALS = 6;

    private static final Map<String, Integer> TOKEN_DECIMALS = Map.of(
            "0.0.1234", 6,
            "0.0.5678", 6
    );


    /**
     * Initiate a sale of tokens for USDC
     * 
     * @param userId          User initiating the sale
     * @param tokenId         Token being sold (e.g., "0.0.12345")
     * @param tokenAmount     Amount of token to sell (decimal string)
     * @param usdcAmount      Amount of USDC to receive (decimal string)
     * @param hederaAccountId User's Hedera account to receive USDC
     */
    @Transactional
    public Transaction initiateSale(
            Long userId,
            String tokenId,
            String tokenAmount,
            String usdcAmount,
            String hederaAccountId) {

        log.info("Initiating sale for user {} - Token: {}, Amount: {}, USDC: {}",
                userId, tokenId, tokenAmount, usdcAmount);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "UserNotFound", "User not found with ID: " + userId));

            // Validate inputs
            validateSaleRequest(tokenAmount, usdcAmount, hederaAccountId);

            // Convert decimal amounts to smallest units
            int tokenDecimals = TOKEN_DECIMALS.getOrDefault(tokenId, 2); // Default to 2 decimals
            BigDecimal tokenDecimal = new BigDecimal(tokenAmount);
            long tokenSmallestUnits = tokenDecimal.multiply(BigDecimal.TEN.pow(tokenDecimals)).longValue();

            BigDecimal usdcDecimal = new BigDecimal(usdcAmount);
            long usdcSmallestUnits = usdcDecimal.multiply(BigDecimal.TEN.pow(USDC_DECIMALS)).longValue();

            // Create transaction record
            Transaction transaction = Transaction.builder()
                    .user(user)
                    .type(TransactionType.SALE)
                    .status(TransactionStatus.INITIATED)
                    .amountUsdc(usdcDecimal)
                    .tokenAmount(tokenDecimal)
                    .hederaAccountId(hederaAccountId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            transaction = transactionRepository.save(transaction);
            log.info("Created transaction record: {}", transaction.getId());

            // Update status to processing
            updateTransactionStatus(transaction.getId(), TransactionStatus.PROCESSING);

            // Execute Hedera burn and USDC transfer
            HederaTransactionResponse hederaResponse = hederaService.sellTokens(
                    userId,
                    tokenId,
                    tokenSmallestUnits,
                    hederaAccountId,
                    usdcSmallestUnits);

            if (hederaResponse.isSuccess()) {
                // Update transaction with Hedera transaction ID
                transaction.setHederaTransactionId(hederaResponse.getTransactionId());
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                transaction = transactionRepository.save(transaction);

                log.info("Sale completed successfully: {}", transaction.getId());

                // Publish event for portfolio update
                eventPublisher.publishEvent(new TokensReceivedEvent(
                        this,
                        transaction.getId(),
                        hederaResponse.getTransactionId(),
                        hederaAccountId,
                        tokenDecimal, // amount in token decimal
                        LocalDateTime.now()));

                // Update portfolio (negative amount to decrease balance)
                portfolioService.updatePortfolio(
                        userId,
                        tokenId,
                        tokenDecimal.negate(),
                        TransactionType.SALE);

                return transaction;
            } else {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "TransactionFailed",
                        "Hedera transaction failed: " + hederaResponse.getMessage());
            }

        } catch (Exception e) {
            log.error("Error during sale initiation: {}", e.getMessage(), e);
            throw handleTransactionError(e);
        }
    }

    /**
     * Get transaction by ID
     */
    public Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "TransactionNotFound",
                        "Transaction not found with ID: " + transactionId));
    }

    /**
     * Get user transactions
     */
    public List<Transaction> getUserTransactions(
            Long userId,
            TransactionStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        if (status != null) {
            return transactionRepository.findByUserUserIdAndStatus(userId, status);
        } else if (startDate != null && endDate != null) {
            return transactionRepository.findByUserUserIdAndCreatedAtBetween(userId, startDate, endDate);
        } else {
            return transactionRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        }
    }

    /**
     * Update transaction status
     */
    @Transactional
    public void updateTransactionStatus(Long transactionId, TransactionStatus newStatus) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "TransactionNotFound", "Transaction not found with ID: " + transactionId));

        TransactionStatus oldStatus = transaction.getStatus();
        transaction.setStatus(newStatus);
        transaction.setUpdatedAt(LocalDateTime.now());

        if (newStatus == TransactionStatus.COMPLETED) {
            transaction.setCompletedAt(LocalDateTime.now());
        }

        transaction = transactionRepository.save(transaction);

        log.info("Transaction {} status updated: {} -> {}",
                transaction.getId(), oldStatus, newStatus);

    }

    private void validateSaleRequest(String tokenAmount, String usdcAmount, String hederaAccountId) {
        if (tokenAmount == null || tokenAmount.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "InvalidInput",
                    "Token amount is required");
        }

        if (usdcAmount == null || usdcAmount.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "InvalidInput",
                    "USDC amount is required");
        }

        try {
            BigDecimal tokenDecimal = new BigDecimal(tokenAmount);
            if (tokenDecimal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "InvalidInput",
                        "Token amount must be greater than 0");
            }

            BigDecimal usdcDecimal = new BigDecimal(usdcAmount);
            if (usdcDecimal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "InvalidInput",
                        "USDC amount must be greater than 0");
            }
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "InvalidInput",
                    "Invalid number format for amount");
        }

        if (!AccountId.fromString(hederaAccountId).toString().equals(hederaAccountId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "InvalidInput",
                    "Invalid Hedera account ID format");
        }
    }

    private ApiException handleTransactionError(Exception e) {
        if (e instanceof ApiException) {
            return (ApiException) e;
        }

        log.error("Unexpected error in transaction: {}", e.getMessage(), e);
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                "TransactionError",
                "An error occurred while processing the transaction: " + e.getMessage());
    }
}
