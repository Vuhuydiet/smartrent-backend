package com.smartrent.enums;

/**
 * Source context of a moderation cycle — enables analytics and SLA tracking per flow.
 */
public enum ModerationSource {
    NEW_SUBMISSION,
    REPORT_RESOLUTION,
    OWNER_EDIT
}
