-- ============================================================
-- V66 - Add broker fields to users table
-- Broker verification flow:
--   NONE -> PENDING (user registers) -> APPROVED / REJECTED (admin review)
-- External verification source: https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20
-- ============================================================

-- is_broker
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'is_broker'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN is_broker BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- broker_verification_status
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'broker_verification_status'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN broker_verification_status VARCHAR(20) NOT NULL DEFAULT ''NONE''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- broker_registered_at
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'broker_registered_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN broker_registered_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- broker_verified_at
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'broker_verified_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN broker_verified_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- broker_verified_by_admin_id (VARCHAR(36) to support UUID-style admin IDs)
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'broker_verified_by_admin_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN broker_verified_by_admin_id VARCHAR(36) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- broker_rejection_reason
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'broker_rejection_reason'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN broker_rejection_reason VARCHAR(500) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- broker_verification_source
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'broker_verification_source'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN broker_verification_source VARCHAR(255) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Index for efficient broker listing filter (is_broker + broker_verification_status)
SET @idx_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND INDEX_NAME = 'idx_users_broker_status'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_users_broker_status ON users(is_broker, broker_verification_status)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
