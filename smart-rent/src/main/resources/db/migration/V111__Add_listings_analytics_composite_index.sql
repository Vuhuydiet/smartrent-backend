-- Migration V111: composite index for the admin listing-analytics endpoint.
-- ============================================================================
-- GET /v1/admin/analytics/listings runs six aggregation queries, and every one is
-- filtered by (is_shadow = false AND is_draft = false AND created_at <range/point>):
-- the daily / monthly creation time series, the cumulative baseline
-- COUNT(created_at < start), and the listing-type / product-type / verification
-- breakdowns (see ListingRepository.countNewListingsBy* and
-- countByCreatedAtBeforeAndIsDraftFalseAndIsShadowFalse).
--
-- V96 added a single-column idx_listings_created_at (created_at) for exactly this
-- endpoint, but the optimizer refuses to use it: with the two boolean predicates
-- present it prefers the low-cardinality idx_is_shadow (is_shadow, added in V13) —
-- a const ref that matches ~half the table (EXPLAIN: ~45k rows, filtered 7.6%),
-- then random-PK-looks up each row to apply created_at / is_draft, plus a temporary
-- table and filesort for the GROUP BY. On the reshaped 100k-row prod listings table
-- with a small buffer pool that pushes the whole endpoint past the 60s client
-- timeout, so the admin "Insights -> Posts" (Phan tich -> Tin dang) page hangs on
-- its loading spinner.
--
-- This composite leads with the two equality predicates (is_shadow, is_draft) and
-- ends with the created_at range, so a single covering range scan (EXPLAIN: type
-- range, Using index, filtered 100%, no filesort) answers each query touching only
-- the matching rows (~6.4k for a 30-day window instead of ~45k + random I/O). It
-- serves every one of the six analytics queries and supersedes
-- idx_listings_created_at for them; that single-column index is intentionally left
-- in place here (a separate redundant-index audit, cf. V110, can retire it).
--
-- Already created directly on prod while diagnosing the hang; this migration makes
-- the change reproducible on fresh Flyway builds and idempotent — the guarded CREATE
-- is a no-op where the index already exists.
--
-- Idempotent guarded-CREATE style matches V96 / V110 (inline PREPARE, no DELIMITER
-- so Flyway's statement splitter handles it).
-- ============================================================================

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    AND table_name = 'listings'
    AND index_name = 'idx_listings_shadow_draft_created');
SET @ddl = IF(@cnt = 0,
    'CREATE INDEX idx_listings_shadow_draft_created ON listings (is_shadow, is_draft, created_at)',
    'SELECT ''Index idx_listings_shadow_draft_created already exists'' AS message');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- Refresh optimizer statistics so the new index is costed correctly.
ANALYZE TABLE listings;
