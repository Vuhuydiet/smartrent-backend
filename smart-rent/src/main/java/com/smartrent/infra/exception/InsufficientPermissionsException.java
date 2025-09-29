package com.smartrent.infra.exception;

public class InsufficientPermissionsException extends RuntimeException {
    public InsufficientPermissionsException() {
        super("Insufficient permissions to perform this action");
    }
    
    public InsufficientPermissionsException(String message) {
        super(message);
    }
}