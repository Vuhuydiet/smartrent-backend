-- ============================================================================
-- Flyway Migration: V40__Create_legacy_provinces_table.sql
-- Description: Create legacy provinces table (63 provinces before 2025 reform)
-- Date: 2025-11-16
-- Author: Generated Script
-- ============================================================================

-- drop table if exists legacy_provinces;

CREATE TABLE legacy_provinces (
    legacy_province_id INT AUTO_INCREMENT PRIMARY KEY,
    province_code VARCHAR(10) NOT NULL UNIQUE,
    province_name VARCHAR(255) NOT NULL,
    province_short_name VARCHAR(255) NOT NULL,
    province_key VARCHAR(255) NOT NULL,
    province_short_key VARCHAR(255),
    province_latitude DECIMAL(10, 7),
    province_longitude DECIMAL(10, 7),
    province_bounds TEXT,
    province_geo_address TEXT,
    province_alias VARCHAR(50),
    province_keywords TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

---- Add comments to table and columns
--COMMENT ON TABLE legacy_provinces IS 'Vietnam legacy provinces (63 units before 2025 administrative reform)';
--COMMENT ON COLUMN legacy_provinces.province_code IS 'Official province code (2 digits)';
--COMMENT ON COLUMN legacy_provinces.province_name IS 'Full province name (e.g., "Thành phố Hà Nội")';
--COMMENT ON COLUMN legacy_provinces.province_short_name IS 'Short province name (e.g., "Hà Nội")';
--COMMENT ON COLUMN legacy_provinces.province_key IS 'Normalized province key for lookup (no accents, lowercase)';
--COMMENT ON COLUMN legacy_provinces.province_latitude IS 'Province center latitude';
--COMMENT ON COLUMN legacy_provinces.province_longitude IS 'Province center longitude';
--COMMENT ON COLUMN legacy_provinces.province_bounds IS 'Geographic bounds (lat1,lon1 – lat2,lon2)';
--COMMENT ON COLUMN legacy_provinces.province_geo_address IS 'Reverse geocoded address';
