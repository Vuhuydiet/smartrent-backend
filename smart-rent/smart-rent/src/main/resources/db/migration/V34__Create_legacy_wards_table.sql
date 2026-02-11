-- ============================================================================
-- Flyway Migration: V42__Create_legacy_wards_table.sql
-- Description: Create legacy wards table (10,040 wards before 2025 reform)
-- Date: 2025-11-16
-- Author: Generated Script
-- ============================================================================

-- drop table if exists legacy_wards;

CREATE TABLE legacy_wards (
    legacy_ward_id INT AUTO_INCREMENT PRIMARY KEY,
    province_code VARCHAR(10) NOT NULL,
    district_code VARCHAR(10) NOT NULL,
    ward_code VARCHAR(10) NOT NULL UNIQUE,
    province_name VARCHAR(255) NOT NULL,
    province_short_name VARCHAR(255) NOT NULL,
    district_name VARCHAR(255) NOT NULL,
    district_short_name VARCHAR(255) NOT NULL,
    ward_name VARCHAR(255) NOT NULL,
    ward_short_name VARCHAR(255) NOT NULL,
    district_type VARCHAR(50) NOT NULL,
    ward_type VARCHAR(50) NOT NULL,
    province_key VARCHAR(255),
    district_key VARCHAR(255),
    ward_key VARCHAR(255),
    province_latitude DECIMAL(10, 7),
    province_longitude DECIMAL(10, 7),
    district_latitude DECIMAL(10, 7),
    district_longitude DECIMAL(10, 7),
    ward_latitude DECIMAL(10, 7),
    ward_longitude DECIMAL(10, 7),
    province_bounds TEXT,
    province_geo_address TEXT,
    district_bounds TEXT,
    district_geo_address TEXT,
    ward_bounds TEXT,
    ward_geo_address TEXT,
    district_short_duplicated BOOLEAN DEFAULT FALSE,
    ward_short_duplicated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_legacy_district FOREIGN KEY (district_code) REFERENCES legacy_districts(district_code) ON DELETE CASCADE
);

---- Add comments to table and columns
--COMMENT ON TABLE legacy_wards IS 'Vietnam legacy wards (10,040 units before 2025 reform)';
--COMMENT ON COLUMN legacy_wards.province_code IS 'Reference to legacy province code';
--COMMENT ON COLUMN legacy_wards.district_code IS 'Reference to legacy district code';
--COMMENT ON COLUMN legacy_wards.ward_code IS 'Official ward code';
--COMMENT ON COLUMN legacy_wards.ward_name IS 'Full ward name (e.g., "Phường Phúc Xá")';
--COMMENT ON COLUMN legacy_wards.ward_short_name IS 'Short ward name (e.g., "Phúc Xá")';
--COMMENT ON COLUMN legacy_wards.district_type IS 'District type: Quận, Huyện, or Thị xã';
--COMMENT ON COLUMN legacy_wards.ward_type IS 'Ward type: Phường, Xã, or Thị trấn';
--COMMENT ON COLUMN legacy_wards.ward_latitude IS 'Ward center latitude';
--COMMENT ON COLUMN legacy_wards.ward_longitude IS 'Ward center longitude';
--COMMENT ON COLUMN legacy_wards.ward_bounds IS 'Geographic bounds (lat1,lon1 – lat2,lon2)';
--COMMENT ON COLUMN legacy_wards.ward_geo_address IS 'Reverse geocoded address';
--COMMENT ON COLUMN legacy_wards.ward_short_duplicated IS 'Flag if ward short name is duplicated';
