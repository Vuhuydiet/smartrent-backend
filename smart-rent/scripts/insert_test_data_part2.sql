-- =====================================================
-- COMPREHENSIVE TEST DATA INSERTION SCRIPT - PART 2
-- Remaining tables: User Membership Benefits, VIP Tiers, Push Details, Push Schedule, Media, Pricing History
-- Run this AFTER insert_test_data_simple.sql
-- =====================================================

USE smartrent;

-- Disable foreign key checks for faster insertion
SET FOREIGN_KEY_CHECKS = 0;

-- Recreate temporary tables (in case they were dropped)
DROP TEMPORARY TABLE IF EXISTS temp_users;
SET @u_row := 0;
CREATE TEMPORARY TABLE temp_users AS
SELECT user_id, (@u_row := @u_row + 1) AS row_num
FROM users
ORDER BY user_id
LIMIT 200;

DROP TEMPORARY TABLE IF EXISTS temp_listings;
SET @l_row := 0;
CREATE TEMPORARY TABLE temp_listings AS
SELECT listing_id, (@l_row := @l_row + 1) AS row_num
FROM listings
ORDER BY listing_id
LIMIT 200;

-- =====================================================
-- PART 13: INSERT USER MEMBERSHIP BENEFITS
-- Each user_membership gets all benefits from their membership_package
-- =====================================================

INSERT INTO user_membership_benefits (user_membership_id, benefit_id, user_id, benefit_type, granted_at, expires_at, total_quantity, quantity_used, status, created_at, updated_at)
SELECT
    um.user_membership_id,
    mpb.benefit_id,
    um.user_id,
    mpb.benefit_type,
    um.start_date,
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    mpb.quantity_per_month,
    0,
    'ACTIVE',
    um.created_at,
    um.updated_at
FROM user_memberships um
INNER JOIN membership_package_benefits mpb ON mpb.membership_id = um.membership_id;

-- =====================================================
-- PART 14: INSERT VIP TIER DETAILS (Base data - 4 tiers)
-- =====================================================

INSERT IGNORE INTO vip_tier_details (tier_code, tier_name, tier_name_en, tier_level, price_per_day, price_10_days, price_15_days, price_30_days, max_images, max_videos, has_badge, badge_name, badge_color, auto_approve, no_ads, priority_display, has_shadow_listing, description, is_active, display_order, created_at, updated_at)
VALUES
    ('NORMAL', 'Tin Thường', 'Normal', 1, 0, 0, 0, 0, 8, 1, false, NULL, NULL, false, false, false, false, 'Tin đăng thường', true, 1, NOW(), NOW()),
    ('SILVER', 'Tin Bạc', 'Silver', 2, 50000, 450000, 650000, 1200000, 12, 2, true, 'Bạc', '#C0C0C0', false, false, true, false, 'Tin đăng bạc với ưu tiên hiển thị', true, 2, NOW(), NOW()),
    ('GOLD', 'Tin Vàng', 'Gold', 3, 80000, 720000, 1050000, 1920000, 16, 3, true, 'Vàng', '#FFD700', true, true, true, true, 'Tin đăng vàng với nhiều tính năng', true, 3, NOW(), NOW()),
    ('DIAMOND', 'Tin Kim Cương', 'Diamond', 4, 120000, 1080000, 1575000, 2880000, 20, 5, true, 'Kim Cương', '#B9F2FF', true, true, true, true, 'Tin đăng kim cương cao cấp nhất', true, 4, NOW(), NOW());

-- =====================================================
-- PART 15: INSERT PUSH DETAILS (Base data - 4 packages)
-- =====================================================

INSERT IGNORE INTO push_details (detail_code, detail_name, detail_name_en, price_per_push, quantity, total_price, discount_percentage, description, is_active, display_order, created_at, updated_at)
VALUES
    ('SINGLE_PUSH', 'Đẩy tin đơn', 'Single Push', 10000, 1, 10000, 0.00, 'Đẩy tin 1 lần', true, 1, NOW(), NOW()),
    ('PUSH_5', 'Gói 5 lượt đẩy', '5 Push Package', 10000, 5, 45000, 10.00, 'Gói 5 lượt đẩy tin tiết kiệm 10%', true, 2, NOW(), NOW()),
    ('PUSH_10', 'Gói 10 lượt đẩy', '10 Push Package', 10000, 10, 80000, 20.00, 'Gói 10 lượt đẩy tin tiết kiệm 20%', true, 3, NOW(), NOW()),
    ('PUSH_20', 'Gói 20 lượt đẩy', '20 Push Package', 10000, 20, 140000, 30.00, 'Gói 20 lượt đẩy tin tiết kiệm 30%', true, 4, NOW(), NOW());

-- =====================================================
-- PART 16: INSERT 200 PUSH SCHEDULES
-- =====================================================

SET @row16 := 0;
INSERT INTO push_schedule (listing_id, user_id, scheduled_time, source, total_pushes, used_pushes, status, created_at, updated_at)
SELECT
    l.listing_id,
    u.user_id,
    TIME(CONCAT(MOD(n.seq, 24), ':', MOD(n.seq * 15, 60), ':00')),
    ELT(MOD(n.seq, 2) + 1, 'MEMBERSHIP', 'DIRECT_PURCHASE'),
    (1 + MOD(n.seq, 5)),
    MOD(n.seq, 3),
    ELT(MOD(n.seq, 3) + 1, 'ACTIVE', 'COMPLETED', 'CANCELLED'),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 30) DAY),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 30) DAY)
