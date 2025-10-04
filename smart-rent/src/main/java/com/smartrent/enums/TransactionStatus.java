package com.smartrent.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum TransactionStatus {
    SUCCESS("SUCCESS", "Transaction successful"),
    PENDING("PENDING", "Transaction pending"),
    FAILED("FAILED", "Transaction failed"),
    CANCELLED("CANCELLED", "Transaction cancelled"),
    EXPIRED("EXPIRED", "Transaction expired"),
    INVALID_AMOUNT("INVALID_AMOUNT", "Invalid amount"),
    INVALID_CARD("INVALID_CARD", "Invalid card"),
    INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", "Insufficient balance"),
    LIMIT_EXCEEDED("LIMIT_EXCEEDED", "Transaction limit exceeded"),
    BANK_MAINTENANCE("BANK_MAINTENANCE", "Bank under maintenance"),
    INVALID_OTP("INVALID_OTP", "Invalid OTP"),
    TIMEOUT("TIMEOUT", "Transaction timeout"),
    DUPLICATE_TRANSACTION("DUPLICATE_TRANSACTION", "Duplicate transaction"),
    BANK_REJECTED("BANK_REJECTED", "Bank rejected transaction"),
    INVALID_SIGNATURE("INVALID_SIGNATURE", "Invalid signature"),
    UNKNOWN_ERROR("UNKNOWN_ERROR", "Unknown error");

    String code;
    String message;

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isFailed() {
        return this == FAILED || this == CANCELLED || this == EXPIRED ||
               this == INVALID_AMOUNT || this == INVALID_CARD || this == INSUFFICIENT_BALANCE ||
               this == LIMIT_EXCEEDED || this == BANK_MAINTENANCE || this == INVALID_OTP ||
               this == TIMEOUT || this == DUPLICATE_TRANSACTION || this == BANK_REJECTED ||
               this == INVALID_SIGNATURE || this == UNKNOWN_ERROR;
    }
}
