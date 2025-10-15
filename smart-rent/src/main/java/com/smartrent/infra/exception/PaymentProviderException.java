package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class PaymentProviderException extends DomainException {

    public PaymentProviderException(String message) {
        super(DomainCode.PAYMENT_PROVIDER_ERROR, message);
    }

    public PaymentProviderException(DomainCode domainCode, Object... args) {
        super(domainCode, args);
    }

    public static PaymentProviderException providerNotFound(String providerType) {
        return new PaymentProviderException(DomainCode.PAYMENT_PROVIDER_NOT_FOUND, providerType);
    }

    public static PaymentProviderException invalidConfiguration(String providerType) {
        return new PaymentProviderException(DomainCode.PAYMENT_PROVIDER_INVALID_CONFIG, providerType);
    }

    public static PaymentProviderException operationNotSupported(String operation, String providerType) {
        return new PaymentProviderException(DomainCode.PAYMENT_OPERATION_NOT_SUPPORTED, operation, providerType);
    }
}
