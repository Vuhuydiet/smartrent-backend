package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

/**
 * Exception thrown when OTP rate limit is exceeded
 */
public class OtpRateLimitException extends OtpException {

    public OtpRateLimitException() {
        super(DomainCode.OTP_RATE_LIMIT_EXCEEDED);
    }
}

