-- Migration V80: Category-scoped composite indexes after moderation-gate fix
-- ============================================================================
-- The public search path (PR #247) now gates on `moderation_status = 'APPROVED'`
-- instead of `verified = true`. The two old composite indexes that backed the
-- public search — `idx_listings_public_filter` and `idx_listings_public_search`
-- — both include `verified` in the middle of the column list, so MySQL can no
-- longer seek to `category_id` without a column predicate on `verified`.
--
-- Symptoms on the 100k-row prod DB (EXPLAIN measured 2026-05-19):
--   • POST /v1/listings/stats/categories : ~2.0s   (homepage section card counts)
--   • SELECT COUNT(*) ... category_id=? AND moderation_status='APPROVED' : ~1.6s
--
-- These two cover the slow paths after the gate change:
--
-- 1. (category_id, moderation_status, is_shadow, is_draft, expired)
--    Backs the per-category aggregation query and the pagination COUNT. Every
--    column is a fixed filter in those queries, so the index is fully
--    seekable. Leaves category_id at the front so the same index also serves
--    the "filter by category, no moderation predicate" path.
--
-- 2. (category_id, vip_type_sort_order, updated_at)
--    Backs the in-category sort (vip-first, then newest). Without it the
--    planner picks `idx_listings_sort_order` which scans the entire table
--    sort order and tests category in memory. With it, the LIMIT 20 finishes
--    in an index-range scan over a single category partition.
--
-- Estimated impact at 100k rows: each index ≈ 4–5 MB.
-- Idempotent via information_schema check, matching V78's project style.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. (category_id, moderation_status, is_shadow, is_draft, expired)
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_cat_mod_filter');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_cat_mod_filter ON listings (category_id, moderation_status, is_shadow, is_draft, expired)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. (category_id, vip_type_sort_order, updated_at)
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_cat_sort');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_cat_sort ON listings (category_id, vip_type_sort_order, updated_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
