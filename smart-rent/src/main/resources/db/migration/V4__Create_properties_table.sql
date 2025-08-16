-- Create properties table
-- This migration creates the properties table for managing rental properties

CREATE TABLE properties (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    country VARCHAR(50) NOT NULL DEFAULT 'USA',
    property_type ENUM('APARTMENT', 'HOUSE', 'CONDO', 'TOWNHOUSE') NOT NULL,
    total_units INT NOT NULL DEFAULT 1,
    description TEXT,
    amenities JSON,
    is_active BOOLEAN DEFAULT TRUE,
    owner_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_properties_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Add indexes for better query performance
CREATE INDEX idx_properties_owner_id ON properties (owner_id);
CREATE INDEX idx_properties_city_state ON properties (city, state);
CREATE INDEX idx_properties_property_type ON properties (property_type);
CREATE INDEX idx_properties_is_active ON properties (is_active);

-- Add unique constraint on property name and address combination
ALTER TABLE properties ADD CONSTRAINT uk_properties_name_address UNIQUE (name, address);
