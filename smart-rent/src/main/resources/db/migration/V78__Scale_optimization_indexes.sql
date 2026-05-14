-- Migration V78: Scale-optimization indexes for listings table (10k → 200k rows)
-- ============================================================================
-- Closes two gaps identified in docs/INDEX_AUDIT.md before the thesis
-- defense performance benchmark seeds the table to 200k rows:
--
-- 1. Province filtering after the listings→addresses JOIN forces a
--    ref-scan on addresses today. Adding (address_id, legacy_province_id)
--    + (address_id, new_province_code) lets MySQL serve the post-join
--    filter directly from the index.
--
-- 2. PRICE_ASC / PRICE_DESC sort spills into a JVM-side in-memory sort
--    in ListingQueryService:125-128 because no existing index can
--    deliver rows pre-sorted by price. Adding
--    (price, vip_type_sort_order, updated_at) lets the optimiser
--    stream sorted output from the index.
--
-- All three migrations are idempotent (existing-index check) following
-- the established project style (cf. V57).
--
-- Estimated index-size impact at 200k rows / ~30k addresses: < 6 MB.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. addresses (address_id, legacy_province_id) — covers post-join province
--    filter on the legacy 3-tier address structure.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'addresses'
                     AND index_name = 'idx_address_legacy_province');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_address_legacy_province ON addresses (address_id, legacy_province_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. addresses (address_id, new_province_code) — same as above for the
--    post-2025-07 2-tier address structure. Both indexes coexist because
--    every listing has one address_type and the query optimiser picks
--    whichever matches.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'addresses'
                     AND index_name = 'idx_address_new_province');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_address_new_province ON addresses (address_id, new_province_code)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. listings (price ASC, vip_type_sort_order ASC, updated_at DESC) —
--    covers PRICE_ASC and PRICE_DESC sort variants. The trailing columns
--    match the default secondary sort so the same index serves both
--    "cheapest first" and "premium tier first within price" without a
--    follow-up filesort.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_price_sort');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_price_sort ON listings (price, vip_type_sort_order, updated_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
