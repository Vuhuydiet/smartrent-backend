package com.smartrent.infra.exception;

import com.smartrent.infra.exception.model.DomainCode;

/**
 * Exception thrown when a requested resource is not found
 * Generic exception for all resource not found scenarios
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(DomainCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(DomainCode specificCode) {
        super(specificCode);
    }

    public ResourceNotFoundException(DomainCode specificCode, Object... args) {
        super(specificCode, args);
    }
}
