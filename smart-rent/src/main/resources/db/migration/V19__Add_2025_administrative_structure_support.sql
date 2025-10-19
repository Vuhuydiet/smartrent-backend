-- V19: Add support for Vietnam 2025 administrative reorganization (34 provinces, no districts)
-- This migration adds columns to support the new 2-tier structure (Province -> Ward)
-- while maintaining backward compatibility with the old 3-tier structure

-- Add direct province relationship to wards table for 2025 structure (idempotent)
SET @col_exists_province = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'wards'
    AND COLUMN_NAME = 'province_id'
);

SET @col_exists_2025 = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'wards'
    AND COLUMN_NAME = 'is_2025_structure'
);

-- Add province_id column only if it doesn't exist
SET @sql_add_province = IF(@col_exists_province = 0,
    'ALTER TABLE wards ADD COLUMN province_id BIGINT NULL AFTER district_id',
    'SELECT "Column province_id already exists" AS message'
);
PREPARE stmt FROM @sql_add_province;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add is_2025_structure column only if it doesn't exist
SET @sql_add_2025 = IF(@col_exists_2025 = 0,
    'ALTER TABLE wards ADD COLUMN is_2025_structure BOOLEAN NOT NULL DEFAULT FALSE AFTER is_active',
    'SELECT "Column is_2025_structure already exists" AS message'
);
PREPARE stmt FROM @sql_add_2025;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Make district_id nullable to support 2025 structure (where wards belong directly to provinces)
ALTER TABLE wards
MODIFY COLUMN district_id BIGINT NULL
    COMMENT 'NULL for 2025 structure wards (province -> ward). NOT NULL for old structure (province -> district -> ward)';

-- Add index for province_id in wards (before adding FK constraint) - idempotent
SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'wards'
    AND INDEX_NAME = 'idx_wards_province_id'
);

SET @sql_add_index = IF(@index_exists = 0,
    'CREATE INDEX idx_wards_province_id ON wards(province_id)',
    'SELECT "Index idx_wards_province_id already exists" AS message'
);
PREPARE stmt FROM @sql_add_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add foreign key for ward -> province relationship (NO CASCADE to avoid CHECK constraint conflict) - idempotent
SET @fk_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'wards'
    AND CONSTRAINT_NAME = 'fk_wards_province'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql_add_fk = IF(@fk_exists = 0,
    'ALTER TABLE wards ADD CONSTRAINT fk_wards_province FOREIGN KEY (province_id) REFERENCES provinces(province_id)',
    'SELECT "Foreign key fk_wards_province already exists" AS message'
);
PREPARE stmt FROM @sql_add_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add column to provinces to indicate if it uses 2025 structure - idempotent
SET @col_exists_provinces_2025 = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'provinces'
    AND COLUMN_NAME = 'is_2025_structure'
);

SET @sql_add_provinces_2025 = IF(@col_exists_provinces_2025 = 0,
    'ALTER TABLE provinces ADD COLUMN is_2025_structure BOOLEAN NOT NULL DEFAULT FALSE AFTER is_merged COMMENT "TRUE if province uses 2025 structure (34 provinces, no districts). FALSE for old 63-province structure"',
    'SELECT "Column is_2025_structure already exists in provinces" AS message'
);
PREPARE stmt FROM @sql_add_provinces_2025;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add comments for clarity on wards columns
ALTER TABLE wards MODIFY COLUMN is_2025_structure BOOLEAN NOT NULL DEFAULT FALSE
    COMMENT 'TRUE if ward belongs directly to province (2025). FALSE if belongs to district (old structure)';

ALTER TABLE wards MODIFY COLUMN province_id BIGINT NULL
    COMMENT 'Direct province relationship for 2025 structure. NULL for old structure (uses district.province instead)';

-- Note: We cannot use CHECK constraint with columns involved in FK constraints in MySQL
-- Instead, this constraint will be enforced at the application level:
-- Rule: (district_id IS NOT NULL AND province_id IS NULL AND is_2025_structure = FALSE) OR
--       (district_id IS NULL AND province_id IS NOT NULL AND is_2025_structure = TRUE)

-- Migration notes:
-- 1. Old structure (63 provinces): is_2025_structure = FALSE, ward.district_id NOT NULL, ward.province_id NULL
-- 2. New structure (34 provinces): is_2025_structure = TRUE, ward.district_id NULL, ward.province_id NOT NULL
-- 3. To convert old data to new structure, run a separate data migration script
-- 4. Both structures can coexist during transition period
