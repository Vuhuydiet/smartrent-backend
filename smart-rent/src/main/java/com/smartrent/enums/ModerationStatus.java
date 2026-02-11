package com.smartrent.enums;

/**
 * Canonical moderation lifecycle states for a listing.
 * Replaces the dual-flag (verified/isVerify) logic over time.
 */
public enum ModerationStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    REVISION_REQUIRED,
    RESUBMITTED,
    SUSPENDED
}
