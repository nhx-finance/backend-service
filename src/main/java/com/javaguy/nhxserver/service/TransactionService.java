package com.javaguy.nhxserver.service;

import com.hedera.hashgraph.sdk.AccountId;
import com.javaguy.nhxserver.event.*;
import com.javaguy.nhxserver.exception.ApiException;
import com.javaguy.nhxserver.model.dto.HederaTransactionResponse;
import com.javaguy.nhxserver.model.dto.SellRequestDto;
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
     * @param request         SellRequestDto containing token details
     */
    @Transactional
    public Transaction initiateSale(
            Long userId, SellRequestDto request) {

        log.info("Initiating sale for user {} - Token: {}, Amount: {}, USDC: {}",
                userId, request.tokenSymbol(), request.amountToBurn(), request.amountUsdcToSend());

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "UserNotFound", "User not found with ID: " + userId));

            // Validate inputs (now using DTO values)
            validateSaleRequest(request);

            // Convert string amounts to long
            long tokenSmallestUnits;
            long usdcSmallestUnits;
            try {
                tokenSmallestUnits = Long.parseLong(request.amountToBurn());
                usdcSmallestUnits = Long.parseLong(request.amountUsdcToSend());
            } catch (NumberFormatException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "InvalidInput", "Invalid number format for amount: " + e.getMessage());
            }

            BigDecimal tokenDecimal = BigDecimal.valueOf(tokenSmallestUnits).movePointLeft(TOKEN_DECIMALS.getOrDefault(request.tokenSymbol(), 2));
            BigDecimal usdcDecimal = BigDecimal.valueOf(usdcSmallestUnits).movePointLeft(USDC_DECIMALS);

            // Create transaction record
            Transaction transaction = Transaction.builder()
                    .user(user)
                    .type(TransactionType.SALE)
                    .status(TransactionStatus.INITIATED)
                    .amountUsdc(usdcDecimal)
                    .tokenAmount(tokenDecimal)
                    .hederaAccountId(request.recipientAccountIdStr())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            transaction = transactionRepository.save(transaction);
            log.info("Created transaction record: {}", transaction.getId());

            // Update status to processing
            updateTransactionStatus(transaction.getId(), TransactionStatus.PROCESSING);

            // Execute Hedera burn and USDC transfer
            HederaTransactionResponse hederaResponse = hederaService.sellTokens(
                    userId, request);

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
                        request.recipientAccountIdStr(),
                        tokenDecimal, // amount in token decimal
                        LocalDateTime.now()));

                // Update portfolio (negative amount to decrease balance)
                portfolioService.updatePortfolio(
                        userId,
                        request.tokenSymbol(),
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

    private void validateSaleRequest(SellRequestDto request) {
        if (request.tokenSymbol() == null || request.tokenSymbol().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "InvalidInput",
                    "Token symbol is required");
        }

        long amountToBurn;
        long amountUsdcToSend;
        try {
            amountToBurn = Long.parseLong(request.amountToBurn());
            amountUsdcToSend = Long.parseLong(request.amountUsdcToSend());
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "InvalidInput", "Invalid number format for amount: " + e.getMessage());
        }

        if (amountToBurn <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "InvalidInput",
                    "Token amount to burn must be greater than 0");
        }

        if (amountUsdcToSend <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "InvalidInput",
                    "USDC amount to send must be greater than 0");
        }

        if (!AccountId.fromString(request.recipientAccountIdStr()).toString().equals(request.recipientAccountIdStr())) {
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
