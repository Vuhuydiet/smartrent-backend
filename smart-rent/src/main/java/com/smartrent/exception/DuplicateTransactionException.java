package com.smartrent.exception;

/**
 * Exception thrown when attempting to create a duplicate transaction
 */
public class DuplicateTransactionException extends RuntimeException {

    private final String transactionId;
    private final String providerTransactionId;

    public DuplicateTransactionException(String transactionId) {
        super(String.format("Transaction already exists: %s", transactionId));
        this.transactionId = transactionId;
        this.providerTransactionId = null;
    }

    public DuplicateTransactionException(String transactionId, String providerTransactionId) {
        super(String.format("Duplicate transaction detected. Transaction ID: %s, Provider Transaction ID: %s", 
                transactionId, providerTransactionId));
        this.transactionId = transactionId;
        this.providerTransactionId = providerTransactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }
}

