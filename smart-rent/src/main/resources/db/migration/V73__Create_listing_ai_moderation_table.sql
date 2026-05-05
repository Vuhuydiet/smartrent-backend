-- Migration to create the listing AI moderation table

CREATE TABLE listing_ai_moderation (
    listing_id BIGINT PRIMARY KEY,
    ai_score DECIMAL(4,3) DEFAULT NULL COMMENT 'AI moderation score (0.0 to 1.0)',
    ai_reason JSON DEFAULT NULL COMMENT 'Structured JSON reason for AI moderation decision',
    manual_override BOOLEAN DEFAULT FALSE COMMENT 'Whether admin has manually overridden AI decision',
    verification_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'AI verification lifecycle status: PENDING, IN_PROGRESS, VERIFIED, REJECTED, UNDER_REVIEW',
    retry_count INT DEFAULT 0 COMMENT 'Number of retry attempts for AI verification',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_listing_ai_moderation_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE
);

CREATE INDEX idx_listing_ai_moderation_status ON listing_ai_moderation (verification_status, manual_override);
