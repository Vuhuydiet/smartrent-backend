-- ============================================================================
-- Script: populate_listings.sql
-- Purpose: Populate 50,000 listings with full coverage of all filter attributes
--          for integration/feature testing
-- Usage:  Run manually via mysql client, NOT via Flyway
--         mysql -u <user> -p <database> < populate_listings.sql
--
-- Prerequisites:
--   - populate_addresses_for_listings.sql must have been run FIRST (50k addresses)
--   - Seed images must be uploaded to R2 as property SETS:
--     seed-images/{category}/set_{001-100}/{1-6}.jpg
--     Categories: room, apartment, house, office, commercial
--     100 sets per category x 6 photos per set = 3,000 images total
--   - Categories (1-5) and Amenities (1-26) must be seeded (V7 migration)
--
-- Coverage:
--   Categories:       All 5 (phong tro, can ho, nha nguyen can, van phong, mat bang)
--   Listing types:    RENT, SALE, SHARE
--   Product types:    ROOM, APARTMENT, HOUSE, OFFICE, STUDIO
--   VIP types:        NORMAL (70%), SILVER (15%), GOLD (10%), DIAMOND (5%)
--   Furnishing:       FULLY_FURNISHED, SEMI_FURNISHED, UNFURNISHED
--   Direction:        All 8 compass directions + NULL
--   Moderation:       APPROVED (80%), PENDING_REVIEW (10%), REJECTED (5%), others (5%)
--   Price range:      500,000 - 100,000,000 VND covering all filter breakpoints
--   Area:             10 - 500 sqm
--   Bedrooms:         0-5
--   Bathrooms:        1-4
--   Address types:    OLD + NEW (from pre-populated addresses)
--   Amenities:        Each listing gets 2-12 amenities from all 5 categories
--   Media:            Each listing gets 1-6 real property images from R2 seed-images
--   Utilities:        Water, electricity, internet, service fee
--   Post sources:     QUOTA, DIRECT_PAYMENT
--   Drafts/Shadow:    Small percentage for edge-case testing
--   Price histories:  For price reduction/increase filter testing
--   Verified mix:     Both verified and unverified listings
--
-- Randomization:
--   Uses CRC32 with per-attribute salt strings to generate INDEPENDENT seeds
--   for each attribute. This prevents the "same category + same VIP = identical
--   listing" problem that simple modular arithmetic causes.
--
use smartrent;
-- To scale to 1M listings: change @total_target to 1000000 and ensure
-- populate_addresses was also run with 1M target
--
-- Estimated run time: 5-15 minutes for 50k, ~2-4 hours for 1M
-- ============================================================================

SET @total_target = 50000;
SET @batch_size = 500;

-- ============================================================================
-- R2 Seed Image Configuration
-- ============================================================================
SET @r2_public_url = 'https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev';
SET @sets_per_category = 100;  -- 100 property sets per category (3-digit padding)

