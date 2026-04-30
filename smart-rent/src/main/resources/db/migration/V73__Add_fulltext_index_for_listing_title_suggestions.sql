-- Add FULLTEXT index for database-native search suggestions.
-- The existing idx_listings_title_norm BTREE index only supports prefix LIKE searches.

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND column_name = 'title_norm');

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'ft_listings_title_norm');

SET @sql = IF(@col_exists > 0 AND @idx_exists = 0,
    'CREATE FULLTEXT INDEX ft_listings_title_norm ON listings (title_norm)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