FROM (
    SELECT (@row16 := @row16 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_listings l ON l.row_num = MOD(n.seq - 1, 200) + 1
INNER JOIN temp_users u ON u.row_num = MOD(n.seq - 1, 200) + 1;

-- =====================================================
-- PART 17: INSERT 200 MEDIA RECORDS
-- =====================================================

SET @row17 := 0;
INSERT INTO media (listing_id, user_id, media_type, source_type, status, storage_key, url, original_filename, mime_type, file_size, is_primary, sort_order, upload_confirmed, created_at, updated_at)
SELECT
    l.listing_id,
    u.user_id,
    'IMAGE',
    'UPLOAD',
    'ACTIVE',
    CONCAT('smartrent/listings/', l.listing_id, '/media_', n.seq),
    'https://res.cloudinary.com/daartoyul/image/upload/v1734086974/x2u01mgrywiu6nmbq4su.jpg',
    CONCAT('property_image_', n.seq, '.jpg'),
    'image/jpeg',
    (1024 * 1024 * MOD(n.seq, 5) + 500000),
    (MOD(n.seq, 10) = 0),
    MOD(n.seq, 20),
    true,
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY)
FROM (
    SELECT (@row17 := @row17 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_listings l ON l.row_num = MOD(n.seq - 1, 200) + 1
INNER JOIN temp_users u ON u.row_num = MOD(n.seq - 1, 200) + 1;

-- =====================================================
-- PART 18: INSERT 200 PRICING HISTORY RECORDS
-- =====================================================

SET @row18 := 0;
INSERT INTO pricing_histories (listing_id, old_price, new_price, old_price_unit, new_price_unit, change_type, change_percentage, change_amount, is_current, changed_at)
SELECT
    l.listing_id,
    CASE WHEN n.seq = 1 THEN NULL ELSE (2000000 + (n.seq * 100000)) END,
    (2000000 + (n.seq * 150000)),
    CASE WHEN n.seq = 1 THEN NULL ELSE 'MONTH' END,
    'MONTH',
    CASE
        WHEN n.seq = 1 THEN 'INITIAL'
        WHEN MOD(n.seq, 3) = 0 THEN 'INCREASE'
        WHEN MOD(n.seq, 3) = 1 THEN 'DECREASE'
        ELSE 'CORRECTION'
    END,
    CASE WHEN n.seq > 1 THEN (((2000000 + (n.seq * 150000)) - (2000000 + (n.seq * 100000))) * 100.0 / (2000000 + (n.seq * 100000))) ELSE NULL END,
    CASE WHEN n.seq > 1 THEN ((2000000 + (n.seq * 150000)) - (2000000 + (n.seq * 100000))) ELSE NULL END,
    (MOD(n.seq, 5) = 0),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 90) DAY)
FROM (
    SELECT (@row18 := @row18 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_listings l ON l.row_num = MOD(n.seq - 1, 200) + 1;

-- Enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

SELECT 'Part 13-18 completed: User Membership Benefits, VIP Tiers, Push Details, Push Schedule, Media, Pricing History' as status;

-- Count all inserted records
SELECT 'Users' as entity, COUNT(*) as count FROM users
UNION ALL
SELECT 'Addresses', COUNT(*) FROM addresses
UNION ALL
SELECT 'Listings', COUNT(*) FROM listings
UNION ALL
SELECT 'Phone Clicks', COUNT(*) FROM phone_clicks
UNION ALL
SELECT 'Views', COUNT(*) FROM views
UNION ALL
SELECT 'Saved Listings', COUNT(*) FROM saved_listings
UNION ALL
SELECT 'Transactions', COUNT(*) FROM transactions
UNION ALL
SELECT 'Push History', COUNT(*) FROM push_history
UNION ALL
SELECT 'Listing Reports', COUNT(*) FROM listing_reports
UNION ALL
SELECT 'Membership Packages', COUNT(*) FROM membership_packages
UNION ALL
SELECT 'Membership Package Benefits', COUNT(*) FROM membership_package_benefits
UNION ALL
SELECT 'User Memberships', COUNT(*) FROM user_memberships
UNION ALL
SELECT 'User Membership Benefits', COUNT(*) FROM user_membership_benefits
UNION ALL
SELECT 'VIP Tier Details', COUNT(*) FROM vip_tier_details
UNION ALL
SELECT 'Push Details', COUNT(*) FROM push_details
UNION ALL
SELECT 'Push Schedules', COUNT(*) FROM push_schedule
UNION ALL
SELECT 'Media', COUNT(*) FROM media
UNION ALL
SELECT 'Pricing Histories', COUNT(*) FROM pricing_histories;

SELECT '=== DATA INSERTION COMPLETE ===' as message;
SELECT 'Total Dynamic Records: 2800' as summary_1;
SELECT 'Total Config Records: 17' as summary_2;
SELECT 'Grand Total: 2817+ records' as summary_3;

