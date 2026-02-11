package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;
import lombok.Getter;

/**
 * Generic application exception that wraps domain error codes
 */
@Getter
public class AppException extends RuntimeException {

    private final DomainCode errorCode;

    public AppException(DomainCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(DomainCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(DomainCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}