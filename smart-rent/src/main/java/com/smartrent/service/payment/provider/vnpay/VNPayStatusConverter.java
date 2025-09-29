package com.smartrent.service.payment.provider.vnpay;

import com.smartrent.enums.TransactionStatus;

/**
 * Utility class to convert between VNPay-specific and generic transaction statuses
 */
public class VNPayStatusConverter {

    public static VNPayTransactionStatus toVNPayStatus(TransactionStatus genericStatus) {
        return switch (genericStatus) {
            case SUCCESS -> VNPayTransactionStatus.SUCCESS;
            case PENDING -> VNPayTransactionStatus.PENDING;
            case FAILED -> VNPayTransactionStatus.FAILED;
            case CANCELLED -> VNPayTransactionStatus.CANCELLED;
            case EXPIRED -> VNPayTransactionStatus.EXPIRED;
            case INVALID_AMOUNT -> VNPayTransactionStatus.INVALID_AMOUNT;
            case INVALID_CARD -> VNPayTransactionStatus.INVALID_CARD;
            case INSUFFICIENT_BALANCE -> VNPayTransactionStatus.INSUFFICIENT_BALANCE;
            case LIMIT_EXCEEDED -> VNPayTransactionStatus.LIMIT_EXCEEDED;
            case BANK_MAINTENANCE -> VNPayTransactionStatus.BANK_MAINTENANCE;
            case INVALID_OTP -> VNPayTransactionStatus.INVALID_OTP;
            case TIMEOUT -> VNPayTransactionStatus.TIMEOUT;
            case DUPLICATE_TRANSACTION -> VNPayTransactionStatus.DUPLICATE_TRANSACTION;
            case BANK_REJECTED -> VNPayTransactionStatus.BANK_REJECTED;
            case INVALID_SIGNATURE -> VNPayTransactionStatus.INVALID_SIGNATURE;
            case UNKNOWN_ERROR -> VNPayTransactionStatus.UNKNOWN_ERROR;
        };
    }

    public static TransactionStatus toGenericStatus(VNPayTransactionStatus vnpayStatus) {
        return switch (vnpayStatus) {
            case SUCCESS -> TransactionStatus.SUCCESS;
            case PENDING -> TransactionStatus.PENDING;
            case FAILED -> TransactionStatus.FAILED;
            case CANCELLED -> TransactionStatus.CANCELLED;
            case EXPIRED -> TransactionStatus.EXPIRED;
            case INVALID_AMOUNT -> TransactionStatus.INVALID_AMOUNT;
            case INVALID_CARD -> TransactionStatus.INVALID_CARD;
            case INSUFFICIENT_BALANCE -> TransactionStatus.INSUFFICIENT_BALANCE;
            case LIMIT_EXCEEDED -> TransactionStatus.LIMIT_EXCEEDED;
            case BANK_MAINTENANCE -> TransactionStatus.BANK_MAINTENANCE;
            case INVALID_OTP -> TransactionStatus.INVALID_OTP;
            case TIMEOUT -> TransactionStatus.TIMEOUT;
            case DUPLICATE_TRANSACTION -> TransactionStatus.DUPLICATE_TRANSACTION;
            case BANK_REJECTED -> TransactionStatus.BANK_REJECTED;
            case INVALID_SIGNATURE -> TransactionStatus.INVALID_SIGNATURE;
            case UNKNOWN_ERROR -> TransactionStatus.UNKNOWN_ERROR;
        };
    }
}
