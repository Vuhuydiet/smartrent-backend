-- ============================================================================
-- Script: populate_pricing_history_local.sql
-- Purpose: Populate realistic pricing history for ALL non-draft listings with
--          district-aware behavior and ~60-day interval data points.
-- Usage:   Run manually via mysql client — NOT via Flyway
--          mysql -u <user> -p smartrent < scripts/populate_pricing_history_local.sql
--
-- ── CONFIG ──────────────────────────────────────────────────────────────────
--   @use_listing_dates = FALSE  →  local dev: fixed 14–26 month window
--   @use_listing_dates = TRUE   →  production: anchored to listing post_date
-- ────────────────────────────────────────────────────────────────────────────
--
-- Record types per listing:
--   INITIAL    — first price when listing was created
--   INCREASE   — owner deliberately raised the price
--   DECREASE   — owner deliberately lowered the price
--   ADJUSTED   — synthetic 60-day interpolated point (for chart density)
--
-- Structure:
--   Anchor points (3 or 4) define the price trajectory via CRC32-seeded
--   UPWARD / VOLATILE / DOWNWARD scenarios, same as before.
--   ADJUSTED records are inserted between consecutive anchors at every
--   60-day mark using linear interpolation.
--
--   Expected records per listing: 7–13
--   (3-4 anchors + ~4-8 ADJUSTED across all gaps)
--
-- District-aware layer (HCMC only):
--   v_volatility — step size multiplier    (>1 = wider price swings)
--   v_trend_bias — scenario override       (+1 upward, -1 downward, 0 neutral)
--
-- Price step — additive, proportional to listing price:
--   step_vnd = GREATEST(ROUND(price × pct% / 100k) × 100k, 100k)
--   Minimum 100 000 VND gap on every anchor-to-anchor move.
--
-- Only processes listings with ≤ 1 existing pricing history row.
-- Safe to re-run: deletes only history for listings it will repopulate.
-- ============================================================================

USE smartrent;

-- ============================================================================
-- CONFIG
-- ============================================================================
SET @use_listing_dates = FALSE;   -- FALSE = local dev,  TRUE = production

-- ============================================================================
-- Stored procedure
-- ============================================================================
DROP PROCEDURE IF EXISTS populate_pricing_history;

DELIMITER //

