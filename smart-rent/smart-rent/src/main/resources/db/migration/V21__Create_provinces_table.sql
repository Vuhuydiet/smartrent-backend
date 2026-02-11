

CREATE TABLE provinces (
    province_id INT AUTO_INCREMENT PRIMARY KEY,
    province_code VARCHAR(10) NOT NULL UNIQUE,
    province_name VARCHAR(255) NOT NULL,
    province_short_name VARCHAR(255) NOT NULL,
    province_key VARCHAR(255) NOT NULL,
    province_short_key VARCHAR(255),
    province_latitude DECIMAL(10, 7),
    province_longitude DECIMAL(10, 7),
    province_alias VARCHAR(50),
    province_keywords TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add comments to table and columns
--COMMENT ON TABLE provinces IS 'Vietnam provinces and cities (34 units as of 2025)';
--COMMENT ON COLUMN provinces.province_code IS 'Official province code (2 digits)';
--COMMENT ON COLUMN provinces.province_name IS 'Full province name (e.g., "Thành phố Hà Nội")';
--COMMENT ON COLUMN provinces.province_short_name IS 'Short province name (e.g., "Hà Nội")';
--COMMENT ON COLUMN provinces.province_key IS 'Normalized province key for lookup (no accents, lowercase)';
--COMMENT ON COLUMN provinces.province_latitude IS 'Province center latitude';
--COMMENT ON COLUMN provinces.province_longitude IS 'Province center longitude';
--COMMENT ON COLUMN provinces.province_alias IS 'Common aliases (e.g., "HN", "HCM")';
--COMMENT ON COLUMN provinces.province_keywords IS 'Search keywords for province lookup';
