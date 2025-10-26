-- V21__Create_address_new.sql
-- Creates address hierarchy tables for Vietnam administrative structure
-- Includes import schema tables for V23 data and legacy tables for V22 data


-- =====================================================================
-- IMPORT SCHEMA TABLES (for V23 data import)
-- These tables use VARCHAR codes as PKs for data import compatibility
-- =====================================================================

-- =====================================================================
-- CREATE administrative_regions TABLE
-- =====================================================================
CREATE TABLE administrative_regions (
    id INTEGER NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    code_name VARCHAR(255) NULL,
    code_name_en VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- CREATE administrative_units TABLE
-- =====================================================================
CREATE TABLE administrative_units (
    id INTEGER NOT NULL PRIMARY KEY,
    full_name VARCHAR(255) NULL,
    full_name_en VARCHAR(255) NULL,
    short_name VARCHAR(255) NULL,
    short_name_en VARCHAR(255) NULL,
    code_name VARCHAR(255) NULL,
    code_name_en VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- CREATE provinces TABLE (for V22 data import)
-- =====================================================================

CREATE TABLE `district` (
  `id` int NOT NULL,
  `_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name_en` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `_prefix` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `_province_id` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `project` (
  `id` int NOT NULL,
  `_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name_en` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `_province_id` int DEFAULT NULL,
  `_district_id` int DEFAULT NULL,
  `_lat` double DEFAULT NULL,
  `_lng` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `province` (
  `id` int NOT NULL,
  `_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name_en` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `street` (
  `id` int NOT NULL,
  `_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name_en` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `_prefix` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `_province_id` int DEFAULT NULL,
  `_district_id` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `ward` (
  `id` int NOT NULL,
  `_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name_en` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `_prefix` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `_province_id` int DEFAULT NULL,
  `_district_id` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- CREATE provinces TABLE (for V23 data import)
-- =====================================================================
CREATE TABLE provinces (
    code VARCHAR(20) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NULL,
    full_name VARCHAR(255) NOT NULL,
    full_name_en VARCHAR(255) NULL,
    code_name VARCHAR(255) NULL,
    administrative_unit_id INTEGER NULL,

    INDEX idx_provinces_unit (administrative_unit_id),
    INDEX idx_provinces_name (name),

    CONSTRAINT fk_provinces_admin_unit FOREIGN KEY (administrative_unit_id)
        REFERENCES administrative_units(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- CREATE wards TABLE (for V23 data import)
-- =====================================================================
CREATE TABLE wards (
    code VARCHAR(20) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NULL,
    full_name VARCHAR(255) NULL,
    full_name_en VARCHAR(255) NULL,
    code_name VARCHAR(255) NULL,
    province_code VARCHAR(20) NULL,
    administrative_unit_id INTEGER NULL,

    INDEX idx_wards_province (province_code),
    INDEX idx_wards_unit (administrative_unit_id),
    INDEX idx_wards_name (name),

    CONSTRAINT fk_wards_admin_unit FOREIGN KEY (administrative_unit_id)
        REFERENCES administrative_units(id) ON DELETE SET NULL,
    CONSTRAINT fk_wards_province FOREIGN KEY (province_code)
        REFERENCES provinces(code) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Province Mapping
CREATE TABLE province_mapping (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  province_legacy_id INTEGER NOT NULL,
  province_new_code VARCHAR(20) NOT NULL,
  effective_date DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_province_mapping_legacy
    FOREIGN KEY (province_legacy_id) REFERENCES provinces(id),
  CONSTRAINT fk_province_mapping_new
    FOREIGN KEY (province_new_code) REFERENCES provinces(code),

  UNIQUE KEY uq_province_mapping (province_legacy_id, province_new_code),
  INDEX idx_province_mapping_legacy_id (province_legacy_id),
  INDEX idx_province_mapping_new_code (province_new_code)
);

-- District to Ward Mapping
CREATE TABLE district_ward_mapping (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  district_legacy_id INTEGER NOT NULL,
  ward_new_code VARCHAR(20) NOT NULL,
  effective_date DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_district_ward_mapping_district
    FOREIGN KEY (district_legacy_id) REFERENCES districts(id),
  CONSTRAINT fk_district_ward_mapping_ward
    FOREIGN KEY (ward_new_code) REFERENCES wards(code),

  UNIQUE KEY uq_district_ward_mapping (district_legacy_id, ward_new_code),
  INDEX idx_district_ward_mapping_district_id (district_legacy_id),
  INDEX idx_district_ward_mapping_ward_code (ward_new_code)
);

-- Ward Mapping
CREATE TABLE ward_mapping (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  ward_legacy_id INTEGER NOT NULL,
  ward_new_code VARCHAR(20) NOT NULL,
  merge_type VARCHAR(50) COMMENT 'unchanged, merged_with_others, split_to_multiple, renamed',
  effective_date DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_ward_mapping_legacy
    FOREIGN KEY (ward_legacy_id) REFERENCES wards(id),
  CONSTRAINT fk_ward_mapping_new
    FOREIGN KEY (ward_new_code) REFERENCES wards(code),

  UNIQUE KEY uq_ward_mapping (ward_legacy_id, ward_new_code),
  INDEX idx_ward_mapping_legacy_id (ward_legacy_id),
  INDEX idx_ward_mapping_new_code (ward_new_code)
);

-- Street Mapping
CREATE TABLE street_mapping (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  street_legacy_id INTEGER NOT NULL,
  province_new_code VARCHAR(20) NOT NULL,
  ward_new_code VARCHAR(20),
  effective_date DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_street_mapping_legacy
    FOREIGN KEY (street_legacy_id) REFERENCES street(id),
  CONSTRAINT fk_street_mapping_province
    FOREIGN KEY (province_new_code) REFERENCES provinces(code),
  CONSTRAINT fk_street_mapping_ward
    FOREIGN KEY (ward_new_code) REFERENCES wards(code),

  INDEX idx_street_mapping_legacy_id (street_legacy_id),
  INDEX idx_street_mapping_province_code (province_new_code),
  INDEX idx_street_mapping_ward_code (ward_new_code)
);
