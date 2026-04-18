-- ============================================================================
-- Script: populate_addresses_for_listings.sql
-- Purpose: Populate 50,000 addresses for listings in production (1:1 with listings)
-- Usage:  Run manually via mysql client, NOT via Flyway
--         mysql -u <user> -p <database> < populate_addresses_for_listings.sql
--
-- Strategy:
--   1. Uses address_mapping to populate BOTH old (63 provinces) and new (34 provinces) fields
--   2. Inserts directly into addresses table (no temp tables)
--   3. Inserts in batches of 1,000 with explicit COMMIT to avoid lock timeout
--
-- Prerequisites:
--   - legacy_wards table must be populated (V34)
--   - address_mapping table must be populated (V35 + V39)
--   - addresses table must have nested columns (V45)
--   - Run BEFORE inserting listings
--
-- Address columns filled:
--   OLD: full_address, legacy_province_id, legacy_district_id, legacy_ward_id, legacy_street
--   NEW: full_newaddress, new_province_code, new_ward_code, new_street
--   COMMON: address_type, latitude, longitude
--
-- After running, your friend queries addresses directly:
--   SELECT address_id, legacy_province_id, new_province_code, ...
--   FROM addresses
--   WHERE address_id > <last_known_id>
--   ORDER BY address_id;
-- ============================================================================
use smartrent;
SET @batch_size = 1000;
-- For 50k listings, we need 50k addresses (1:1 mapping)
-- To scale to 1M listings later, change this to 1000000
SET @total_target = 50000;

-- ============================================================================
-- Stored procedure to batch-insert addresses
-- ============================================================================
DROP PROCEDURE IF EXISTS populate_addresses;

DELIMITER //

