-- Migration V93: simplify the public VIP-tier carousel index
-- ============================================================================
-- The homepage tier carousels (GET /v1/listings/homepage-tier) and the per-tier
-- search both query ONE vip_type at a time:
--
--     WHERE vip_type = ? AND verified = true
--           AND is_draft = false AND is_shadow = false
--     ORDER BY updated_at DESC          -- vip_type pinned ⇒ no vip_type_sort_order needed
--     LIMIT N
--
-- V92 had built the index as
--     (vip_type, verified, is_draft, is_shadow, vip_type_sort_order, updated_at DESC)
-- to match an ORDER BY of (vip_type_sort_order ASC, updated_at DESC). That has two
-- problems for this single-tier query:
--   1. It depends on a DESCENDING index column, which only MySQL 8.0+ honours;
--      on older engines DESC is ignored → filesort.
--   2. With vip_type_sort_order sitting between the equality columns and
--      updated_at, the planner can't use the index to order by updated_at alone
--      (it doesn't know vip_type_sort_order is constant) → filesort over the
--      whole tier (catastrophic for NORMAL — the bulk of all listings, the slow
--      "Tin mới" carousel).
--
-- Fix: drop vip_type_sort_order from both the query (done in code) and the index.
-- A plain trailing updated_at lets MySQL satisfy `ORDER BY updated_at DESC` with a
-- BACKWARD index scan on any version — a 10-row read, no filesort.
--
-- Final shape: (vip_type, verified, is_draft, is_shadow, updated_at)
-- (identical to V91's original; V92's variant is superseded.)
--
-- Idempotent via information_schema, matching V78-V92 style. Safe whether the
-- index currently exists in V91, V92, or no form.
-- ============================================================================

-- 1. Drop whatever shape currently exists.
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

-- 2. Recreate with the simple, version-portable shape.
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_public_vip_tier');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_public_vip_tier ON listings (vip_type, verified, is_draft, is_shadow, updated_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
