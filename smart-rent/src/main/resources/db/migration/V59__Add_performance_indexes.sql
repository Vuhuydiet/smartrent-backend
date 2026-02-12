-- Performance indexes for frequently-used query patterns

-- 1. addresses(latitude, longitude) - for map bounds queries
--    withinMapBounds spec does: latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'addresses'
                     AND index_name = 'idx_addresses_lat_lng');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_addresses_lat_lng ON addresses (latitude, longitude)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. address_metadata(address_id) - for location subquery join in ListingSpecification
--    Spec does: root.get("address").get("addressId") = metadataRoot.get("address").get("addressId")
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'address_metadata'
                     AND index_name = 'idx_address_metadata_address_id');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_address_metadata_address_id ON address_metadata (address_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. listing_owner_actions(listing_id, status) - composite index for batch query
--    findByListingIdInAndStatus uses both columns
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listing_owner_actions'
                     AND index_name = 'idx_owner_action_listing_status');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_owner_action_listing_status ON listing_owner_actions (listing_id, status)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. listings(user_id, is_shadow, updated_at) - for my-listings queries
--    Owner query filters by userId + isShadow=false, sorts by updatedAt
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_user_filter');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_user_filter ON listings (user_id, is_shadow, updated_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. media(listing_id, status, sort_order) - for batch media queries
--    findActiveMediaByListingIds filters by listing_id IN (...) AND status='ACTIVE' ORDER BY sort_order
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'media'
                     AND index_name = 'idx_media_listing_status_sort');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_media_listing_status_sort ON media (listing_id, status, sort_order)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. pricing_histories(listing_id, is_current, change_type) - for price filter subqueries
--    Spec does: listing_id = ? AND is_current = true AND change_type = ?
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'pricing_histories'
                     AND index_name = 'idx_pricing_history_filter');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_pricing_history_filter ON pricing_histories (listing_id, is_current, change_type)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;