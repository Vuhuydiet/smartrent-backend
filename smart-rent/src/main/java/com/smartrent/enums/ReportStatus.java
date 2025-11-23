package com.smartrent.enums;

/**
 * Status of a listing report - tracks whether admin has reviewed and resolved it
 */
public enum ReportStatus {
    PENDING,    // Report submitted, awaiting admin review
    RESOLVED,   // Admin reviewed and resolved the report (took action)
    REJECTED    // Admin reviewed and rejected the report (no action needed)
}

