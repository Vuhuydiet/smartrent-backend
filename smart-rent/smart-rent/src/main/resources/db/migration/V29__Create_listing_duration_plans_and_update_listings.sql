-- Create listing_duration_plans table
CREATE TABLE listing_duration_plans (
    plan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    duration_days INT NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_duration_days (duration_days),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default duration plans
INSERT INTO listing_duration_plans (duration_days, is_active) VALUES
(5, TRUE),
(7, TRUE),
(10, TRUE),
(15, TRUE),
(30, TRUE);

-- Note: transaction_id column already exists in listings table (added in V14)
-- No need to add it again
