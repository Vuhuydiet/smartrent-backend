package com.smartrent.enums;

public enum NotificationType {
    // Report-related
    NEW_REPORT,
    REPORT_RESOLVED,
    REPORT_REJECTED,
    REPORT_ACTION_REQUIRED,
    REPORT_LISTING_REMOVED,   // owner notified when a report is resolved as a confirmed violation and the listing is permanently removed

    // Moderation-related
    LISTING_APPROVED,
    LISTING_REJECTED,
    LISTING_REVISION_REQUIRED,
    LISTING_SUSPENDED,
    LISTING_RESUBMITTED,
    LISTING_PENDING_REVIEW,          // owner notified when listing is routed to manual review (e.g. duplicate detected)
    LISTING_DUPLICATE_DETECTED,      // admins notified when AI flags a listing as duplicate/suspicious

    // Broker-related
    BROKER_REGISTRATION_RECEIVED,   // admins are notified when a user submits registration
    BROKER_APPROVED,                // user is notified when admin approves
    BROKER_REJECTED,                // user is notified when admin rejects

    // Listing lifecycle
    LISTING_EXPIRING,               // owner is notified at D-7 / D-3 / D-1 before expiry

    // Follow-related
    NEW_LISTING_FROM_FOLLOWED_USER, // follower is notified when a followed user publishes a new listing

    // Membership lifecycle
    MEMBERSHIP_EXPIRING,            // member is notified at D-7 / D-3 before membership expiry
    MEMBERSHIP_ACTIVATED,           // member is notified when their queued membership activates
    // Posting block
    POSTING_BLOCKED,                // user is notified when admin blocks them from posting listings
    POSTING_UNBLOCKED,              // user is notified when admin lifts the posting block

    /**
     * Legacy — no code path creates this anymore, but historical rows with
     * type='PHONE_CLICK' still exist in the notifications table (Notification.type
     * is @Enumerated(EnumType.STRING), so a DB value with no matching constant
     * throws IllegalArgumentException and breaks GET /v1/notifications for that
     * user, not just the read of that one row). Kept only so old rows load.
     */
    PHONE_CLICK,

    /**
     * Legacy — no code path creates this anymore, but historical rows with
     * type='VIEW_MILESTONE' still exist in the notifications table, same
     * failure mode as PHONE_CLICK above. Kept only so old rows load.
     */
    VIEW_MILESTONE,

    /**
     * Legacy — no code path creates this anymore, but historical rows with
     * type='NEW_FOLLOWER' still exist in the notifications table, same
     * failure mode as PHONE_CLICK above. Kept only so old rows load.
     */
    NEW_FOLLOWER
}
