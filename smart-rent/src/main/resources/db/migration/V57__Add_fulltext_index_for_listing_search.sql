-- Add FULLTEXT index on search_text for fast keyword search
-- Replaces LIKE '%keyword%' which causes full table scans

-- Check if search_text column exists before adding FULLTEXT index
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND column_name = 'search_text');

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'ft_listings_search_text');

SET @sql = IF(@col_exists > 0 AND @idx_exists = 0,
    'CREATE FULLTEXT INDEX ft_listings_search_text ON listings (search_text)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add composite index for the most common public search filter combination
-- (is_shadow, is_draft, verified, expired, province filtering via address_id)
SET @idx2_exists = (SELECT COUNT(*) FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'listings'
                      AND index_name = 'idx_listings_public_filter');

SET @sql2 = IF(@idx2_exists = 0,
    'CREATE INDEX idx_listings_public_filter ON listings (is_shadow, is_draft, verified, expired, listing_type, category_id, price)',
    'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
