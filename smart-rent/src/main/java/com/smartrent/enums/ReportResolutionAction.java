package com.smartrent.enums;

/**
 * The concrete action an admin took when resolving a listing report.
 * <p>
 * {@link ReportStatus} only distinguishes PENDING / RESOLVED / REJECTED, and both
 * "suspend the listing" and "request the owner to revise it" collapse into RESOLVED.
 * This enum records which action actually happened so the admin console can show it
 * afterwards without inferring it from the listing's (mutable) moderation status.
 */
public enum ReportResolutionAction {
    SUSPENDED,          // Listing đình chỉ / removed (confirmed violation)
    REVISION_REQUESTED, // Owner asked to fix and resubmit the listing
    DISMISSED,          // Report bỏ qua — no action needed
    RESOLVED            // Handled without a specific listing action (generic / legacy)
}
