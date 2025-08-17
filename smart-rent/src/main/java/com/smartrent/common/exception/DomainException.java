package com.smartrent.common.exception;

public class DomainException extends RuntimeException {

    private final DomainCode domainCode;

    public DomainException(DomainCode domainCode, Object... args) {
        super(domainCode.getMessage());
        this.domainCode = domainCode;
    }

    public DomainCode getDomainCode() {
        return this.domainCode;
    }
}
