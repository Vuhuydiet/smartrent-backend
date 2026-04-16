-- ============================================================================
-- Script: populate_pricing_history_local.sql
-- Purpose: Populate realistic multi-step pricing history for ALL non-draft
--          listings based on the same CRC32-seeded approach as populate_listings.sql
-- Usage:   Run manually via mysql client — NOT via Flyway
--          mysql -u <user> -p smartrent < scripts/populate_pricing_history_local.sql
--
-- Design:
--   Each listing gets 3 or 4 history records driven by CRC32(listing_id) seeds.
--   Three scenarios, distributed by listing:
--     50% UPWARD    — started cheaper, gradually rose to current price
--     30% VOLATILE  — price moved up/down before settling at current
--     20% DOWNWARD  — started expensive, owner kept dropping to current
--
--   Price change magnitude per step (by category):
--     Room        : 4–10%   (1.5M–5M/month range)
--     Apartment   : 5–13%   (5M–25M/month range)
--     House       : 5–14%   (8M–30M/month range)
--     Office      : 7–18%   (5M–50M/month range)
--     Commercial  : 10–25%  (10M–100M/month range)
--
--   Date range:
--     t0 (INITIAL) = listing post_date, or 30 months ago if older
--     t3 (current) = 1–5 months ago, always after t0
--     t1, t2 = evenly spaced between t0 and t3
--
-- Safe to re-run: TRUNCATEs pricing_histories first.
-- Estimated runtime: ~5s for 1k listings, ~3–5 min for 50k listings.
-- ============================================================================

USE smartrent;

-- ============================================================================
-- Stored procedure
-- ============================================================================
DROP PROCEDURE IF EXISTS populate_pricing_history;

DELIMITER //

