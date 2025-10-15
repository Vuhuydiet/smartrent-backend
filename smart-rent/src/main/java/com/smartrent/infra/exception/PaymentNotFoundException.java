package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

public class PaymentNotFoundException extends DomainException {

    public PaymentNotFoundException(String transactionRef) {
        super(DomainCode.PAYMENT_NOT_FOUND, transactionRef);
    }
}
