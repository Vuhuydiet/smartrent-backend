package com.smartrent.exception;

/**
 * Exception thrown when payment callback is invalid
 */
public class InvalidPaymentCallbackException extends RuntimeException {

    private final String transactionId;
    private final String reason;

    public InvalidPaymentCallbackException(String transactionId, String reason) {
        super(String.format("Invalid payment callback for transaction %s: %s", transactionId, reason));
        this.transactionId = transactionId;
        this.reason = reason;
    }

    public InvalidPaymentCallbackException(String reason) {
        super(String.format("Invalid payment callback: %s", reason));
        this.transactionId = null;
        this.reason = reason;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getReason() {
        return reason;
    }
}

