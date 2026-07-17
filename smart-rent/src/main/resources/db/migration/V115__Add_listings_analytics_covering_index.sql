-- Migration V115: covering index for the admin /v1/admin/analytics/listings endpoint.
-- ============================================================================
-- That endpoint runs 5 aggregate queries over `listings`, all filtered by the
-- same predicate: (is_shadow = false, is_draft = false, created_at in range).
-- The daily time-series and the cumulative baseline COUNT are covered by
-- idx_listings_shadow_draft_created (is_shadow, is_draft, created_at) and run in
-- <200ms. But the three breakdown queries also read a column that is NOT in that
-- index — listing_type / product_type / verified — so the index stops being
-- covering for them. With the low-cardinality boolean statistics mis-estimating
-- selectivity, the optimizer then abandons it and falls back to idx_is_shadow /
-- idx_listings_public_search, scanning ~99k rows (the entire non-shadow table)
-- with random row lookups. Measured on prod: 5.1s / 6.4s / 34.3s for the three
-- breakdowns, so the endpoint took ~44s end-to-end and timed out at the proxy
-- (the admin console showed "fail to load" and retried in a loop).
--
-- This composite keeps the identical (is_shadow, is_draft) equality + created_at
-- range prefix, then appends the three breakdown columns purely as a covering
-- suffix. Every one of the five queries becomes an index-only range scan over
-- just the requested date slice (~15.7k rows). Verified on prod: the product_type
-- breakdown dropped from 6.38s to 0.07s (rows examined 99,029 -> 15,785).
--
-- created_at stays the last range-usable column; the trailing three columns are
-- never used for filtering, only to avoid table access.
--
-- idx_listings_shadow_draft_created is now a STRICT PREFIX of this index (it also
-- serves the daily series and the baseline COUNT), so it is dropped as redundant.
-- Note: idx_listings_shadow_draft_created was created directly on prod during a
-- 2026-07-12 index audit but never codified as a migration, which is exactly why
-- it survived on the old DB yet this whole endpoint regressed after listings were
-- reshaped to 100k and the DB moved hosts — nothing recreated it, and stale stats
-- pushed the breakdowns onto the boolean indexes. Codifying here so fresh Flyway
-- builds and future host moves keep the covering index.
--
-- Idempotent guarded style matches V94-V110 (inline PREPARE, no DELIMITER so
-- Flyway's statement splitter handles it). On prod both DDLs are effectively
-- no-ops: the covering index already exists (created during diagnosis) and the
-- prefix index still needs dropping.
-- ============================================================================

-- 1) Create the covering index (no-op if it already exists).
SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'listings' AND index_name = 'idx_listings_analytics_breakdown');
SET @ddl = IF(@cnt = 0, 'CREATE INDEX idx_listings_analytics_breakdown ON listings (is_shadow, is_draft, created_at, listing_type, product_type, verified)', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) Drop the now-redundant prefix index (no-op if already gone).
SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'listings' AND index_name = 'idx_listings_shadow_draft_created');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE listings DROP INDEX idx_listings_shadow_draft_created', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3) Refresh optimizer statistics so the new index is costed correctly.
ANALYZE TABLE listings;
