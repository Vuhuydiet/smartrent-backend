package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

/**
 * Base exception for OTP-related errors
 */
public class OtpException extends DomainException {

    public OtpException(DomainCode domainCode) {
        super(domainCode);
    }

    public OtpException(DomainCode domainCode, Object... args) {
        super(domainCode, args);
    }
}

