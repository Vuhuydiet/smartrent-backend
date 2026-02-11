-- Create Categories table
CREATE TABLE IF NOT EXISTS categories (
    category_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) UNIQUE,
    description TEXT,
    icon VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_slug (slug),
    INDEX idx_name (name),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS amenities (
    amenity_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    description TEXT,
    category ENUM('BASIC', 'CONVENIENCE', 'SECURITY', 'ENTERTAINMENT', 'TRANSPORT') NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_name (name),
    INDEX idx_category (category),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create Addresses table (basic version, will be enhanced in V23)
CREATE TABLE IF NOT EXISTS addresses (
    address_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_address TEXT,
    full_newaddress TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create Listings table
CREATE TABLE IF NOT EXISTS listings (
    listing_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description LONGTEXT,
    user_id VARCHAR(36) NOT NULL,
    post_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP,
    listing_type ENUM('RENT', 'SALE', 'SHARE') NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    vip_type ENUM('NORMAL', 'VIP', 'PREMIUM') NOT NULL DEFAULT 'NORMAL',
    category_id BIGINT NOT NULL,
    product_type ENUM('ROOM', 'APARTMENT', 'HOUSE', 'OFFICE', 'STUDIO') NOT NULL,
    price DECIMAL(15, 0) NOT NULL,
    price_unit ENUM('MONTH', 'DAY', 'YEAR') NOT NULL DEFAULT 'MONTH',
    address_id BIGINT NOT NULL,
    area FLOAT,
    bedrooms INT,
    bathrooms INT,
    direction ENUM('NORTH', 'SOUTH', 'EAST', 'WEST', 'NORTHEAST', 'NORTHWEST', 'SOUTHEAST', 'SOUTHWEST'),
    furnishing ENUM('FULLY_FURNISHED', 'SEMI_FURNISHED', 'UNFURNISHED'),
    property_type ENUM('APARTMENT', 'HOUSE', 'ROOM', 'STUDIO', 'OFFICE'),
    room_capacity INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,

    INDEX idx_user_created (user_id, created_at),
    INDEX idx_category_type (category_id, listing_type),
    INDEX idx_address (address_id),
    INDEX idx_price_type (price, listing_type),
    INDEX idx_status (verified, expired, vip_type),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_post_date (post_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create Pricing Histories table
CREATE TABLE IF NOT EXISTS pricing_histories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    old_price DECIMAL(15, 0),
    new_price DECIMAL(15, 0) NOT NULL,
    old_price_unit ENUM('MONTH', 'DAY', 'YEAR'),
    new_price_unit ENUM('MONTH', 'DAY', 'YEAR') NOT NULL,
    change_type ENUM('INITIAL', 'INCREASE', 'DECREASE', 'UNIT_CHANGE', 'CORRECTION') NOT NULL,
    change_percentage DECIMAL(5, 2),
    change_amount DECIMAL(15, 0),
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    changed_by VARCHAR(36),
    change_reason VARCHAR(500),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_listing_id (listing_id),
    INDEX idx_listing_date (listing_id, changed_at),
    INDEX idx_changed_at (changed_at),
    INDEX idx_is_current (is_current)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create Images table
CREATE TABLE IF NOT EXISTS images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(200),
    sort_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    file_size BIGINT,
    mime_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_listing_id (listing_id),
    INDEX idx_listing_sort (listing_id, sort_order),
    INDEX idx_is_primary (is_primary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create Videos table
CREATE TABLE IF NOT EXISTS videos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    title VARCHAR(200),
    description TEXT,
    duration_seconds INT,
    file_size BIGINT,
    mime_type VARCHAR(50),
    thumbnail_url VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_listing_id (listing_id),
    INDEX idx_listing_sort (listing_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create Listing Amenities junction table
CREATE TABLE IF NOT EXISTS listing_amenities (
    listing_id BIGINT NOT NULL,
    amenity_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (listing_id, amenity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create Favorites table
CREATE TABLE IF NOT EXISTS favorites (
    user_id VARCHAR(36) NOT NULL,
    listing_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, listing_id),

    INDEX idx_user_id (user_id),
    INDEX idx_listing_id (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create Views table
CREATE TABLE IF NOT EXISTS views (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    user_id VARCHAR(36),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    is_unique BOOLEAN NOT NULL DEFAULT TRUE,
    referrer VARCHAR(500),
    session_id VARCHAR(100),
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_listing_id (listing_id),
    INDEX idx_listing_time (listing_id, viewed_at),
    INDEX idx_user_id (user_id),
    INDEX idx_ip_time (ip_address, viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default categories
INSERT INTO categories (name, slug, description, icon, is_active) VALUES
    ('Cho thuê phòng trọ', 'cho-thue-phong-tro', 'Phòng trọ giá rẻ, phòng trọ sinh viên', 'room', TRUE),
    ('Cho thuê căn hộ', 'cho-thue-can-ho', 'Căn hộ chung cư, căn hộ dịch vụ', 'apartment', TRUE),
    ('Cho thuê nhà nguyên căn', 'cho-thue-nha-nguyen-can', 'Nhà nguyên căn, villa, biệt thự', 'house', TRUE),
    ('Cho thuê văn phòng', 'cho-thue-van-phong', 'Văn phòng, mặt bằng kinh doanh', 'office', TRUE),
    ('Cho thuê mặt bằng', 'cho-thue-mat-bang', 'Mặt bằng kinh doanh, cửa hàng', 'store', TRUE);

-- Insert default amenities
INSERT INTO amenities (name, icon, category, is_active) VALUES
    -- Basic amenities
    ('Điều hòa', 'ac', 'BASIC', TRUE),
    ('Tủ lạnh', 'fridge', 'BASIC', TRUE),
    ('Máy giặt', 'washing-machine', 'BASIC', TRUE),
    ('Tủ quần áo', 'wardrobe', 'BASIC', TRUE),
    ('Giường', 'bed', 'BASIC', TRUE),
    ('Bàn ghế', 'table-chair', 'BASIC', TRUE),

    -- Convenience amenities
    ('WiFi miễn phí', 'wifi', 'CONVENIENCE', TRUE),
    ('Thang máy', 'elevator', 'CONVENIENCE', TRUE),
    ('Chỗ để xe', 'parking', 'CONVENIENCE', TRUE),
    ('Ban công', 'balcony', 'CONVENIENCE', TRUE),
    ('Cửa sổ', 'window', 'CONVENIENCE', TRUE),
    ('WC riêng', 'bathroom', 'CONVENIENCE', TRUE),

    -- Security amenities
    ('Camera an ninh', 'security-camera', 'SECURITY', TRUE),
    ('Bảo vệ 24/7', 'security-guard', 'SECURITY', TRUE),
    ('Cửa từ', 'magnetic-door', 'SECURITY', TRUE),
    ('Khóa vân tay', 'fingerprint', 'SECURITY', TRUE),

    -- Entertainment amenities
    ('TV', 'tv', 'ENTERTAINMENT', TRUE),
    ('Hồ bơi', 'swimming-pool', 'ENTERTAINMENT', TRUE),
    ('Phòng gym', 'gym', 'ENTERTAINMENT', TRUE),
    ('Sân tennis', 'tennis', 'ENTERTAINMENT', TRUE),

    -- Transport amenities
    ('Gần trạm xe buýt', 'bus-stop', 'TRANSPORT', TRUE),
    ('Gần ga tàu điện', 'train-station', 'TRANSPORT', TRUE),
    ('Gần sân bay', 'airport', 'TRANSPORT', TRUE),
    ('Gần bệnh viện', 'hospital', 'TRANSPORT', TRUE),
    ('Gần trường học', 'school', 'TRANSPORT', TRUE),
    ('Gần chợ/siêu thị', 'market', 'TRANSPORT', TRUE);


-- Add foreign key constraints only if they don't exist
-- Listings table foreign keys
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND CONSTRAINT_NAME = 'fk_listings_user');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE listings ADD CONSTRAINT fk_listings_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_listings_user already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND CONSTRAINT_NAME = 'fk_listings_category');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE listings ADD CONSTRAINT fk_listings_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_listings_category already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND CONSTRAINT_NAME = 'fk_listings_address');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE listings ADD CONSTRAINT fk_listings_address FOREIGN KEY (address_id) REFERENCES addresses(address_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_listings_address already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Pricing histories foreign key
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'pricing_histories' AND CONSTRAINT_NAME = 'fk_pricing_histories_listing');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE pricing_histories ADD CONSTRAINT fk_pricing_histories_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_pricing_histories_listing already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Images foreign key
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'images' AND CONSTRAINT_NAME = 'fk_images_listing');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE images ADD CONSTRAINT fk_images_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_images_listing already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Videos foreign key
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'videos' AND CONSTRAINT_NAME = 'fk_videos_listing');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE videos ADD CONSTRAINT fk_videos_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_videos_listing already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Listing amenities foreign keys
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'listing_amenities' AND CONSTRAINT_NAME = 'fk_listing_amenities_listing');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE listing_amenities ADD CONSTRAINT fk_listing_amenities_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_listing_amenities_listing already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'listing_amenities' AND CONSTRAINT_NAME = 'fk_listing_amenities_amenity');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE listing_amenities ADD CONSTRAINT fk_listing_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES amenities(amenity_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_listing_amenities_amenity already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Favorites foreign keys
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'favorites' AND CONSTRAINT_NAME = 'fk_favorites_user');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE favorites ADD CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_favorites_user already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'favorites' AND CONSTRAINT_NAME = 'fk_favorites_listing');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE favorites ADD CONSTRAINT fk_favorites_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_favorites_listing already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Views foreign keys
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'views' AND CONSTRAINT_NAME = 'fk_views_listing');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE views ADD CONSTRAINT fk_views_listing FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE',
    'SELECT ''Constraint fk_views_listing already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'views' AND CONSTRAINT_NAME = 'fk_views_user');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE views ADD CONSTRAINT fk_views_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL',
    'SELECT ''Constraint fk_views_user already exists'' AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;