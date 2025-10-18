-- =====================================================
-- SMARTRENT VIP TIER AND PUSH DETAILS TABLES
-- Version: V18
-- Description: Create tables to store VIP tier details and push pricing details
-- =====================================================

-- =====================================================
-- 1. VIP TIER DETAILS TABLE
-- =====================================================
CREATE TABLE vip_tier_details (
    tier_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tier_code VARCHAR(20) NOT NULL UNIQUE COMMENT 'NORMAL, SILVER, GOLD, DIAMOND',
    tier_name VARCHAR(100) NOT NULL COMMENT 'Display name in Vietnamese',
    tier_name_en VARCHAR(100) NOT NULL COMMENT 'Display name in English',
    tier_level INT NOT NULL COMMENT 'Priority level: 1=NORMAL, 2=SILVER, 3=GOLD, 4=DIAMOND',
    
    -- Pricing
    price_per_day DECIMAL(15, 0) NOT NULL COMMENT 'Base price per day in VND',
    price_10_days DECIMAL(15, 0) NOT NULL COMMENT 'Total price for 10 days',
    price_15_days DECIMAL(15, 0) NOT NULL COMMENT 'Total price for 15 days (11% discount)',
    price_30_days DECIMAL(15, 0) NOT NULL COMMENT 'Total price for 30 days (18.5% discount)',
    
    -- Features
    max_images INT NOT NULL DEFAULT 5 COMMENT 'Maximum number of images allowed',
    max_videos INT NOT NULL DEFAULT 1 COMMENT 'Maximum number of videos allowed',
    has_badge BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether tier has a badge',
    badge_name VARCHAR(100) COMMENT 'Badge display name',
    badge_color VARCHAR(50) COMMENT 'Badge color (e.g., blue, gold, red)',
    auto_approve BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Auto-approve listings without review',
    no_ads BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'No advertisement banners',
    priority_display BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Priority in search results',
    has_shadow_listing BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Creates shadow normal listing (Diamond only)',
    
    -- Description
    description TEXT COMMENT 'Tier description and features',
    features JSON COMMENT 'JSON array of feature descriptions',
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether tier is currently available',
    display_order INT NOT NULL DEFAULT 0 COMMENT 'Display order in UI',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_tier_code (tier_code),
    INDEX idx_tier_level (tier_level),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='VIP tier details with pricing and features';

-- =====================================================
-- 2. PUSH DETAILS TABLE
-- =====================================================
CREATE TABLE push_details (
    push_detail_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    detail_code VARCHAR(20) NOT NULL UNIQUE COMMENT 'SINGLE_PUSH, PUSH_PACKAGE_3, etc.',
    detail_name VARCHAR(100) NOT NULL COMMENT 'Display name',
    detail_name_en VARCHAR(100) NOT NULL COMMENT 'Display name in English',
    
    -- Pricing
    price_per_push DECIMAL(15, 0) NOT NULL COMMENT 'Price per single push in VND',
    quantity INT NOT NULL DEFAULT 1 COMMENT 'Number of pushes in package',
    total_price DECIMAL(15, 0) NOT NULL COMMENT 'Total price for package',
    discount_percentage DECIMAL(5, 2) DEFAULT 0.00 COMMENT 'Discount percentage if package',
    
    -- Description
    description TEXT COMMENT 'Push package description',
    features JSON COMMENT 'JSON array of feature descriptions',
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether package is currently available',
    display_order INT NOT NULL DEFAULT 0 COMMENT 'Display order in UI',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_detail_code (detail_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Push pricing details and packages';

-- =====================================================
-- 3. INSERT DEFAULT VIP TIER DATA
-- =====================================================
INSERT INTO vip_tier_details (
    tier_code, tier_name, tier_name_en, tier_level,
    price_per_day, price_10_days, price_15_days, price_30_days,
    max_images, max_videos, has_badge, badge_name, badge_color,
    auto_approve, no_ads, priority_display, has_shadow_listing,
    description, features, is_active, display_order
) VALUES
-- NORMAL Tier
('NORMAL', 'Tin Thường', 'Normal Listing', 1,
 2700, 27000, 36000, 66000,
 5, 1, FALSE, NULL, NULL,
 FALSE, FALSE, FALSE, FALSE,
 'Tin đăng thường với các tính năng cơ bản',
 JSON_ARRAY('Hiển thị theo thứ tự thời gian', 'Cần chờ kiểm duyệt (4-8h)', 'Có banner quảng cáo', 'Tối đa 5 ảnh, 1 video', 'Hiển thị dưới cùng'),
 TRUE, 1),

-- SILVER Tier
('SILVER', 'VIP Bạc', 'VIP Silver', 2,
 50000, 500000, 667500, 1222500,
 10, 2, TRUE, 'VIP BẠC', 'blue',
 TRUE, TRUE, TRUE, FALSE,
 'Tin VIP Bạc với ưu tiên hiển thị và không có quảng cáo',
 JSON_ARRAY('Hiển thị ngay (không chờ kiểm duyệt)', 'Không có banner quảng cáo', 'Badge "VIP BẠC" màu xanh ngọc', 'Ưu tiên hiển thị trên tin thường', 'Tối đa 10 ảnh, 2 video'),
 TRUE, 2),

-- GOLD Tier
('GOLD', 'VIP Vàng', 'VIP Gold', 3,
 110000, 1100000, 1468500, 2689500,
 12, 2, TRUE, 'VIP VÀNG', 'gold',
 TRUE, TRUE, TRUE, FALSE,
 'Tin VIP Vàng với ưu tiên hiển thị cao và vị trí tốt hơn',
 JSON_ARRAY('Tất cả tính năng của VIP Bạc', 'Badge "VIP VÀNG" màu vàng đồng', 'Ưu tiên hiển thị trên VIP Bạc', 'Tối đa 12 ảnh, 2 video', 'Hiển thị ở vị trí tốt hơn'),
 TRUE, 3),

-- DIAMOND Tier
('DIAMOND', 'VIP Kim Cương', 'VIP Diamond', 4,
 280000, 2800000, 3738000, 6846000,
 15, 3, TRUE, 'VIP KIM CƯƠNG', 'red',
 TRUE, TRUE, TRUE, TRUE,
 'Tin VIP Kim Cương với ưu tiên hiển thị cao nhất và tặng kèm tin thường',
 JSON_ARRAY('Tất cả tính năng của VIP Vàng', 'Badge "VIP KIM CƯƠNG" màu đỏ', 'Ưu tiên hiển thị CAO NHẤT', 'Tối đa 15 ảnh, 3 video', 'Hiển thị vị trí TOP trang chủ', 'Nhân đôi hiển thị: Tặng kèm 1 tin Thường', 'Đẩy Kim Cương → Tin Thường đi kèm cũng được đẩy free'),
 TRUE, 4);

-- =====================================================
-- 4. INSERT DEFAULT PUSH DETAILS DATA
-- =====================================================
INSERT INTO push_details (
    detail_code, detail_name, detail_name_en,
    price_per_push, quantity, total_price, discount_percentage,
    description, features, is_active, display_order
) VALUES
-- Single Push
('SINGLE_PUSH', 'Đẩy tin đơn lẻ', 'Single Push', 
 40000, 1, 40000, 0.00,
 'Đẩy tin lên đầu danh sách 1 lần',
 JSON_ARRAY('Đẩy tin lên đầu danh sách', 'Reset lại thời gian post_date', 'Hiệu lực ngay lập tức'),
 TRUE, 1),

-- Push Package 3
('PUSH_PACKAGE_3', 'Gói 3 lượt đẩy tin', 'Push Package 3',
 38000, 3, 114000, 5.00,
 'Gói 3 lượt đẩy tin với giá ưu đãi',
 JSON_ARRAY('3 lượt đẩy tin', 'Tiết kiệm 5%', 'Có thể sử dụng cho nhiều tin khác nhau'),
 TRUE, 2),

-- Push Package 5
('PUSH_PACKAGE_5', 'Gói 5 lượt đẩy tin', 'Push Package 5',
 36000, 5, 180000, 10.00,
 'Gói 5 lượt đẩy tin với giá ưu đãi',
 JSON_ARRAY('5 lượt đẩy tin', 'Tiết kiệm 10%', 'Có thể sử dụng cho nhiều tin khác nhau'),
 TRUE, 3),

-- Push Package 10
('PUSH_PACKAGE_10', 'Gói 10 lượt đẩy tin', 'Push Package 10',
 34000, 10, 340000, 15.00,
 'Gói 10 lượt đẩy tin với giá ưu đãi cao nhất',
 JSON_ARRAY('10 lượt đẩy tin', 'Tiết kiệm 15%', 'Có thể sử dụng cho nhiều tin khác nhau', 'Giá tốt nhất'),
 TRUE, 4);

-- =====================================================
-- END OF MIGRATION V18
-- =====================================================

