-- Migration V100: index the admin list's default (unfiltered) browse+sort path
-- ============================================================================
-- POST /v1/listings/admin/list (ListingServiceImpl.getAllListingsForAdmin) sets
-- excludeExpired=false and leaves verified/isVerify/isDraft/moderationStatus
-- unconstrained unless the admin explicitly filters. In that common case —
-- "show me all listings", paging through the dashboard with no filter — the
-- only predicate ListingSpecification.fromFilterRequest adds is
-- `is_shadow = false`, then sorts by vip_type_sort_order ASC, updated_at DESC.
--
-- Every existing sort-supporting index (idx_listings_public_default_sort,
-- idx_listings_cat_sort, idx_listings_public_cursor_default, ...) leads with
-- moderation_status, category_id, user_id, or vip_type — none of which are
-- bound for this query shape, so none of them can be used and the planner
-- filesorts the (near full-table) matching set before applying LIMIT/OFFSET.
-- With the tiny prod buffer pool (32MB, see application.yml), that filesort
-- means real disk I/O, not just CPU.
--
-- Fix: a dedicated index on just the predicate this query actually has plus
-- the two sort columns, in order — an index range scan for is_shadow=false
-- ordered by vip_type_sort_order/updated_at, no filesort, O(LIMIT) instead of
-- O(table).
--
-- Idempotent via information_schema check, matching the V78-V99 style.
-- ============================================================================

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_admin_default_sort');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_admin_default_sort ON listings (is_shadow, vip_type_sort_order, updated_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
