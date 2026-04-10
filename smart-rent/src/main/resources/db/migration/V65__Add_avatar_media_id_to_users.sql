-- Add avatar_media_id FK on users so avatars can reference a media record uploaded via the
-- presigned URL flow. ON DELETE SET NULL: if the media record is hard-deleted we keep the user
-- but clear the reference. avatar_url is kept so existing clients that only read the string URL
-- keep working during the transition.

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'avatar_media_id'
);

SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN avatar_media_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND INDEX_NAME = 'idx_users_avatar_media_id'
);

SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_users_avatar_media_id ON users(avatar_media_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND CONSTRAINT_NAME = 'fk_users_avatar_media'
);

SET @sql := IF(@fk_exists = 0,
    'ALTER TABLE users ADD CONSTRAINT fk_users_avatar_media FOREIGN KEY (avatar_media_id) REFERENCES media(media_id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
