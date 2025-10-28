package com.javaguy.nhxserver.model.enums;

/**
 * Transaction types
 */
public enum TransactionType {
    PURCHASE, // Buy nhSAF with USDC
    SALE, // Sell nhSAF for USDC
    TOKEN_TRANSFER // Transfer tokens after frontend burn
}
