package com.smartrent.service.payment.provider.vnpay;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum VNPayTransactionStatus {
    SUCCESS("00", "Transaction successful"),
    PENDING("01", "Transaction pending"),
    FAILED("02", "Transaction failed"),
    CANCELLED("24", "Transaction cancelled"),
    EXPIRED("11", "Transaction expired"),
    INVALID_AMOUNT("04", "Invalid amount"),
    INVALID_CARD("05", "Invalid card"),
    INSUFFICIENT_BALANCE("06", "Insufficient balance"),
    LIMIT_EXCEEDED("07", "Transaction limit exceeded"),
    BANK_MAINTENANCE("09", "Bank under maintenance"),
    INVALID_OTP("10", "Invalid OTP"),
    TIMEOUT("12", "Transaction timeout"),
    DUPLICATE_TRANSACTION("13", "Duplicate transaction"),
    BANK_REJECTED("51", "Bank rejected transaction"),
    INVALID_SIGNATURE("97", "Invalid signature"),
    UNKNOWN_ERROR("99", "Unknown error");

    String code;
    String message;

    public static VNPayTransactionStatus fromCode(String code) {
        for (VNPayTransactionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return UNKNOWN_ERROR;
    }

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
