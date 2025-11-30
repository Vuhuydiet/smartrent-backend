package com.smartrent.enums;

/**
 * Enum representing the various states a listing can be in for owner view
 * These statuses are computed based on multiple listing fields
 */
public enum ListingStatus {
    /**
     * Listing has expired (expiryDate < now or expired flag is true)
     */
    EXPIRED(1),

    /**
     * Listing will expire soon (expiryDate within 3-7 days from now)
     */
    EXPIRING_SOON(2),

    /**
     * Listing is currently being displayed (verified, not expired, expiryDate > now)
     */
    DISPLAYING(3),

    /**
     * Listing is pending review (isVerify=true, verified=false)
     */
    IN_REVIEW(4),

    /**
     * Listing is waiting for payment (has unpaid transaction)
     */
    PENDING_PAYMENT(5),

    /**
     * Listing was rejected (verified=false, isVerify=false, not draft, postDate exists)
     */
    REJECTED(6),

    /**
     * Listing is verified and active (verified=true)
     */
    VERIFIED(7);

    private final int code;

    ListingStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ListingStatus fromCode(int code) {
        for (ListingStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ListingStatus code: " + code);
    }
}
