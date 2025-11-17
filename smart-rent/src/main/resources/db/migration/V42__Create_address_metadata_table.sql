-- Flyway migration: create address_metadata table used by AddressMetadata entity
CREATE TABLE IF NOT EXISTS `address_metadata` (
  `metadata_id` BIGINT NOT NULL AUTO_INCREMENT,
  `address_id` BIGINT NOT NULL,
  `address_type` VARCHAR(10) NOT NULL,
  `province_id` INT NULL,
  `district_id` INT NULL,
  `ward_id` INT NULL,
  `new_province_code` VARCHAR(10) NULL,
  `new_ward_code` VARCHAR(10) NULL,
  `street_id` INT NULL,
  `project_id` INT NULL,
  `street_number` VARCHAR(20) NULL,
  PRIMARY KEY (`metadata_id`),
  UNIQUE KEY `uk_address_metadata_address` (`address_id`),
  CONSTRAINT `fk_address_metadata_address` FOREIGN KEY (`address_id`) REFERENCES `addresses` (`address_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_address_type` ON `address_metadata` (`address_type`);
CREATE INDEX `idx_old_province` ON `address_metadata` (`province_id`);
CREATE INDEX `idx_old_district` ON `address_metadata` (`district_id`);
CREATE INDEX `idx_old_ward` ON `address_metadata` (`ward_id`);
CREATE INDEX `idx_new_province` ON `address_metadata` (`new_province_code`);
CREATE INDEX `idx_new_ward` ON `address_metadata` (`new_ward_code`);
CREATE INDEX `idx_street` ON `address_metadata` (`street_id`);
CREATE INDEX `idx_project` ON `address_metadata` (`project_id`);

-- End migration