-- ============================================================================
-- Step 0: Create 50 test users (if they don't exist)
-- ============================================================================
INSERT IGNORE INTO users (user_id, phone_code, phone_number, email, first_name, last_name, is_verified, contact_phone_number, contact_phone_verified)
VALUES
('00000000-test-0001-0000-000000000001', '+84', '0903281547', 'vanan.nguyen92@gmail.com', 'Nguyễn', 'Văn An', TRUE, '0903281547', TRUE),
('00000000-test-0002-0000-000000000002', '+84', '0912473856', 'thibinh.tran88@gmail.com', 'Trần', 'Thị Bình', TRUE, '0912473856', TRUE),
('00000000-test-0003-0000-000000000003', '+84', '0356891472', 'hoangcuong.le@gmail.com', 'Lê', 'Hoàng Cường', TRUE, '0356891472', TRUE),
('00000000-test-0004-0000-000000000004', '+84', '0978362814', 'minhduc.pham95@gmail.com', 'Phạm', 'Minh Đức', TRUE, '0978362814', TRUE),
('00000000-test-0005-0000-000000000005', '+84', '0868174293', 'thiem.hoang@gmail.com', 'Hoàng', 'Thị Em', TRUE, '0868174293', TRUE),
('00000000-test-0006-0000-000000000006', '+84', '0936528417', 'dinhphong.vu93@gmail.com', 'Vũ', 'Đình Phong', TRUE, '0936528417', TRUE),
('00000000-test-0007-0000-000000000007', '+84', '0789315628', 'quanggiang.do87@gmail.com', 'Đỗ', 'Quang Giang', TRUE, '0789315628', TRUE),
('00000000-test-0008-0000-000000000008', '+84', '0918742156', 'thihoa.bui91@gmail.com', 'Bùi', 'Thị Hoa', TRUE, '0918742156', TRUE),
('00000000-test-0009-0000-000000000009', '+84', '0376284915', 'vankhang.ngo@gmail.com', 'Ngô', 'Văn Khang', TRUE, '0376284915', TRUE),
('00000000-test-0010-0000-000000000010', '+84', '0988631247', 'thilan.duong96@gmail.com', 'Dương', 'Thị Lan', TRUE, '0988631247', TRUE),
('00000000-test-0011-0000-000000000011', '+84', '0852419736', 'minhlong.ly89@gmail.com', 'Lý', 'Minh Long', TRUE, '0852419736', TRUE),
('00000000-test-0012-0000-000000000012', '+84', '0384572618', 'thimai.phan@gmail.com', 'Phan', 'Thị Mai', TRUE, '0384572618', TRUE),
('00000000-test-0013-0000-000000000013', '+84', '0708391524', 'vannam.huynh94@gmail.com', 'Huỳnh', 'Văn Nam', TRUE, '0708391524', FALSE),
('00000000-test-0014-0000-000000000014', '+84', '0367841259', 'phuocoanh.dinh90@gmail.com', 'Đinh', 'Phước Oanh', TRUE, '0367841259', FALSE),
('00000000-test-0015-0000-000000000015', '+84', '0932617483', 'quocphu.truong@gmail.com', 'Trương', 'Quốc Phú', TRUE, '0932617483', TRUE),
('00000000-test-0016-0000-000000000016', '+84', '0903748291', 'thiquynh.vo97@gmail.com', 'Võ', 'Thị Quỳnh', FALSE, '0903748291', FALSE),
('00000000-test-0017-0000-000000000017', '+84', '0356293847', 'sonrang.dang86@gmail.com', 'Đặng', 'Sơn Rạng', FALSE, '0356293847', FALSE),
('00000000-test-0018-0000-000000000018', '+84', '0912574831', 'minhson.ta93@gmail.com', 'Tạ', 'Minh Sơn', TRUE, '0912574831', TRUE),
('00000000-test-0019-0000-000000000019', '+84', '0978163524', 'thitrang.mai@gmail.com', 'Mai', 'Thị Trang', TRUE, '0978163524', TRUE),
('00000000-test-0020-0000-000000000020', '+84', '0868429175', 'vanuy.chau88@gmail.com', 'Châu', 'Văn Uy', TRUE, '0868429175', TRUE),
('00000000-test-0021-0000-000000000021', '+84', '0936851342', 'ducvinh.luong95@gmail.com', 'Lương', 'Đức Vinh', TRUE, '0936851342', TRUE),
('00000000-test-0022-0000-000000000022', '+84', '0918376429', 'thixuan.cao@gmail.com', 'Cao', 'Thị Xuân', TRUE, '0918376429', TRUE),
('00000000-test-0023-0000-000000000023', '+84', '0789514273', 'minhyen.ho91@gmail.com', 'Hồ', 'Minh Yên', TRUE, '0789514273', TRUE),
('00000000-test-0024-0000-000000000024', '+84', '0376842619', 'quocanh.tu89@gmail.com', 'Từ', 'Quốc Anh', TRUE, '0376842619', TRUE),
('00000000-test-0025-0000-000000000025', '+84', '0988253741', 'thibich.to@gmail.com', 'Tô', 'Thị Bích', TRUE, NULL, FALSE),
('00000000-test-0026-0000-000000000026', '+84', '0852791435', 'congdanh.quach94@gmail.com', 'Quách', 'Công Danh', TRUE, NULL, FALSE),
('00000000-test-0027-0000-000000000027', '+84', '0384618257', 'thidieu.thai87@gmail.com', 'Thái', 'Thị Diệu', TRUE, '0384618257', TRUE),
('00000000-test-0028-0000-000000000028', '+84', '0708573248', 'hoangdung.kieu@gmail.com', 'Kiều', 'Hoàng Dũng', TRUE, '0708573248', TRUE),
('00000000-test-0029-0000-000000000029', '+84', '0367429516', 'minhhai.la96@gmail.com', 'La', 'Minh Hải', TRUE, '0367429516', TRUE),
('00000000-test-0030-0000-000000000030', '+84', '0932847162', 'thihang.trieu90@gmail.com', 'Triệu', 'Thị Hằng', TRUE, '0932847162', TRUE),
('00000000-test-0031-0000-000000000031', '+84', '0903516284', 'vanhung.luc93@gmail.com', 'Lục', 'Văn Hùng', TRUE, '0903516284', TRUE),
('00000000-test-0032-0000-000000000032', '+84', '0912384729', 'thihuong.mac@gmail.com', 'Mạc', 'Thị Hương', TRUE, '0912384729', TRUE),
('00000000-test-0033-0000-000000000033', '+84', '0356729148', 'vankhanh.ong85@gmail.com', 'Ông', 'Văn Khánh', TRUE, '0356729148', TRUE),
('00000000-test-0034-0000-000000000034', '+84', '0978415863', 'minhluan.tang92@gmail.com', 'Tăng', 'Minh Luân', FALSE, NULL, FALSE),
('00000000-test-0035-0000-000000000035', '+84', '0868592374', 'thingoc.nghiem@gmail.com', 'Nghiêm', 'Thị Ngọc', FALSE, NULL, FALSE),
('00000000-test-0036-0000-000000000036', '+84', '0936271845', 'quocphong.sam88@gmail.com', 'Sầm', 'Quốc Phong', TRUE, '0936271845', TRUE),
('00000000-test-0037-0000-000000000037', '+84', '0918634281', 'vanquyet.ninh95@gmail.com', 'Ninh', 'Văn Quyết', TRUE, '0918634281', TRUE),
('00000000-test-0038-0000-000000000038', '+84', '0789158423', 'thisen.khong@gmail.com', 'Khổng', 'Thị Sen', TRUE, '0789158423', TRUE),
('00000000-test-0039-0000-000000000039', '+84', '0376941527', 'duongtu.au91@gmail.com', 'Âu', 'Dương Tú', TRUE, '0376941527', TRUE),
('00000000-test-0040-0000-000000000040', '+84', '0988372614', 'minhvuong.cu87@gmail.com', 'Cù', 'Minh Vượng', TRUE, '0988372614', TRUE),
('00000000-test-0041-0000-000000000041', '+84', '0852614839', 'vanduy.ha94@gmail.com', 'Hà', 'Văn Duy', TRUE, '0852614839', TRUE),
('00000000-test-0042-0000-000000000042', '+84', '0384729315', 'thigiang.thi@gmail.com', 'Thi', 'Thị Giang', TRUE, '0384729315', TRUE),
('00000000-test-0043-0000-000000000043', '+84', '0708483726', 'hoailinh.phung93@gmail.com', 'Phùng', 'Hoài Linh', TRUE, '0708483726', TRUE),
('00000000-test-0044-0000-000000000044', '+84', '0367261594', 'minhnhat.trinh96@gmail.com', 'Trịnh', 'Minh Nhật', TRUE, '0367261594', TRUE),
('00000000-test-0045-0000-000000000045', '+84', '0932847523', 'thipha.thieu@gmail.com', 'Thiều', 'Thị Pha', TRUE, '0932847523', TRUE),
('00000000-test-0046-0000-000000000046', '+84', '0903192847', 'quangtrung.kha89@gmail.com', 'Kha', 'Quang Trung', TRUE, '0903192847', TRUE),
('00000000-test-0047-0000-000000000047', '+84', '0356871429', 'thiuyen.vi92@gmail.com', 'Vi', 'Thị Uyên', TRUE, '0356871429', TRUE),
('00000000-test-0048-0000-000000000048', '+84', '0912546183', 'quocviet.doan@gmail.com', 'Doãn', 'Quốc Việt', TRUE, '0912546183', TRUE),
('00000000-test-0049-0000-000000000049', '+84', '0978314527', 'thanhxuan.diep90@gmail.com', 'Diệp', 'Thanh Xuân', TRUE, '0978314527', TRUE),
('00000000-test-0050-0000-000000000050', '+84', '0868725194', 'vanyen.tong86@gmail.com', 'Tống', 'Văn Yên', TRUE, '0868725194', TRUE);

SELECT CONCAT('Created/verified 50 test users') AS step_0;

-- ============================================================================
-- Step 1: Stored procedure for populating listings + amenities + media + price history
-- ============================================================================
DROP PROCEDURE IF EXISTS populate_listings;

DELIMITER //

CREATE PROCEDURE populate_listings()
BEGIN
    -- ---- Control ----
    DECLARE v_total_inserted BIGINT DEFAULT 0;
    DECLARE v_batch_count INT DEFAULT 0;
    DECLARE v_first_listing_id BIGINT DEFAULT 0;

    -- ---- Address range ----
    DECLARE v_min_addr_id BIGINT;
    DECLARE v_max_addr_id BIGINT;
    DECLARE v_addr_range BIGINT;

    -- ---- Listing fields ----
    DECLARE v_user_id VARCHAR(36);
    DECLARE v_user_idx INT;
    DECLARE v_category_id BIGINT;
    DECLARE v_listing_type VARCHAR(10);
    DECLARE v_product_type VARCHAR(20);
    DECLARE v_vip_type VARCHAR(10);
    DECLARE v_vip_sort INT;
    DECLARE v_furnishing VARCHAR(20);
    DECLARE v_direction VARCHAR(20);
    DECLARE v_price DECIMAL(15,0);
    DECLARE v_price_unit VARCHAR(10);
    DECLARE v_area FLOAT;
    DECLARE v_bedrooms INT;
    DECLARE v_bathrooms INT;
    DECLARE v_room_capacity INT;
    DECLARE v_address_id BIGINT;
    DECLARE v_title VARCHAR(200);
    DECLARE v_description LONGTEXT;
    DECLARE v_verified BOOLEAN;
    DECLARE v_is_verify BOOLEAN;
    DECLARE v_expired BOOLEAN;
    DECLARE v_is_draft BOOLEAN;
    DECLARE v_is_shadow BOOLEAN;
    DECLARE v_post_source VARCHAR(20);
    DECLARE v_moderation_status VARCHAR(30);
    DECLARE v_post_date TIMESTAMP;
    DECLARE v_expiry_date TIMESTAMP;
    DECLARE v_water_price VARCHAR(50);
    DECLARE v_electricity_price VARCHAR(50);
    DECLARE v_internet_price VARCHAR(50);
    DECLARE v_service_fee VARCHAR(50);
    DECLARE v_duration_days INT;
    DECLARE v_search_text VARCHAR(512);
    DECLARE v_title_norm VARCHAR(256);
    DECLARE v_pushed_at TIMESTAMP;

    -- ---- Independent CRC32 seeds (CRC32 returns UNSIGNED 32-bit, use BIGINT) ----
    DECLARE h_cat BIGINT;
    DECLARE h_vip BIGINT;
    DECLARE h_price BIGINT;
    DECLARE h_area BIGINT;
    DECLARE h_bed BIGINT;
    DECLARE h_bath BIGINT;
    DECLARE h_dir BIGINT;
    DECLARE h_furn BIGINT;
    DECLARE h_title BIGINT;
    DECLARE h_img BIGINT;
    DECLARE h_media BIGINT;
    DECLARE h_status BIGINT;
    DECLARE h_type BIGINT;
    DECLARE h_util BIGINT;
    DECLARE h_user BIGINT;
    DECLARE h_amenity BIGINT;
    DECLARE h_date BIGINT;

    -- ---- Loop helpers ----
    DECLARE v_i INT;

    -- ---- Amenity helpers ----
    DECLARE v_listing_id BIGINT;
    DECLARE v_amenity_count INT;
    DECLARE v_a INT;
    DECLARE v_amenity_id INT;

    -- ---- Media helpers ----
    DECLARE v_media_count INT;
    DECLARE v_m INT;
    DECLARE v_image_category VARCHAR(20);
    DECLARE v_storage_key VARCHAR(500);
    DECLARE v_media_url VARCHAR(500);

    -- ---- Price history helpers ----
    DECLARE v_old_price DECIMAL(15,0);
    DECLARE v_change_type VARCHAR(20);
    DECLARE v_change_pct DECIMAL(5,2);

    -- ---- Title templates ----
    DECLARE v_title_prefix VARCHAR(100);
    DECLARE v_title_suffix VARCHAR(100);

    -- ---- Address location ----
    DECLARE v_addr_province_name VARCHAR(255);
    DECLARE v_addr_district_name VARCHAR(255);
    DECLARE v_addr_ward_name VARCHAR(255);
    DECLARE v_addr_full_address TEXT;
    DECLARE v_addr_street VARCHAR(255);

    -- ---- Image set helpers ----
    DECLARE v_image_set INT;

    -- Get address range
    SELECT MIN(address_id), MAX(address_id) INTO v_min_addr_id, v_max_addr_id FROM addresses;
    SET v_addr_range = v_max_addr_id - v_min_addr_id + 1;

    IF v_addr_range < @total_target THEN
        SELECT CONCAT('WARNING: Only ', v_addr_range, ' addresses available. Need ', @total_target,
                       '. Will reuse addresses.') AS warning;
    END IF;

    SELECT COALESCE(MAX(listing_id), 0) INTO v_first_listing_id FROM listings;

    SELECT CONCAT('Starting listing population.',
                  ' Address range: ', v_min_addr_id, '-', v_max_addr_id,
                  ' (', v_addr_range, ' addresses)',
                  ', Target: ', @total_target,
                  ', Starting after listing_id: ', v_first_listing_id) AS plan;

    SET autocommit = 0;

    SET v_i = 1;
    WHILE v_i <= @total_target DO

        -- ================================================================
        -- INDEPENDENT SEEDS via CRC32 (each attribute gets its own hash)
        -- This ensures category, VIP, price, image, etc. are uncorrelated
        -- ================================================================
        SET h_cat    = CRC32(CONCAT(v_i, ':cat'));
        SET h_vip    = CRC32(CONCAT(v_i, ':vip'));
        SET h_price  = CRC32(CONCAT(v_i, ':price'));
        SET h_area   = CRC32(CONCAT(v_i, ':area'));
        SET h_bed    = CRC32(CONCAT(v_i, ':bed'));
        SET h_bath   = CRC32(CONCAT(v_i, ':bath'));
        SET h_dir    = CRC32(CONCAT(v_i, ':dir'));
        SET h_furn   = CRC32(CONCAT(v_i, ':furn'));
        SET h_title  = CRC32(CONCAT(v_i, ':title'));
        SET h_img    = CRC32(CONCAT(v_i, ':img'));
        SET h_media  = CRC32(CONCAT(v_i, ':media'));
        SET h_status = CRC32(CONCAT(v_i, ':status'));
        SET h_type   = CRC32(CONCAT(v_i, ':type'));
        SET h_util   = CRC32(CONCAT(v_i, ':util'));
        SET h_user   = CRC32(CONCAT(v_i, ':user'));
        SET h_amenity = CRC32(CONCAT(v_i, ':amen'));
        SET h_date   = CRC32(CONCAT(v_i, ':date'));

        -- ================================================================
        -- USER: distribute across 50 test users (scrambled)
        -- ================================================================
        SET v_user_idx = (h_user % 50) + 1;
        SET v_user_id = CONCAT('00000000-test-', LPAD(v_user_idx, 4, '0'), '-0000-000000000', LPAD(v_user_idx, 3, '0'));

        -- ================================================================
        -- CATEGORY + PRODUCT TYPE
        -- ================================================================
        SET v_category_id = (h_cat % 5) + 1;

        CASE v_category_id
            WHEN 1 THEN SET v_product_type = 'ROOM';
            WHEN 2 THEN SET v_product_type = IF((h_cat >> 8) % 3 = 0, 'STUDIO', 'APARTMENT');
            WHEN 3 THEN SET v_product_type = 'HOUSE';
            WHEN 4 THEN SET v_product_type = 'OFFICE';
            WHEN 5 THEN SET v_product_type = IF((h_cat >> 8) % 3 = 0, 'OFFICE', IF((h_cat >> 8) % 3 = 1, 'HOUSE', 'APARTMENT'));
        END CASE;

        -- ================================================================
        -- LISTING TYPE: 70% RENT, 15% SALE, 15% SHARE
        -- ================================================================
        IF (h_type % 100) < 70 THEN
            SET v_listing_type = 'RENT';
        ELSEIF (h_type % 100) < 85 THEN
            SET v_listing_type = 'SALE';
        ELSE
            SET v_listing_type = 'SHARE';
        END IF;

        -- ================================================================
        -- VIP TYPE: NORMAL 70%, SILVER 15%, GOLD 10%, DIAMOND 5%
        -- ================================================================
        IF (h_vip % 100) < 70 THEN
            SET v_vip_type = 'NORMAL';
            SET v_vip_sort = 4;
        ELSEIF (h_vip % 100) < 85 THEN
            SET v_vip_type = 'SILVER';
            SET v_vip_sort = 3;
        ELSEIF (h_vip % 100) < 95 THEN
            SET v_vip_type = 'GOLD';
            SET v_vip_sort = 2;
        ELSE
            SET v_vip_type = 'DIAMOND';
            SET v_vip_sort = 1;
        END IF;

        -- ================================================================
        -- FURNISHING (independent seed)
        -- ================================================================
        SET v_furnishing = ELT((h_furn % 3) + 1, 'FULLY_FURNISHED', 'SEMI_FURNISHED', 'UNFURNISHED');

        -- ================================================================
        -- DIRECTION: 8 values + ~10% NULL (independent seed)
        -- ================================================================
        IF (h_dir % 10) = 0 THEN
            SET v_direction = NULL;
        ELSE
            SET v_direction = ELT((h_dir % 8) + 1,
                'NORTH', 'SOUTH', 'EAST', 'WEST',
                'NORTHEAST', 'NORTHWEST', 'SOUTHEAST', 'SOUTHWEST');
        END IF;

        -- ================================================================
        -- PRICE (independent seed, fine-grained jitter)
        --   room:       1.5M - 5M
        --   apartment:  5M - 25M
        --   house:      8M - 30M
        --   office:     5M - 50M
        --   commercial: 10M - 100M
        -- h_price % 1000 gives 1000 distinct price buckets (vs 100 before)
        -- ================================================================
        CASE v_category_id
            WHEN 1 THEN  -- room: 1.5M - 5M
                SET v_price = 1500000 + ((h_price % 1000) * 3500);
            WHEN 2 THEN  -- apartment: 5M - 25M
                SET v_price = 5000000 + ((h_price % 1000) * 20000);
            WHEN 3 THEN  -- house: 8M - 30M
                SET v_price = 8000000 + ((h_price % 1000) * 22000);
            WHEN 4 THEN  -- office: 5M - 50M
                SET v_price = 5000000 + ((h_price % 1000) * 45000);
            WHEN 5 THEN  -- commercial: 10M - 100M
                SET v_price = 10000000 + ((h_price % 1000) * 90000);
            ELSE
                SET v_price = 3000000 + ((h_price % 1000) * 20000);
        END CASE;

        -- Round price to nearest 100,000
        SET v_price = ROUND(v_price / 100000) * 100000;

        -- ================================================================
        -- PRICE UNIT (always MONTH)
        -- ================================================================
        SET v_price_unit = 'MONTH';

        -- ================================================================
        -- AREA (independent seed, continuous range)
        -- ================================================================
        CASE v_category_id
            WHEN 1 THEN  -- room: 12-30
                SET v_area = 12 + (h_area % 190) / 10.0;
            WHEN 2 THEN  -- apartment: 30-120
                SET v_area = 30 + (h_area % 900) / 10.0;
            WHEN 3 THEN  -- house: 40-200
                SET v_area = 40 + (h_area % 1600) / 10.0;
            WHEN 4 THEN  -- office: 20-500
                SET v_area = 20 + (h_area % 4800) / 10.0;
            WHEN 5 THEN  -- commercial: 30-300
                SET v_area = 30 + (h_area % 2700) / 10.0;
            ELSE
                SET v_area = 20 + (h_area % 800) / 10.0;
        END CASE;

        -- ================================================================
        -- BEDROOMS / BATHROOMS / ROOM CAPACITY (independent seeds)
        -- ================================================================
        CASE v_product_type
            WHEN 'ROOM' THEN
                SET v_bedrooms = IF((h_bed % 3) = 0, 0, 1);
                SET v_bathrooms = 1;
                SET v_room_capacity = 1 + (h_bed % 5);
            WHEN 'STUDIO' THEN
                SET v_bedrooms = 1;
                SET v_bathrooms = 1;
                SET v_room_capacity = 1 + (h_bed % 3);
            WHEN 'APARTMENT' THEN
                SET v_bedrooms = 1 + (h_bed % 5);
                SET v_bathrooms = 1 + (h_bath % 3);
                SET v_room_capacity = NULL;
            WHEN 'HOUSE' THEN
                SET v_bedrooms = 2 + (h_bed % 5);
                SET v_bathrooms = 1 + (h_bath % 4);
                SET v_room_capacity = NULL;
            WHEN 'OFFICE' THEN
                SET v_bedrooms = 0;
                SET v_bathrooms = 1 + (h_bath % 3);
                SET v_room_capacity = NULL;
            ELSE
                SET v_bedrooms = 1;
                SET v_bathrooms = 1;
                SET v_room_capacity = NULL;
        END CASE;

        -- ================================================================
        -- ADDRESS (scrambled assignment)
        -- ================================================================
        SET v_address_id = v_min_addr_id + ((v_i - 1) % v_addr_range);

        SELECT
            lw.province_name,
            lw.district_name,
            lw.ward_name,
            a.full_address,
            a.legacy_street
        INTO v_addr_province_name, v_addr_district_name, v_addr_ward_name,
             v_addr_full_address, v_addr_street
        FROM addresses a
        LEFT JOIN legacy_wards lw ON a.legacy_ward_id = lw.legacy_ward_id
        WHERE a.address_id = v_address_id;

        IF v_addr_province_name IS NULL THEN
            SET v_addr_province_name = '';
            SET v_addr_district_name = '';
            SET v_addr_ward_name = '';
        END IF;

        -- ================================================================
        -- VERIFICATION & STATUS FLAGS (independent seed)
        -- ================================================================
        IF (h_status % 100) < 80 THEN
            SET v_moderation_status = 'APPROVED';
            SET v_verified = TRUE;
            SET v_is_verify = TRUE;
            SET v_expired = FALSE;
        ELSEIF (h_status % 100) < 90 THEN
            SET v_moderation_status = 'PENDING_REVIEW';
            SET v_verified = FALSE;
            SET v_is_verify = TRUE;
            SET v_expired = FALSE;
        ELSEIF (h_status % 100) < 95 THEN
            SET v_moderation_status = 'REJECTED';
            SET v_verified = FALSE;
            SET v_is_verify = FALSE;
            SET v_expired = FALSE;
        ELSEIF (h_status % 100) < 97 THEN
            SET v_moderation_status = 'REVISION_REQUIRED';
            SET v_verified = FALSE;
            SET v_is_verify = FALSE;
            SET v_expired = FALSE;
        ELSEIF (h_status % 100) < 99 THEN
            SET v_moderation_status = 'RESUBMITTED';
            SET v_verified = FALSE;
            SET v_is_verify = TRUE;
            SET v_expired = FALSE;
        ELSE
            SET v_moderation_status = 'SUSPENDED';
            SET v_verified = FALSE;
            SET v_is_verify = FALSE;
            SET v_expired = TRUE;
        END IF;

        -- 5% expired among approved
        IF v_moderation_status = 'APPROVED' AND (h_status >> 8) % 20 = 0 THEN
            SET v_expired = TRUE;
        END IF;

        -- 3% drafts
        SET v_is_draft = ((h_status >> 16) % 100 < 3);

        -- 2% shadow
        SET v_is_shadow = ((h_status >> 24) % 100 < 2);

        -- ================================================================
        -- POST SOURCE
        -- ================================================================
        SET v_post_source = IF((h_type >> 8) % 3 = 0, 'DIRECT_PAYMENT', 'QUOTA');

        -- ================================================================
        -- DATES (independent seed for spread)
        -- ================================================================
        SET v_post_date = DATE_SUB(NOW(), INTERVAL (h_date % 180) DAY);
        SET v_expiry_date = DATE_ADD(v_post_date, INTERVAL 30 DAY);

        IF v_vip_type != 'NORMAL' AND (h_date >> 8) % 5 = 0 THEN
            SET v_pushed_at = DATE_SUB(NOW(), INTERVAL ((h_date >> 16) % 10) DAY);
        ELSE
            SET v_pushed_at = NULL;
        END IF;

        SET v_duration_days = ELT((h_date % 5) + 1, 7, 15, 30, 30, 60);

        -- ================================================================
        -- UTILITIES (independent seed, more variety)
        -- ================================================================
        SET v_water_price = ELT((h_util % 10) + 1,
            '50000', '70000', '80000', '100000', '120000',
            '150000', '200000', 'Miễn phí', '80000/người', '100000/người');
        SET v_electricity_price = ELT(((h_util >> 8) % 8) + 1,
            '3500/kWh', '4000/kWh', '3800/kWh', '4500/kWh',
            'Giá nhà nước', '3500/kWh', '5000/kWh', 'Miễn phí');
        SET v_internet_price = ELT(((h_util >> 16) % 5) + 1,
            '100000', '150000', '200000', 'Miễn phí', '80000');
        SET v_service_fee = ELT(((h_util >> 24) % 5) + 1,
            '0', '100000', '200000', '300000', '500000');

        -- ================================================================
        -- TITLE (30 suffixes instead of 15, independent seed)
        -- ================================================================
        SET v_title_prefix = ELT(v_category_id,
            'Cho thuê phòng trọ',
            'Cho thuê căn hộ',
            'Cho thuê nhà nguyên căn',
            'Cho thuê văn phòng',
            'Cho thuê mặt bằng kinh doanh');

        SET v_title_suffix = ELT((h_title % 30) + 1,
            'giá rẻ, thoáng mát',
            'mới xây, đầy đủ nội thất',
            'gần trung tâm, tiện đi lại',
            'view đẹp, an ninh tốt',
            'cao cấp, full nội thất',
            'giá tốt, không chung chủ',
            'rộng rãi, có ban công',
            'sạch sẽ, yên tĩnh',
            'có gác lửng, thoáng',
            'ngay mặt tiền, thuận lợi',
            'có thang máy, bảo vệ 24/7',
            'mới 100%, dọn vào ở ngay',
            'có hồ bơi, gym miễn phí',
            'khu dân cư an ninh',
            'gần chợ, trường học, bệnh viện',
            'nội thất gỗ cao cấp',
            'tầng cao, view thoáng',
            'mặt tiền đường lớn',
            'gần công viên, yên tĩnh',
            'thiết kế hiện đại',
            'có sân thượng, ban công rộng',
            'gần bến xe, siêu thị',
            'phòng mới sơn, sàn gạch',
            'có chỗ để xe hơi',
            'gần trạm metro, BRT',
            'hướng mát, không ngập nước',
            'đường ô tô, hẻm thông',
            'khu yên tĩnh, dân trí cao',
            'điện nước giá dân',
            'cửa sổ lớn, ánh sáng tự nhiên');

        SET v_title = CONCAT(v_title_prefix, ' ', v_title_suffix,
            IF(v_addr_district_name != '', CONCAT(' ', v_addr_district_name), ''),
            ' - ', ROUND(v_area), 'm²');

        IF LENGTH(v_title) > 200 THEN
            SET v_title = LEFT(v_title, 197);
            SET v_title = CONCAT(v_title, '...');
        END IF;

        -- ================================================================
        -- DESCRIPTION
        -- ================================================================
        SET v_description = CONCAT(
            v_title, '\n\n',
            'Vị trí: ', COALESCE(v_addr_full_address, 'Liên hệ để biết địa chỉ cụ thể'), '\n',
            IF(v_addr_ward_name != '', CONCAT('Khu vực ', v_addr_ward_name, ', ', v_addr_district_name, ', ', v_addr_province_name, '\n'), ''),
            'Gần các tiện ích công cộng, đi lại thuận tiện.\n\n',
            'Diện tích: ', ROUND(v_area), ' m²\n',
            IF(v_bedrooms > 0, CONCAT('Phòng ngủ: ', v_bedrooms, '\n'), ''),
            'Phòng tắm: ', v_bathrooms, '\n',
            IF(v_room_capacity IS NOT NULL, CONCAT('Sức chứa: ', v_room_capacity, ' người\n'), ''),
            '\nGiá: ', FORMAT(v_price, 0), ' VND/', LOWER(v_price_unit), '\n',
            'Nước: ', v_water_price, ' VND/tháng\n',
            'Điện: ', v_electricity_price, '\n',
            'Internet: ', v_internet_price, ' VND/tháng\n',
            IF(v_service_fee != '0', CONCAT('Phí dịch vụ: ', v_service_fee, ' VND/tháng\n'), ''),
            '\nTiện ích nổi bật:\n',
            '- Khu vực an ninh, yên tĩnh\n',
            '- Gần chợ, trường học, bệnh viện\n',
            IF(v_addr_district_name != '', CONCAT('- Thuận tiện di chuyển trong ', v_addr_district_name, '\n'), '- Đường rộng rãi, thuận tiện đi lại\n'),
            '\nLiên hệ ngay để xem phòng!\n',
            'Thời hạn đăng: ', v_duration_days, ' ngày'
        );

        -- ================================================================
        -- SEARCH TEXT + TITLE NORM
        -- ================================================================
        SET v_search_text = LEFT(CONCAT(v_title, ' ', v_title_prefix, ' ', v_addr_district_name, ' ', v_addr_province_name), 512);
        SET v_title_norm = LEFT(LOWER(v_title), 256);

        -- ================================================
        -- INSERT LISTING
        -- ================================================
        INSERT INTO listings (
            title, description, user_id,
            post_date, expiry_date,
            listing_type, verified, is_verify, expired,
            vip_type, vip_type_sort_order,
            post_source, is_shadow, is_draft,
            category_id, product_type,
            price, price_unit,
            address_id, area,
            bedrooms, bathrooms, direction, furnishing,
            room_capacity,
            water_price, electricity_price, internet_price, service_fee,
            duration_days, use_membership_quota, pushed_at,
            search_text, title_norm,
            moderation_status, revision_count,
            created_at, updated_at
        ) VALUES (
            v_title, v_description, v_user_id,
            v_post_date, v_expiry_date,
            v_listing_type, v_verified, v_is_verify, v_expired,
            v_vip_type, v_vip_sort,
            v_post_source, v_is_shadow, v_is_draft,
            v_category_id, v_product_type,
            v_price, v_price_unit,
            v_address_id, v_area,
            v_bedrooms, v_bathrooms, v_direction, v_furnishing,
            v_room_capacity,
            v_water_price, v_electricity_price, v_internet_price, v_service_fee,
            v_duration_days, IF(v_post_source = 'QUOTA', TRUE, FALSE), v_pushed_at,
            v_search_text, v_title_norm,
            v_moderation_status, IF(v_moderation_status = 'RESUBMITTED', 1, 0),
            v_post_date,
            DATE_ADD(v_post_date, INTERVAL ((h_date >> 24) % 30) DAY)
        );

        SET v_listing_id = LAST_INSERT_ID();

        -- ================================================
        -- INSERT AMENITIES (2-12 per listing, independent seed)
        -- ================================================
        CASE v_product_type
            WHEN 'ROOM' THEN SET v_amenity_count = 2 + (h_amenity % 8);
            WHEN 'STUDIO' THEN SET v_amenity_count = 3 + (h_amenity % 8);
            WHEN 'APARTMENT' THEN SET v_amenity_count = 5 + (h_amenity % 8);
            WHEN 'HOUSE' THEN SET v_amenity_count = 5 + (h_amenity % 8);
            WHEN 'OFFICE' THEN SET v_amenity_count = 4 + (h_amenity % 8);
            ELSE SET v_amenity_count = 3 + (h_amenity % 5);
        END CASE;

        IF v_amenity_count > 20 THEN SET v_amenity_count = 20; END IF;

        -- Use scrambled offset so adjacent listings get different amenity sets
        SET v_a = 1;
        WHILE v_a <= v_amenity_count DO
            SET v_amenity_id = (CRC32(CONCAT(v_i, ':a', v_a)) % 26) + 1;

            INSERT IGNORE INTO listing_amenities (listing_id, amenity_id)
            VALUES (v_listing_id, v_amenity_id);

            SET v_a = v_a + 1;
        END WHILE;

        -- ================================================
        -- INSERT MEDIA (1-6 images from SAME property set)
        -- Each listing picks ONE set via independent seed
        -- ================================================
        SET v_media_count = 1 + (h_media % 5) + IF(v_vip_type != 'NORMAL', 1, 0);
        IF v_media_count > 6 THEN SET v_media_count = 6; END IF;

        SET v_image_category = CASE v_category_id
            WHEN 1 THEN 'room'
            WHEN 2 THEN 'apartment'
            WHEN 3 THEN 'house'
            WHEN 4 THEN 'office'
            WHEN 5 THEN 'commercial'
        END;

        -- Independent image set selection (uncorrelated with category position)
        SET v_image_set = (h_img % @sets_per_category) + 1;

        SET v_m = 1;
        WHILE v_m <= v_media_count DO
            SET v_storage_key = CONCAT('seed-images/', v_image_category,
                '/set_', LPAD(v_image_set, 3, '0'), '/', v_m, '.jpg');
            SET v_media_url = CONCAT(@r2_public_url, '/', v_storage_key);

            INSERT INTO media (
                listing_id, user_id,
                media_type, source_type, status,
                storage_key, url, original_filename, mime_type,
                is_primary, sort_order,
                upload_confirmed, confirmed_at,
                created_at, updated_at
            ) VALUES (
                v_listing_id, v_user_id,
                'IMAGE', 'UPLOAD', 'ACTIVE',
                v_storage_key,
                v_media_url,
                CONCAT(v_image_category, '_set', LPAD(v_image_set, 3, '0'), '_', v_m, '.jpg'),
                'image/jpeg',
                IF(v_m = 1, TRUE, FALSE),
                v_m,
                TRUE,
                v_post_date,
                v_post_date,
                v_post_date
            );

            SET v_m = v_m + 1;
        END WHILE;

        -- ================================================
        -- INSERT PRICE HISTORY
        -- ================================================
        INSERT INTO pricing_histories (
            listing_id, old_price, new_price,
            old_price_unit, new_price_unit,
            change_type, change_percentage, change_amount,
            is_current, changed_by, change_reason, changed_at
        ) VALUES (
            v_listing_id, NULL, v_price,
            NULL, v_price_unit,
            'INITIAL', NULL, NULL,
            TRUE, v_user_id, 'Giá ban đầu', v_post_date
        );

        -- 15% price DECREASE
        IF (h_price >> 24) % 100 < 15 THEN
            SET v_old_price = ROUND(v_price * (1 + ((h_price >> 12) % 15 + 5) / 100));
            SET v_change_pct = -1 * ROUND((v_old_price - v_price) / v_old_price * 100, 2);

            UPDATE pricing_histories
            SET is_current = FALSE
            WHERE listing_id = v_listing_id AND change_type = 'INITIAL';

            INSERT INTO pricing_histories (
                listing_id, old_price, new_price,
                old_price_unit, new_price_unit,
                change_type, change_percentage, change_amount,
                is_current, changed_by, change_reason, changed_at
            ) VALUES (
                v_listing_id, v_old_price, v_price,
                v_price_unit, v_price_unit,
                'DECREASE', v_change_pct, v_price - v_old_price,
                TRUE, v_user_id, 'Giảm giá để cho thuê nhanh',
                DATE_ADD(v_post_date, INTERVAL ((h_price >> 8) % 10 + 3) DAY)
            );
        END IF;

        -- 10% price INCREASE
        IF (h_price >> 24) % 100 >= 85 THEN
            SET v_old_price = ROUND(v_price * (1 - ((h_price >> 12) % 13 + 3) / 100));
            SET v_change_pct = ROUND((v_price - v_old_price) / v_old_price * 100, 2);

            UPDATE pricing_histories
            SET is_current = FALSE
            WHERE listing_id = v_listing_id AND change_type = 'INITIAL';

            INSERT INTO pricing_histories (
                listing_id, old_price, new_price,
                old_price_unit, new_price_unit,
                change_type, change_percentage, change_amount,
                is_current, changed_by, change_reason, changed_at
            ) VALUES (
                v_listing_id, v_old_price, v_price,
                v_price_unit, v_price_unit,
                'INCREASE', v_change_pct, v_price - v_old_price,
                TRUE, v_user_id, 'Tăng giá theo thị trường',
                DATE_ADD(v_post_date, INTERVAL ((h_price >> 8) % 10 + 5) DAY)
            );
        END IF;

        -- ================================================
        -- BATCH COMMIT
        -- ================================================
        SET v_total_inserted = v_total_inserted + 1;
        SET v_batch_count = v_batch_count + 1;

        IF v_batch_count >= @batch_size THEN
            COMMIT;
            SET v_batch_count = 0;

            IF v_total_inserted % 5000 = 0 THEN
                SELECT CONCAT('Progress: ', v_total_inserted, ' / ', @total_target,
                              ' (', ROUND(v_total_inserted / @total_target * 100, 1), '%)',
                              ' | Last listing_id: ', v_listing_id) AS progress;
            END IF;
        END IF;

        SET v_i = v_i + 1;
    END WHILE;

    COMMIT;
    SET autocommit = 1;

    SELECT CONCAT('DONE. Total listings inserted: ', v_total_inserted,
                  ', Listing ID range: ', v_first_listing_id + 1, ' to ',
                  v_first_listing_id + v_total_inserted) AS result;
END //

DELIMITER ;

-- ============================================================================
-- Execute
-- ============================================================================
CALL populate_listings();

DROP PROCEDURE IF EXISTS populate_listings;

-- ============================================================================
-- Verification queries
-- ============================================================================

SELECT '=== TOTAL LISTINGS ===' AS section;
SELECT COUNT(*) AS total_listings FROM listings
WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings);

