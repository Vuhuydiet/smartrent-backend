package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

/**
 * Exception thrown when OTP is not found or expired
 */
public class OtpNotFoundException extends OtpException {

    public OtpNotFoundException() {
        super(DomainCode.OTP_NOT_FOUND);
    }
}

