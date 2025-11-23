-- ============================================================================
-- Flyway Migration: V31__Create_indexes.sql
-- Description: Create indexes for provinces and wards tables to optimize queries
-- Date: 2025-11-16
-- Author: Generated Script
-- Note: MySQL 5.7 doesn't support IF NOT EXISTS for indexes, using DROP IF EXISTS first
-- ============================================================================

-- ============================================================================
-- PROVINCES TABLE INDEXES
-- ============================================================================

-- Index on province_code (already UNIQUE, but explicit index for foreign key lookups)
DROP INDEX IF EXISTS idx_provinces_code ON provinces;
CREATE INDEX idx_provinces_code ON provinces(province_code);

-- Index on province_key for lookup/search operations
DROP INDEX IF EXISTS idx_provinces_key ON provinces;
CREATE INDEX idx_provinces_key ON provinces(province_key);

-- Index on province_short_key for short name lookups
DROP INDEX IF EXISTS idx_provinces_short_key ON provinces;
CREATE INDEX idx_provinces_short_key ON provinces(province_short_key);

-- Index on province_name for full-text search
DROP INDEX IF EXISTS idx_provinces_name ON provinces;
CREATE INDEX idx_provinces_name ON provinces(province_name);

-- Index on province_short_name for short name search
DROP INDEX IF EXISTS idx_provinces_short_name ON provinces;
CREATE INDEX idx_provinces_short_name ON provinces(province_short_name);

-- Composite index for geographic queries (lat, lon)
DROP INDEX IF EXISTS idx_provinces_location ON provinces;
CREATE INDEX idx_provinces_location ON provinces(province_latitude, province_longitude);

-- Index on province_alias for alias lookup
DROP INDEX IF EXISTS idx_provinces_alias ON provinces;
CREATE INDEX idx_provinces_alias ON provinces(province_alias);

-- ============================================================================
-- WARDS TABLE INDEXES
-- ============================================================================

-- Index on ward_code (already UNIQUE, but explicit index)
DROP INDEX IF EXISTS idx_wards_code ON wards;
CREATE INDEX idx_wards_code ON wards(ward_code);

-- Index on province_code for foreign key and filtering by province
DROP INDEX IF EXISTS idx_wards_province_code ON wards;
CREATE INDEX idx_wards_province_code ON wards(province_code);

-- Index on ward_key for lookup/search operations
DROP INDEX IF EXISTS idx_wards_key ON wards;
CREATE INDEX idx_wards_key ON wards(ward_key);

-- Index on ward_short_key for short name lookups
DROP INDEX IF EXISTS idx_wards_short_key ON wards;
CREATE INDEX idx_wards_short_key ON wards(ward_short_key);

-- Index on ward_name for full-text search
DROP INDEX IF EXISTS idx_wards_name ON wards;
CREATE INDEX idx_wards_name ON wards(ward_name);

-- Index on ward_short_name for short name search
DROP INDEX IF EXISTS idx_wards_short_name ON wards;
CREATE INDEX idx_wards_short_name ON wards(ward_short_name);

-- Index on ward_type for filtering by type (Phường/Xã/Thị trấn)
DROP INDEX IF EXISTS idx_wards_type ON wards;
CREATE INDEX idx_wards_type ON wards(ward_type);

-- Index for ward_unique flag queries (MySQL doesn't support partial indexes)
DROP INDEX IF EXISTS idx_wards_unique ON wards;
CREATE INDEX idx_wards_unique ON wards(ward_unique);

-- Composite index for geographic queries (lat, lon)
DROP INDEX IF EXISTS idx_wards_location ON wards;
CREATE INDEX idx_wards_location ON wards(ward_latitude, ward_longitude);

-- Composite index for province + ward lookup
DROP INDEX IF EXISTS idx_wards_province_ward ON wards;
CREATE INDEX idx_wards_province_ward ON wards(province_code, ward_code);

-- Composite index for province + ward name lookup
DROP INDEX IF EXISTS idx_wards_province_name_ward_name ON wards;
CREATE INDEX idx_wards_province_name_ward_name ON wards(province_name, ward_name);

-- Index on ward_area_km2 for area-based queries
DROP INDEX IF EXISTS idx_wards_area ON wards;
CREATE INDEX idx_wards_area ON wards(ward_area_km2);
