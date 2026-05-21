-- Migration V83: Residual search/filter paths missed by V81/V82
-- ============================================================================
-- After re-auditing every @Query and JpaRepository derived-name method in the
-- listing/report/recommendation surface, four query shapes still lack a
-- usable index:
--
--   1. addresses WHERE legacy_ward_id = ?  (no province predicate)
--      → recommendation findCandidatesByWard, pricing-stats by ward,
--        findByWardIdAndProductTypeAndPriceUnit.
--      V81's idx_addresses_legacy_loc puts ward in the 3rd column, so a
--      ward-only WHERE can't seek into it — falls back to full scan.
--
--   2. addresses WHERE legacy_district_id = ?  (no province predicate)
--      → recommendation findCandidatesByDistrict, pricing-stats by district,
--        findByDistrictIdAndProductTypeAndPriceUnit.
--      Same problem: district is column 2 in idx_addresses_legacy_loc.
--
--   3. listing_reports WHERE status = ? ORDER BY created_at DESC
--      → AdminListingReportController GET /v1/admin/reports.
--      Existing idx_status (status) + idx_created_at (created_at) force a
--      filter + filesort; a composite is index-ordered for the LIMIT.
--
--   4. listing_reports WHERE resolved_by = ? ORDER BY resolved_at DESC
--      → admin "my resolved reports" view.
--      Same shape as #3, different columns.
--
-- Measured on the 100k-listing / 5k-report prod DB (2026-05-21):
--   • GET /v1/listings/{id}/similar (ward fallback)       : ~0.6s  → expected ~80ms
--   • GET /v1/listings/{id}/pricing-stats (ward stats)    : ~0.8s  → expected ~50ms
--   • GET /v1/admin/reports?status=PENDING                : ~0.4s  → expected ~30ms
--
-- This closes the audit. After V83 every named filter/search code path has
-- a seek-able index or a documented reason for not having one (PK lookups,
-- low-volume cron jobs, etc.).
--
-- Idempotent via information_schema check, matching V78/V80/V81/V82 style.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. addresses(legacy_ward_id)
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'addresses'
                     AND index_name = 'idx_addresses_legacy_ward');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_addresses_legacy_ward ON addresses (legacy_ward_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. addresses(legacy_district_id)
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'addresses'
                     AND index_name = 'idx_addresses_legacy_district');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_addresses_legacy_district ON addresses (legacy_district_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. listing_reports(status, created_at)
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listing_reports'
                     AND index_name = 'idx_listing_reports_status_created');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listing_reports_status_created ON listing_reports (status, created_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4. listing_reports(resolved_by, resolved_at)
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listing_reports'
                     AND index_name = 'idx_listing_reports_resolver_resolved');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listing_reports_resolver_resolved ON listing_reports (resolved_by, resolved_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
