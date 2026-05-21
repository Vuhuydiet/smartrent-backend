-- Migration V84: Seller-VIP composite — the one real gap left after V81-V83
-- ============================================================================
-- The 4 public seller-profile endpoints — /v1/listings/sellers/{userId}/diamond
-- (and /gold, /silver, /normal) — share one query shape that no existing
-- index can seek on cleanly:
--
--     WHERE user_id = ? AND vip_type = ?
--           AND is_shadow = false AND expired = false
--     ORDER BY vip_type_sort_order ASC, updated_at DESC
--
-- Note vip_type is pinned by the endpoint, so vip_type_sort_order is constant
-- post-filter → effective sort is `updated_at DESC`.
--
-- Why current indexes don't work for this shape:
--   • V81 idx_listings_user_draft_updated (user_id, is_draft, updated_at)
--     — no `is_draft` predicate in the seller-VIP path (the public-search
--     defaults that inject is_draft=false / moderation_status=APPROVED only
--     apply when filter.userId is null; here it's set). Planner uses the
--     leading user_id seek then filters vip_type in memory — every read,
--     no early termination.
--   • V80 idx_listings_cat_sort (category_id, vip_type_sort_order, updated_at)
--     — no category_id predicate.
--   • V82 idx_listings_public_default_sort starts with moderation_status —
--     not a predicate here.
--
-- Measured on a 2k-listing broker (worst case) at prod 2026-05-21:
--   • /sellers/{brokerId}/diamond?size=12  : ~280ms  → expected ~15ms
--
-- For typical sellers (<200 listings) the existing user_id seek + in-memory
-- filter is already fast (<10ms); this index only matters for high-volume
-- broker profiles. Adding it anyway because the seller-profile page is a
-- per-impression hit (4 carousels per page load × VIP tier).
--
-- Idempotent via information_schema check, matching V78-V83 style.
-- ============================================================================

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_user_vip_updated');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_user_vip_updated ON listings (user_id, vip_type, updated_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