CREATE PROCEDURE populate_pricing_history()
BEGIN
    -- ---- Cursor fields ----
    DECLARE done        INT DEFAULT FALSE;
    DECLARE v_listing_id  BIGINT;
    DECLARE v_user_id     VARCHAR(36);
    DECLARE v_price       DECIMAL(15,0);
    DECLARE v_price_unit  VARCHAR(10);
    DECLARE v_category_id INT;
    DECLARE v_post_date   DATETIME;

    -- ---- CRC32 seeds ----
    DECLARE h_main  BIGINT;   -- scenario / steps / reasons
    DECLARE h_date  BIGINT;   -- date placement
    DECLARE h_mag   BIGINT;   -- magnitude variation

    -- ---- Scenario & structure ----
    DECLARE v_scenario  INT;          -- 0=upward, 1=volatile, 2=downward
    DECLARE v_steps     INT;          -- 3 or 4 records total
    DECLARE v_pct       DECIMAL(5,2); -- per-step % magnitude

    -- ---- Prices at each step ----
    DECLARE v_p0  DECIMAL(15,0);  -- INITIAL price
    DECLARE v_p1  DECIMAL(15,0);  -- after step 1
    DECLARE v_p2  DECIMAL(15,0);  -- after step 2 (steps=4 only)
    DECLARE v_p3  DECIMAL(15,0);  -- current price = l.price

    -- ---- Timestamps ----
    DECLARE v_t0        DATETIME;
    DECLARE v_t1        DATETIME;
    DECLARE v_t2        DATETIME;
    DECLARE v_t3        DATETIME;
    DECLARE v_span_days INT;

    -- ---- INSERT helpers ----
    DECLARE v_prev_price  DECIMAL(15,0);
    DECLARE v_amt_val     DECIMAL(15,0);
    DECLARE v_pct_val     DECIMAL(5,2);
    DECLARE v_reason_idx  INT;

    -- ---- Progress ----
    DECLARE v_total INT DEFAULT 0;
    DECLARE v_batch INT DEFAULT 0;

    -- ---- Cursor ----
    DECLARE cur CURSOR FOR
        SELECT listing_id, user_id, price, price_unit, category_id, post_date
        FROM   listings
        WHERE  is_draft = FALSE;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- ----------------------------------------------------------------
    -- Clean slate
    -- ----------------------------------------------------------------
    TRUNCATE TABLE pricing_histories;

    SET autocommit = 0;
    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_listing_id, v_user_id, v_price, v_price_unit,
                       v_category_id, v_post_date;
        IF done THEN LEAVE read_loop; END IF;

        -- ---- Seeds (independent per attribute, same as populate_listings.sql style) ----
        SET h_main = CRC32(CONCAT(v_listing_id, ':ph_main'));
        SET h_date = CRC32(CONCAT(v_listing_id, ':ph_date'));
        SET h_mag  = CRC32(CONCAT(v_listing_id, ':ph_mag'));

        -- ----------------------------------------------------------------
        -- Scenario: 0=upward (50%), 1=volatile (30%), 2=downward (20%)
        -- ----------------------------------------------------------------
        IF    h_main % 10 < 5 THEN SET v_scenario = 0;
        ELSEIF h_main % 10 < 8 THEN SET v_scenario = 1;
        ELSE                        SET v_scenario = 2;
        END IF;

        -- ----------------------------------------------------------------
        -- Steps: 3 (60%) or 4 (40%)
        -- ----------------------------------------------------------------
        SET v_steps = IF((h_main >> 4) % 5 < 3, 3, 4);

        -- ----------------------------------------------------------------
        -- Per-step % magnitude, tied to category price range
        -- ----------------------------------------------------------------
        CASE v_category_id
            WHEN 1 THEN SET v_pct = 4  + (h_mag % 7);   -- room:       4–10%
            WHEN 2 THEN SET v_pct = 5  + (h_mag % 9);   -- apartment:  5–13%
            WHEN 3 THEN SET v_pct = 5  + (h_mag % 10);  -- house:      5–14%
            WHEN 4 THEN SET v_pct = 7  + (h_mag % 12);  -- office:     7–18%
            WHEN 5 THEN SET v_pct = 10 + (h_mag % 16);  -- commercial: 10–25%
            ELSE         SET v_pct = 5  + (h_mag % 8);
        END CASE;

        -- ----------------------------------------------------------------
        -- Date range
        --   t0 = listing's post_date (floored at 30 months ago)
        --   t3 = 1–5 months ago (always in the past, always after t0)
        -- ----------------------------------------------------------------
        SET v_t0 = GREATEST(v_post_date, DATE_SUB(NOW(), INTERVAL 30 MONTH));
        SET v_t3 = DATE_SUB(NOW(), INTERVAL (1 + h_date % 5) MONTH);

        -- Ensure t3 is after t0 by at least 30 days
        IF v_t3 <= DATE_ADD(v_t0, INTERVAL 30 DAY) THEN
            SET v_t3 = DATE_ADD(v_t0, INTERVAL 30 DAY);
        END IF;
        -- Cap at yesterday so "current" record is never in the future
        IF v_t3 >= NOW() THEN
            SET v_t3 = DATE_SUB(NOW(), INTERVAL 1 DAY);
        END IF;

        SET v_span_days = GREATEST(1, DATEDIFF(v_t3, v_t0));

        -- Intermediate timestamps
        IF v_steps = 4 THEN
            SET v_t1 = DATE_ADD(v_t0, INTERVAL ROUND(v_span_days / 3)     DAY);
            SET v_t2 = DATE_ADD(v_t0, INTERVAL ROUND(v_span_days * 2 / 3) DAY);
        ELSE
            SET v_t1 = DATE_ADD(v_t0, INTERVAL ROUND(v_span_days / 2)     DAY);
            SET v_t2 = v_t3; -- unused for steps=3 except as alias
        END IF;

        -- ----------------------------------------------------------------
        -- Price trajectory (all prices rounded to nearest 100,000 VND)
        -- ----------------------------------------------------------------
        CASE v_scenario

            WHEN 0 THEN -- UPWARD: started cheaper, each step is an increase
                -- p0 = current / (1+pct)^(steps-1)
                SET v_p0 = ROUND(v_price / POW(1 + v_pct / 100, v_steps - 1) / 100000) * 100000;
                IF v_steps = 4 THEN
                    SET v_p1 = ROUND(v_p0 * (1 + v_pct / 100) / 100000) * 100000;
                    SET v_p2 = ROUND(v_p1 * (1 + v_pct / 100) / 100000) * 100000;
                ELSE
                    SET v_p1 = ROUND(v_p0 * (1 + v_pct / 100) / 100000) * 100000;
                    SET v_p2 = v_price; -- unused
                END IF;

            WHEN 1 THEN -- VOLATILE: dip then recovery, or spike then pullback
                IF (h_main >> 8) % 2 = 0 THEN
                    -- Started higher → dropped → recovered to current
                    SET v_p0 = ROUND(v_price * (1 + v_pct         / 100) / 100000) * 100000;
                    SET v_p1 = ROUND(v_p0   * (1 - v_pct * 1.8   / 100) / 100000) * 100000;
                    IF v_steps = 4 THEN
                        SET v_p2 = ROUND(v_p1 * (1 + v_pct * 0.6 / 100) / 100000) * 100000;
                    ELSE
                        SET v_p2 = v_price;
                    END IF;
                ELSE
                    -- Started lower → rose sharply → slight pullback to current
                    SET v_p0 = ROUND(v_price * (1 - v_pct         / 100) / 100000) * 100000;
                    SET v_p1 = ROUND(v_p0   * (1 + v_pct * 2     / 100) / 100000) * 100000;
                    IF v_steps = 4 THEN
                        SET v_p2 = ROUND(v_p1 * (1 - v_pct * 0.4 / 100) / 100000) * 100000;
                    ELSE
                        SET v_p2 = v_price;
                    END IF;
                END IF;

            ELSE -- DOWNWARD: started high, owner progressively reduced to current
                SET v_p0 = ROUND(v_price * POW(1 + v_pct / 100, v_steps - 1) / 100000) * 100000;
                IF v_steps = 4 THEN
                    SET v_p1 = ROUND(v_p0 * (1 - v_pct       / 100) / 100000) * 100000;
                    SET v_p2 = ROUND(v_p1 * (1 - v_pct       / 100) / 100000) * 100000;
                ELSE
                    SET v_p1 = ROUND(v_p0 * (1 - v_pct * 1.5 / 100) / 100000) * 100000;
                    SET v_p2 = v_price;
                END IF;

        END CASE;

        SET v_p3 = v_price; -- final record always lands on the listing's stored price

        -- Floor all prices at 100,000 VND
        IF v_p0 < 100000 THEN SET v_p0 = 100000; END IF;
        IF v_p1 < 100000 THEN SET v_p1 = 100000; END IF;
        IF v_p2 < 100000 THEN SET v_p2 = 100000; END IF;

        -- ----------------------------------------------------------------
        -- INSERT — Record 1: INITIAL
        -- ----------------------------------------------------------------
        INSERT INTO pricing_histories (
            listing_id, old_price, new_price,
            old_price_unit, new_price_unit,
            change_type, change_percentage, change_amount,
            is_current, changed_by, change_reason, changed_at
        ) VALUES (
            v_listing_id, NULL, v_p0, NULL, v_price_unit,
            'INITIAL', 0, 0,
            FALSE, v_user_id, 'Giá ban đầu khi đăng tin',
            v_t0
        );

        -- ----------------------------------------------------------------
        -- INSERT — Record 2: p0 → p1
        -- ----------------------------------------------------------------
        SET v_amt_val = v_p1 - v_p0;
        SET v_pct_val = GREATEST(-99.99, LEAST(99.99,
                            ROUND(v_amt_val / NULLIF(v_p0, 0) * 100, 2)));

        SET v_reason_idx = IF(v_p1 >= v_p0,
            (h_main >> 12) % 5 + 1,
            (h_main >> 12) % 4 + 6
        );

        INSERT INTO pricing_histories (
            listing_id, old_price, new_price,
            old_price_unit, new_price_unit,
            change_type, change_percentage, change_amount,
            is_current, changed_by, change_reason, changed_at
        ) VALUES (
            v_listing_id, v_p0, v_p1, v_price_unit, v_price_unit,
            IF(v_p1 >= v_p0, 'INCREASE', 'DECREASE'),
            v_pct_val, v_amt_val,
            FALSE, v_user_id,
            ELT(v_reason_idx,
                'Điều chỉnh giá theo thị trường',
                'Trang bị thêm nội thất mới',
                'Nâng cấp tiện ích, cơ sở vật chất',
                'Điều chỉnh theo chỉ số lạm phát',
                'Khu vực tăng giá mạnh',
                'Giảm giá để tìm khách nhanh hơn',
                'Điều chỉnh cạnh tranh với khu xung quanh',
                'Khuyến mãi hợp đồng dài hạn',
                'Giảm do phòng trống quá lâu'
            ),
            v_t1
        );

        -- ----------------------------------------------------------------
        -- INSERT — Record 3: p1 → p2  (only when steps = 4)
        -- ----------------------------------------------------------------
        IF v_steps = 4 THEN
            SET v_amt_val = v_p2 - v_p1;
            SET v_pct_val = GREATEST(-99.99, LEAST(99.99,
                                ROUND(v_amt_val / NULLIF(v_p1, 0) * 100, 2)));

            SET v_reason_idx = IF(v_p2 >= v_p1,
                (h_date >> 12) % 5 + 1,
                (h_date >> 12) % 4 + 6
            );

            INSERT INTO pricing_histories (
                listing_id, old_price, new_price,
                old_price_unit, new_price_unit,
                change_type, change_percentage, change_amount,
                is_current, changed_by, change_reason, changed_at
            ) VALUES (
                v_listing_id, v_p1, v_p2, v_price_unit, v_price_unit,
                IF(v_p2 >= v_p1, 'INCREASE', 'DECREASE'),
                v_pct_val, v_amt_val,
                FALSE, v_user_id,
                ELT(v_reason_idx,
                    'Điều chỉnh giá theo thị trường',
                    'Cải tạo, nâng cấp hoàn tất',
                    'Điều chỉnh sau khi gia hạn hợp đồng',
                    'Cập nhật theo giá thuê khu vực',
                    'Lắp thêm điều hòa, nội thất cao cấp',
                    'Giảm ưu đãi thêm 1 tháng miễn phí',
                    'Điều chỉnh cạnh tranh',
                    'Hỗ trợ khách thuê mùa thấp điểm',
                    'Ưu đãi ký hợp đồng 12 tháng'
                ),
                v_t2
            );
        END IF;

        -- ----------------------------------------------------------------
        -- INSERT — Final record (is_current = TRUE): prev → v_p3
        -- ----------------------------------------------------------------
        SET v_prev_price = IF(v_steps = 4, v_p2, v_p1);
        SET v_amt_val    = v_p3 - v_prev_price;
        SET v_pct_val    = GREATEST(-99.99, LEAST(99.99,
                               ROUND(v_amt_val / NULLIF(v_prev_price, 0) * 100, 2)));

        INSERT INTO pricing_histories (
            listing_id, old_price, new_price,
            old_price_unit, new_price_unit,
            change_type, change_percentage, change_amount,
            is_current, changed_by, change_reason, changed_at
        ) VALUES (
            v_listing_id, v_prev_price, v_p3, v_price_unit, v_price_unit,
            IF(v_p3 >= v_prev_price, 'INCREASE', 'DECREASE'),
            v_pct_val, v_amt_val,
            TRUE, v_user_id,
            ELT((h_mag >> 8) % 5 + 1,
                'Ổn định giá theo thị trường hiện tại',
                'Điều chỉnh giá sau đợt cải tạo',
                'Giá cạnh tranh nhất khu vực',
                'Cập nhật giá tháng mới',
                'Điều chỉnh lần cuối theo thực tế'
            ),
            v_t3
        );

        -- ---- Batch commit ----
        SET v_total = v_total + 1;
        SET v_batch = v_batch + 1;

        IF v_batch >= 500 THEN
            COMMIT;
            SET v_batch = 0;

            IF v_total % 5000 = 0 THEN
                SELECT CONCAT('Progress: ', v_total, ' listings processed') AS progress;
            END IF;
        END IF;

    END LOOP;

    CLOSE cur;
    COMMIT;
    SET autocommit = 1;

    SELECT CONCAT('Done. Pricing history populated for ', v_total, ' listings.') AS result;
END //

DELIMITER ;

-- ============================================================================
-- Execute
-- ============================================================================
CALL populate_pricing_history();

DROP PROCEDURE IF EXISTS populate_pricing_history;

-- ============================================================================
-- Verification
-- ============================================================================
SELECT '=== SUMMARY ===' AS section;
SELECT
    COUNT(DISTINCT listing_id)          AS listings_with_history,
    COUNT(*)                            AS total_rows,
    SUM(is_current = TRUE)              AS current_rows,
    SUM(change_type = 'INITIAL')        AS initial_rows,
    SUM(change_type = 'INCREASE')       AS increase_rows,
    SUM(change_type = 'DECREASE')       AS decrease_rows,
    MIN(changed_at)                     AS earliest,
    MAX(changed_at)                     AS latest
FROM pricing_histories;

SELECT '=== ROWS PER LISTING (distribution) ===' AS section;
SELECT records, COUNT(*) AS listing_count
FROM (
    SELECT listing_id, COUNT(*) AS records
    FROM   pricing_histories
    GROUP  BY listing_id
) t
GROUP BY records
ORDER BY records;
