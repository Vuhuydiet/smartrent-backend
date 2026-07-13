package com.smartrent.enums;

/**
 * Canonical moderation lifecycle states for a listing.
 * Replaces the dual-flag (verified/isVerify) logic over time.
 */
public enum ModerationStatus {
    PENDING_REVIEW,
    APPROVED,
    // Admin rejected the listing in the review queue. Terminal from the owner's
    // side (distinct from REVISION_REQUIRED, which invites a fix + resubmit).
    REJECTED,
    REVISION_REQUIRED,
    RESUBMITTED,
    // Temporarily hidden while a report against the listing is under review.
    // Reversible by the admin (unhide) — the owner is not expected to act.
    SUSPENDED,
    // Permanently removed for a confirmed report violation. Terminal, never
    // resubmittable. Was previously encoded as SUSPENDED + permanentlyRemoved=true.
    REMOVED
}
