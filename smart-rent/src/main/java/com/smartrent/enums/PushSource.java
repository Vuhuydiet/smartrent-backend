package com.smartrent.enums;

/**
 * Enum representing the source of a listing boost/push
 *
 * MEMBERSHIP_QUOTA: Boost using membership quota (free)
 * DIRECT_PAYMENT: Boost by paying per-boost via VNPay
 * SCHEDULED: Automated scheduled boost
 * ADMIN: Manual boost by admin
 */
public enum PushSource {
    /**
     * Boost using membership quota
     */
    MEMBERSHIP_QUOTA,

    /**
     * Boost by direct payment via VNPay
     */
    DIRECT_PAYMENT,

    /**
     * Automated scheduled boost
     */
    SCHEDULED,

    /**
     * Manual boost by admin
     */
    ADMIN
}

