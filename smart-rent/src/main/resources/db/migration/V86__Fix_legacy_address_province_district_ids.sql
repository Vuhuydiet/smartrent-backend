-- Migration V86: Repair corrupted legacy_province_id / legacy_district_id on addresses
-- ============================================================================
-- BUG: the bulk address seeder (scripts/populate_addresses_for_listings.sql)
-- wrote the official GSO *codes* into the addresses.legacy_province_id and
-- addresses.legacy_district_id columns instead of the legacy_provinces /
-- legacy_districts surrogate primary keys those columns are supposed to hold:
--
--     legacy_province_id  <- CAST(province_code AS UNSIGNED)   -- wrong
--     legacy_district_id  <- CAST(district_code AS UNSIGNED)   -- wrong
--
-- Because legacy_provinces.legacy_province_id is AUTO_INCREMENT (assigned in
-- code order, 1..63) it diverges from province_code for every province past
-- the second. e.g. Đồng Tháp: code '87' but PK 56; Khánh Hòa: code '56' but
-- PK 37. The frontend province dropdown emits the PK (province.id) and
-- ListingSpecification matches addresses.legacy_province_id against that PK,
-- so filtering "Đồng Tháp" (provinceId=56) returned Khánh Hòa listings
-- (whose seeded legacy_province_id happened to be the code 56).
--
-- FIX: the seeder stored legacy_ward_id CORRECTLY (the real legacy_wards PK),
-- so we re-derive the right province/district PKs from the ward. The update
-- is idempotent — the WHERE clause only touches rows whose stored value
-- disagrees with the derived-correct value, so genuine user-created listings
-- (which already hold the right PK) and re-runs are no-ops.
--
-- legacy_districts.district_code is globally UNIQUE, so joining on it alone
-- resolves the district unambiguously.
-- ============================================================================

UPDATE addresses a
JOIN legacy_wards     w ON w.legacy_ward_id = a.legacy_ward_id
JOIN legacy_provinces p ON p.province_code  = w.province_code
JOIN legacy_districts d ON d.district_code  = w.district_code
SET a.legacy_province_id = p.legacy_province_id,
    a.legacy_district_id = d.legacy_district_id
WHERE a.legacy_ward_id IS NOT NULL
  AND (a.legacy_province_id <> p.legacy_province_id
       OR a.legacy_district_id <> d.legacy_district_id);