SELECT '=== BY CATEGORY ===' AS section;
SELECT c.name AS category, COUNT(*) AS count
FROM listings l JOIN categories c ON l.category_id = c.category_id
WHERE l.listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY c.name ORDER BY count DESC;

SELECT '=== BY LISTING TYPE ===' AS section;
SELECT listing_type, COUNT(*) AS count
FROM listings WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY listing_type;

SELECT '=== BY PRODUCT TYPE ===' AS section;
SELECT product_type, COUNT(*) AS count
FROM listings WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY product_type ORDER BY count DESC;

SELECT '=== BY VIP TYPE ===' AS section;
SELECT vip_type, COUNT(*) AS count
FROM listings WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY vip_type ORDER BY FIELD(vip_type, 'DIAMOND','GOLD','SILVER','NORMAL');

SELECT '=== BY FURNISHING ===' AS section;
SELECT furnishing, COUNT(*) AS count
FROM listings WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY furnishing;

SELECT '=== BY DIRECTION ===' AS section;
SELECT COALESCE(direction, 'NULL') AS direction, COUNT(*) AS count
FROM listings WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY direction;

SELECT '=== BY MODERATION STATUS ===' AS section;
SELECT moderation_status, COUNT(*) AS count
FROM listings WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY moderation_status ORDER BY count DESC;

