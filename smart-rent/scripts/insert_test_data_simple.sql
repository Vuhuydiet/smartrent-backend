-- =====================================================
-- COMPREHENSIVE TEST DATA INSERTION SCRIPT
-- Simplified version using temporary tables
-- =====================================================

USE smartrent;

-- Disable foreign key checks for faster insertion
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- PART 1: INSERT 200 USERS
-- =====================================================

SET @row1 := 0;
INSERT INTO users (user_id, first_name, last_name, email, phone_code, phone_number, password, is_verified, avatar_url, created_at, updated_at)
SELECT
    UUID(),
    CONCAT('Văn ', CHAR(65 + MOD(seq - 1, 26))),
    'Nguyễn',
    CONCAT('user', seq, '@smartrent.vn'),
    '+84',
    CONCAT('9', MOD(seq, 4), LPAD(seq, 8, '0')),
    '$2a$12$5EeWd/K8iKLk1UOw0AkfLOuzwIcA.uSx2nQhqvstoQUckLiXOgiim',
    true,
    'https://res.cloudinary.com/daartoyul/image/upload/v1734086974/x2u01mgrywiu6nmbq4su.jpg',
    DATE_SUB(NOW(), INTERVAL MOD(seq, 90) DAY),
    DATE_SUB(NOW(), INTERVAL MOD(seq, 90) DAY)
