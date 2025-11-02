CREATE TABLE IF NOT EXISTS saved_listings (
    user_id VARCHAR(36) NOT NULL,
    listing_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add foreign key constraints only if they don't exist
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'saved_listings' AND CONSTRAINT_NAME = 'fk_saved_listings_user');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE saved_listings ADD CONSTRAINT fk_saved_listings_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_saved_listings_user already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'saved_listings' AND CONSTRAINT_NAME = 'fk_saved_listings_listing');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE saved_listings ADD CONSTRAINT fk_saved_listings_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_saved_listings_listing already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
