package com.smartrent.infra.repository.entity.enums;

public enum VerificationStatus {
    PENDING,        // Newly created, waiting for AI
    IN_PROGRESS,    // AI is currently verifying
    VERIFIED,       // AI or Admin approved
    REJECTED,       // AI or Admin rejected
    UNDER_REVIEW    // AI flagged for manual review
}
