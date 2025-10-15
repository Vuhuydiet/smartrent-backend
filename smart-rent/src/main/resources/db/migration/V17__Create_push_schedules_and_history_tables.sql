-- Add pushed_at field to listings table
-- By default, pushed_at will be created_at
ALTER TABLE listings
ADD COLUMN pushed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER created_at,
ADD INDEX idx_pushed_at (pushed_at);

-- Update existing listings to set pushed_at = created_at
UPDATE listings SET pushed_at = created_at WHERE pushed_at IS NULL;

-- Create Push Schedules table
CREATE TABLE push_schedules (
    schedule_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    scheduled_time TIME NOT NULL COMMENT 'Hour of the day to push (e.g., 09:00:00, 15:00:00)',
    end_time TIMESTAMP NOT NULL COMMENT 'After this time, the schedule will no longer be processed',
    status ENUM('ACTIVE', 'INACTIVE', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_listing_id (listing_id),
    INDEX idx_scheduled_time (scheduled_time),
    INDEX idx_status (status),
    INDEX idx_listing_status (listing_id, status),
    INDEX idx_end_time (end_time),

    CONSTRAINT fk_push_schedules_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE,
    -- Enforce uniqueness of ACTIVE schedule per listing using a generated column
    , active_only BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN listing_id ELSE NULL END) STORED
    , UNIQUE KEY unique_active_schedule_per_listing (active_only)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores push schedules for listings. Each listing can have at most one ACTIVE schedule.';

-- Create Push History table
CREATE TABLE push_history (
    push_history_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    status ENUM('SUCCESS', 'FAIL') NOT NULL,
    message VARCHAR(500) COMMENT 'Reason for failure or additional information',
    pushed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Actual time when push was executed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_schedule_id (schedule_id),
    INDEX idx_listing_id (listing_id),
    INDEX idx_status (status),
    INDEX idx_pushed_at (pushed_at),
    INDEX idx_schedule_status (schedule_id, status),

    CONSTRAINT fk_push_history_schedule FOREIGN KEY (schedule_id) REFERENCES push_schedules(schedule_id) ON DELETE CASCADE,
    CONSTRAINT fk_push_history_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Records the history of all listing push operations';
