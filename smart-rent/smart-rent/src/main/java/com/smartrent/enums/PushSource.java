package com.smartrent.enums;

/**
 * Enum representing the source of a listing push
 *
 * MEMBERSHIP_QUOTA: Push using membership quota (free)
 * DIRECT_PAYMENT: Push by paying per-push via VNPay
 * SCHEDULED: Automated scheduled push
 * ADMIN: Manual push by admin
 */
public enum PushSource {
    /**
     * Push using membership quota
     */
    MEMBERSHIP_QUOTA,

    /**
     * Push by direct payment via VNPay
     */
    DIRECT_PAYMENT,

    /**
     * Automated scheduled push
     */
    SCHEDULED,

    /**
     * Manual push by admin
     */
    ADMIN
}

