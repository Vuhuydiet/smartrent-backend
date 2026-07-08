-- Migration V98: Denormalize lat/lng onto listings + covering index for
-- /map-bounds, so the bounding-box query drops the addresses join entirely.
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
-- JOIN: for each in-box address it looks up listings by address_id then fetches
-- the WIDE clustered row (LONGTEXT description) to read the visibility flags +
-- sort keys -- ~24k random disk reads on the tiny buffer pool.
--
-- FIX. latitude/longitude are fixed reference data on the address; copy them
-- onto listings (exactly like V97 did for the province/ward keys) so the map
-- filter (bbox + visibility) AND the sort all live on listings. The query then
-- becomes a single-table index range scan on idx_listings_map_bounds -- no join
-- to addresses at all. Equality prefix (is_draft, verified, expired) + primary
-- range (latitude) + covering suffix (longitude, expiry_date + the sort keys)
-- so the scan is index-only; only the final LIMIT 200 rows fetch full rows.
--
-- Order: add columns -> backfill -> then index (index the populated columns once
-- rather than maintain it during the bulk UPDATE). Idempotent via
-- information_schema checks, matching V97 style. The @PrePersist/@PreUpdate hook
-- (Listing.updateSearchFields) keeps the copies in sync on create/update.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Add the two denormalized coordinate columns (nullable -- a listing whose
--    address has no coordinates simply won't appear on the map).
--    Types mirror addresses.latitude DECIMAL(10,8) / longitude DECIMAL(11,8).
-- ---------------------------------------------------------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND column_name = 'latitude');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE listings ADD COLUMN latitude DECIMAL(10,8) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND column_name = 'longitude');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE listings ADD COLUMN longitude DECIMAL(11,8) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. Backfill from addresses (one-time; addresses are fixed reference data).
--    PK-join on listings.address_id, runs before the index exists.
-- ---------------------------------------------------------------------------
UPDATE listings l
JOIN addresses a ON l.address_id = a.address_id
SET l.latitude  = a.latitude,
    l.longitude = a.longitude;

-- ---------------------------------------------------------------------------
-- 3. Covering index for ListingSpecification.withinMapBounds. Equality prefix
--    (is_draft, verified, expired) + latitude range + covering suffix
--    (longitude, expiry_date, vip_type_sort_order, updated_at, listing_id) so
--    the whole filter + sort-key read is index-only, no clustered-row fetch.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_map_bounds');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_map_bounds ON listings (is_draft, verified, expired, latitude, longitude, expiry_date, vip_type_sort_order, updated_at, listing_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4. Refresh optimizer statistics so the planner adopts the new index/columns
--    immediately. Fast on the ~100k-row table.
-- ---------------------------------------------------------------------------
ANALYZE TABLE listings;
