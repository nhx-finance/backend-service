package com.javaguy.nhxserver.scheduler;

import com.hedera.hashgraph.sdk.AccountId;
import com.javaguy.nhxserver.event.LowTreasuryBalanceEvent;
import com.javaguy.nhxserver.model.entity.Transaction;
import com.javaguy.nhxserver.model.enums.TransactionStatus;
import com.javaguy.nhxserver.repository.TransactionRepository;
import com.javaguy.nhxserver.service.PortfolioService;
import com.javaguy.nhxserver.service.hedera.HederaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Scheduled jobs for background processing and monitoring
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobs {

    private final PortfolioService portfolioService;
    private final HederaService hederaService;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${limits.treasury-min-balance:100.00}")
    private BigDecimal treasuryMinBalance;

    @Value("${exchange.default-rate:10.00}")
    private BigDecimal exchangeRate;

    /**
     * Generate portfolio snapshots every 6 hours
     */
    @Scheduled(cron = "0 0 */6 * * *") // Every 6 hours
    public void generatePortfolioSnapshots() {
        log.info("Starting scheduled portfolio snapshot generation");
        
        try {
            portfolioService.generateAllSnapshots(exchangeRate);
            log.info("Portfolio snapshot generation completed successfully");
        } catch (Exception e) {
            log.error("Error during portfolio snapshot generation: {}", e.getMessage(), e);
        }
    }

    /**
     * Monitor treasury balance every 10 minutes
     */
    @Scheduled(fixedDelay = 600000) // Every 10 minutes
    public void monitorTreasuryBalance() {
        log.debug("Monitoring treasury balance");
        
        try {
            AccountId treasuryId = AccountId.fromString(hederaService.getTreasuryAccountId());
            
            // Check nhSAF balance
            long nhsafBalance = hederaService.getTokenBalance(
                    treasuryId,
                    com.hedera.hashgraph.sdk.TokenId.fromString(hederaService.getNhsafTokenId())
            );
            
            BigDecimal nhsafBalanceDecimal = new BigDecimal(nhsafBalance).divide(new BigDecimal("100"));
            
            if (nhsafBalanceDecimal.compareTo(treasuryMinBalance) < 0) {
                log.warn("Treasury nhSAF balance is low: {}", nhsafBalanceDecimal);
                
                eventPublisher.publishEvent(new LowTreasuryBalanceEvent(
                        this,
                        hederaService.getNhsafTokenId(),
                        nhsafBalanceDecimal,
                        treasuryMinBalance,
                        LocalDateTime.now()
                ));
            }
            
            // Check USDC balance
            long usdcBalance = hederaService.getTokenBalance(
                    treasuryId,
                    com.hedera.hashgraph.sdk.TokenId.fromString(hederaService.getUsdcTokenId())
            );
            
            BigDecimal usdcBalanceDecimal = new BigDecimal(usdcBalance).divide(new BigDecimal("1000000"));
            BigDecimal usdcMinBalance = treasuryMinBalance.multiply(exchangeRate);
            
            if (usdcBalanceDecimal.compareTo(usdcMinBalance) < 0) {
                log.warn("Treasury USDC balance is low: {}", usdcBalanceDecimal);
                
                eventPublisher.publishEvent(new LowTreasuryBalanceEvent(
                        this,
                        hederaService.getUsdcTokenId(),
                        usdcBalanceDecimal,
                        usdcMinBalance,
                        LocalDateTime.now()
                ));
            }
            
        } catch (Exception e) {
            log.error("Error monitoring treasury balance: {}", e.getMessage(), e);
        }
    }

    /**
     * Check for stuck transactions every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void monitorStuckTransactions() {
        log.debug("Checking for stuck transactions");
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
            
            List<TransactionStatus> pendingStatuses = Arrays.asList(
                    TransactionStatus.INITIATED,
                    TransactionStatus.PROCESSING,
                    TransactionStatus.PAYMENT_PENDING,
                    TransactionStatus.AWAITING_TOKENS,
                    TransactionStatus.TRANSFERRING_TOKENS
            );
            
            List<Transaction> stuckTransactions = transactionRepository
                    .findStuckTransactions(pendingStatuses, cutoffTime);
            
            if (!stuckTransactions.isEmpty()) {
                log.warn("Found {} stuck transactions", stuckTransactions.size());
                
                for (Transaction transaction : stuckTransactions) {
                    log.warn("Stuck transaction detected - ID: {}, Status: {}, Created: {}", 
                            transaction.getId(),
                            transaction.getStatus(),
                            transaction.getCreatedAt());
                    
                    // Mark as expired
                    transaction.setStatus(TransactionStatus.EXPIRED);
                    transaction.setUpdatedAt(LocalDateTime.now());
                    transactionRepository.save(transaction);
                }
            }
            
        } catch (Exception e) {
            log.error("Error monitoring stuck transactions: {}", e.getMessage(), e);
        }
    }

    /**
     * Daily reconciliation at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void dailyReconciliation() {
        log.info("Starting daily reconciliation");
        
        try {
            // Get all completed transactions from last 24 hours
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            
            List<Transaction> completedTransactions = transactionRepository
                    .findByStatus(TransactionStatus.COMPLETED)
                    .stream()
                    .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().isAfter(yesterday)) // Filter by completedAt
                    .toList();
            
            log.info("Reconciling {} completed transactions from last 24 hours", 
                    completedTransactions.size());
            
            // Calculate total volumes
            BigDecimal totalPurchases = completedTransactions.stream()
                    .filter(t -> t.getType() == com.javaguy.nhxserver.model.enums.TransactionType.PURCHASE)
                    .map(Transaction::getAmountUsdc)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalSales = completedTransactions.stream()
                    .filter(t -> t.getType() == com.javaguy.nhxserver.model.enums.TransactionType.SALE)
                    .map(Transaction::getAmountUsdc)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            log.info("Daily reconciliation complete - Purchases: {} USDC, Sales: {} USDC", 
                    totalPurchases, totalSales);
            
        } catch (Exception e) {
            log.error("Error during daily reconciliation: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleanup old snapshots monthly
     */
    @Scheduled(cron = "0 0 3 1 * *") // First day of month at 3 AM
    public void cleanupOldSnapshots() {
        log.info("Starting cleanup of old portfolio snapshots");
        
        try {
            // Keep snapshots for last 90 days only
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
            portfolioService.deleteSnapshotsBefore(cutoffDate); // Use the new method
            log.info("Cleanup completed - removed snapshots older than {}", cutoffDate);
            
        } catch (Exception e) {
            log.error("Error during snapshot cleanup: {}", e.getMessage(), e);
        }
    }
}
