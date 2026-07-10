-- Migration V101: index the admin "pending review" queue query shape
-- ============================================================================
-- The admin dashboard's default tab sends:
--   { moderationStatus: "PENDING_REVIEW", listingStatus: "IN_REVIEW", sortBy: "NEWEST" }
-- ListingSpecification.fromFilterRequest + buildStatusPredicate(IN_REVIEW) +
-- the PENDING_REVIEW moderationStatus branch together resolve this to:
--
--     WHERE is_verify = true
--       AND (verified = false OR verified IS NULL)
--       AND (moderation_status = 'PENDING_REVIEW' OR moderation_status IS NULL)
--       AND (expired = false OR expired IS NULL)
--       AND (expiry_date IS NULL OR expiry_date > NOW())
--       AND is_shadow = false
--     ORDER BY vip_type_sort_order ASC, updated_at DESC
--
-- Measured at ~13s on prod for page=1, size=20 — this is the endpoint's default
-- landing view (admins open the dashboard straight into the review queue), so
-- it's the highest-traffic admin/list shape, not an edge case.
--
-- Why no existing index serves this: every sort-oriented composite
-- (idx_listings_public_default_sort, idx_listings_cat_sort, ...) leads with
-- moderation_status, category_id, user_id, or vip_type — none of which are a
-- clean equality prefix here (moderation_status itself is an OR/nullable
-- condition, not a single bound value). Nothing keys off is_verify at all, so
-- the planner has no way to narrow before scanning + filesorting the (near
-- full) table.
--
-- Fix: an index on the two hard equality predicates (is_shadow, is_verify)
-- with the OR/nullable columns (verified, moderation_status, expired)
-- trailing. MySQL range-scans the equality prefix, then applies the trailing
-- predicates via index condition pushdown — filtering down to the actual
-- review-queue rows using only index data, no full-row fetch for non-matches,
-- and no full-table scan. The subsequent filesort only has to sort the
-- (small) matching set instead of the whole table.
--
-- Idempotent via information_schema check, matching the V78-V100 style.
-- ============================================================================

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_admin_review_queue');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_admin_review_queue ON listings (is_shadow, is_verify, verified, moderation_status, expired)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
