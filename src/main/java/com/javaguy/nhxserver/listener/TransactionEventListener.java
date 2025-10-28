package com.javaguy.nhxserver.listener;

import com.javaguy.nhxserver.event.*;
import com.javaguy.nhxserver.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listeners for handling transaction events
 * Decouples transaction processing from business logic
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    // private final PortfolioService portfolioService; // Removed unused field
    private final NotificationService notificationService;

    /**
     * Handle token transfer completion
     * Updates user portfolio and sends notification
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleTokenTransferCompleted(TokenTransferCompletedEvent event) {
        log.info("Handling TokenTransferCompletedEvent for transaction: {}", event.getTransactionId());
        
        try {
            // Portfolio is already updated in TransactionService
            // Send notification to user
            notificationService.sendTransactionCompletedNotification(
                    event.getTransactionId(),
                    "Purchase completed successfully",
                    String.format("You have received %.2f nhSAF tokens", event.getTokenAmount())
            );
            
            log.info("Successfully processed token transfer completion for transaction: {}", 
                    event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Error handling token transfer completion: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't affect transaction
        }
    }

    /**
     * Handle tokens received by treasury
     * Triggers USDC disbursement for sales
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleTokensReceived(TokensReceivedEvent event) {
        log.info("Handling TokensReceivedEvent for transaction: {}", event.getTransactionId());
        
        try {
            // Portfolio is already updated in TransactionService
            // Send notification to user
            notificationService.sendTransactionCompletedNotification(
                    event.getTransactionId(),
                    "Sale completed successfully",
                    String.format("You have sold %.2f nhSAF tokens", event.getTokenAmount())
            );
            
            log.info("Successfully processed tokens received for transaction: {}", 
                    event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Error handling tokens received: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't affect transaction
        }
    }

    /**
     * Handle transaction failures
     * Logs error and sends notification
     */
    @EventListener
    @Async
    public void handleTransactionFailed(TransactionFailedEvent event) {
        log.error("Transaction failed - ID: {}, Reason: {}, Stage: {}", 
                event.getTransactionId(), 
                event.getReason(), 
                event.getFailureStage());
        
        try {
            // Send failure notification
            notificationService.sendTransactionFailedNotification(
                    event.getTransactionId(),
                    event.getReason(),
                    event.getFailureStage()
            );
            
        } catch (Exception e) {
            log.error("Error sending failure notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle portfolio updates
     * Used for manual portfolio adjustments
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePortfolioUpdate(PortfolioUpdateEvent event) {
        log.info("Handling PortfolioUpdateEvent for user: {}", event.getUserId());
        
        try {
            // Portfolio update is handled in the service that publishes the event
            log.info("Portfolio update processed for user: {}", event.getUserId());
            
        } catch (Exception e) {
            log.error("Error handling portfolio update: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle low treasury balance alerts
     * Sends alert to admin
     */
    @EventListener
    @Async
    public void handleLowTreasuryBalance(LowTreasuryBalanceEvent event) {
        log.warn("Low treasury balance detected - Token: {}, Balance: {}, Threshold: {}", 
                event.getTokenId(), 
                event.getCurrentBalance(), 
                event.getThreshold());
        
        try {
            // Send alert to admin
            notificationService.sendAdminAlert(
                    "Low Treasury Balance",
                    String.format("Treasury balance for token %s is low: %.2f (threshold: %.2f)",
                            event.getTokenId(),
                            event.getCurrentBalance(),
                            event.getThreshold())
            );
            
        } catch (Exception e) {
            log.error("Error sending low balance alert: {}", e.getMessage(), e);
        }
    }
}
