package com.smartrent.enums;

public enum NotificationType {
    // Report-related
    NEW_REPORT,
    REPORT_RESOLVED,
    REPORT_REJECTED,
    REPORT_ACTION_REQUIRED,

    // Moderation-related
    LISTING_APPROVED,
    LISTING_REJECTED,
    LISTING_REVISION_REQUIRED,
    LISTING_SUSPENDED,
    LISTING_RESUBMITTED,

    // Broker-related
    BROKER_REGISTRATION_RECEIVED,   // admins are notified when a user submits registration
    BROKER_APPROVED,                // user is notified when admin approves
    BROKER_REJECTED                 // user is notified when admin rejects
}
