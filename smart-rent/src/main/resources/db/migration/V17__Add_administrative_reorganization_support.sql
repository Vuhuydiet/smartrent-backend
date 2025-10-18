-- V17__Add_administrative_reorganization_support.sql
-- Adds support for Vietnam's administrative reorganization (effective July 1, 2025)
-- Key changes:
-- 1. Ward merger support (similar to existing province mergers)
-- 2. Administrative structure version tracking (OLD/NEW/BOTH)
-- 3. Address conversion mapping table for old → new structure conversion
-- NOTE: This migration is idempotent and can be run multiple times safely

-- ==================== WARD MERGER SUPPORT ====================

-- Add parent_ward_id column (idempotent)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND COLUMN_NAME = 'parent_ward_id');

SET @sql = CASE
    WHEN @col_exists = 0 THEN 'ALTER TABLE wards ADD COLUMN parent_ward_id BIGINT AFTER district_id'
    ELSE 'SELECT "Column parent_ward_id already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add is_merged column (idempotent)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND COLUMN_NAME = 'is_merged');

SET @sql = CASE
    WHEN @col_exists = 0 THEN 'ALTER TABLE wards ADD COLUMN is_merged BOOLEAN NOT NULL DEFAULT FALSE AFTER is_active'
    ELSE 'SELECT "Column is_merged already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add merged_date column (idempotent)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND COLUMN_NAME = 'merged_date');

SET @sql = CASE
    WHEN @col_exists = 0 THEN 'ALTER TABLE wards ADD COLUMN merged_date DATE AFTER is_merged'
    ELSE 'SELECT "Column merged_date already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add original_name column (idempotent)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND COLUMN_NAME = 'original_name');

SET @sql = CASE
    WHEN @col_exists = 0 THEN 'ALTER TABLE wards ADD COLUMN original_name VARCHAR(100) AFTER merged_date'
    ELSE 'SELECT "Column original_name already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add structure_version column to wards (idempotent)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND COLUMN_NAME = 'structure_version');

SET @sql = CASE
    WHEN @col_exists = 0 THEN 'ALTER TABLE wards ADD COLUMN structure_version ENUM(''OLD'', ''NEW'', ''BOTH'') NOT NULL DEFAULT ''BOTH'' AFTER original_name'
    ELSE 'SELECT "Column structure_version already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add foreign key constraint fk_wards_parent (idempotent)
SET @fk_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                  WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'wards'
                  AND CONSTRAINT_NAME = 'fk_wards_parent');

SET @sql = CASE
    WHEN @fk_exists = 0 THEN 'ALTER TABLE wards ADD CONSTRAINT fk_wards_parent FOREIGN KEY (parent_ward_id) REFERENCES wards(ward_id) ON DELETE SET NULL'
    ELSE 'SELECT "Foreign key fk_wards_parent already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index idx_parent_ward (idempotent)
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND INDEX_NAME = 'idx_parent_ward');

SET @sql = CASE
    WHEN @idx_exists = 0 THEN 'ALTER TABLE wards ADD INDEX idx_parent_ward (parent_ward_id)'
    ELSE 'SELECT "Index idx_parent_ward already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index idx_is_merged (idempotent)
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND INDEX_NAME = 'idx_is_merged');

SET @sql = CASE
    WHEN @idx_exists = 0 THEN 'ALTER TABLE wards ADD INDEX idx_is_merged (is_merged)'
    ELSE 'SELECT "Index idx_is_merged already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index idx_structure_version on wards (idempotent)
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND INDEX_NAME = 'idx_structure_version');

SET @sql = CASE
    WHEN @idx_exists = 0 THEN 'ALTER TABLE wards ADD INDEX idx_structure_version (structure_version)'
    ELSE 'SELECT "Index idx_structure_version on wards already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== PROVINCE STRUCTURE VERSION ====================

-- Add structure_version column to provinces (idempotent)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'provinces'
                   AND COLUMN_NAME = 'structure_version');

