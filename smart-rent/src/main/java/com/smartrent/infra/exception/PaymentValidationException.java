package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class PaymentValidationException extends DomainException {

    public PaymentValidationException(String message) {
        super(DomainCode.PAYMENT_VALIDATION_ERROR, message);
    }

    public PaymentValidationException(DomainCode domainCode, Object... args) {
        super(domainCode, args);
    }

    public static PaymentValidationException invalidAmount(String amount) {
        return new PaymentValidationException(DomainCode.INVALID_PAYMENT_AMOUNT, amount);
    }

    public static PaymentValidationException invalidCurrency(String currency) {
        return new PaymentValidationException(DomainCode.INVALID_CURRENCY, currency);
    }

    public static PaymentValidationException invalidTransactionRef(String transactionRef) {
        return new PaymentValidationException(DomainCode.INVALID_TRANSACTION_REF, transactionRef);
    }

    public static PaymentValidationException userNotAuthenticated() {
        return new PaymentValidationException(DomainCode.USER_NOT_AUTHENTICATED);
    }
}