SELECT '=== PRICE RANGES ===' AS section;
SELECT
    CASE
        WHEN price < 1000000 THEN '< 1M'
        WHEN price < 3000000 THEN '1M - 3M'
        WHEN price < 5000000 THEN '3M - 5M'
        WHEN price < 10000000 THEN '5M - 10M'
        WHEN price < 20000000 THEN '10M - 20M'
        WHEN price < 50000000 THEN '20M - 50M'
        ELSE '50M+'
    END AS price_range,
    COUNT(*) AS count
FROM listings WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY price_range
ORDER BY FIELD(price_range, '< 1M','1M - 3M','3M - 5M','5M - 10M','10M - 20M','20M - 50M','50M+');

SELECT '=== AREA RANGES ===' AS section;
SELECT
    CASE
        WHEN area < 20 THEN '< 20m²'
        WHEN area < 50 THEN '20 - 50m²'
        WHEN area < 100 THEN '50 - 100m²'
        WHEN area < 200 THEN '100 - 200m²'
        ELSE '200m²+'
    END AS area_range,
    COUNT(*) AS count
FROM listings WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY area_range;

SELECT '=== BEDROOMS ===' AS section;
SELECT bedrooms, COUNT(*) AS count
FROM listings WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY bedrooms ORDER BY bedrooms;

SELECT '=== BATHROOMS ===' AS section;
SELECT bathrooms, COUNT(*) AS count
FROM listings WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY bathrooms ORDER BY bathrooms;

SELECT '=== MEDIA PER LISTING ===' AS section;
SELECT media_count, COUNT(*) AS listings_with_count FROM (
    SELECT l.listing_id, COUNT(m.media_id) AS media_count
    FROM listings l
    LEFT JOIN media m ON l.listing_id = m.listing_id
    WHERE l.listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
    GROUP BY l.listing_id
) t GROUP BY media_count ORDER BY media_count;

SELECT '=== PRICE CHANGES ===' AS section;
SELECT change_type, COUNT(*) AS count
FROM pricing_histories
WHERE listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY change_type;

SELECT '=== TOP 10 PROVINCES ===' AS section;
SELECT a.legacy_province_id, COUNT(*) AS listing_count
FROM listings l JOIN addresses a ON l.address_id = a.address_id
WHERE l.listing_id > (SELECT MAX(listing_id) - 50000 FROM listings)
GROUP BY a.legacy_province_id
ORDER BY listing_count DESC
LIMIT 10;

SELECT '=== POPULATION COMPLETE ===' AS done;
