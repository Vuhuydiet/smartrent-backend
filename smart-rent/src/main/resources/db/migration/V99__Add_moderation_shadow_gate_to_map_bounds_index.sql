-- Migration V99: withinMapBounds() was missing the moderation_status=APPROVED
-- and is_shadow=false gates that every other public-visibility query
-- (fromFilterRequest, findHomepageTier) already applies -- listings that were
-- shadow-banned or never admin-approved could still show as pins on the
-- public map even though verified=true alone doesn't guarantee that (see
-- ListingModerationServiceImpl, which can flip verified back to false
-- independently of moderation_status on revision/resubmission).
--
-- ListingSpecification.withinMapBounds now filters on
-- (is_draft, is_shadow, verified, moderation_status, expired, latitude,
-- longitude, expiry_date) -- rebuild idx_listings_map_bounds so the new
-- equality predicates (is_shadow, moderation_status) stay in the index
-- prefix ahead of the latitude range scan, instead of falling back to a
-- row-by-row filter after the index lookup.
--
-- Idempotent via information_schema checks, matching V97/V98 style.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Drop the old index (equality prefix no longer matches the query).
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_map_bounds');
SET @sql = IF(@idx_exists > 0,
    'DROP INDEX idx_listings_map_bounds ON listings',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. Recreate with the full equality prefix used by withinMapBounds:
--    is_draft, is_shadow, verified, moderation_status, expired -- then the
--    latitude range, then the covering suffix (longitude, expiry_date +
--    the ORDER BY keys) exactly as V98 laid out.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_map_bounds');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_map_bounds ON listings (is_draft, is_shadow, verified, moderation_status, expired, latitude, longitude, expiry_date, vip_type_sort_order, updated_at, listing_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. Refresh optimizer statistics so the planner adopts the rebuilt index
--    immediately.
-- ---------------------------------------------------------------------------
ANALYZE TABLE listings;
