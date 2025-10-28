package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.exception.ApiException;
import com.javaguy.nhxserver.model.dto.PortfolioPerformance;
import com.javaguy.nhxserver.model.dto.PortfolioPerformanceDto;
import com.javaguy.nhxserver.model.entity.Portfolio;
import com.javaguy.nhxserver.model.entity.PortfolioSnapshot;
import com.javaguy.nhxserver.model.enums.TransactionType;
import com.javaguy.nhxserver.repository.PortfolioRepository;
import com.javaguy.nhxserver.repository.PortfolioSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioSnapshotRepository snapshotRepository;

    /**
     * Update user portfolio after a transaction
     */
    @Transactional
    public Portfolio updatePortfolio(
            Long userId, // Changed to Long
            String tokenId,
            BigDecimal amount,
            TransactionType transactionType) {
        
        log.info("Updating portfolio for user: {} - Token: {}, Amount: {}, Type: {}", 
                userId, tokenId, amount, transactionType);

        try {
            // Find or create portfolio entry
            Portfolio portfolio = portfolioRepository
                    .findByUserIdAndTokenId(userId, tokenId)
                    .orElseGet(() -> {
                        Portfolio newPortfolio = Portfolio.builder()
                                .id(null) // Changed to null for @GeneratedValue
                                .userId(userId)
                                .tokenId(tokenId)
                                .balance(BigDecimal.ZERO)
                                .lastUpdated(LocalDateTime.now())
                                .build();
                        return portfolioRepository.save(newPortfolio);
                    });

            // Update balance based on transaction type
            BigDecimal newBalance;
            if (transactionType == TransactionType.PURCHASE) {
                newBalance = portfolio.getBalance().add(amount);
            } else if (transactionType == TransactionType.SALE) {
                newBalance = portfolio.getBalance().subtract(amount);
                
                // Validate sufficient balance
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "InsufficientBalance",
                            "Insufficient token balance for sale");
                }
            } else {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "InvalidTransactionType",
                        "Invalid transaction type for portfolio update");
            }

            portfolio.setBalance(newBalance);
            portfolio.setLastUpdated(LocalDateTime.now());
            
            portfolio = portfolioRepository.save(portfolio);
            
            log.info("Portfolio updated successfully - New balance: {}", newBalance);
            
            return portfolio;
            
        } catch (Exception e) {
            log.error("Error updating portfolio: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get user's current portfolio
     */
    public List<Portfolio> getCurrentPortfolio(Long userId) {
        return portfolioRepository.findByUserId(userId);
    }

    /**
     * Get portfolio for specific token
     */
    public Portfolio getPortfolioByToken(Long userId, String tokenId) {
        return portfolioRepository
                .findByUserIdAndTokenId(userId, tokenId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "PortfolioNotFound",
                        "No portfolio entry found for this token"));
    }

    /**
     * Generate portfolio snapshot for user
     */
    @Transactional
    public PortfolioSnapshot generateSnapshot(Long userId, BigDecimal exchangeRate) { // Changed to Long
        log.info("Generating portfolio snapshot for user: {}", userId);

        try {
            List<Portfolio> portfolios = getCurrentPortfolio(userId);
            
            // Calculate total value in USDC
            BigDecimal totalValueUsdc = portfolios.stream()
                    .map(p -> p.getBalance().multiply(exchangeRate))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                    .userId(userId)
                    .snapshotTime(LocalDateTime.now())
                    .totalValueUsdc(totalValueUsdc)
                    .exchangeRate(exchangeRate)
                    .build();

            snapshot = snapshotRepository.save(snapshot);
            
            log.info("Portfolio snapshot created: {} - Total value: {} USDC", 
                    snapshot.getId(), totalValueUsdc);
            
            return snapshot;
            
        } catch (Exception e) {
            log.error("Error generating portfolio snapshot: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "SnapshotGenerationFailed",
                    "Failed to generate portfolio snapshot: " + e.getMessage());
        }
    }

    /**
     * Get portfolio history (snapshots) for a user
     */
    public List<PortfolioSnapshot> getPortfolioHistory(
            Long userId, // Changed to Long
            LocalDateTime startDate,
            LocalDateTime endDate) {
        
        if (startDate != null && endDate != null) {
            return snapshotRepository
                    .findByUserIdAndSnapshotTimeBetweenOrderBySnapshotTimeDesc(
                            userId, startDate, endDate);
        } else {
            return snapshotRepository
                    .findByUserIdOrderBySnapshotTimeDesc(userId);
        }
    }

    /**
     * Get latest snapshot for user
     */
    public PortfolioSnapshot getLatestSnapshot(Long userId) {
        return snapshotRepository
                .findTopByUserIdOrderBySnapshotTimeDesc(userId)
                .orElse(null);
    }

    /**
     * Generate snapshots for all users
     * Called by scheduled job
     */
    @Transactional
    public void generateAllSnapshots(BigDecimal exchangeRate) {
        log.info("Generating snapshots for all users");

        try {
            List<Long> userIds = portfolioRepository.findDistinctUserIds(); // Changed to Long
            
            int successCount = 0;
            int failCount = 0;
            
            for (Long userId : userIds) { // Changed to Long
                try {
                    generateSnapshot(userId, exchangeRate);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to generate snapshot for user {}: {}", 
                            userId, e.getMessage());
                    failCount++;
                }
            }
            
            log.info("Snapshot generation completed - Success: {}, Failed: {}", 
                    successCount, failCount);
            
        } catch (Exception e) {
            log.error("Error in bulk snapshot generation: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate portfolio performance
     */
    public PortfolioPerformanceDto calculatePerformance(Long userId) {
        List<PortfolioSnapshot> snapshots = snapshotRepository
                .findByUserIdOrderBySnapshotTimeAsc(userId);

        if (snapshots.isEmpty()) {
            return PortfolioPerformanceDto.builder()
                    .currentValue(BigDecimal.ZERO)
                    .totalGainLoss(BigDecimal.ZERO)
                    .percentageChange(BigDecimal.ZERO)
                    .build();
        }

        PortfolioSnapshot oldest = snapshots.get(0);
        PortfolioSnapshot latest = snapshots.get(snapshots.size() - 1);

        BigDecimal currentValue = latest.getTotalValueUsdc();
        BigDecimal initialValue = oldest.getTotalValueUsdc();
        BigDecimal gainLoss = currentValue.subtract(initialValue);
        
        BigDecimal percentageChange = BigDecimal.ZERO;
        if (initialValue.compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = gainLoss
                    .divide(initialValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return com.javaguy.nhxserver.model.dto.PortfolioPerformanceDto.builder()
                .currentValue(currentValue)
                .initialValue(initialValue)
                .totalGainLoss(gainLoss)
                .percentageChange(percentageChange)
                .snapshotCount(snapshots.size())
                .firstSnapshotDate(oldest.getSnapshotTime())
                .latestSnapshotDate(latest.getSnapshotTime())
                .build();
    }

    /**
     * Delete snapshots before a specific cutoff time.
     */
    @Transactional
    public void deleteSnapshotsBefore(LocalDateTime cutoffTime) {
        log.info("Deleting portfolio snapshots before: {}", cutoffTime);
        snapshotRepository.deleteBySnapshotTimeBefore(cutoffTime);
    }
}
