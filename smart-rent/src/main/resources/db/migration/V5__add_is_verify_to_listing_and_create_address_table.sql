-- V5__add_is_verify_to_listing_and_create_address_table.sql

-- Add is_verify column to listings table
ALTER TABLE listings
    ADD COLUMN is_verify BOOLEAN NOT NULL DEFAULT FALSE;

-- Create addresses table (flexible for Vietnam's administrative changes)
CREATE TABLE IF NOT EXISTS addresses (
    address_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    street_number VARCHAR(20),
    street_id BIGINT NOT NULL,
    ward_id BIGINT NOT NULL,
    district_id BIGINT NOT NULL,
    province_id BIGINT NOT NULL,
    full_address TEXT,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_street FOREIGN KEY (street_id) REFERENCES streets(street_id),
    CONSTRAINT fk_ward FOREIGN KEY (ward_id) REFERENCES wards(ward_id),
    CONSTRAINT fk_district FOREIGN KEY (district_id) REFERENCES districts(district_id),
    CONSTRAINT fk_province FOREIGN KEY (province_id) REFERENCES provinces(province_id)
);

-- Indexes for flexible search and admin changes
CREATE INDEX idx_street_id ON addresses (street_id);
CREATE INDEX idx_ward_id ON addresses (ward_id);
CREATE INDEX idx_district_id ON addresses (district_id);
CREATE INDEX idx_province_id ON addresses (province_id);
CREATE INDEX idx_coordinates ON addresses (latitude, longitude);
CREATE INDEX idx_is_verified ON addresses (is_verified);
