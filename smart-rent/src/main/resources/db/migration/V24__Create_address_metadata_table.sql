-- Create address_metadata table for structured address querying
-- This allows filtering listings by province, district, ward, etc.

CREATE TABLE address_metadata (
    metadata_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    address_id BIGINT NOT NULL UNIQUE,
    address_type ENUM('OLD', 'NEW') NOT NULL,

    -- Old address structure (63 provinces, 3-tier)
    province_id INT,
    district_id INT,
    ward_id INT,

    -- New address structure (34 provinces, 2-tier)
    new_province_code VARCHAR(10),
    new_ward_code VARCHAR(10),

    -- Common fields
    street_id INT,
    project_id INT,
    street_number VARCHAR(20),

    CONSTRAINT fk_address_metadata_address
        FOREIGN KEY (address_id) REFERENCES addresses(address_id) ON DELETE CASCADE,

    INDEX idx_address_type (address_type),
    INDEX idx_old_province (province_id),
    INDEX idx_old_district (district_id),
    INDEX idx_old_ward (ward_id),
    INDEX idx_new_province (new_province_code),
    INDEX idx_new_ward (new_ward_code),
    INDEX idx_street (street_id),
    INDEX idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
