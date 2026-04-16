-- ============================================================================
-- Script: verify_pricing_history.sql
-- Purpose: Verify pricing history data quality in one combined result table.
-- Usage:   mysql -u <user> -p smartrent < scripts/verify_pricing_history.sql
-- ============================================================================

USE smartrent;

SELECT
    check_id,
    check_name,
    result,
    expected,
    IF(status_val = 0, 'PASS',
       IF(status_val = 1, 'WARN', 'FAIL'))  AS status
FROM (

    -- 1. Coverage: every non-draft listing has history
    SELECT 1 AS check_id, 'Coverage' AS check_name,
        CONCAT(
            (SELECT COUNT(DISTINCT listing_id) FROM pricing_histories),
            ' / ',
            (SELECT COUNT(*) FROM listings WHERE is_draft = FALSE),
            ' listings have history'
        ) AS result,
        'all listings covered' AS expected,
        IF((SELECT COUNT(*) FROM listings l WHERE is_draft = FALSE
            AND NOT EXISTS (SELECT 1 FROM pricing_histories ph
                            WHERE ph.listing_id = l.listing_id)) = 0, 0, 2) AS status_val

    UNION ALL

    -- 2. Total rows
    SELECT 2, 'Total rows',
        CONCAT(COUNT(*), ' rows (', COUNT(DISTINCT listing_id), ' listings, avg ',
               ROUND(COUNT(*) / NULLIF(COUNT(DISTINCT listing_id), 0), 1), ' rows/listing)'),
        '7–13 rows per listing expected',
        IF(COUNT(*) BETWEEN COUNT(DISTINCT listing_id) * 3
                        AND COUNT(DISTINCT listing_id) * 20, 0, 2)
    FROM pricing_histories

    UNION ALL

    -- 3. Record distribution (listings outside 3–15 row range)
    SELECT 3, 'Record distribution',
        CONCAT(
            SUM(records BETWEEN 3 AND 15), ' listings in range [3,15], ',
            SUM(records < 3), ' under, ',
            SUM(records > 15), ' over'
        ),
        'all listings: 3–15 rows',
        IF(SUM(records NOT BETWEEN 3 AND 15) = 0, 0, 2)
    FROM (SELECT listing_id, COUNT(*) AS records FROM pricing_histories GROUP BY listing_id) t

    UNION ALL

    -- 4. Exactly one is_current = TRUE per listing
    SELECT 4, 'is_current uniqueness',
        CONCAT(
            (SELECT COUNT(*) FROM (
                SELECT listing_id, SUM(is_current) AS cnt
                FROM pricing_histories GROUP BY listing_id HAVING cnt != 1
            ) x), ' listings with wrong is_current count'
        ),
        '0 bad listings',
        IF((SELECT COUNT(*) FROM (
                SELECT listing_id, SUM(is_current) AS cnt
                FROM pricing_histories GROUP BY listing_id HAVING cnt != 1
            ) x) = 0, 0, 2)

    UNION ALL

    -- 5. Final price matches listing.price
    SELECT 5, 'Final price match',
        CONCAT(
            (SELECT COUNT(*) FROM pricing_histories ph
             JOIN listings l ON l.listing_id = ph.listing_id
             WHERE ph.is_current = TRUE AND ph.new_price != l.price),
            ' mismatches'
        ),
        '0 mismatches',
        IF((SELECT COUNT(*) FROM pricing_histories ph
            JOIN listings l ON l.listing_id = ph.listing_id
            WHERE ph.is_current = TRUE AND ph.new_price != l.price) = 0, 0, 2)

    UNION ALL

    -- 6. Zero-change records (ADJUSTED allowed; only INCREASE/DECREASE checked)
    SELECT 6, 'Zero-change records',
        CONCAT(
            (SELECT COUNT(*) FROM pricing_histories
             WHERE change_type IN ('INCREASE','DECREASE') AND change_amount = 0),
            ' zero-change anchor rows  |  ',
            (SELECT COUNT(*) FROM pricing_histories
             WHERE change_type = 'ADJUSTED' AND change_amount = 0),
            ' zero-change ADJUSTED rows (ok)'
        ),
        '0 anchor zero-change rows',
        IF((SELECT COUNT(*) FROM pricing_histories
            WHERE change_type IN ('INCREASE','DECREASE') AND change_amount = 0) = 0, 0, 2)

    UNION ALL

    -- 7. change_type correctness
    SELECT 7, 'change_type correctness',
        CONCAT(
            (SELECT COUNT(*) FROM pricing_histories
             WHERE (change_type = 'INCREASE' AND new_price <= old_price)
                OR (change_type = 'DECREASE' AND new_price >= old_price)),
            ' wrong-type rows'
        ),
        '0',
        IF((SELECT COUNT(*) FROM pricing_histories
            WHERE (change_type = 'INCREASE' AND new_price <= old_price)
               OR (change_type = 'DECREASE' AND new_price >= old_price)) = 0, 0, 2)

    UNION ALL

    -- 8. Price chain continuity (old_price = prev new_price)
    SELECT 8, 'Price chain continuity',
        CONCAT(
            (SELECT COUNT(*) FROM (
                SELECT old_price,
                       LAG(new_price) OVER (PARTITION BY listing_id ORDER BY changed_at) AS prev_new
                FROM pricing_histories WHERE change_type != 'INITIAL'
            ) t WHERE old_price != prev_new),
            ' broken chain rows'
        ),
        '0',
        IF((SELECT COUNT(*) FROM (
                SELECT old_price,
                       LAG(new_price) OVER (PARTITION BY listing_id ORDER BY changed_at) AS prev_new
                FROM pricing_histories WHERE change_type != 'INITIAL'
            ) t WHERE old_price != prev_new) = 0, 0, 1)  -- WARN not FAIL (rounding)

    UNION ALL

    -- 9. Chronological order
    SELECT 9, 'Chronological order',
        CONCAT(
            (SELECT COUNT(*) FROM (
                SELECT changed_at,
                       LAG(changed_at) OVER (PARTITION BY listing_id ORDER BY changed_at) AS prev_at
                FROM pricing_histories
            ) t WHERE prev_at IS NOT NULL AND changed_at <= prev_at),
            ' out-of-order rows'
        ),
        '0',
        IF((SELECT COUNT(*) FROM (
                SELECT changed_at,
                       LAG(changed_at) OVER (PARTITION BY listing_id ORDER BY changed_at) AS prev_at
                FROM pricing_histories
            ) t WHERE prev_at IS NOT NULL AND changed_at <= prev_at) = 0, 0, 2)

    UNION ALL

    -- 10. Price floor (no price < 100k)
    SELECT 10, 'Price floor (≥ 100k)',
        CONCAT(
            (SELECT COUNT(*) FROM pricing_histories
             WHERE new_price < 100000 OR (old_price IS NOT NULL AND old_price < 100000)),
            ' below-floor rows'
        ),
        '0',
        IF((SELECT COUNT(*) FROM pricing_histories
            WHERE new_price < 100000 OR (old_price IS NOT NULL AND old_price < 100000)) = 0, 0, 2)

    UNION ALL

    -- 11. Date span (INITIAL records should span ≥ 6 months)
    SELECT 11, 'Date span',
        CONCAT(
            ROUND(DATEDIFF(MAX(changed_at), MIN(changed_at)) / 30),
            ' months  (',
            DATE_FORMAT(MIN(changed_at), '%Y-%m-%d'), ' → ',
            DATE_FORMAT(MAX(changed_at), '%Y-%m-%d'), ')'
        ),
        '≥ 6 months',
        IF(DATEDIFF(MAX(changed_at), MIN(changed_at)) >= 180, 0, 1)
    FROM pricing_histories WHERE change_type = 'INITIAL'

    UNION ALL

    -- 12. District volatility check:
    --     TP.Thủ Đức avg step > Quận 1 avg step
    SELECT 12, 'District volatility order',
        CONCAT(
            'Q.1 avg step = ',
            ROUND((SELECT AVG(ABS(ph.change_amount)) FROM pricing_histories ph
                   JOIN listings l ON l.listing_id = ph.listing_id
                   JOIN addresses a ON a.address_id = l.address_id
                   WHERE a.legacy_district_id = 760
                     AND ph.change_type != 'INITIAL') / 1000000, 2),
            'M  |  TP.Thủ Đức avg step = ',
            ROUND((SELECT AVG(ABS(ph.change_amount)) FROM pricing_histories ph
                   JOIN listings l ON l.listing_id = ph.listing_id
                   JOIN addresses a ON a.address_id = l.address_id
                   WHERE a.legacy_district_id = 769
                     AND ph.change_type != 'INITIAL') / 1000000, 2),
            'M'
        ),
        'Thủ Đức > Quận 1',
        IF(
            (SELECT AVG(ABS(ph.change_amount)) FROM pricing_histories ph
             JOIN listings l ON l.listing_id = ph.listing_id
             JOIN addresses a ON a.address_id = l.address_id
             WHERE a.legacy_district_id = 769 AND ph.change_type != 'INITIAL')
            >
            (SELECT AVG(ABS(ph.change_amount)) FROM pricing_histories ph
             JOIN listings l ON l.listing_id = ph.listing_id
             JOIN addresses a ON a.address_id = l.address_id
             WHERE a.legacy_district_id = 760 AND ph.change_type != 'INITIAL'),
            0, 1)

) checks
ORDER BY check_id;
