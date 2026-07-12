-- Migration V109: index the admin list's moderation-status-filtered browse+sort
-- path (the "Đã Duyệt" / "Từ Chối" / etc. tabs of POST /v1/listings/admin/list).
-- ============================================================================
-- When an admin picks a status tab, ListingSpecification.fromFilterRequest emits
-- (for the APPROVED / "Đã Duyệt" tab, 73k rows on prod):
--
--     WHERE is_shadow = 0
--           AND moderation_status = 'APPROVED'
--           AND verified = 1 AND expired = 0
--           AND (expiry_date IS NULL OR expiry_date > NOW())
--     ORDER BY vip_type_sort_order ASC, updated_at DESC
--     LIMIT 20
--
-- EXPLAIN ANALYZE on prod measured this at ~9.3s. No single index fit, so the
-- optimizer index_merge-INTERSECTED three single-column indexes
-- (idx_status ∩ idx_is_shadow ∩ idx_listings_moderation_status → 71,591 row IDs)
-- and then FILESORTED all 71,591 rows to return the top 20.
--
-- Why the existing indexes don't serve it:
--   * idx_listings_public_cursor_default
--       (moderation_status, is_draft, is_shadow, vip_type_sort_order ASC,
--        updated_at DESC, listing_id DESC)
--     has the right sort directions but leads its prefix with is_draft AFTER
--     moderation_status. The admin status tabs do NOT constrain is_draft (the
--     admin path deliberately leaves it unbound — see
--     ListingSpecification line ~73), so is_draft is an unconstrained gap and
--     the planner can't walk the sort columns through it. (This is exactly the
--     REJECTED tab's saving grace — that tab DOES bind is_draft=0 via its
--     listingStatus=REJECTED predicate, so cursor_default serves it with no
--     filesort. APPROVED/DISPLAYING does not.)
--   * idx_listings_admin_default_sort (is_shadow, vip_type_sort_order,
--     updated_at) — no moderation_status in the prefix, and stored all-ASC, so
--     it can't satisfy the mixed ASC/DESC order either (see V94's note).
--
-- Fix: mirror idx_listings_admin_default_sort (V100) with moderation_status
-- inserted into the equality prefix and the sort columns in their REAL
-- directions:
--
--     (is_shadow, moderation_status, vip_type_sort_order ASC, updated_at DESC)
--
-- Both leading columns are pure equality for every status tab ⇒ an ordered
-- index range scan on (is_shadow=0, moderation_status=?) that yields
-- vip_type_sort_order ASC / updated_at DESC directly — no index_merge, no
-- filesort, O(LIMIT). verified / expired / expiry_date remain residual filters
-- (near-100% hit rate for APPROVED, so the LIMIT fills within a few tens of
-- rows). Serves every moderation-status tab (APPROVED, REVISION_REQUIRED,
-- SUSPENDED, …), not just APPROVED.
--
-- The mixed ASC/DESC order needs a descending index column, honoured only on
-- MySQL 8.0+ (this project targets MySQL 8 — same basis as V94). On older
-- engines the DESC keyword is ignored and the query falls back to filesort,
-- i.e. no worse than today.
--
-- The pagination COUNT keeps using the covering idx_listings_admin_review_queue
-- (this index isn't covering for verified/expired, so the optimizer won't
-- adopt it there) — no count regression; the ~9.3s was the ORDER BY data query.
--
-- Idempotent via information_schema check, matching the V94-V108 style.
-- ============================================================================

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_admin_moderation_sort');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_admin_moderation_sort ON listings (is_shadow, moderation_status, vip_type_sort_order ASC, updated_at DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Refresh optimizer statistics so the planner adopts the new index immediately.
ANALYZE TABLE listings;
