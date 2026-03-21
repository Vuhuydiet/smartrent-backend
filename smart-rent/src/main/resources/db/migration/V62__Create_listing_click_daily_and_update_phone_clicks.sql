CREATE TABLE IF NOT EXISTS listing_click_daily (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    click_date DATE NOT NULL,
    click_count INT NOT NULL DEFAULT 0,

    UNIQUE KEY uk_listing_date (listing_id, click_date),
    INDEX idx_listing_id (listing_id),
    INDEX idx_click_date (click_date),

    CONSTRAINT fk_listing_click_daily_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE phone_clicks ADD INDEX idx_ip_clicked_at (ip_address, clicked_at);
ALTER TABLE phone_clicks ADD INDEX idx_listing_clicked_at (listing_id, clicked_at);
