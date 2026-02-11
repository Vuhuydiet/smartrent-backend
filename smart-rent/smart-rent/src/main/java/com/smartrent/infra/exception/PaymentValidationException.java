package com.smartrent.infra.exception;

public class PaymentValidationException extends RuntimeException {

    public PaymentValidationException(String message) {
        super(message);
    }

    public PaymentValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static PaymentValidationException invalidAmount() {
        return new PaymentValidationException("Payment amount must be greater than zero");
    }

    public static PaymentValidationException invalidCurrency(String currency) {
        return new PaymentValidationException("Invalid currency: " + currency);
    }

    public static PaymentValidationException missingRequiredField(String fieldName) {
        return new PaymentValidationException("Missing required field: " + fieldName);
    }

    public static PaymentValidationException invalidProvider(String provider) {
        return new PaymentValidationException("Invalid payment provider: " + provider);
    }

    public static PaymentValidationException userNotAuthenticated() {
        return new PaymentValidationException("User is not authenticated");
    }
}

