-- Migration V92: fix the public VIP-tier carousel index to avoid a filesort
-- ============================================================================
-- V91 created idx_listings_public_vip_tier as
--     (vip_type, verified, is_draft, is_shadow, updated_at)
-- which seeks the WHERE clause but still FILESORTS the homepage tier query:
--
--     WHERE vip_type = ? AND verified = true
--           AND is_draft = false AND is_shadow = false
--     ORDER BY vip_type_sort_order ASC, updated_at DESC
--     LIMIT 10
--
-- The ORDER BY leads with vip_type_sort_order. Even though vip_type is pinned
-- (so vip_type_sort_order is constant for the result set), MySQL does NOT infer
-- that — vip_type and vip_type_sort_order are different columns and only
-- vip_type is in the WHERE — so it cannot drop vip_type_sort_order from the sort
-- and falls back to a filesort over the whole matched set. For the NORMAL tier
-- (the bulk of all listings) that set is huge → the LIMIT 10 still sorts
-- everything → the "query cực lâu" symptom.
--
-- Fix: put vip_type_sort_order and updated_at INTO the index, in the ORDER BY's
-- exact direction (vip_type_sort_order ASC, updated_at DESC). After the four
-- equality columns, the index is then physically ordered the way the query
-- wants, so MySQL reads the first 10 entries — no filesort, regardless of tier
-- size. updated_at DESC is required: a plain ASC column can only serve
-- (ASC,ASC) forward or (DESC,DESC) backward, not the mixed (ASC,DESC) here.
--
-- Drops V91's index first (it's a strict prefix → fully redundant once this
-- exists). Idempotent via information_schema, matching V78-V91 style. Safe to
-- run whether or not V91 was applied.
--
-- NOTE: not yet EXPLAIN-validated on prod data — confirm "Using index" / no
-- "Using filesort" on the 100k-row DB, as V81-V84 did.
-- ============================================================================

-- 1. Drop the V91 prefix index if present.
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_public_vip_tier');
SET @sql = IF(@idx_exists > 0,
    'DROP INDEX idx_listings_public_vip_tier ON listings',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Recreate it with the sort columns appended (updated_at descending).
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_public_vip_tier');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_public_vip_tier ON listings (vip_type, verified, is_draft, is_shadow, vip_type_sort_order, updated_at DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