SET @sql = CASE
    WHEN @col_exists = 0 THEN 'ALTER TABLE provinces ADD COLUMN structure_version ENUM(''OLD'', ''NEW'', ''BOTH'') NOT NULL DEFAULT ''BOTH'' AFTER original_name'
    ELSE 'SELECT "Column structure_version on provinces already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index idx_structure_version on provinces (idempotent)
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'provinces'
                   AND INDEX_NAME = 'idx_structure_version');

SET @sql = CASE
    WHEN @idx_exists = 0 THEN 'ALTER TABLE provinces ADD INDEX idx_structure_version (structure_version)'
    ELSE 'SELECT "Index idx_structure_version on provinces already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== DISTRICT STRUCTURE VERSION ====================

-- Add structure_version column to districts (idempotent)
-- Note: Districts only exist in OLD structure (dissolved in new structure)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'districts'
                   AND COLUMN_NAME = 'structure_version');

SET @sql = CASE
    WHEN @col_exists = 0 THEN 'ALTER TABLE districts ADD COLUMN structure_version ENUM(''OLD'', ''NEW'', ''BOTH'') NOT NULL DEFAULT ''OLD'' AFTER effective_to'
    ELSE 'SELECT "Column structure_version on districts already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index idx_structure_version on districts (idempotent)
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'districts'
                   AND INDEX_NAME = 'idx_structure_version');

SET @sql = CASE
    WHEN @idx_exists = 0 THEN 'ALTER TABLE districts ADD INDEX idx_structure_version (structure_version)'
    ELSE 'SELECT "Index idx_structure_version on districts already exists" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== ADDRESS CONVERSION MAPPING TABLE ====================

