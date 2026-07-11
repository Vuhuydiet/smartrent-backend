-- Indexes for the admin reported-authors filters (email / name / phone).
-- email already has idx_users_email (V1). Add phone_number and name indexes so
-- prefix filters on the reported-authors list are index-backed.
-- Uses the conditional pattern (check INFORMATION_SCHEMA first) since MySQL
-- CREATE INDEX has no IF NOT EXISTS.

-- phone_number (uk_users_phone leads with phone_code, so a phone_number-only
-- prefix filter cannot use it)
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_phone_number');
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_users_phone_number ON users (phone_number)',
    'SELECT ''Index idx_users_phone_number already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- first_name prefix filter
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_first_name');
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_users_first_name ON users (first_name)',
    'SELECT ''Index idx_users_first_name already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- last_name prefix filter
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_last_name');
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_users_last_name ON users (last_name)',
    'SELECT ''Index idx_users_last_name already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
