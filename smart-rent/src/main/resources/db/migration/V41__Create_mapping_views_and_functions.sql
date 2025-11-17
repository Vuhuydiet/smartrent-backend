-- ============================================================================
-- Flyway Migration: V41__Create_mapping_views_and_functions.sql
-- Description: Create views and stored procedures for province/address mapping
-- Date: 2025-11-16
-- Author: Generated Script (MySQL version)
-- ============================================================================

-- ============================================================================
-- VIEW 1: Province Mapping (Legacy → New)
-- ============================================================================

CREATE OR REPLACE VIEW v_province_mapping AS
SELECT DISTINCT
    lp.province_code AS legacy_province_code,
    lp.province_name AS legacy_province_name,
    lp.province_short_name AS legacy_province_short,
    p.province_code AS new_province_code,
    p.province_name AS new_province_name,
    p.province_short_name AS new_province_short,
    CASE
        WHEN lp.province_code != p.province_code THEN TRUE
        ELSE FALSE
    END AS is_province_changed,
    am.is_merged_province
FROM legacy_provinces lp
JOIN address_mapping am ON lp.province_code = am.legacy_province_code
JOIN provinces p ON am.new_province_code = p.province_code
ORDER BY lp.province_code;

-- MySQL view comments stored as documentation:
-- v_province_mapping: Mapping between legacy provinces (63) and new provinces (34)

-- ============================================================================
-- VIEW 2: District Mapping Summary
-- ============================================================================

CREATE OR REPLACE VIEW v_district_mapping AS
SELECT DISTINCT
    ld.province_code AS legacy_province_code,
    ld.district_code AS legacy_district_code,
    ld.district_name AS legacy_district_name,
    ld.district_short_name AS legacy_district_short,
    ld.district_type,
    am.new_province_code,
    p.province_name AS new_province_name,
    COUNT(DISTINCT am.new_ward_code) AS num_new_wards
FROM legacy_districts ld
JOIN address_mapping am ON ld.district_code = am.legacy_district_code
JOIN provinces p ON am.new_province_code = p.province_code
GROUP BY
    ld.province_code,
    ld.district_code,
    ld.district_name,
    ld.district_short_name,
    ld.district_type,
    am.new_province_code,
    p.province_name
ORDER BY ld.province_code, ld.district_code;

-- v_district_mapping: Legacy districts mapped to new provinces (districts were abolished in 2025)

-- ============================================================================
-- VIEW 3: Ward Mapping (Legacy → New)
-- ============================================================================

CREATE OR REPLACE VIEW v_ward_mapping AS
SELECT
    -- Legacy address
    am.legacy_province_code,
    am.legacy_district_code,
    am.legacy_ward_code,
    am.legacy_province_name,
    am.legacy_district_name,
    am.legacy_ward_name,
    CONCAT(am.legacy_province_short, ', ', am.legacy_district_short, ', ', am.legacy_ward_short) AS legacy_full_address,

    -- New address
    am.new_province_code,
    am.new_ward_code,
    am.new_province_name,
    am.new_ward_name,
    CONCAT(am.new_province_short, ', ', am.new_ward_short) AS new_full_address,

    -- Mapping metadata
    am.is_merged_province,
    am.is_merged_ward,
    am.is_divided_ward,
    am.is_default_new_ward,
    am.is_nearest_new_ward,
    am.is_new_ward_polygon_contains_ward,

    -- Geographic data
    am.legacy_ward_lat,
    am.legacy_ward_lon,
    am.new_ward_lat,
    am.new_ward_lon,

    -- Calculate distance in km between legacy and new ward
    CASE
        WHEN am.legacy_ward_lat IS NOT NULL
         AND am.legacy_ward_lon IS NOT NULL
         AND am.new_ward_lat IS NOT NULL
         AND am.new_ward_lon IS NOT NULL
        THEN
            6371 * ACOS(
                COS(RADIANS(am.legacy_ward_lat))
                * COS(RADIANS(am.new_ward_lat))
                * COS(RADIANS(am.new_ward_lon) - RADIANS(am.legacy_ward_lon))
                + SIN(RADIANS(am.legacy_ward_lat))
                * SIN(RADIANS(am.new_ward_lat))
            )
        ELSE NULL
    END AS distance_km
FROM address_mapping am
ORDER BY am.legacy_province_code, am.legacy_district_code, am.legacy_ward_code;

-- v_ward_mapping: Complete ward mapping with distance calculation between legacy and new locations

-- ============================================================================
-- VIEW 4: Divided Wards (1 Legacy Ward → Multiple New Wards)
-- ============================================================================

