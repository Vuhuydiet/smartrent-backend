-- ============================================================================
-- Flyway Migration: V41__Create_legacy_districts_table.sql
-- Description: Create legacy districts table (quận/huyện/thị xã before 2025 reform)
-- Date: 2025-11-16
-- Author: Generated Script
-- ============================================================================

-- drop table if exists legacy_districts;

CREATE TABLE legacy_districts (
    legacy_district_id INT AUTO_INCREMENT PRIMARY KEY,
    province_code VARCHAR(10) NOT NULL,
    district_code VARCHAR(10) NOT NULL UNIQUE,
    province_name VARCHAR(255) NOT NULL,
    province_short_name VARCHAR(255) NOT NULL,
    district_name VARCHAR(255) NOT NULL,
    district_short_name VARCHAR(255) NOT NULL,
    district_type VARCHAR(50) NOT NULL,
    district_key VARCHAR(255),
    district_short_key VARCHAR(255),
    district_latitude DECIMAL(10, 7),
    district_longitude DECIMAL(10, 7),
    district_bounds TEXT,
    district_geo_address TEXT,
    district_short_duplicated BOOLEAN DEFAULT FALSE,
    district_alias VARCHAR(100),
    district_keywords TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_legacy_province FOREIGN KEY (province_code) REFERENCES legacy_provinces(province_code) ON DELETE CASCADE
);

---- Add comments to table and columns
--COMMENT ON TABLE legacy_districts IS 'Vietnam legacy districts (quận/huyện/thị xã before 2025 reform)';
--COMMENT ON COLUMN legacy_districts.province_code IS 'Reference to legacy province code';
--COMMENT ON COLUMN legacy_districts.district_code IS 'Official district code';
--COMMENT ON COLUMN legacy_districts.district_name IS 'Full district name (e.g., "Quận Ba Đình")';
--COMMENT ON COLUMN legacy_districts.district_short_name IS 'Short district name (e.g., "Ba Đình")';
--COMMENT ON COLUMN legacy_districts.district_type IS 'District type: Quận, Huyện, or Thị xã';
--COMMENT ON COLUMN legacy_districts.district_latitude IS 'District center latitude';
--COMMENT ON COLUMN legacy_districts.district_longitude IS 'District center longitude';
--COMMENT ON COLUMN legacy_districts.district_bounds IS 'Geographic bounds (lat1,lon1 – lat2,lon2)';
--COMMENT ON COLUMN legacy_districts.district_geo_address IS 'Reverse geocoded address';
--COMMENT ON COLUMN legacy_districts.district_short_duplicated IS 'Flag if district short name is duplicated';
