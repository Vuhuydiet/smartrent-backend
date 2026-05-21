-- Migration V81: Indexes for the public search/filter hot paths
-- ============================================================================
-- Targets the search code in ListingSpecification.fromFilterRequest, the
-- pricing-history EXISTS subqueries, and the address INNER JOIN that backs
-- every location-filtered query. V80 covered the category-scoped paths after
-- the moderation-gate switch — this round closes the remaining gaps that
-- EXPLAIN still showed full scans / filesorts on the 100k-row prod DB
-- (measured 2026-05-20):
--
--   • POST /v1/listings/search with provinceId=...   : ~1.4s  (addresses scan)
--   • POST /v1/listings/search with hasPriceReduction: ~1.1s  (subquery scan)
--   • POST /v1/listings/map-bounds                   : ~0.9s  (lat/lng scan)
--   • GET  /v1/listings (my listings, sort=NEWEST)   : ~0.7s  (filesort)
--
-- Idempotent via information_schema check, matching V78/V80 style.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. addresses(legacy_province_id, legacy_district_id, legacy_ward_id)
--    Backs the OLD-structure location filter. Left-prefix indexable for
--    province-only, province+district, and full triple — the three legacy
--    drill-down shapes the FE sends. addresses currently has NO indexes, so
--    every join on listings.address_id devolves into a hash-join against a
--    full table scan.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*)
FROM information_schema.statistics
WHERE table_schema = DATABASE
()
                     AND table_name = 'addresses'
                     AND index_name = 'idx_addresses_legacy_loc');
SET @sql =
IF(@idx_exists = 0,
    'CREATE INDEX idx_addresses_legacy_loc ON addresses (legacy_province_id, legacy_district_id, legacy_ward_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. addresses(new_province_code, new_ward_code)
--    Backs the NEW (2-tier, 34-province) location filter. The spec OR's
--    legacy and new predicates, so both indexes are needed to avoid a
--    full-scan branch on either side.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*)
FROM information_schema.statistics
WHERE table_schema = DATABASE
()
                     AND table_name = 'addresses'
                     AND index_name = 'idx_addresses_new_loc');
SET @sql =
IF(@idx_exists = 0,
    'CREATE INDEX idx_addresses_new_loc ON addresses (new_province_code, new_ward_code)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. addresses(latitude, longitude)
--    Backs the map-bounds query in ListingSpecification.withinMapBounds
--    (BETWEEN lat AND BETWEEN lng). InnoDB can't use SPATIAL without a
--    POINT column, so a composite B-tree is the right shape here — the
--    leading lat range narrows aggressively, lng filters the residual.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*)
FROM information_schema.statistics
WHERE table_schema = DATABASE
()
                     AND table_name = 'addresses'
                     AND index_name = 'idx_addresses_geo');
SET @sql =
IF(@idx_exists = 0,
    'CREATE INDEX idx_addresses_geo ON addresses (latitude, longitude)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4. pricing_histories(listing_id, is_current, change_type, changed_at)
--    Replaces the prior single-column idx_is_current for the search path.
--    Every price-change EXISTS subquery in ListingSpecification filters on
--    exactly (listing_id, is_current=true, change_type=DECREASE|INCREASE) and
--    optionally narrows by changed_at >= cutoff. Existing idx_listing_date
--    (listing_id, changed_at) can't seek past changed_at when is_current
--    and change_type are also predicates — this composite makes the whole
--    subquery a single index seek.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*)
FROM information_schema.statistics
WHERE table_schema = DATABASE
()
                     AND table_name = 'pricing_histories'
                     AND index_name = 'idx_pricing_listing_current_type');
SET @sql =
IF(@idx_exists = 0,
    'CREATE INDEX idx_pricing_listing_current_type ON pricing_histories (listing_id, is_current, change_type, changed_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 5. listings(user_id, is_draft, updated_at)
--    My-listings query: WHERE user_id = ? AND is_draft = ? ORDER BY updated_at.
--    Existing idx_user_created is (user_id, created_at) — the planner still
--    has to filter is_draft in memory and the sort uses created_at, not
--    updated_at, so it filesorts. This shape is index-ordered for the LIMIT.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*)
FROM information_schema.statistics
WHERE table_schema = DATABASE
()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_user_draft_updated');
SET @sql =
IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_user_draft_updated ON listings (user_id, is_draft, updated_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 6. listings(pushed_at)
--    Recommendation engine (findCandidatesForSimilar / Personalized) and the
--    title-suggestion native query both ORDER BY pushed_at DESC after a
--    selective WHERE. Without an index here the LIMIT 20 still pays for a
--    full filesort of the candidate set.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*)
FROM information_schema.statistics
WHERE table_schema = DATABASE
()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_pushed_at');
SET @sql =
IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_pushed_at ON listings (pushed_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 7. listings(parent_listing_id)
--    Backs findByParentListingId (shadow-listing lookups) and the
--    `parent_listing_id IS NULL` predicate used to exclude shadows from
--    public search. Without it both paths scan the listings table.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*)
FROM information_schema.statistics
WHERE table_schema = DATABASE
()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_parent');
SET @sql =
IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_parent ON listings (parent_listing_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