-- Create table for mapping addresses between old and new structures (idempotent)
CREATE TABLE IF NOT EXISTS address_conversion_mappings (
    mapping_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,

    -- Old structure (before July 1, 2025): Province → District → Ward
    old_province_id BIGINT,
    old_district_id BIGINT,
    old_ward_id BIGINT,

    -- New structure (after July 1, 2025): Province → Ward (no districts)
    new_province_id BIGINT,
    new_ward_id BIGINT,

    conversion_note TEXT COMMENT 'Notes about this conversion (e.g., "Ward merged with another", "Province boundary changed")',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether this mapping is currently active',
    conversion_accuracy INT COMMENT 'Percentage (0-100) indicating accuracy of this conversion',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Composite index for old address lookup (most common query)
    INDEX idx_old_address (old_province_id, old_district_id, old_ward_id),

    -- Composite index for new address reverse lookup
    INDEX idx_new_address (new_province_id, new_ward_id),

    -- Individual indexes for filtering and joins
    INDEX idx_old_province (old_province_id),
    INDEX idx_old_district (old_district_id),
    INDEX idx_old_ward (old_ward_id),
    INDEX idx_new_province (new_province_id),
    INDEX idx_new_ward (new_ward_id),
    INDEX idx_is_active (is_active),

    -- Foreign key constraints
    CONSTRAINT fk_mapping_old_province FOREIGN KEY (old_province_id)
        REFERENCES provinces(province_id) ON DELETE CASCADE,
    CONSTRAINT fk_mapping_old_district FOREIGN KEY (old_district_id)
        REFERENCES districts(district_id) ON DELETE CASCADE,
    CONSTRAINT fk_mapping_old_ward FOREIGN KEY (old_ward_id)
        REFERENCES wards(ward_id) ON DELETE CASCADE,
    CONSTRAINT fk_mapping_new_province FOREIGN KEY (new_province_id)
        REFERENCES provinces(province_id) ON DELETE CASCADE,
    CONSTRAINT fk_mapping_new_ward FOREIGN KEY (new_ward_id)
        REFERENCES wards(ward_id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Mapping table for converting addresses between old and new administrative structures';

-- ==================== ADMINISTRATIVE CONFIGURATION TABLE ====================

-- Create configuration table for managing reorganization metadata (idempotent)
CREATE TABLE IF NOT EXISTS administrative_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Configuration table for administrative system settings';

-- Insert default configuration values (idempotent using INSERT IGNORE)
INSERT IGNORE INTO administrative_config (config_key, config_value, description) VALUES
('REORGANIZATION_DATE', '2025-07-01', 'Effective date of Vietnam administrative reorganization'),
('CURRENT_STRUCTURE', 'NEW', 'Current administrative structure in use (OLD/NEW)'),
('API_VERSION', '1.0.0', 'API version for administrative units'),
('CONVERSION_API_ENABLED', 'true', 'Whether address conversion API is enabled');

-- ==================== DATA MIGRATION ====================

-- Update existing records to mark them as valid in both structures by default
-- This ensures backward compatibility with existing data

-- Note: Specific structure versions should be updated manually based on actual administrative data
-- For example:
-- UPDATE provinces SET structure_version = 'OLD' WHERE merged_date IS NOT NULL AND merged_date < '2025-07-01';
-- UPDATE provinces SET structure_version = 'NEW' WHERE effective_from >= '2025-07-01';

-- ==================== COMMENTS ====================

-- Add table and column comments for documentation (using MODIFY COLUMN which is idempotent)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND COLUMN_NAME = 'parent_ward_id');

SET @sql = CASE
    WHEN @col_exists > 0 THEN 'ALTER TABLE wards MODIFY COLUMN parent_ward_id BIGINT COMMENT ''Reference to parent ward if this ward was merged into another'''
    ELSE 'SELECT "Column parent_ward_id does not exist, skipping comment" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND COLUMN_NAME = 'is_merged');

SET @sql = CASE
    WHEN @col_exists > 0 THEN 'ALTER TABLE wards MODIFY COLUMN is_merged BOOLEAN NOT NULL DEFAULT FALSE COMMENT ''TRUE if this ward was merged into another ward'''
    ELSE 'SELECT "Column is_merged does not exist, skipping comment" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND COLUMN_NAME = 'merged_date');

SET @sql = CASE
    WHEN @col_exists > 0 THEN 'ALTER TABLE wards MODIFY COLUMN merged_date DATE COMMENT ''Date when this ward was merged (if applicable)'''
    ELSE 'SELECT "Column merged_date does not exist, skipping comment" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND COLUMN_NAME = 'original_name');

SET @sql = CASE
    WHEN @col_exists > 0 THEN 'ALTER TABLE wards MODIFY COLUMN original_name VARCHAR(100) COMMENT ''Original name of ward before merger'''
    ELSE 'SELECT "Column original_name does not exist, skipping comment" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'wards'
                   AND COLUMN_NAME = 'structure_version');

SET @sql = CASE
    WHEN @col_exists > 0 THEN 'ALTER TABLE wards MODIFY COLUMN structure_version ENUM(''OLD'', ''NEW'', ''BOTH'') NOT NULL DEFAULT ''BOTH'' COMMENT ''Administrative structure version: OLD (before 2025-07-01), NEW (after 2025-07-01), BOTH (valid in both)'''
    ELSE 'SELECT "Column structure_version does not exist, skipping comment" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'provinces'
                   AND COLUMN_NAME = 'structure_version');

SET @sql = CASE
    WHEN @col_exists > 0 THEN 'ALTER TABLE provinces MODIFY COLUMN structure_version ENUM(''OLD'', ''NEW'', ''BOTH'') NOT NULL DEFAULT ''BOTH'' COMMENT ''Administrative structure version: OLD (before 2025-07-01), NEW (after 2025-07-01), BOTH (valid in both)'''
    ELSE 'SELECT "Column structure_version does not exist, skipping comment" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'districts'
                   AND COLUMN_NAME = 'structure_version');

SET @sql = CASE
    WHEN @col_exists > 0 THEN 'ALTER TABLE districts MODIFY COLUMN structure_version ENUM(''OLD'', ''NEW'', ''BOTH'') NOT NULL DEFAULT ''OLD'' COMMENT ''Districts only exist in OLD structure (dissolved in new structure after 2025-07-01)'''
    ELSE 'SELECT "Column structure_version does not exist, skipping comment" AS info'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;