-- Migration: Create wards table for Vietnam administrative units
-- Description: Creates the wards table to store wards/communes data (phường/xã/thị trấn)
-- Author: Generated from vietnamadminunits dataset
-- Date: 2025-11-16

-- drop table if exists wards;
DROP TABLE IF EXISTS wards;

CREATE TABLE wards (
    ward_id INT AUTO_INCREMENT PRIMARY KEY,
    province_code VARCHAR(10) NOT NULL,
    ward_code VARCHAR(10) NOT NULL UNIQUE,
    province_name VARCHAR(255) NOT NULL,
    province_short_name VARCHAR(255) NOT NULL,
    ward_name VARCHAR(255) NOT NULL,
    ward_short_name VARCHAR(255) NOT NULL,
    ward_type VARCHAR(50) NOT NULL,
    province_key VARCHAR(255),
    province_short_key VARCHAR(255),
    ward_key VARCHAR(255),
    ward_short_key VARCHAR(255),
    province_latitude DECIMAL(10, 7),
    province_longitude DECIMAL(10, 7),
    ward_latitude DECIMAL(10, 7),
    ward_longitude DECIMAL(10, 7),
    ward_area_km2 DECIMAL(10, 2),
    ward_key_duplicated BOOLEAN DEFAULT FALSE,
    ward_short_key_duplicated BOOLEAN DEFAULT FALSE,
    ward_unique BOOLEAN DEFAULT FALSE,
    ward_alias VARCHAR(100),
    ward_keywords TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_province FOREIGN KEY (province_code) REFERENCES provinces(province_code) ON DELETE CASCADE
);

-- Add comments to table and columns
--COMMENT ON TABLE wards IS 'Vietnam wards, communes and towns (phường/xã/thị trấn) - 3,321 units as of 2025';
--COMMENT ON COLUMN wards.province_code IS 'Reference to province code';
--COMMENT ON COLUMN wards.ward_code IS 'Official ward code (5 digits)';
--COMMENT ON COLUMN wards.ward_name IS 'Full ward name (e.g., "Phường Hồng Hà")';
--COMMENT ON COLUMN wards.ward_short_name IS 'Short ward name (e.g., "Hồng Hà")';
--COMMENT ON COLUMN wards.ward_type IS 'Ward type: Phường, Xã, or Thị trấn';
--COMMENT ON COLUMN wards.ward_latitude IS 'Ward center latitude';
--COMMENT ON COLUMN wards.ward_longitude IS 'Ward center longitude';
--COMMENT ON COLUMN wards.ward_area_km2 IS 'Ward area in square kilometers';
--COMMENT ON COLUMN wards.ward_key_duplicated IS 'Flag if ward key is duplicated across country';
--COMMENT ON COLUMN wards.ward_unique IS 'Flag if ward name is unique in the country';
