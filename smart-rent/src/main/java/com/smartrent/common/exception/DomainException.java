package com.smartrent.common.exception;

public class DomainException extends RuntimeException {
<<<<<<< HEAD
    
=======

    private final DomainCode domainCode;

    public DomainException(DomainCode domainCode, Object... args) {
        super(domainCode.getMessage());
        this.domainCode = domainCode;
    }

    public DomainCode getDomainCode() {
        return this.domainCode;
    }
>>>>>>> cbf0b5c8b7e5f97ed992c6bb45574048d15cfa7e
}
