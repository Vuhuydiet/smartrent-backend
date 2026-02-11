-- V48: Create listing_drafts table for storing draft listings
-- All fields are optional except user_id to support auto-save functionality

CREATE TABLE IF NOT EXISTS listing_drafts (
    draft_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    
    -- Core Information
    title VARCHAR(200),
    description LONGTEXT,
    listing_type VARCHAR(20),
    vip_type VARCHAR(20),
    category_id BIGINT,
    product_type VARCHAR(20),
    
    -- Pricing Information
    price DECIMAL(15, 0),
    price_unit VARCHAR(20),
    
    -- Address Information
    address_type VARCHAR(10),
    province_id BIGINT,
    district_id BIGINT,
    ward_id BIGINT,
    province_code VARCHAR(20),
    ward_code VARCHAR(20),
    street VARCHAR(255),
    street_id BIGINT,
    project_id BIGINT,
    latitude DOUBLE,
    longitude DOUBLE,
    
    -- Property Specifications
    area FLOAT,
    bedrooms INT,
    bathrooms INT,
    direction VARCHAR(20),
    furnishing VARCHAR(30),
    room_capacity INT,
    
    -- Utility Costs
    water_price VARCHAR(50),
    electricity_price VARCHAR(50),
    internet_price VARCHAR(50),
    service_fee VARCHAR(50),
    
    -- Related IDs (stored as comma-separated strings)
    amenity_ids VARCHAR(500),
    media_ids VARCHAR(500),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_draft_user_id (user_id),
    INDEX idx_draft_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

