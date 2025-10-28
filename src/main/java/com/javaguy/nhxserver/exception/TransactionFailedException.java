package com.javaguy.nhxserver.exception;

import java.util.UUID;

public class TransactionFailedException extends RuntimeException {
    private final UUID transactionId;

    public TransactionFailedException(String message, UUID transactionId) {
        super(message);
        this.transactionId = transactionId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }
}