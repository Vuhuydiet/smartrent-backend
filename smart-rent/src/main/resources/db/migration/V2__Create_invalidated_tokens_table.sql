CREATE TABLE IF NOT EXISTS invalidated_tokens (
    access_id VARCHAR(255) NOT NULL PRIMARY KEY,
    refresh_id VARCHAR(255),
    expiration_time TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for invalidated_tokens table
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'invalidated_tokens'
    AND INDEX_NAME = 'idx_invalidated_tokens_refresh_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_invalidated_tokens_refresh_id ON invalidated_tokens (refresh_id)', 'SELECT ''Index idx_invalidated_tokens_refresh_id already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'invalidated_tokens'
    AND INDEX_NAME = 'idx_invalidated_tokens_expiration_time');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_invalidated_tokens_expiration_time ON invalidated_tokens (expiration_time)', 'SELECT ''Index idx_invalidated_tokens_expiration_time already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
