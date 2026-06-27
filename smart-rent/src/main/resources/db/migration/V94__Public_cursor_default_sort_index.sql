-- Migration V94: index the public default feed for keyset (cursor) pagination
-- ============================================================================
-- The public listing feed (POST /v1/listings/search and /search/cursor, used by
-- the /properties page) with no explicit sort runs:
--
--     WHERE moderation_status = 'APPROVED'   -- public, verified filter unset
--           AND is_draft  = false
--           AND is_shadow = false
--           [ + optional filters: category, price, area, isBroker semijoin, … ]
--     ORDER BY vip_type_sort_order ASC, updated_at DESC, listing_id DESC
--     LIMIT N
--
-- The cursor seek predicate is a lexicographic range over exactly those three
-- ORDER BY columns (listing_id is the unique tiebreaker — see
-- ListingCursorSupport), so the ideal index is the equality prefix followed by
-- the three sort columns in their exact directions.
--
-- Why the existing indexes don't serve it:
--   * idx_listings_public_default_sort
--       (moderation_status, is_shadow, is_draft, EXPIRED, vip_type_sort_order, updated_at)
--     puts `expired` BETWEEN the equality prefix and the sort columns. The public
--     default search does NOT constrain `expired`, so that column is an
--     unconstrained gap — the planner can't walk vip_type_sort_order/updated_at
--     in order through it → filesort. It also lacks listing_id, so the keyset
--     tiebreaker can't be resolved from the index.
--   * It stores updated_at ASCENDING, but the feed wants updated_at DESC after
--     vip_type_sort_order ASC — a MIXED-direction order that a forward or
--     backward scan of an all-ascending index cannot satisfy → filesort.
--
-- Fix: a dedicated index with the equality columns contiguous (no `expired`
-- gap) and the sort columns in their real directions, including the tiebreaker:
--
--     (moderation_status, is_draft, is_shadow,
--      vip_type_sort_order ASC, updated_at DESC, listing_id DESC)
--
-- On MySQL 8 this matches the ORDER BY exactly → an ordered index range scan for
-- the cursor seek, no filesort, no COUNT, O(LIMIT). The isBroker / category /
-- price filters are applied as residual predicates / semijoins on top of this
-- ordered scan (the broker semijoin is already covered by idx_users_broker_status).
--
-- Note on portability: the mixed ASC/DESC ordering inherently needs a descending
-- index column, which only MySQL 8.0+ honours. On older engines the DESC keyword
-- is ignored (index built ascending) and the query falls back to a filesort —
-- i.e. no worse than today. The project targets MySQL 8.
--
-- Category-filtered feed: the category navigation pushes ?categoryId=…, so
-- "browse a category" is a primary path. A category-filtered cursor query adds
-- `category_id = ?` to the same WHERE/ORDER BY. The default-feed index above
-- can't serve it (category_id would be an unconstrained leading gap), so we add
-- a sibling index that LEADS with category_id (the most selective equality),
-- then the public flags, then the same three sort columns:
--
--     (category_id, moderation_status, is_draft, is_shadow,
--      vip_type_sort_order ASC, updated_at DESC, listing_id DESC)
--
-- → ordered index range scan per category, no filesort, O(LIMIT).
--
-- Idempotent via information_schema, matching the V91-V93 style.
-- ============================================================================

-- 1. Default feed (no category filter).
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_public_cursor_default');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_public_cursor_default ON listings (moderation_status, is_draft, is_shadow, vip_type_sort_order ASC, updated_at DESC, listing_id DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Category-filtered feed (?categoryId=…) — leads with category_id.
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_public_cursor_category');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_public_cursor_category ON listings (category_id, moderation_status, is_draft, is_shadow, vip_type_sort_order ASC, updated_at DESC, listing_id DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