CREATE PROCEDURE populate_pricing_history()
BEGIN
    -- ---- Cursor fields ----
    DECLARE done          INT DEFAULT FALSE;
    DECLARE v_listing_id  BIGINT;
    DECLARE v_user_id     VARCHAR(36);
    DECLARE v_price       DECIMAL(15,0);
    DECLARE v_price_unit  VARCHAR(10);
    DECLARE v_category_id INT;
    DECLARE v_post_date   DATETIME;
    DECLARE v_district_id INT;

    -- ---- CRC32 seeds ----
    DECLARE h_main  BIGINT;
    DECLARE h_date  BIGINT;
    DECLARE h_mag   BIGINT;

    -- ---- Scenario & structure ----
    DECLARE v_scenario  INT;
    DECLARE v_steps     INT;
    DECLARE v_pct       DECIMAL(5,2);

    -- ---- District behavior ----
    DECLARE v_volatility  DECIMAL(5,2);
    DECLARE v_trend_bias  INT;

    -- ---- Step size ----
    DECLARE v_step_vnd  DECIMAL(15,0);

    -- ---- Anchor prices ----
    DECLARE v_p0  DECIMAL(15,0);
    DECLARE v_p1  DECIMAL(15,0);
    DECLARE v_p2  DECIMAL(15,0);
    DECLARE v_p3  DECIMAL(15,0);

    -- ---- Anchor timestamps ----
    DECLARE v_t0  DATETIME;
    DECLARE v_t1  DATETIME;
    DECLARE v_t2  DATETIME;
    DECLARE v_t3  DATETIME;
    DECLARE v_span_days INT;

    -- ---- Interpolation (ADJUSTED records) ----
    DECLARE v_gap_days      INT;
    DECLARE v_num_adj       INT;
    DECLARE v_adj_i         INT;
    DECLARE v_t_adj         DATETIME;
    DECLARE v_p_adj         DECIMAL(15,0);
    DECLARE v_t_gap_start   DATETIME;
    DECLARE v_p_gap_start   DECIMAL(15,0);
    DECLARE v_p_gap_end     DECIMAL(15,0);

    -- ---- INSERT helpers ----
    DECLARE v_last_price  DECIMAL(15,0);  -- price of the previous inserted row
    DECLARE v_amt_val     DECIMAL(15,0);
    DECLARE v_pct_val     DECIMAL(5,2);
    DECLARE v_reason_idx  INT;

    -- ---- Progress ----
    DECLARE v_total INT DEFAULT 0;
    DECLARE v_batch INT DEFAULT 0;

    -- ---- Cursor: listings with ≤ 1 existing pricing history row ----
    DECLARE cur CURSOR FOR
        SELECT l.listing_id,
               l.user_id,
               l.price,
               l.price_unit,
               l.category_id,
               l.post_date,
               COALESCE(a.legacy_district_id, 0)
        FROM   listings l
        LEFT  JOIN addresses a ON a.address_id = l.address_id
        WHERE  l.is_draft = FALSE
          AND  (SELECT COUNT(*) FROM pricing_histories ph
                WHERE  ph.listing_id = l.listing_id) <= 1;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    SET autocommit = 0;
    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_listing_id, v_user_id, v_price, v_price_unit,
                       v_category_id, v_post_date, v_district_id;
        IF done THEN LEAVE read_loop; END IF;

        -- Delete any existing 0–1 row for this listing
        DELETE FROM pricing_histories
        WHERE  listing_id = v_listing_id AND listing_id > 0;

        -- ---- Seeds ----
        SET h_main = CRC32(CONCAT(v_listing_id, ':ph_main'));
        SET h_date = CRC32(CONCAT(v_listing_id, ':ph_date'));
        SET h_mag  = CRC32(CONCAT(v_listing_id, ':ph_mag'));

        -- ---- Scenario ----
        IF    h_main % 10 < 5 THEN SET v_scenario = 0;
        ELSEIF h_main % 10 < 8 THEN SET v_scenario = 1;
        ELSE                        SET v_scenario = 2;
        END IF;

        -- ---- Steps ----
        SET v_steps = IF((h_main >> 4) % 5 < 3, 3, 4);

        -- ---- Category-based pct ----
        CASE v_category_id
            WHEN 1 THEN SET v_pct = 4  + (h_mag % 7);
            WHEN 2 THEN SET v_pct = 5  + (h_mag % 9);
            WHEN 3 THEN SET v_pct = 5  + (h_mag % 10);
            WHEN 4 THEN SET v_pct = 7  + (h_mag % 12);
            WHEN 5 THEN SET v_pct = 10 + (h_mag % 16);
            ELSE         SET v_pct = 5  + (h_mag % 8);
        END CASE;

        -- ---- District behavior ----
        SET v_volatility = 1.00;
        SET v_trend_bias = 0;

        CASE v_district_id
            WHEN 760 THEN SET v_volatility = 0.70; SET v_trend_bias =  0;  -- Quận 1
            WHEN 770 THEN SET v_volatility = 0.75; SET v_trend_bias =  0;  -- Quận 3
            WHEN 768 THEN SET v_volatility = 0.80; SET v_trend_bias =  0;  -- Phú Nhuận
            WHEN 765 THEN SET v_volatility = 1.00; SET v_trend_bias =  1;  -- Bình Thạnh
            WHEN 778 THEN SET v_volatility = 1.10; SET v_trend_bias =  1;  -- Quận 7
            WHEN 774 THEN SET v_volatility = 1.00; SET v_trend_bias =  1;  -- Quận 4
            WHEN 775 THEN SET v_volatility = 0.85; SET v_trend_bias =  0;  -- Quận 5
            WHEN 771 THEN SET v_volatility = 0.90; SET v_trend_bias =  0;  -- Quận 10
            WHEN 772 THEN SET v_volatility = 0.90; SET v_trend_bias =  0;  -- Quận 11
            WHEN 766 THEN SET v_volatility = 0.90; SET v_trend_bias =  0;  -- Tân Bình
            WHEN 764 THEN SET v_volatility = 1.00; SET v_trend_bias =  0;  -- Gò Vấp
            WHEN 767 THEN SET v_volatility = 1.10; SET v_trend_bias =  0;  -- Tân Phú
            WHEN 776 THEN SET v_volatility = 1.00; SET v_trend_bias =  0;  -- Quận 6
            WHEN 777 THEN SET v_volatility = 1.10; SET v_trend_bias =  1;  -- Quận 8
            WHEN 761 THEN SET v_volatility = 1.20; SET v_trend_bias =  1;  -- Quận 12
            WHEN 773 THEN SET v_volatility = 1.25; SET v_trend_bias =  1;  -- Bình Tân
            WHEN 769 THEN SET v_volatility = 1.50; SET v_trend_bias =  1;  -- TP. Thủ Đức
            WHEN 785 THEN SET v_volatility = 1.35; SET v_trend_bias =  1;  -- Bình Chánh
            WHEN 784 THEN SET v_volatility = 1.30; SET v_trend_bias =  1;  -- Hóc Môn
            WHEN 783 THEN SET v_volatility = 1.20; SET v_trend_bias =  0;  -- Củ Chi
            WHEN 786 THEN SET v_volatility = 1.20; SET v_trend_bias =  1;  -- Nhà Bè
            WHEN 787 THEN SET v_volatility = 0.80; SET v_trend_bias =  0;  -- Cần Giờ
            ELSE           SET v_volatility = 1.00; SET v_trend_bias =  0;
        END CASE;

        IF v_trend_bias =  1 AND v_scenario = 2 THEN SET v_scenario = 0; END IF;
        IF v_trend_bias = -1 AND v_scenario = 0 THEN SET v_scenario = 2; END IF;

        -- ---- Step size ----
        SET v_step_vnd = GREATEST(
            ROUND(v_price * v_pct / 100 / 100000) * 100000, 100000);
        SET v_step_vnd = GREATEST(
            ROUND(v_step_vnd * v_volatility / 100000) * 100000, 100000);

        -- ---- Date range ----
        IF @use_listing_dates = TRUE THEN
            SET v_t0 = v_post_date;
            SET v_t3 = DATE_SUB(NOW(), INTERVAL (1 + (h_date >> 8) % 3) MONTH);
            IF v_t3 <= DATE_ADD(v_t0, INTERVAL 30 DAY) THEN
                SET v_t3 = DATE_ADD(v_t0, INTERVAL 30 DAY);
            END IF;
        ELSE
            SET v_t0 = DATE_SUB(NOW(), INTERVAL (14 + h_date % 13)       MONTH);
            SET v_t3 = DATE_SUB(NOW(), INTERVAL (1  + (h_date >> 8) % 3) MONTH);
        END IF;

        SET v_span_days = DATEDIFF(v_t3, v_t0);

        IF v_steps = 4 THEN
            SET v_t1 = DATE_ADD(v_t0, INTERVAL ROUND(v_span_days / 3)     DAY);
            SET v_t2 = DATE_ADD(v_t0, INTERVAL ROUND(v_span_days * 2 / 3) DAY);
        ELSE
            SET v_t1 = DATE_ADD(v_t0, INTERVAL ROUND(v_span_days / 2)     DAY);
            SET v_t2 = v_t3;
        END IF;

        -- ---- Anchor price trajectory ----
        CASE v_scenario
            WHEN 0 THEN
                SET v_p0 = v_price - (v_steps - 1) * v_step_vnd;
                SET v_p1 = v_p0 + v_step_vnd;
                SET v_p2 = v_p1 + v_step_vnd;
            WHEN 1 THEN
                IF (h_main >> 8) % 2 = 0 THEN
                    IF v_steps = 3 THEN
                        SET v_p0 = v_price + v_step_vnd;
                        SET v_p1 = v_price - v_step_vnd;
                        SET v_p2 = 0;
                    ELSE
                        SET v_p0 = v_price + 2 * v_step_vnd;
                        SET v_p1 = v_price + v_step_vnd;
                        SET v_p2 = v_price - v_step_vnd;
                    END IF;
                ELSE
                    IF v_steps = 3 THEN
                        SET v_p0 = v_price - v_step_vnd;
                        SET v_p1 = v_price + v_step_vnd;
                        SET v_p2 = 0;
                    ELSE
                        SET v_p0 = v_price - v_step_vnd;
                        SET v_p1 = v_price + v_step_vnd;
                        SET v_p2 = v_price + 2 * v_step_vnd;
                    END IF;
                END IF;
            ELSE
                SET v_p0 = v_price + (v_steps - 1) * v_step_vnd;
                SET v_p1 = v_p0 - v_step_vnd;
                SET v_p2 = v_p1 - v_step_vnd;
        END CASE;

        SET v_p3 = v_price;

        IF v_p0 < 100000 THEN SET v_p0 = 100000; END IF;
        IF v_p1 < 100000 THEN SET v_p1 = 100000; END IF;
        IF v_p2 < 100000 THEN SET v_p2 = 100000; END IF;

        -- ================================================================
        -- INSERT all records interleaved with ADJUSTED points
        -- v_last_price tracks the new_price of the most recently inserted row
        -- ================================================================

        -- ── INITIAL ──────────────────────────────────────────────────────
        INSERT INTO pricing_histories (
            listing_id, old_price, new_price, old_price_unit, new_price_unit,
            change_type, change_percentage, change_amount,
            is_current, changed_by, change_reason, changed_at
        ) VALUES (
            v_listing_id, NULL, v_p0, NULL, v_price_unit,
            'INITIAL', 0, 0, FALSE, v_user_id,
            'Giá ban đầu khi đăng tin', v_t0
        );
        SET v_last_price = v_p0;

        -- ── ADJUSTED: gap t0 → t1 ────────────────────────────────────────
        SET v_gap_days    = DATEDIFF(v_t1, v_t0);
        SET v_num_adj     = GREATEST(FLOOR(v_gap_days / 60) - 1, 0);
        SET v_t_gap_start = v_t0;
        SET v_p_gap_start = v_p0;
        SET v_p_gap_end   = v_p1;
        SET v_adj_i = 1;
        WHILE v_adj_i <= v_num_adj DO
            SET v_t_adj = DATE_ADD(v_t_gap_start, INTERVAL v_adj_i * 60 DAY);
            SET v_p_adj = GREATEST(
                ROUND((v_p_gap_start + (v_p_gap_end - v_p_gap_start)
                       * (v_adj_i * 60.0) / NULLIF(v_gap_days, 1)) / 100000) * 100000,
                100000);
            SET v_amt_val = v_p_adj - v_last_price;
            SET v_pct_val = GREATEST(-99.99, LEAST(99.99,
                ROUND(v_amt_val / NULLIF(v_last_price, 0) * 100, 2)));
            INSERT INTO pricing_histories (
                listing_id, old_price, new_price, old_price_unit, new_price_unit,
                change_type, change_percentage, change_amount,
                is_current, changed_by, change_reason, changed_at
            ) VALUES (
                v_listing_id, v_last_price, v_p_adj, v_price_unit, v_price_unit,
                'ADJUSTED', v_pct_val, v_amt_val, FALSE, v_user_id,
                ELT(CRC32(CONCAT(v_listing_id, ':a0:', v_adj_i)) % 4 + 1,
                    'Điều chỉnh định kỳ theo thị trường',
                    'Cập nhật giá theo biến động khu vực',
                    'Điều chỉnh nhẹ phù hợp nhu cầu thuê',
                    'Theo dõi và cập nhật định kỳ'),
                v_t_adj);
            SET v_last_price = v_p_adj;
            SET v_adj_i = v_adj_i + 1;
        END WHILE;

        -- ── Anchor t1 ────────────────────────────────────────────────────
        SET v_amt_val = v_p1 - v_last_price;
        SET v_pct_val = GREATEST(-99.99, LEAST(99.99,
            ROUND(v_amt_val / NULLIF(v_last_price, 0) * 100, 2)));
        SET v_reason_idx = IF(v_p1 >= v_last_price,
            (h_main >> 12) % 5 + 1, (h_main >> 12) % 4 + 6);
        INSERT INTO pricing_histories (
            listing_id, old_price, new_price, old_price_unit, new_price_unit,
            change_type, change_percentage, change_amount,
            is_current, changed_by, change_reason, changed_at
        ) VALUES (
            v_listing_id, v_last_price, v_p1, v_price_unit, v_price_unit,
            IF(v_p1 > v_last_price, 'INCREASE', IF(v_p1 < v_last_price, 'DECREASE', 'CORRECTION')),
            v_pct_val, v_amt_val, FALSE, v_user_id,
            ELT(v_reason_idx,
                'Điều chỉnh giá theo thị trường',
                'Trang bị thêm nội thất mới',
                'Nâng cấp tiện ích, cơ sở vật chất',
                'Điều chỉnh theo chỉ số lạm phát',
                'Khu vực tăng giá mạnh',
                'Giảm giá để tìm khách nhanh hơn',
                'Điều chỉnh cạnh tranh với khu xung quanh',
                'Khuyến mãi hợp đồng dài hạn',
                'Giảm do phòng trống quá lâu'),
            v_t1);
        SET v_last_price = v_p1;

        -- ── ADJUSTED + Anchor t2 (steps = 4 only) ────────────────────────
        IF v_steps = 4 THEN
            SET v_gap_days    = DATEDIFF(v_t2, v_t1);
            SET v_num_adj     = GREATEST(FLOOR(v_gap_days / 60) - 1, 0);
            SET v_t_gap_start = v_t1;
            SET v_p_gap_start = v_p1;
            SET v_p_gap_end   = v_p2;
            SET v_adj_i = 1;
            WHILE v_adj_i <= v_num_adj DO
                SET v_t_adj = DATE_ADD(v_t_gap_start, INTERVAL v_adj_i * 60 DAY);
                SET v_p_adj = GREATEST(
                    ROUND((v_p_gap_start + (v_p_gap_end - v_p_gap_start)
                           * (v_adj_i * 60.0) / NULLIF(v_gap_days, 1)) / 100000) * 100000,
                    100000);
                SET v_amt_val = v_p_adj - v_last_price;
                SET v_pct_val = GREATEST(-99.99, LEAST(99.99,
                    ROUND(v_amt_val / NULLIF(v_last_price, 0) * 100, 2)));
                INSERT INTO pricing_histories (
                    listing_id, old_price, new_price, old_price_unit, new_price_unit,
                    change_type, change_percentage, change_amount,
                    is_current, changed_by, change_reason, changed_at
                ) VALUES (
                    v_listing_id, v_last_price, v_p_adj, v_price_unit, v_price_unit,
                    'ADJUSTED', v_pct_val, v_amt_val, FALSE, v_user_id,
                    ELT(CRC32(CONCAT(v_listing_id, ':a1:', v_adj_i)) % 4 + 1,
                        'Điều chỉnh định kỳ theo thị trường',
                        'Cập nhật giá theo biến động khu vực',
                        'Điều chỉnh nhẹ phù hợp nhu cầu thuê',
                        'Theo dõi và cập nhật định kỳ'),
                    v_t_adj);
                SET v_last_price = v_p_adj;
                SET v_adj_i = v_adj_i + 1;
            END WHILE;

            SET v_amt_val = v_p2 - v_last_price;
            SET v_pct_val = GREATEST(-99.99, LEAST(99.99,
                ROUND(v_amt_val / NULLIF(v_last_price, 0) * 100, 2)));
            SET v_reason_idx = IF(v_p2 >= v_last_price,
                (h_date >> 12) % 5 + 1, (h_date >> 12) % 4 + 6);
            INSERT INTO pricing_histories (
                listing_id, old_price, new_price, old_price_unit, new_price_unit,
                change_type, change_percentage, change_amount,
                is_current, changed_by, change_reason, changed_at
            ) VALUES (
                v_listing_id, v_last_price, v_p2, v_price_unit, v_price_unit,
                IF(v_p2 > v_last_price, 'INCREASE', IF(v_p2 < v_last_price, 'DECREASE', 'CORRECTION')),
                v_pct_val, v_amt_val, FALSE, v_user_id,
                ELT(v_reason_idx,
                    'Điều chỉnh giá theo thị trường',
                    'Cải tạo, nâng cấp hoàn tất',
                    'Điều chỉnh sau khi gia hạn hợp đồng',
                    'Cập nhật theo giá thuê khu vực',
                    'Lắp thêm điều hòa, nội thất cao cấp',
                    'Giảm ưu đãi thêm 1 tháng miễn phí',
                    'Điều chỉnh cạnh tranh',
                    'Hỗ trợ khách thuê mùa thấp điểm',
                    'Ưu đãi ký hợp đồng 12 tháng'),
                v_t2);
            SET v_last_price = v_p2;
        END IF;

        -- ── ADJUSTED: gap (last anchor) → t3 ─────────────────────────────
        SET v_t_gap_start = IF(v_steps = 4, v_t2, v_t1);
        SET v_p_gap_start = IF(v_steps = 4, v_p2, v_p1);
        SET v_p_gap_end   = v_p3;
        SET v_gap_days    = DATEDIFF(v_t3, v_t_gap_start);
        SET v_num_adj     = GREATEST(FLOOR(v_gap_days / 60) - 1, 0);
        SET v_adj_i = 1;
        WHILE v_adj_i <= v_num_adj DO
            SET v_t_adj = DATE_ADD(v_t_gap_start, INTERVAL v_adj_i * 60 DAY);
            SET v_p_adj = GREATEST(
                ROUND((v_p_gap_start + (v_p_gap_end - v_p_gap_start)
                       * (v_adj_i * 60.0) / NULLIF(v_gap_days, 1)) / 100000) * 100000,
                100000);
            SET v_amt_val = v_p_adj - v_last_price;
            SET v_pct_val = GREATEST(-99.99, LEAST(99.99,
                ROUND(v_amt_val / NULLIF(v_last_price, 0) * 100, 2)));
            INSERT INTO pricing_histories (
                listing_id, old_price, new_price, old_price_unit, new_price_unit,
                change_type, change_percentage, change_amount,
                is_current, changed_by, change_reason, changed_at
            ) VALUES (
                v_listing_id, v_last_price, v_p_adj, v_price_unit, v_price_unit,
                'ADJUSTED', v_pct_val, v_amt_val, FALSE, v_user_id,
                ELT(CRC32(CONCAT(v_listing_id, ':a2:', v_adj_i)) % 4 + 1,
                    'Điều chỉnh định kỳ theo thị trường',
                    'Cập nhật giá theo biến động khu vực',
                    'Điều chỉnh nhẹ phù hợp nhu cầu thuê',
                    'Theo dõi và cập nhật định kỳ'),
                v_t_adj);
            SET v_last_price = v_p_adj;
            SET v_adj_i = v_adj_i + 1;
        END WHILE;

        -- ── Final anchor (is_current = TRUE) ─────────────────────────────
        SET v_amt_val = v_p3 - v_last_price;
        SET v_pct_val = GREATEST(-99.99, LEAST(99.99,
            ROUND(v_amt_val / NULLIF(v_last_price, 0) * 100, 2)));
        INSERT INTO pricing_histories (
            listing_id, old_price, new_price, old_price_unit, new_price_unit,
            change_type, change_percentage, change_amount,
            is_current, changed_by, change_reason, changed_at
        ) VALUES (
            v_listing_id, v_last_price, v_p3, v_price_unit, v_price_unit,
            IF(v_p3 > v_last_price, 'INCREASE', IF(v_p3 < v_last_price, 'DECREASE', 'CORRECTION')),
            v_pct_val, v_amt_val, TRUE, v_user_id,
            ELT((h_mag >> 8) % 5 + 1,
                'Ổn định giá theo thị trường hiện tại',
                'Điều chỉnh giá sau đợt cải tạo',
                'Giá cạnh tranh nhất khu vực',
                'Cập nhật giá tháng mới',
                'Điều chỉnh lần cuối theo thực tế'),
            v_t3);

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
-- TRUNCATE first (local dev) then execute
-- Comment out TRUNCATE if running on production
-- ============================================================================
TRUNCATE TABLE pricing_histories;

CALL populate_pricing_history();

DROP PROCEDURE IF EXISTS populate_pricing_history;

-- ============================================================================
-- Quick verification
-- ============================================================================
SELECT
    COUNT(DISTINCT listing_id)          AS listings,
    COUNT(*)                            AS total_rows,
    ROUND(COUNT(*) / COUNT(DISTINCT listing_id), 1) AS avg_rows_per_listing,
    SUM(change_type = 'INITIAL')        AS initial_rows,
    SUM(change_type = 'INCREASE')       AS increase_rows,
    SUM(change_type = 'DECREASE')       AS decrease_rows,
    SUM(change_type = 'ADJUSTED')       AS adjusted_rows,
    MIN(changed_at)                     AS earliest,
    MAX(changed_at)                     AS latest
FROM pricing_histories;
