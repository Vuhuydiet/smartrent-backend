-- The admin analytics "listings" endpoint runs several queries filtered purely by
-- created_at (daily/monthly time series, cumulative baseline count, listing-type
-- breakdown, product-type breakdown, verification breakdown) with no user_id
-- predicate. The only existing index touching created_at is idx_user_created
-- (user_id, created_at), which MySQL cannot use for a created_at-only filter
-- (user_id is not a leading equality predicate), so every one of those queries
-- falls back to a full table scan on listings. That's why /v1/admin/analytics/listings
-- times out while /users and /reports (whose tables already have a standalone
-- created_at index, see V63) respond fine.

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'listings'
    AND INDEX_NAME = 'idx_listings_created_at');
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_listings_created_at ON listings (created_at)',
    'SELECT ''Index idx_listings_created_at already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
