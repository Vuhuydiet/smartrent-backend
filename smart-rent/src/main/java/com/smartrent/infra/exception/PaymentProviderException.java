package com.smartrent.infra.exception;

public class PaymentProviderException extends RuntimeException {

    public PaymentProviderException(String message) {
        super(message);
    }

    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public static PaymentProviderException providerNotFound(String providerName) {
        return new PaymentProviderException("Payment provider not found: " + providerName);
    }

    public static PaymentProviderException invalidConfiguration(String providerName) {
        return new PaymentProviderException("Invalid configuration for payment provider: " + providerName);
    }

    public static PaymentProviderException providerUnavailable(String providerName) {
        return new PaymentProviderException("Payment provider is unavailable: " + providerName);
    }

    public static PaymentProviderException invalidSignature() {
        return new PaymentProviderException("Invalid payment signature");
    }

    public static PaymentProviderException paymentFailed(String reason) {
        return new PaymentProviderException("Payment failed: " + reason);
    }

    public static PaymentProviderException operationNotSupported(String operation, String providerName) {
        return new PaymentProviderException("Operation '" + operation + "' is not supported by provider: " + providerName);
    }
}

