package com.smartrent.enums;

public enum VerificationType {
    REGISTRATION("Thank you for registering with us. To complete your account verification, please use the verification code below:"),
    PASSWORD_RESET("We received a request to reset your password. Please use the verification code below to proceed:"),
    EMAIL_CHANGE("We received a request to change your email address. Please use the verification code below to confirm this change:"),
    LOGIN_VERIFICATION("For your security, please verify your identity using the verification code below:"),
    PHONE_VERIFICATION("Please verify your phone number using the verification code below:"),
    TWO_FACTOR_AUTH("Your two-factor authentication code is ready. Please use the verification code below:");

    private final String message;

    VerificationType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
