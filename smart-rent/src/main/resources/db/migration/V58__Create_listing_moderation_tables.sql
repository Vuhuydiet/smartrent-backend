-- ============================================================
-- V58: Listing Moderation & Report-Resolution Flow
-- Creates audit trail table, owner action tracking, and
-- adds moderation columns to the listings table.
-- ============================================================

-- 1) Immutable audit trail
CREATE TABLE IF NOT EXISTS listing_moderation_events (
    event_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id        BIGINT       NOT NULL,
    source            VARCHAR(30)  NOT NULL COMMENT 'NEW_SUBMISSION | REPORT_RESOLUTION | OWNER_EDIT',
    from_status       VARCHAR(30)           COMMENT 'Previous moderation status (null for first event)',
    to_status         VARCHAR(30)  NOT NULL COMMENT 'New moderation status',
    action            VARCHAR(30)  NOT NULL COMMENT 'APPROVE | REJECT | REQUEST_REVISION | RESUBMIT | SUSPEND',
    reason_code       VARCHAR(50)           COMMENT 'Structured reason code (e.g. MISSING_INFO)',
    reason_text       TEXT                  COMMENT 'Human-readable reason',
    admin_id          VARCHAR(36)           COMMENT 'Admin who performed the action',
    triggered_by_user_id VARCHAR(36)        COMMENT 'Owner who triggered (e.g. resubmit)',
    report_id         BIGINT                COMMENT 'Linked listing_reports.report_id when source=REPORT_RESOLUTION',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_mod_event_listing (listing_id),
    INDEX idx_mod_event_created (created_at),
    INDEX idx_mod_event_admin (admin_id),

    CONSTRAINT fk_mod_event_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) Owner obligation tracking
CREATE TABLE IF NOT EXISTS listing_owner_actions (
    owner_action_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id        BIGINT       NOT NULL,
    trigger_type      VARCHAR(30)  NOT NULL COMMENT 'REPORT_RESOLVED | LISTING_REJECTED',
    trigger_ref_id    BIGINT                COMMENT 'report_id or moderation event_id that triggered this',
    required_action   VARCHAR(30)  NOT NULL COMMENT 'UPDATE_LISTING | CONTACT_SUPPORT',
    status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING_OWNER',
    deadline_at       DATETIME              COMMENT 'Optional deadline for owner to act',
    completed_at      DATETIME,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_owner_action_listing (listing_id),
    INDEX idx_owner_action_status (status),

    CONSTRAINT fk_owner_action_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) Add moderation columns to listings (all nullable for backward compat)
ALTER TABLE listings
    ADD COLUMN moderation_status          VARCHAR(30)  NULL COMMENT 'PENDING_REVIEW | APPROVED | REJECTED | REVISION_REQUIRED | RESUBMITTED | SUSPENDED',
    ADD COLUMN last_moderated_by          VARCHAR(36)  NULL,
    ADD COLUMN last_moderated_at          DATETIME     NULL,
    ADD COLUMN last_moderation_reason_code VARCHAR(50) NULL,
    ADD COLUMN last_moderation_reason_text TEXT        NULL,
    ADD COLUMN revision_count             INT          NOT NULL DEFAULT 0;

CREATE INDEX idx_listings_moderation_status ON listings(moderation_status);

-- 4) Backfill moderation_status from existing verified/isVerify flags
UPDATE listings SET moderation_status = 'APPROVED'       WHERE verified = true;
UPDATE listings SET moderation_status = 'PENDING_REVIEW'  WHERE verified = false AND is_verify = true;
UPDATE listings SET moderation_status = 'REJECTED'        WHERE verified = false AND is_verify = false AND is_draft = false AND post_date IS NOT NULL;
