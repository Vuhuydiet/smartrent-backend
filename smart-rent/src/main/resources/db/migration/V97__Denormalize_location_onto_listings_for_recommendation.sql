-- Migration V97: Denormalize address location keys onto listings + composite
-- indexes for the recommendation candidate-retrieval hot path.
-- ============================================================================
-- WHY. The recommendation candidate queries (RecommendationServiceImpl,
-- getPersonalizedFeed / getSimilarListings) FILTER on addresses columns
-- (new_province_code / new_ward_code / legacy_district_id / legacy_ward_id /
-- legacy_province_id) but SORT on listings columns (pushed_at, post_date). No
-- single B-tree index can serve a filter on one table and an ORDER BY on
-- another, so MySQL filesorts the whole province-matched set (tens of thousands
-- of rows for a big province) just to return ~100. On the 1GB / 32MB-buffer-pool
-- prod DB, three such channels running in parallel took 63-81s (measured via the
-- [PerfTrace] log: candidateRetrieval≈63000ms, everything else in ms).
--
-- FIX. Copy the five location keys onto listings (addresses are fixed reference
-- data — they do not change), then build one composite index per location shape
-- with the visibility booleans as the equality prefix and (pushed_at, post_date)
-- as the ordered suffix — mirroring idx_listings_public_default_sort (V82). Each
-- candidate query becomes a single-table index-ordered range scan: no filesort,
-- reads only ~LIMIT rows, fast even on a 32MB buffer pool.
--
-- Order matters: add columns → backfill → THEN create indexes (indexing populated
-- columns once is cheaper than maintaining six indexes during the bulk UPDATE).
-- Idempotent via information_schema checks, matching V81/V82/V83 style.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Add the five denormalized columns (nullable — a listing has either a new
--    code or only a legacy id, exactly like its address).
-- ---------------------------------------------------------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND column_name = 'new_province_code');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE listings ADD COLUMN new_province_code VARCHAR(10) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND column_name = 'new_ward_code');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE listings ADD COLUMN new_ward_code VARCHAR(10) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND column_name = 'legacy_province_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE listings ADD COLUMN legacy_province_id INT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND column_name = 'legacy_district_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE listings ADD COLUMN legacy_district_id INT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND column_name = 'legacy_ward_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE listings ADD COLUMN legacy_ward_id INT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. Backfill from addresses (one-time; addresses are fixed reference data).
--    Joins on listings.address_id → addresses PK (idx_address on listings),
--    so this is an efficient PK-join UPDATE. Runs before indexes exist.
-- ---------------------------------------------------------------------------
UPDATE listings l
JOIN addresses a ON l.address_id = a.address_id
SET l.new_province_code  = a.new_province_code,
    l.new_ward_code      = a.new_ward_code,
    l.legacy_province_id = a.legacy_province_id,
    l.legacy_district_id = a.legacy_district_id,
    l.legacy_ward_id     = a.legacy_ward_id;

-- ---------------------------------------------------------------------------
-- 3. Composite indexes — one per location shape. Equality prefix
--    (location + visibility booleans) + ordered suffix (pushed_at, post_date)
--    ⇒ index-ordered scan, no filesort. Mirrors idx_listings_public_default_sort.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_reco_new_prov');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_reco_new_prov ON listings (new_province_code, is_draft, is_shadow, verified, expired, pushed_at, post_date)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_reco_new_ward');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_reco_new_ward ON listings (new_ward_code, is_draft, is_shadow, verified, expired, pushed_at, post_date)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_reco_legacy_dist');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_reco_legacy_dist ON listings (legacy_district_id, is_draft, is_shadow, verified, expired, pushed_at, post_date)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_reco_legacy_ward');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_reco_legacy_ward ON listings (legacy_ward_id, is_draft, is_shadow, verified, expired, pushed_at, post_date)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_reco_legacy_prov');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_reco_legacy_prov ON listings (legacy_province_id, is_draft, is_shadow, verified, expired, pushed_at, post_date)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_reco_fresh');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_reco_fresh ON listings (is_draft, is_shadow, verified, expired, pushed_at, post_date)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4. Refresh optimizer statistics so the planner immediately sees the new
--    indexes as selective and picks them (verified via EXPLAIN: the forced plan
--    is a Backward-index-scan on idx_listings_reco_new_prov with NO filesort;
--    without fresh stats on a freshly-migrated DB the planner can fall back to
--    idx_status + filesort). Fast on the ~100k-row table.
-- ---------------------------------------------------------------------------
ANALYZE TABLE listings;
