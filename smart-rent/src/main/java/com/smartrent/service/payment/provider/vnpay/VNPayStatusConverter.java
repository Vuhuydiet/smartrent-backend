package com.smartrent.service.payment.provider.vnpay;

import com.smartrent.enums.TransactionStatus;

/**
 * Utility class to convert between VNPay-specific and generic transaction statuses
 */
public class VNPayStatusConverter {

    public static VNPayTransactionStatus toVNPayStatus(TransactionStatus genericStatus) {
        return switch (genericStatus) {
            case COMPLETED -> VNPayTransactionStatus.SUCCESS;
            case PENDING -> VNPayTransactionStatus.PENDING;
            case FAILED -> VNPayTransactionStatus.FAILED;
            case CANCELLED -> VNPayTransactionStatus.CANCELLED;
            case REFUNDED -> VNPayTransactionStatus.FAILED; // Map refunded to failed for VNPay
        };
    }

    public static TransactionStatus toGenericStatus(VNPayTransactionStatus vnpayStatus) {
        return switch (vnpayStatus) {
            case SUCCESS -> TransactionStatus.COMPLETED;
            case PENDING -> TransactionStatus.PENDING;
            case CANCELLED -> TransactionStatus.CANCELLED;
            // All failure cases map to FAILED
            case FAILED, EXPIRED, INVALID_AMOUNT, INVALID_CARD, INSUFFICIENT_BALANCE,
                 LIMIT_EXCEEDED, BANK_MAINTENANCE, INVALID_OTP, TIMEOUT,
                 DUPLICATE_TRANSACTION, BANK_REJECTED, INVALID_SIGNATURE, UNKNOWN_ERROR -> TransactionStatus.FAILED;
        };
    }
}
