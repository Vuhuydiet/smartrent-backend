package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

/**
 * Exception thrown when OTP verification fails
 */
public class OtpVerificationException extends OtpException {

    public OtpVerificationException(DomainCode domainCode) {
        super(domainCode);
    }
}

