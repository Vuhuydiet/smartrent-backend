package com.smartrent.enums;

/**
 * Enum representing how a listing was created/posted
 * 
 * QUOTA: Listing was created using membership quota (free)
 * DIRECT_PAYMENT: Listing was created by paying per-post via VNPay
 */
public enum PostSource {
    /**
     * Listing created using membership quota
     * User had available VIP/Premium quota and used it
     */
    QUOTA,
    
    /**
     * Listing created by direct payment
     * User paid per-post fee via VNPay (no quota used)
     */
    DIRECT_PAYMENT
}

