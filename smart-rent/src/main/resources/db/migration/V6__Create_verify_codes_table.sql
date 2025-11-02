CREATE TABLE IF NOT EXISTS verify_codes (
    verify_code VARCHAR(6) NOT NULL PRIMARY KEY,
    expiration_time TIMESTAMP NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add foreign key constraint only if it doesn't exist
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'verify_codes'
    AND CONSTRAINT_NAME = 'fk_verify_codes_user');

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE verify_codes ADD CONSTRAINT fk_verify_codes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_verify_codes_user already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add indexes for performance
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'verify_codes'
    AND INDEX_NAME = 'idx_verify_codes_user_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_verify_codes_user_id ON verify_codes (user_id)', 'SELECT ''Index idx_verify_codes_user_id already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'verify_codes'
    AND INDEX_NAME = 'idx_verify_codes_expiration_time');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_verify_codes_expiration_time ON verify_codes (expiration_time)', 'SELECT ''Index idx_verify_codes_expiration_time already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