CREATE PROCEDURE populate_addresses()
BEGIN
    DECLARE v_done INT DEFAULT FALSE;

    -- Legacy ward fields
    DECLARE v_legacy_ward_id INT;
    DECLARE v_legacy_province_code VARCHAR(10);
    DECLARE v_legacy_district_code VARCHAR(10);
    DECLARE v_legacy_ward_code VARCHAR(10);
    DECLARE v_legacy_province_name VARCHAR(255);
    DECLARE v_legacy_province_short VARCHAR(255);
    DECLARE v_legacy_district_name VARCHAR(255);
    DECLARE v_legacy_district_short VARCHAR(255);
    DECLARE v_legacy_ward_name VARCHAR(255);
    DECLARE v_legacy_ward_short VARCHAR(255);
    DECLARE v_legacy_ward_lat DECIMAL(10,7);
    DECLARE v_legacy_ward_lon DECIMAL(10,7);

    -- New address fields (from address_mapping)
    DECLARE v_new_province_code VARCHAR(10);
    DECLARE v_new_ward_code VARCHAR(10);
    DECLARE v_new_province_name VARCHAR(255);
    DECLARE v_new_ward_name VARCHAR(255);
    DECLARE v_new_ward_lat DECIMAL(10,7);
    DECLARE v_new_ward_lon DECIMAL(10,7);
    DECLARE v_has_new_mapping BOOLEAN;

    -- Control variables
    DECLARE v_total_inserted BIGINT DEFAULT 0;
    DECLARE v_batch_count INT DEFAULT 0;
    DECLARE v_i INT;
    DECLARE v_listings_per_ward INT;
    DECLARE v_ward_count INT DEFAULT 0;
    DECLARE v_total_wards INT DEFAULT 0;
    DECLARE v_first_address_id BIGINT DEFAULT 0;

    -- Address building variables
    DECLARE v_street_name VARCHAR(255);
    DECLARE v_street_num INT;
    DECLARE v_full_address TEXT;
    DECLARE v_full_newaddress TEXT;
    DECLARE v_lat DECIMAL(10,8);
    DECLARE v_lon DECIMAL(11,8);
    DECLARE v_address_type VARCHAR(10);

    -- Cursor: all legacy wards with their NEW mapping
    DECLARE ward_cursor CURSOR FOR
        SELECT
            lw.legacy_ward_id,
            lw.province_code,
            lw.district_code,
            lw.ward_code,
            lw.province_name,
            lw.province_short_name,
            lw.district_name,
            lw.district_short_name,
            lw.ward_name,
            lw.ward_short_name,
            COALESCE(lw.ward_latitude, lw.district_latitude, lw.province_latitude, 10.7769),
            COALESCE(lw.ward_longitude, lw.district_longitude, lw.province_longitude, 106.7009),
            am.new_province_code,
            am.new_ward_code,
            am.new_province_name,
            am.new_ward_name,
            am.new_ward_lat,
            am.new_ward_lon
        FROM legacy_wards lw
        LEFT JOIN address_mapping am
            ON lw.province_code = am.legacy_province_code
            AND lw.district_code = am.legacy_district_code
            AND lw.ward_code = am.legacy_ward_code
        ORDER BY lw.province_code, lw.district_code, lw.ward_code;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;

    -- Count total wards to distribute evenly
    SELECT COUNT(*) INTO v_total_wards FROM legacy_wards;
    SET v_listings_per_ward = CEIL(@total_target / v_total_wards);

    -- Record the starting address_id so friend knows the range
    SELECT COALESCE(MAX(address_id), 0) INTO v_first_address_id FROM addresses;

    SELECT CONCAT('Total wards: ', v_total_wards,
                  ', Addresses per ward: ', v_listings_per_ward,
                  ', Target: ', @total_target,
                  ', Starting after address_id: ', v_first_address_id) AS plan;

    SET autocommit = 0;

    OPEN ward_cursor;

    ward_loop: LOOP
        FETCH ward_cursor INTO
            v_legacy_ward_id,
            v_legacy_province_code, v_legacy_district_code, v_legacy_ward_code,
            v_legacy_province_name, v_legacy_province_short,
            v_legacy_district_name, v_legacy_district_short,
            v_legacy_ward_name, v_legacy_ward_short,
            v_legacy_ward_lat, v_legacy_ward_lon,
            v_new_province_code, v_new_ward_code,
            v_new_province_name, v_new_ward_name,
            v_new_ward_lat, v_new_ward_lon;

        IF v_done THEN
            LEAVE ward_loop;
        END IF;

        IF v_total_inserted >= @total_target THEN
            LEAVE ward_loop;
        END IF;

        SET v_ward_count = v_ward_count + 1;
        SET v_has_new_mapping = (v_new_province_code IS NOT NULL AND v_new_ward_code IS NOT NULL);
        SET v_i = 1;

        WHILE v_i <= v_listings_per_ward AND v_total_inserted < @total_target DO

            -- ---- Address type: 70% OLD, 30% NEW (when mapping exists) ----
            IF v_has_new_mapping AND (v_i % 10) >= 7 THEN
                SET v_address_type = 'NEW';
            ELSE
                SET v_address_type = 'OLD';
            END IF;

            -- ---- Street name (30 common Vietnamese streets) ----
            SET v_street_name = ELT((v_i % 30) + 1,
                'Nguyễn Trãi','Lê Lợi','Trần Hưng Đạo','Hai Bà Trưng','Lý Thường Kiệt',
                'Điện Biên Phủ','Nguyễn Huệ','Phan Đình Phùng','Trần Phú','Hoàng Hoa Thám',
                'Nguyễn Văn Cừ','Lê Duẩn','Pasteur','Nam Kỳ Khởi Nghĩa','Cách Mạng Tháng 8',
                'Võ Văn Tần','Nguyễn Thị Minh Khai','Bùi Viện','Đồng Khởi','Tôn Đức Thắng',
                'Phạm Văn Đồng','Nguyễn Xiển','Lê Văn Lương','Trường Chinh','Giải Phóng',
                'Láng Hạ','Kim Mã','Đội Cấn','Hoàng Quốc Việt','Xuân Thủy'
            );
            SET v_street_num = (v_legacy_ward_id % 300) + v_i * 2;

            -- ---- Full address (OLD: 3-tier) ----
            SET v_full_address = CONCAT(
                'Số ', v_street_num, ' Đường ', v_street_name, ', ',
                v_legacy_ward_name, ', ', v_legacy_district_name, ', ', v_legacy_province_name
            );

            -- ---- Full new address (NEW: 2-tier) ----
            IF v_has_new_mapping THEN
                SET v_full_newaddress = CONCAT(
                    'Số ', v_street_num, ' Đường ', v_street_name, ', ',
                    v_new_ward_name, ', ', v_new_province_name
                );
            ELSE
                SET v_full_newaddress = NULL;
            END IF;

            -- ---- Coordinates ----
            IF v_address_type = 'NEW' AND v_new_ward_lat IS NOT NULL THEN
                SET v_lat = v_new_ward_lat + ((v_i - v_listings_per_ward / 2) * 0.0003);
                SET v_lon = v_new_ward_lon + ((v_i - v_listings_per_ward / 2) * 0.0003);
            ELSE
                SET v_lat = v_legacy_ward_lat + ((v_i - v_listings_per_ward / 2) * 0.0003);
                SET v_lon = v_legacy_ward_lon + ((v_i - v_listings_per_ward / 2) * 0.0003);
            END IF;

            -- ================================================
            -- INSERT ADDRESS
            -- ================================================
            INSERT INTO addresses (
                full_address,
                legacy_province_id, legacy_district_id, legacy_ward_id,
                legacy_street,
                full_newaddress,
                new_province_code, new_ward_code, new_street,
                address_type,
                latitude, longitude
            ) VALUES (
                v_full_address,
                CAST(v_legacy_province_code AS UNSIGNED),
                CAST(v_legacy_district_code AS UNSIGNED),
                v_legacy_ward_id,
                CONCAT('Số ', v_street_num, ' Đường ', v_street_name),
                v_full_newaddress,
                COALESCE(v_new_province_code, v_legacy_province_code),
                v_new_ward_code,
                CONCAT('Số ', v_street_num, ' Đường ', v_street_name),
                v_address_type,
                v_lat, v_lon
            );

            SET v_total_inserted = v_total_inserted + 1;
            SET v_batch_count = v_batch_count + 1;

            -- ---- COMMIT every @batch_size rows ----
            IF v_batch_count >= @batch_size THEN
                COMMIT;
                SET v_batch_count = 0;

                IF v_total_inserted % 10000 = 0 THEN
                    SELECT CONCAT('Progress: ', v_total_inserted, ' / ', @total_target,
                                  ' (', ROUND(v_total_inserted / @total_target * 100, 1), '%)')
                    AS progress;
                END IF;
            END IF;

            SET v_i = v_i + 1;
        END WHILE;

    END LOOP;

    COMMIT;
    CLOSE ward_cursor;
    SET autocommit = 1;

    SELECT CONCAT('DONE. Total addresses inserted: ', v_total_inserted,
                  ', Wards processed: ', v_ward_count,
                  ', Address ID range: ', v_first_address_id + 1, ' to ', v_first_address_id + v_total_inserted) AS result;

