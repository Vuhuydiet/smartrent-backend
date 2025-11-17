-- ============================================================================
-- Flyway Migration: V43__Create_address_mapping_table.sql
-- Description: Create address mapping table for legacy to 2025 conversion
-- Date: 2025-11-16
-- Author: Generated Script
-- ============================================================================

-- drop table if exists address_mapping;
DROP TABLE IF EXISTS address_mapping;

CREATE TABLE address_mapping (
    mapping_id INT AUTO_INCREMENT PRIMARY KEY,

    -- Legacy address (63 provinces)
    legacy_province_code VARCHAR(10) NOT NULL,
    legacy_district_code VARCHAR(10),
    legacy_ward_code VARCHAR(10),
    legacy_province_name VARCHAR(255) NOT NULL,
    legacy_district_name VARCHAR(255),
    legacy_ward_name VARCHAR(255),
    legacy_province_short VARCHAR(255),
    legacy_district_short VARCHAR(255),
    legacy_ward_short VARCHAR(255),

    -- New address (34 provinces)
    new_province_code VARCHAR(10) NOT NULL,
    new_ward_code VARCHAR(10),
    new_province_name VARCHAR(255) NOT NULL,
    new_ward_name VARCHAR(255),
    new_province_short VARCHAR(255),
    new_ward_short VARCHAR(255),
    new_ward_type VARCHAR(50),

    -- Geographic data
    legacy_province_lat DECIMAL(10, 7),
    legacy_province_lon DECIMAL(10, 7),
    legacy_district_lat DECIMAL(10, 7),
    legacy_district_lon DECIMAL(10, 7),
    legacy_ward_lat DECIMAL(10, 7),
    legacy_ward_lon DECIMAL(10, 7),
    new_province_lat DECIMAL(10, 7),
    new_province_lon DECIMAL(10, 7),
    new_ward_lat DECIMAL(10, 7),
    new_ward_lon DECIMAL(10, 7),
    new_ward_area_km2 DECIMAL(10, 4),  -- Changed from DECIMAL(10,2) to DECIMAL(10,4) to accommodate 3+ decimal places

    -- Mapping metadata flags
    is_merged_province BOOLEAN DEFAULT FALSE,
    is_merged_ward BOOLEAN DEFAULT FALSE,
    is_divided_ward BOOLEAN DEFAULT FALSE,
    is_default_new_ward BOOLEAN DEFAULT FALSE,
    is_nearest_new_ward BOOLEAN DEFAULT FALSE,
    is_new_ward_polygon_contains_ward BOOLEAN DEFAULT FALSE,

    -- Bounds data
    legacy_province_bounds TEXT,
    legacy_district_bounds TEXT,
    legacy_ward_bounds TEXT,

    -- Address data
    legacy_province_geo_address TEXT,
    legacy_district_geo_address TEXT,
    legacy_ward_geo_address TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_mapping_legacy_province FOREIGN KEY (legacy_province_code) REFERENCES legacy_provinces(province_code),
    CONSTRAINT fk_mapping_legacy_district FOREIGN KEY (legacy_district_code) REFERENCES legacy_districts(district_code),
    CONSTRAINT fk_mapping_legacy_ward FOREIGN KEY (legacy_ward_code) REFERENCES legacy_wards(ward_code),
    CONSTRAINT fk_mapping_new_province FOREIGN KEY (new_province_code) REFERENCES provinces(province_code),
    CONSTRAINT fk_mapping_new_ward FOREIGN KEY (new_ward_code) REFERENCES wards(ward_code)
);


-- Create indexes for common queries
CREATE INDEX idx_mapping_legacy_province ON address_mapping(legacy_province_code);
CREATE INDEX idx_mapping_legacy_district ON address_mapping(legacy_district_code);
CREATE INDEX idx_mapping_legacy_ward ON address_mapping(legacy_ward_code);
CREATE INDEX idx_mapping_new_province ON address_mapping(new_province_code);
CREATE INDEX idx_mapping_new_ward ON address_mapping(new_ward_code);
-- MySQL doesn't support partial indexes, removed WHERE clauses
CREATE INDEX idx_mapping_merged_province ON address_mapping(is_merged_province);
CREATE INDEX idx_mapping_merged_ward ON address_mapping(is_merged_ward);
CREATE INDEX idx_mapping_divided_ward ON address_mapping(is_divided_ward);
CREATE INDEX idx_mapping_default_ward ON address_mapping(is_default_new_ward);

-- Composite index for full address lookup
CREATE INDEX idx_mapping_full_legacy_address ON address_mapping(legacy_province_code, legacy_district_code, legacy_ward_code);
CREATE INDEX idx_mapping_full_new_address ON address_mapping(new_province_code, new_ward_code);
