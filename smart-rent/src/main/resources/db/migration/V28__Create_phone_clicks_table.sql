-- Create phone_clicks table to track when users click on phone numbers in listing details
CREATE TABLE IF NOT EXISTS phone_clicks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    clicked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),

    INDEX idx_listing_id (listing_id),
    INDEX idx_user_id (user_id),
    INDEX idx_listing_user (listing_id, user_id),
    INDEX idx_clicked_at (clicked_at),
    
    CONSTRAINT fk_phone_clicks_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE,
    CONSTRAINT fk_phone_clicks_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

