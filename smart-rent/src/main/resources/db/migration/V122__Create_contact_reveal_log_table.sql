-- Create contact_reveal_log table.
--
-- Unified audit trail of contact reveals: records WHO (viewer) revealed WHICH
-- seller's contact, through which CHANNEL (phone / email / zalo), optionally
-- from which listing, and WHEN. Written by the authenticated
-- POST /v1/users/{sellerUserId}/reveal-contact endpoint so contact access can
-- be tracked per user going forward.
--
-- Complements the existing phone_clicks table (which only covers phone clicks
-- and powers owner analytics) by covering every contact channel in one place.
CREATE TABLE IF NOT EXISTS contact_reveal_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    viewer_user_id VARCHAR(36) NOT NULL,
    seller_user_id VARCHAR(36) NOT NULL,
    listing_id BIGINT NULL,
    channel VARCHAR(16) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    revealed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_contact_reveal_viewer (viewer_user_id, revealed_at),
    INDEX idx_contact_reveal_seller (seller_user_id, revealed_at),
    INDEX idx_contact_reveal_listing (listing_id),
    CONSTRAINT fk_contact_reveal_viewer FOREIGN KEY (viewer_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_contact_reveal_seller FOREIGN KEY (seller_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_contact_reveal_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
