package com.smartrent.exception;

/**
 * Exception thrown when a payment fails
 */
public class PaymentFailedException extends RuntimeException {

    private final String transactionId;
    private final String providerTransactionId;
    private final String responseCode;
    private final String reason;

    public PaymentFailedException(String transactionId, String reason) {
        super(String.format("Payment failed for transaction %s: %s", transactionId, reason));
        this.transactionId = transactionId;
        this.providerTransactionId = null;
        this.responseCode = null;
        this.reason = reason;
    }

    public PaymentFailedException(String transactionId, String providerTransactionId, String responseCode, String reason) {
        super(String.format("Payment failed for transaction %s (provider: %s, code: %s): %s", 
                transactionId, providerTransactionId, responseCode, reason));
        this.transactionId = transactionId;
        this.providerTransactionId = providerTransactionId;
        this.responseCode = responseCode;
        this.reason = reason;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getReason() {
        return reason;
    }
}

