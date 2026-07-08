-- Migration V98: Covering index for the /map-bounds nested-loop join.
-- ============================================================================
-- WHY. EXPLAIN ANALYZE of the map-bounds query (zoom 11, ~9k matches) on the
-- 32MB-buffer-pool prod DB:
--
--   Sort (vip_type_sort_order, updated_at)            actual 20752ms
--     Nested loop inner join                          actual 20602ms   <-- cost
--       range scan addresses using idx_addresses_geo  actual   50ms (4156 rows)
--       Index lookup on listings using idx_address    actual  ~8ms x loops=2515
--
-- The geo index is fine (50ms) and the sort is cheap (~150ms). The 20s is the
-- join: for each of 2515 in-box addresses it looks up listings by address_id
-- (idx_address is address_id only), then must fetch the WIDE clustered row
-- (listings has a LONGTEXT description) to read the visibility flags + sort
-- keys. With a 32MB buffer pool those ~24k row fetches are random disk reads
-- (~8ms each) -> ~20s even single-threaded; concurrent panning pushed it to 42s.
--
-- FIX. A covering index whose leading column is address_id (the join key) and
-- which also carries every listings column the query reads -- the visibility
-- predicates (is_draft, verified, expired, expiry_date) and the sort keys
-- (vip_type_sort_order, updated_at, listing_id). The nested-loop lookup then
-- reads everything from the (dense) index leaf: no clustered-row fetch, the ~9
-- listings per address sit contiguously, so each address is ~1 page read
-- instead of ~9 random ones. Only the final LIMIT 200 rows fetch the full row.
--
-- Idempotent via information_schema check, matching V81/V97 style.
-- ============================================================================
SET @idx_exists = (SELECT COUNT(*)
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_map_bounds');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_map_bounds ON listings (address_id, is_draft, verified, expired, expiry_date, vip_type_sort_order, updated_at, listing_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Refresh optimizer statistics so the planner picks the new covering index for
-- the nested-loop lookup immediately. Fast on the ~100k-row table.
ANALYZE TABLE listings;
