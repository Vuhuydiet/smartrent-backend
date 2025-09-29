package com.smartrent.infra.exception;

public class PhoneNotVerifiedException extends RuntimeException {
    public PhoneNotVerifiedException() {
        super("User's phone number is not verified. Cannot create listing.");
    }
    
    public PhoneNotVerifiedException(String message) {
        super(message);
    }
}