FROM (
    SELECT (@row1 := @row1 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) numbers;

-- =====================================================
-- PART 2: INSERT 200 ADDRESSES
-- =====================================================

SET @row2 := 0;
INSERT INTO addresses (full_address, latitude, longitude, address_type, created_at, updated_at)
SELECT
    CONCAT(
        'Số ', seq, ', Đường ',
        ELT(MOD(seq, 10) + 1, 'Lê Lợi', 'Trần Hưng Đạo', 'Nguyễn Huệ', 'Hai Bà Trưng', 'Lý Thường Kiệt', 'Điện Biên Phủ', 'Võ Văn Tần', 'Pasteur', 'Cách Mạng Tháng 8', 'Hoàng Văn Thụ'),
        ', Phường ', MOD(seq, 20) + 1,
        ', Quận ', MOD(seq, 12) + 1,
        ', ',
        ELT(MOD(seq, 10) + 1, 'Hà Nội', 'Hồ Chí Minh', 'Đà Nẵng', 'Hải Phòng', 'Cần Thơ', 'Biên Hòa', 'Nha Trang', 'Huế', 'Vũng Tàu', 'Buôn Ma Thuột')
    ),
    (10.0 + (MOD(seq, 100) * 0.1)),
    (105.0 + (MOD(seq, 100) * 0.1)),
    'OLD',
    DATE_SUB(NOW(), INTERVAL MOD(seq, 90) DAY),
    DATE_SUB(NOW(), INTERVAL MOD(seq, 90) DAY)
FROM (
    SELECT (@row2 := @row2 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) numbers;

-- =====================================================
-- CREATE TEMPORARY TABLES FOR EASIER JOINS
-- =====================================================

DROP TEMPORARY TABLE IF EXISTS temp_users;
SET @u_row := 0;
CREATE TEMPORARY TABLE temp_users AS
SELECT user_id, (@u_row := @u_row + 1) AS row_num
FROM users
ORDER BY created_at
LIMIT 200;

DROP TEMPORARY TABLE IF EXISTS temp_addresses;
SET @a_row := 0;
CREATE TEMPORARY TABLE temp_addresses AS
SELECT address_id, (@a_row := @a_row + 1) AS row_num
FROM addresses
ORDER BY address_id
LIMIT 200;

-- =====================================================
-- PART 3: INSERT 200 LISTINGS
-- =====================================================

SET @row3 := 0;
INSERT INTO listings (title, description, user_id, listing_type, verified, is_verify, expired, vip_type, post_source, category_id, product_type, price, price_unit, address_id, area, bedrooms, bathrooms, direction, furnishing, room_capacity, expiry_date, post_date)
SELECT
    CONCAT(
        ELT(MOD(n.seq, 5) + 1, 'Căn hộ cao cấp', 'Phòng trọ sinh viên', 'Nhà nguyên căn', 'Studio hiện đại', 'Văn phòng cho thuê'),
        ' - Số ', n.seq
    ),
    CONCAT('Mô tả chi tiết cho bất động sản số ', n.seq, '. Vị trí đẹp, tiện nghi đầy đủ, giá cả hợp lý.'),
    u.user_id,
    'RENT',
    true,
    true,
    false,
    ELT(MOD(n.seq, 4) + 1, 'NORMAL', 'SILVER', 'GOLD', 'DIAMOND'),
    'QUOTA',
    1,
    ELT(MOD(n.seq, 5) + 1, 'APARTMENT', 'ROOM', 'HOUSE', 'STUDIO', 'OFFICE'),
    (2000000 + (n.seq * 100000)),
    'MONTH',
    a.address_id,
    (20.0 + (n.seq * 0.5)),
    MOD(n.seq, 5) + 1,
    MOD(n.seq, 3) + 1,
    ELT(MOD(n.seq, 8) + 1, 'NORTH', 'SOUTH', 'EAST', 'WEST', 'NORTHEAST', 'NORTHWEST', 'SOUTHEAST', 'SOUTHWEST'),
    ELT(MOD(n.seq, 3) + 1, 'FULLY_FURNISHED', 'SEMI_FURNISHED', 'UNFURNISHED'),
    MOD(n.seq, 8) + 2,
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 30) DAY)
FROM (
    SELECT (@row3 := @row3 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_users u ON u.row_num = MOD(n.seq - 1, 200) + 1
INNER JOIN temp_addresses a ON a.row_num = MOD(n.seq - 1, 200) + 1;

-- Create temporary table for listings
DROP TEMPORARY TABLE IF EXISTS temp_listings;
SET @l_row := 0;
CREATE TEMPORARY TABLE temp_listings AS
SELECT listing_id, (@l_row := @l_row + 1) AS row_num
FROM listings
ORDER BY listing_id
LIMIT 200;

-- =====================================================
-- PART 4: INSERT 200 PHONE CLICKS
-- =====================================================

SET @row4 := 0;
INSERT INTO phone_clicks (listing_id, user_id, clicked_at, ip_address, user_agent)
SELECT
    l.listing_id,
    u.user_id,
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY),
    CONCAT('192.168.', MOD(n.seq, 255), '.', MOD(n.seq * 2, 255)),
    CONCAT('Mozilla/5.0 (',
           ELT(MOD(n.seq, 3) + 1, 'Windows NT 10.0', 'Macintosh; Intel Mac OS X 10_15_7', 'X11; Linux x86_64'),
           ') AppleWebKit/537.36 (KHTML, like Gecko) ',
           ELT(MOD(n.seq, 4) + 1, 'Chrome/120.0.0.0', 'Firefox/121.0', 'Safari/17.2', 'Edge/120.0.0.0'),
           ' Safari/537.36')
FROM (
    SELECT (@row4 := @row4 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_listings l ON l.row_num = MOD(n.seq - 1, 200) + 1
INNER JOIN temp_users u ON u.row_num = MOD(n.seq - 1, 200) + 1;

-- =====================================================
-- PART 5: INSERT 200 VIEWS
-- =====================================================

SET @row5 := 0;
INSERT INTO views (listing_id, user_id, viewed_at, ip_address, user_agent)
SELECT
    l.listing_id,
    u.user_id,
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 90) DAY),
    CONCAT('10.0.', MOD(n.seq, 255), '.', MOD(n.seq * 3, 255)),
    CONCAT('Mozilla/5.0 (', ELT(MOD(n.seq, 3) + 1, 'Windows NT 10.0', 'Macintosh; Intel Mac OS X 10_15_7', 'X11; Linux x86_64'), ') AppleWebKit/537.36')
FROM (
    SELECT (@row5 := @row5 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_listings l ON l.row_num = MOD(n.seq - 1, 200) + 1
INNER JOIN temp_users u ON u.row_num = MOD(n.seq - 1, 200) + 1;

-- =====================================================
-- PART 6: INSERT 200 SAVED LISTINGS
-- =====================================================

SET @row6 := 0;
INSERT INTO saved_listings (user_id, listing_id, created_at, updated_at)
SELECT
    u.user_id,
    l.listing_id,
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 45) DAY),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 45) DAY)
FROM (
    SELECT (@row6 := @row6 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_users u ON u.row_num = MOD(n.seq - 1, 200) + 1
INNER JOIN temp_listings l ON l.row_num = MOD(n.seq - 1, 200) + 1;

-- =====================================================
-- PART 7: INSERT 200 TRANSACTIONS
-- =====================================================

SET @row7 := 0;
INSERT INTO transactions (transaction_id, user_id, transaction_type, amount, reference_type, reference_id, additional_info, status, payment_provider, provider_transaction_id, created_at, updated_at)
SELECT
    UUID(),
    u.user_id,
    ELT(MOD(n.seq, 3) + 1, 'MEMBERSHIP_PURCHASE', 'POST_FEE', 'PUSH_FEE'),
    (100000 + (n.seq * 50000)),
    ELT(MOD(n.seq, 3) + 1, 'MEMBERSHIP', 'LISTING', 'PUSH'),
    CONCAT('REF', LPAD(n.seq, 10, '0')),
    CONCAT('Giao dịch số ', n.seq),
    ELT(MOD(n.seq, 4) + 1, 'PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'),
    ELT(MOD(n.seq, 4) + 1, 'VNPAY', 'MOMO', 'BANK_TRANSFER', 'CASH'),
    CONCAT('TXN', LPAD(n.seq, 10, '0')),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY)
FROM (
    SELECT (@row7 := @row7 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_users u ON u.row_num = MOD(n.seq - 1, 200) + 1;

SELECT 'Part 1-7 completed: Users, Addresses, Listings, Phone Clicks, Views, Saved Listings, Transactions' as status;

-- =====================================================
-- PART 8: INSERT 200 PUSH HISTORY
-- =====================================================

SET @row8 := 0;
INSERT INTO push_history (listing_id, push_source, status, message, pushed_at)
SELECT
    l.listing_id,
    ELT(MOD(n.seq, 4) + 1, 'MEMBERSHIP_QUOTA', 'DIRECT_PAYMENT', 'ADMIN', 'SCHEDULED'),
    ELT(MOD(n.seq, 10) + 1, 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'SUCCESS', 'FAIL'),
    CASE WHEN MOD(n.seq, 10) = 9 THEN 'Push failed due to system error' ELSE 'Push successful' END,
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 30) DAY)
FROM (
    SELECT (@row8 := @row8 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_listings l ON l.row_num = MOD(n.seq - 1, 200) + 1;

-- =====================================================
-- PART 9: INSERT 200 LISTING REPORTS
-- =====================================================

SET @row9 := 0;
INSERT INTO listing_reports (listing_id, reporter_name, reporter_phone, reporter_email, category, other_feedback, status, admin_notes, resolved_by, resolved_at, created_at, updated_at)
SELECT
    l.listing_id,
    CONCAT('Người báo cáo ', n.seq),
    CONCAT('09', MOD(n.seq, 9) + 1, LPAD(n.seq, 7, '0')),
    CONCAT('reporter', n.seq, '@example.com'),
    ELT(MOD(n.seq, 2) + 1, 'LISTING', 'MAP'),
    CONCAT('Báo cáo số ', n.seq, ': Thông tin không chính xác'),
    ELT(MOD(n.seq, 3) + 1, 'PENDING', 'RESOLVED', 'REJECTED'),
    CASE WHEN MOD(n.seq, 3) != 0 THEN CONCAT('Đã xử lý báo cáo số ', n.seq) ELSE NULL END,
    CASE WHEN MOD(n.seq, 3) != 0 THEN (SELECT admin_id FROM admins LIMIT 1) ELSE NULL END,
    CASE WHEN MOD(n.seq, 3) != 0 THEN DATE_SUB(NOW(), INTERVAL MOD(n.seq, 30) DAY) ELSE NULL END,
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY)
FROM (
    SELECT (@row9 := @row9 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_listings l ON l.row_num = MOD(n.seq - 1, 200) + 1
INNER JOIN temp_users u ON u.row_num = MOD(n.seq - 1, 200) + 1;

-- Create temporary table for membership packages
-- NOTE: Membership package benefits are already inserted by V13 migration
DROP TEMPORARY TABLE IF EXISTS temp_packages;
SET @p_row := 0;
CREATE TEMPORARY TABLE temp_packages AS
SELECT membership_id, (@p_row := @p_row + 1) AS row_num
FROM membership_packages
ORDER BY membership_id;

-- =====================================================
-- PART 12: INSERT 200 USER MEMBERSHIPS
-- =====================================================

SET @row12 := 0;
INSERT INTO user_memberships (user_id, membership_id, start_date, end_date, duration_days, status, total_paid, created_at, updated_at)
SELECT
    u.user_id,
    p.membership_id,
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY),
    DATE_ADD(DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY), INTERVAL 30 DAY),
    30,
    ELT(MOD(n.seq, 2) + 1, 'ACTIVE', 'EXPIRED'),
    (SELECT sale_price FROM membership_packages WHERE membership_id = p.membership_id),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY),
    DATE_SUB(NOW(), INTERVAL MOD(n.seq, 60) DAY)
FROM (
    SELECT (@row12 := @row12 + 1) AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT 0 UNION ALL SELECT 1) t3
    LIMIT 200
) n
INNER JOIN temp_users u ON u.row_num = MOD(n.seq - 1, 200) + 1
INNER JOIN temp_packages p ON p.row_num = MOD(n.seq, 3) + 1;

-- Enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Part 8-12 completed: Push History, Listing Reports, User Memberships (Membership Packages and Benefits are inserted by V13 migration)' as status;
