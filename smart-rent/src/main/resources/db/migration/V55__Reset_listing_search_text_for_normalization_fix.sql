-- Reset search_text and title_norm to NULL so backfill runner can regenerate
-- with fixed Vietnamese đ/Đ normalization
-- After this migration, run backfill: application.search.backfill.enabled=true

-- Safe: only runs if the columns exist (added by V53)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND column_name = 'search_text');

SET @sql = IF(@col_exists > 0,
    'UPDATE listings SET search_text = NULL, title_norm = NULL WHERE search_text IS NOT NULL OR title_norm IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
