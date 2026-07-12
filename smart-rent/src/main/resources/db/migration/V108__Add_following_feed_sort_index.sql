-- Migration V108: the "from users I follow" feed
-- (GET /v1/listings/my-following-feed -> getListingsFromFollowedUsers ->
-- findPublicListingsByUserIdIn) is always scoped to a single followed user in
-- practice, so the hot query is:
--
--   WHERE user_id = ? AND is_draft = 0 AND is_shadow = 0
--         AND expired = 0 AND verified = 1
--   ORDER BY post_date DESC, created_at DESC
--   LIMIT ...
--
-- The only user_id-leading indexes are idx_user_created (user_id, created_at),
-- idx_listings_user_draft_updated (user_id, is_draft, updated_at) and
-- idx_listings_user_vip_updated (user_id, vip_type, updated_at). None of them
-- orders by post_date after user_id, so the planner reads the user's rows via
-- idx_user_created, applies the four boolean filters, then FILESORTS by
-- (post_date, created_at) to satisfy the ORDER BY.
--
-- Add a dedicated index whose equality prefix (user_id + the four visibility
-- booleans) is followed by the ORDER BY keys, so the feed becomes a single
-- index-ordered range scan with no filesort -- mirroring idx_listings_reco_*
-- (V97) and the map-bounds index (V98/V99). Both sort keys are DESC, so an
-- ascending index serves them via a backward index scan on MySQL 8.
--
-- Idempotent via information_schema checks, matching V97-V99 style.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Create the follow-feed sort index if it does not already exist.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_follow_feed');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_follow_feed ON listings (user_id, is_draft, is_shadow, verified, expired, post_date, created_at)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. Refresh optimizer statistics so the planner adopts the new index
--    immediately.
-- ---------------------------------------------------------------------------
ANALYZE TABLE listings;
