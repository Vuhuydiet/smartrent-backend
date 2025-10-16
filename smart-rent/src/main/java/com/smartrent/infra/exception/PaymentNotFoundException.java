package com.smartrent.infra.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String transactionRef) {
        super("Payment not found with transaction reference: " + transactionRef);
    }

    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