CREATE OR REPLACE VIEW v_divided_wards AS
SELECT
    am.legacy_province_name,
    am.legacy_district_name,
    am.legacy_ward_name,
    am.legacy_ward_code,
    COUNT(*) AS num_new_wards,
    GROUP_CONCAT(am.new_ward_name ORDER BY am.new_ward_name SEPARATOR '; ') AS new_ward_names,
    MAX(CASE WHEN am.is_default_new_ward = TRUE THEN am.new_ward_name END) AS default_new_ward
FROM address_mapping am
WHERE am.is_divided_ward = TRUE
GROUP BY
    am.legacy_province_name,
    am.legacy_district_name,
    am.legacy_ward_name,
    am.legacy_ward_code
ORDER BY am.legacy_province_name, am.legacy_district_name, am.legacy_ward_name;

-- v_divided_wards: Legacy wards that were divided into multiple new wards

-- ============================================================================
-- VIEW 5: Merged Wards (Multiple Legacy Wards → 1 New Ward)
-- ============================================================================

CREATE OR REPLACE VIEW v_merged_wards AS
SELECT
    am.new_province_name,
    am.new_ward_name,
    am.new_ward_code,
    COUNT(*) AS num_legacy_wards,
    GROUP_CONCAT(
        CONCAT(am.legacy_province_name, ' - ', am.legacy_district_name, ' - ', am.legacy_ward_name)
        ORDER BY am.legacy_province_name, am.legacy_district_name, am.legacy_ward_name
        SEPARATOR '; '
    ) AS legacy_ward_names
FROM address_mapping am
WHERE am.is_merged_ward = TRUE
GROUP BY
    am.new_province_name,
    am.new_ward_name,
    am.new_ward_code
HAVING COUNT(*) > 1
ORDER BY am.new_province_name, am.new_ward_name;

-- v_merged_wards: New wards that merged multiple legacy wards

-- ============================================================================
-- STORED PROCEDURE 1: Convert Legacy Province to New Province
-- ============================================================================

DROP PROCEDURE IF EXISTS sp_convert_legacy_province_to_new;

DELIMITER $$

CREATE PROCEDURE sp_convert_legacy_province_to_new(
    IN p_legacy_province_code VARCHAR(10)
)
BEGIN
    SELECT DISTINCT
        p.province_code AS new_province_code,
        p.province_name AS new_province_name,
        p.province_short_name AS new_province_short,
        am.is_merged_province AS is_merged
    FROM address_mapping am
    JOIN provinces p ON am.new_province_code = p.province_code
    WHERE am.legacy_province_code = p_legacy_province_code
    LIMIT 1;
END$$

DELIMITER ;

-- sp_convert_legacy_province_to_new: Convert a legacy province code to new province info

-- ============================================================================
-- STORED PROCEDURE 2: Convert Legacy Full Address to New Address
-- ============================================================================

DROP PROCEDURE IF EXISTS sp_convert_legacy_address_to_new;

DELIMITER $$

CREATE PROCEDURE sp_convert_legacy_address_to_new(
    IN p_legacy_province_code VARCHAR(10),
    IN p_legacy_district_code VARCHAR(10),
    IN p_legacy_ward_code VARCHAR(10)
)
BEGIN
    SELECT
        am.new_province_code,
        am.new_province_name,
        am.new_ward_code,
        am.new_ward_name,
        CONCAT(am.new_province_short, ', ', am.new_ward_short) AS new_full_address,
        am.is_merged_ward,
        am.is_divided_ward,
        am.is_default_new_ward AS is_default_mapping,
        CASE
            WHEN am.legacy_ward_lat IS NOT NULL
             AND am.legacy_ward_lon IS NOT NULL
             AND am.new_ward_lat IS NOT NULL
             AND am.new_ward_lon IS NOT NULL
            THEN
                6371 * ACOS(
                    COS(RADIANS(am.legacy_ward_lat))
                    * COS(RADIANS(am.new_ward_lat))
                    * COS(RADIANS(am.new_ward_lon) - RADIANS(am.legacy_ward_lon))
                    + SIN(RADIANS(am.legacy_ward_lat))
                    * SIN(RADIANS(am.new_ward_lat))
                )
            ELSE NULL
        END AS distance_km
    FROM address_mapping am
    WHERE am.legacy_province_code = p_legacy_province_code
      AND am.legacy_district_code = p_legacy_district_code
      AND am.legacy_ward_code = p_legacy_ward_code
    ORDER BY
        -- Prioritize: polygon contains > default > nearest
        am.is_new_ward_polygon_contains_ward DESC,
        am.is_default_new_ward DESC,
        am.is_nearest_new_ward DESC;
END$$

DELIMITER ;

-- sp_convert_legacy_address_to_new: Convert a complete legacy address (province, district, ward) to new address with best match

-- ============================================================================
-- STORED PROCEDURE 3: Find All Possible New Addresses for Legacy Address
-- ============================================================================

