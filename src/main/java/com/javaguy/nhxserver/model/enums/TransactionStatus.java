package com.javaguy.nhxserver.model.enums;

/**
 * Transaction status state machine
 */
public enum TransactionStatus {
    // Common states
    INITIATED,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED,
    
    // Purchase-specific states
    PAYMENT_PENDING,
    PAYMENT_CONFIRMED,
    TRANSFERRING_TOKENS,
    
    // Sale-specific states
    AWAITING_TOKENS,
    TOKENS_RECEIVED,
    DISBURSING_FUNDS,
    
    // Error states
    PAYMENT_FAILED,
    TRANSFER_FAILED,
    TOKENS_NOT_RECEIVED,
    DISBURSEMENT_FAILED
}
