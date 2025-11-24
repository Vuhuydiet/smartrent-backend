-- Update addresses table to support nested legacy/new address structure
-- This migration adds detailed address component columns for better data granularity

-- ==================== LEGACY ADDRESS STRUCTURE (63 provinces) ====================

-- Add legacy address components
ALTER TABLE addresses
    ADD COLUMN legacy_province_id INT COMMENT 'Legacy province ID (63 provinces structure)',
    ADD COLUMN legacy_district_id INT COMMENT 'Legacy district ID',
    ADD COLUMN legacy_ward_id INT COMMENT 'Legacy ward ID',
    ADD COLUMN legacy_street VARCHAR(255) COMMENT 'Legacy street name';

-- ==================== NEW ADDRESS STRUCTURE (34 provinces) ====================

-- Add new address components
ALTER TABLE addresses
    ADD COLUMN new_province_code VARCHAR(10) COMMENT 'New province code (34 provinces structure)',
    ADD COLUMN new_ward_code VARCHAR(10) COMMENT 'New ward code (2-tier structure)',
    ADD COLUMN new_street VARCHAR(255) COMMENT 'New street name';

-- ==================== COMMON FIELDS ====================

-- Add common fields
ALTER TABLE addresses
    ADD COLUMN address_type ENUM('OLD', 'NEW') NOT NULL DEFAULT 'OLD' COMMENT 'Address structure type',
    ADD COLUMN project_id INT COMMENT 'Project/building/complex ID (optional)';

-- ==================== INDEXES FOR PERFORMANCE ====================

-- Indexes for legacy structure queries
CREATE INDEX idx_legacy_province ON addresses(legacy_province_id);
CREATE INDEX idx_legacy_district ON addresses(legacy_district_id);
CREATE INDEX idx_legacy_ward ON addresses(legacy_ward_id);
CREATE INDEX idx_legacy_location ON addresses(legacy_province_id, legacy_district_id, legacy_ward_id);

-- Indexes for new structure queries
CREATE INDEX idx_new_province ON addresses(new_province_code);
CREATE INDEX idx_new_ward ON addresses(new_ward_code);
CREATE INDEX idx_new_location ON addresses(new_province_code, new_ward_code);

-- Index for address type
CREATE INDEX idx_address_type ON addresses(address_type);

-- ==================== COMMENTS ====================

-- Update table comment
ALTER TABLE addresses COMMENT = 'Addresses table supporting both legacy (63 provinces, 3-tier) and new (34 provinces, 2-tier) structures with nested data';