DROP PROCEDURE IF EXISTS sp_find_all_new_addresses_for_legacy;

DELIMITER $$

CREATE PROCEDURE sp_find_all_new_addresses_for_legacy(
    IN p_legacy_province_code VARCHAR(10),
    IN p_legacy_district_code VARCHAR(10),
    IN p_legacy_ward_code VARCHAR(10)
)
BEGIN
    SELECT
        am.new_province_name,
        am.new_ward_name,
        CONCAT(am.new_province_short, ', ', am.new_ward_short) AS new_full_address,
        am.is_default_new_ward AS is_default,
        am.is_nearest_new_ward AS is_nearest,
        am.is_new_ward_polygon_contains_ward AS is_polygon_contains,
        CASE
            WHEN am.legacy_ward_lat IS NOT NULL
             AND am.legacy_ward_lon IS NOT NULL
             AND am.new_ward_lat IS NOT NULL
             AND am.new_ward_lon IS NOT NULL
            THEN
                6371 * ACOS(
                    COS(RADIANS(am.legacy_ward_lat))
                    * COS(RADIANS(am.new_ward_lat))
                    * COS(RADIANS(am.new_ward_lon) - RADIANS(am.legacy_ward_lon))
                    + SIN(RADIANS(am.legacy_ward_lat))
                    * SIN(RADIANS(am.new_ward_lat))
                )
            ELSE NULL
        END AS distance_km,
        CASE
            WHEN am.is_new_ward_polygon_contains_ward = TRUE THEN 1
            WHEN am.is_default_new_ward = TRUE THEN 2
            WHEN am.is_nearest_new_ward = TRUE THEN 3
            ELSE 4
        END AS mapping_priority
    FROM address_mapping am
    WHERE am.legacy_province_code = p_legacy_province_code
      AND am.legacy_district_code = p_legacy_district_code
      AND am.legacy_ward_code = p_legacy_ward_code
    ORDER BY mapping_priority, distance_km;
END$$

DELIMITER ;

-- sp_find_all_new_addresses_for_legacy: Find all possible new address mappings for a legacy address, sorted by priority

-- ============================================================================
-- STORED PROCEDURE 4: Get Province Change Summary
-- ============================================================================

DROP PROCEDURE IF EXISTS sp_get_province_change_summary;

DELIMITER $$

CREATE PROCEDURE sp_get_province_change_summary()
BEGIN
    SELECT
        (SELECT COUNT(*) FROM legacy_provinces) AS total_legacy_provinces,
        (SELECT COUNT(*) FROM provinces) AS total_new_provinces,
        (SELECT COUNT(DISTINCT legacy_province_code)
         FROM address_mapping
         WHERE is_merged_province = TRUE) AS merged_provinces,
        (SELECT COUNT(DISTINCT lp.province_code)
         FROM legacy_provinces lp
         JOIN provinces p ON lp.province_code = p.province_code) AS unchanged_provinces;
END$$

DELIMITER ;

-- sp_get_province_change_summary: Get summary statistics of province changes from 63 to 34

-- ============================================================================
-- INDEXES for better performance on views
-- ============================================================================

-- Already created in V35, but ensure these exist for view performance

-- CREATE INDEX IF NOT EXISTS (already created in V35) idx_mapping_legacy_full ON address_mapping(legacy_province_code, legacy_district_code, legacy_ward_code);
-- CREATE INDEX IF NOT EXISTS (already created in V35) idx_mapping_new_full ON address_mapping(new_province_code, new_ward_code);
-- CREATE INDEX IF NOT EXISTS (already created in V35) idx_mapping_flags ON address_mapping(is_merged_ward, is_divided_ward, is_default_new_ward);

-- ============================================================================
-- Documentation
-- ============================================================================

-- VIEWS:
-- - v_province_mapping: Shows legacy to new province mapping
-- - v_district_mapping: Shows legacy district to new ward mapping summary
-- - v_ward_mapping: Complete ward-level mapping with distance calculations
-- - v_divided_wards: Legacy wards that split into multiple new wards
-- - v_merged_wards: New wards created from merging multiple legacy wards
--
-- STORED PROCEDURES (call with CALL procedure_name(params)):
-- - sp_convert_legacy_province_to_new(legacy_code): Convert province code
-- - sp_convert_legacy_address_to_new(prov, dist, ward): Convert full address
-- - sp_find_all_new_addresses_for_legacy(prov, dist, ward): Find all possible mappings
-- - sp_get_province_change_summary(): Get summary statistics
--
-- Example usage:
-- CALL sp_convert_legacy_province_to_new('1');
-- CALL sp_convert_legacy_address_to_new('1', '001', '00001');
-- CALL sp_find_all_new_addresses_for_legacy('1', '001', '00001');
-- CALL sp_get_province_change_summary();
