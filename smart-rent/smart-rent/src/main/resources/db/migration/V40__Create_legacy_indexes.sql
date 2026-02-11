-- ============================================================================
-- Flyway Migration: V48__Create_legacy_indexes.sql
-- Description: Create indexes for legacy tables to optimize queries
-- Date: 2025-11-16
-- Author: Generated Script
-- ============================================================================

-- ============================================================================
-- LEGACY PROVINCES TABLE INDEXES
-- ============================================================================

-- Index on province_code (already UNIQUE, but explicit index)
CREATE INDEX idx_legacy_provinces_code ON legacy_provinces(province_code);

-- Index on province_key for lookup/search operations
CREATE INDEX idx_legacy_provinces_key ON legacy_provinces(province_key);

-- Index on province_short_key for short name lookups
CREATE INDEX idx_legacy_provinces_short_key ON legacy_provinces(province_short_key);

-- Index on province_name for full-text search
CREATE INDEX idx_legacy_provinces_name ON legacy_provinces(province_name);

-- Index on province_short_name for short name search
CREATE INDEX idx_legacy_provinces_short_name ON legacy_provinces(province_short_name);

-- Composite index for geographic queries (lat, lon)
CREATE INDEX idx_legacy_provinces_location ON legacy_provinces(province_latitude, province_longitude);

-- ============================================================================
-- LEGACY DISTRICTS TABLE INDEXES
-- ============================================================================

-- Index on district_code (already UNIQUE, but explicit index)
CREATE INDEX idx_legacy_districts_code ON legacy_districts(district_code);

-- Index on province_code for filtering by province
CREATE INDEX idx_legacy_districts_province_code ON legacy_districts(province_code);

-- Index on district_key for lookup/search operations
CREATE INDEX idx_legacy_districts_key ON legacy_districts(district_key);

-- Index on district_short_key for short name lookups
CREATE INDEX idx_legacy_districts_short_key ON legacy_districts(district_short_key);

-- Index on district_name for full-text search
CREATE INDEX idx_legacy_districts_name ON legacy_districts(district_name);

-- Index on district_short_name for short name search
CREATE INDEX idx_legacy_districts_short_name ON legacy_districts(district_short_name);

-- Index on district_type for filtering by type
CREATE INDEX idx_legacy_districts_type ON legacy_districts(district_type);

-- Composite index for geographic queries
CREATE INDEX idx_legacy_districts_location ON legacy_districts(district_latitude, district_longitude);

-- Composite index for province + district lookup
CREATE INDEX idx_legacy_districts_province_district ON legacy_districts(province_code, district_code);

-- Index on duplicated flag (MySQL doesn't support partial indexes)
CREATE INDEX idx_legacy_districts_duplicated ON legacy_districts(district_short_duplicated);

-- ============================================================================
-- LEGACY WARDS TABLE INDEXES
-- ============================================================================

-- Index on ward_code (already UNIQUE, but explicit index)
CREATE INDEX idx_legacy_wards_code ON legacy_wards(ward_code);

-- Index on province_code for filtering by province
CREATE INDEX idx_legacy_wards_province_code ON legacy_wards(province_code);

-- Index on district_code for filtering by district
CREATE INDEX idx_legacy_wards_district_code ON legacy_wards(district_code);

-- Index on ward_key for lookup/search operations
CREATE INDEX idx_legacy_wards_key ON legacy_wards(ward_key);

-- Index on ward_name for full-text search
CREATE INDEX idx_legacy_wards_name ON legacy_wards(ward_name);

-- Index on ward_short_name for short name search
CREATE INDEX idx_legacy_wards_short_name ON legacy_wards(ward_short_name);

-- Index on ward_type for filtering by type
CREATE INDEX idx_legacy_wards_type ON legacy_wards(ward_type);

-- Composite index for geographic queries
CREATE INDEX idx_legacy_wards_location ON legacy_wards(ward_latitude, ward_longitude);

-- Composite index for full address lookup (province + district + ward)
CREATE INDEX idx_legacy_wards_full_address ON legacy_wards(province_code, district_code, ward_code);

-- Composite index for province + district
CREATE INDEX idx_legacy_wards_province_district ON legacy_wards(province_code, district_code);

-- Index on duplicated flags (MySQL doesn't support partial indexes)
CREATE INDEX idx_legacy_wards_district_duplicated ON legacy_wards(district_short_duplicated);
CREATE INDEX idx_legacy_wards_ward_duplicated ON legacy_wards(ward_short_duplicated);

-- ============================================================================
-- ANALYZE TABLES
-- ============================================================================

-- Update table statistics for query optimization (MySQL syntax)
ANALYZE TABLE legacy_provinces;
ANALYZE TABLE legacy_districts;
ANALYZE TABLE legacy_wards;
ANALYZE TABLE address_mapping;

-- ============================================================================
-- COMMENTS
-- ============================================================================

-- MySQL doesn't support COMMENT ON INDEX, these are documented here for reference:
-- idx_legacy_provinces_code: Index on legacy province code for fast lookups
-- idx_legacy_provinces_key: Index on normalized province key for search operations
-- idx_legacy_provinces_location: Composite index for geographic queries
--
-- idx_legacy_districts_code: Index on legacy district code for fast lookups
-- idx_legacy_districts_province_code: Index on province code for filtering districts by province
-- idx_legacy_districts_province_district: Composite index for province + district lookups
-- idx_legacy_districts_type: Index on district type for filtering by Quận/Huyện/Thị xã
--
-- idx_legacy_wards_code: Index on legacy ward code for fast lookups
-- idx_legacy_wards_district_code: Index on district code for filtering wards by district
-- idx_legacy_wards_full_address: Composite index for full legacy address lookups
-- idx_legacy_wards_type: Index on ward type for filtering by Phường/Xã/Thị trấn
