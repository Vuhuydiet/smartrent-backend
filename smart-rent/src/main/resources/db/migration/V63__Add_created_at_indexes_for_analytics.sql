-- Add index on users.created_at for analytics queries (user growth chart)
-- listing_reports.created_at already has idx_created_at from entity definition
-- listings already has idx_user_created covering (user_id, created_at)

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_created_at');
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_users_created_at ON users (created_at)',
    'SELECT ''Index idx_users_created_at already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