END //

DELIMITER ;

-- ============================================================================
-- Execute
-- ============================================================================
CALL populate_addresses();

DROP PROCEDURE IF EXISTS populate_addresses;

-- ============================================================================
-- Verify
-- ============================================================================

-- Total new addresses
SELECT COUNT(*) AS total_new_addresses FROM addresses
WHERE address_id > (SELECT MAX(address_id) - @total_target FROM addresses);

-- Distribution by address type
SELECT address_type, COUNT(*) AS count
FROM addresses
WHERE address_id > (SELECT MAX(address_id) - @total_target FROM addresses)
GROUP BY address_type;

-- Distribution by province (top 10)
SELECT legacy_province_id, new_province_code, COUNT(*) AS count
FROM addresses
WHERE address_id > (SELECT MAX(address_id) - @total_target FROM addresses)
GROUP BY legacy_province_id, new_province_code
ORDER BY count DESC
LIMIT 10;

-- ============================================================================
-- HOW YOUR FRIEND QUERIES ADDRESSES FOR LISTING INSERTION:
-- ============================================================================
--
-- All 500,000 addresses are in the `addresses` table directly.
-- No temp table needed. Your friend queries:
--
--   SELECT address_id, address_type,
--          legacy_province_id, legacy_district_id, legacy_ward_id,
--          new_province_code, new_ward_code,
--          full_address, full_newaddress
--   FROM addresses
--   WHERE address_id BETWEEN <start_id> AND <end_id>
--   ORDER BY address_id
--   LIMIT 1000;
--
-- Then INSERT listings with address_id from the result.
-- Process in batches of 1000 to avoid timeout.
-- ============================================================================
