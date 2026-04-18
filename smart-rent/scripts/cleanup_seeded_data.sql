-- ============================================================================
-- Script: cleanup_seeded_data.sql
-- Purpose: Remove all seeded/populated data from production before re-populating
-- Usage:  Run manually via mysql client, NOT via Flyway
--         mysql -u <user> -p <database> < cleanup_seeded_data.sql
--
-- IMPORTANT: Configure the variables below before running!
--
-- This script handles TWO types of seeded data:
--   1. OLD seeded data: bulk-inserted under a real user_id (e.g. 6406c49f-...)
--      → Identified by @old_seed_user_id
--   2. NEW seeded data: inserted by populate_listings.sql with test users
--      → Identified by user_id LIKE '00000000-test-%'
--
-- Safety:
--   - Shows a DRY RUN count first so you can verify before deleting
--   - Deletes child tables first (FK order)
--   - Batched DELETEs with COMMIT to avoid lock timeout
--   - Orphaned addresses cleaned up at the end
--   - Real user account is NOT deleted (only test users are deleted)
--
-- Run this BEFORE populate_addresses_for_listings.sql
-- ============================================================================

-- ============================================================================
-- CONFIGURE THESE BEFORE RUNNING
-- ============================================================================

-- The real user_id that was used to bulk-insert old seeded listings.
-- Set to NULL if there is no old seeded data to clean up.
SET @old_seed_user_id = '6406c49f-a945-4846-b654-5792195db347' COLLATE utf8mb4_unicode_ci;

-- Batch size for deletes (lower = safer for production, higher = faster)
SET @batch_size = 5000;

-- ============================================================================
-- Step 0: DRY RUN — show what will be deleted
-- ============================================================================
SELECT '=== DRY RUN: Data to be deleted ===' AS section;

SELECT 'old_seed_listings' AS category, COUNT(*) AS count
FROM listings WHERE user_id = @old_seed_user_id AND @old_seed_user_id IS NOT NULL
UNION ALL
SELECT 'new_test_listings', COUNT(*)
FROM listings WHERE user_id LIKE '00000000-test-%'
UNION ALL
SELECT 'old_seed_media', COUNT(*)
FROM media m INNER JOIN listings l ON m.listing_id = l.listing_id
WHERE l.user_id = @old_seed_user_id AND @old_seed_user_id IS NOT NULL
UNION ALL
SELECT 'new_test_media', COUNT(*)
FROM media m INNER JOIN listings l ON m.listing_id = l.listing_id
WHERE l.user_id LIKE '00000000-test-%'
UNION ALL
SELECT 'old_seed_amenities', COUNT(*)
FROM listing_amenities la INNER JOIN listings l ON la.listing_id = l.listing_id
WHERE l.user_id = @old_seed_user_id AND @old_seed_user_id IS NOT NULL
UNION ALL
SELECT 'new_test_amenities', COUNT(*)
FROM listing_amenities la INNER JOIN listings l ON la.listing_id = l.listing_id
WHERE l.user_id LIKE '00000000-test-%'
UNION ALL
SELECT 'old_seed_pricing', COUNT(*)
FROM pricing_histories ph INNER JOIN listings l ON ph.listing_id = l.listing_id
WHERE l.user_id = @old_seed_user_id AND @old_seed_user_id IS NOT NULL
UNION ALL
SELECT 'new_test_pricing', COUNT(*)
FROM pricing_histories ph INNER JOIN listings l ON ph.listing_id = l.listing_id
WHERE l.user_id LIKE '00000000-test-%'
UNION ALL
SELECT 'test_users', COUNT(*)
FROM users WHERE user_id LIKE '00000000-test-%';

-- Show sample of old seed listings so you can verify it's the right data
SELECT 'Sample old seed listings (first 5):' AS info;
SELECT listing_id, title, user_id, created_at
FROM listings
WHERE user_id = @old_seed_user_id AND @old_seed_user_id IS NOT NULL
ORDER BY listing_id
LIMIT 5;

-- ============================================================================
-- Stored procedure: batched cleanup
-- ============================================================================
DROP PROCEDURE IF EXISTS cleanup_seeded_data;

DELIMITER //

CREATE PROCEDURE cleanup_seeded_data()
BEGIN
    DECLARE v_rows_affected INT DEFAULT 1;
    DECLARE v_total_deleted BIGINT DEFAULT 0;
    DECLARE v_batch INT;

    SET v_batch = @batch_size;

    -- Disable safe update mode (temp tables don't satisfy KEY requirement)
    SET SQL_SAFE_UPDATES = 0;

    -- ==================================================================
    -- Collect all listing_ids to delete into a temp table
    -- ==================================================================
    DROP TEMPORARY TABLE IF EXISTS tmp_listings_to_delete;
    CREATE TEMPORARY TABLE tmp_listings_to_delete (
        listing_id BIGINT PRIMARY KEY
    );

    INSERT INTO tmp_listings_to_delete (listing_id)
    SELECT listing_id FROM listings
    WHERE user_id LIKE '00000000-test-%'
       OR (user_id = @old_seed_user_id AND @old_seed_user_id IS NOT NULL);

    SELECT CONCAT('Total listings to delete: ', COUNT(*)) AS plan FROM tmp_listings_to_delete;

    -- Save address_ids before we delete listings
    DROP TEMPORARY TABLE IF EXISTS tmp_address_ids_to_cleanup;
    CREATE TEMPORARY TABLE tmp_address_ids_to_cleanup (
        address_id BIGINT PRIMARY KEY
    );

    INSERT IGNORE INTO tmp_address_ids_to_cleanup (address_id)
    SELECT DISTINCT l.address_id
    FROM listings l
    INNER JOIN tmp_listings_to_delete t ON l.listing_id = t.listing_id
    WHERE l.address_id IS NOT NULL;

    SET autocommit = 0;

    -- ==================================================================
    -- Step 1: Delete media (child of listings)
    -- ==================================================================
    SELECT 'Step 1: Deleting media...' AS step;

    SET v_total_deleted = 0;
    SET v_rows_affected = 1;
    WHILE v_rows_affected > 0 DO
        DELETE FROM media
        WHERE media_id IN (
            SELECT media_id FROM (
                SELECT m.media_id
                FROM media m
                INNER JOIN tmp_listings_to_delete t ON m.listing_id = t.listing_id
                LIMIT 5000
            ) AS batch
        );
        SET v_rows_affected = ROW_COUNT();
        SET v_total_deleted = v_total_deleted + v_rows_affected;
        COMMIT;
    END WHILE;

    SELECT CONCAT('  Media deleted: ', v_total_deleted) AS media_result;

    -- ==================================================================
    -- Step 2: Delete listing_amenities (child of listings)
    -- ==================================================================
    SELECT 'Step 2: Deleting listing_amenities...' AS step;

    SET v_total_deleted = 0;
    SET v_rows_affected = 1;
    WHILE v_rows_affected > 0 DO
        DELETE FROM listing_amenities
        WHERE listing_id IN (
            SELECT listing_id FROM (
                SELECT la.listing_id
                FROM listing_amenities la
                INNER JOIN tmp_listings_to_delete t ON la.listing_id = t.listing_id
                LIMIT 5000
            ) AS batch
        );
        SET v_rows_affected = ROW_COUNT();
        SET v_total_deleted = v_total_deleted + v_rows_affected;
        COMMIT;
    END WHILE;

    SELECT CONCAT('  Amenities deleted: ', v_total_deleted) AS amenities_result;

    -- ==================================================================
    -- Step 3: Delete pricing_histories (child of listings)
    -- ==================================================================
    SELECT 'Step 3: Deleting pricing_histories...' AS step;

    SET v_total_deleted = 0;
    SET v_rows_affected = 1;
    WHILE v_rows_affected > 0 DO
        DELETE FROM pricing_histories
        WHERE listing_id IN (
            SELECT listing_id FROM (
                SELECT ph.listing_id
                FROM pricing_histories ph
                INNER JOIN tmp_listings_to_delete t ON ph.listing_id = t.listing_id
                LIMIT 5000
            ) AS batch
        );
        SET v_rows_affected = ROW_COUNT();
        SET v_total_deleted = v_total_deleted + v_rows_affected;
        COMMIT;
    END WHILE;

    SELECT CONCAT('  Price histories deleted: ', v_total_deleted) AS pricing_result;

    -- ==================================================================
    -- Step 4: Delete listings
    -- ==================================================================
    SELECT 'Step 4: Deleting listings...' AS step;

    SET v_total_deleted = 0;
    SET v_rows_affected = 1;
    WHILE v_rows_affected > 0 DO
        DELETE FROM listings
        WHERE listing_id IN (
            SELECT listing_id FROM (
                SELECT listing_id FROM tmp_listings_to_delete LIMIT 5000
            ) AS batch
        );
        SET v_rows_affected = ROW_COUNT();
        SET v_total_deleted = v_total_deleted + v_rows_affected;

        -- Also remove from temp table so LIMIT keeps working
        DELETE FROM tmp_listings_to_delete
        WHERE listing_id NOT IN (SELECT listing_id FROM listings);

        COMMIT;
    END WHILE;

    SELECT CONCAT('  Listings deleted: ', v_total_deleted) AS listings_result;

    DROP TEMPORARY TABLE IF EXISTS tmp_listings_to_delete;

    -- ==================================================================
    -- Step 5: Delete orphaned addresses (were used by deleted listings)
    -- ==================================================================
    SELECT 'Step 5: Deleting orphaned addresses...' AS step;

    SET v_total_deleted = 0;
    SET v_rows_affected = 1;
    WHILE v_rows_affected > 0 DO
        DELETE FROM addresses
        WHERE address_id IN (
            SELECT address_id FROM (
                SELECT t.address_id
                FROM tmp_address_ids_to_cleanup t
                LEFT JOIN listings l ON t.address_id = l.address_id
                WHERE l.listing_id IS NULL
                LIMIT 5000
            ) AS batch
        );
        SET v_rows_affected = ROW_COUNT();
        SET v_total_deleted = v_total_deleted + v_rows_affected;

        -- Remove cleaned-up ids from temp table
        DELETE FROM tmp_address_ids_to_cleanup
        WHERE address_id NOT IN (SELECT address_id FROM addresses);

        COMMIT;
    END WHILE;

    DROP TEMPORARY TABLE IF EXISTS tmp_address_ids_to_cleanup;

    SELECT CONCAT('  Orphaned addresses deleted: ', v_total_deleted) AS addresses_result;

    -- ==================================================================
    -- Step 6: Delete test users (NOT the real user)
    -- ==================================================================
    SELECT 'Step 6: Deleting test users...' AS step;

    DELETE FROM users WHERE user_id LIKE '00000000-test-%';
    COMMIT;

    SELECT CONCAT('  Test users deleted. Remaining: ',
        (SELECT COUNT(*) FROM users WHERE user_id LIKE '00000000-test-%')
    ) AS users_result;

    SET autocommit = 1;
    SET SQL_SAFE_UPDATES = 1;

END //

DELIMITER ;

-- ============================================================================
-- Execute
-- ============================================================================
CALL cleanup_seeded_data();

DROP PROCEDURE IF EXISTS cleanup_seeded_data;

-- NOTE: The real user (@old_seed_user_id) is NOT deleted.
-- Their listings are removed but the user account stays.

-- ============================================================================
-- Verification
-- ============================================================================
SELECT '=== CLEANUP VERIFICATION ===' AS section;

SELECT 'old_seed_listings' AS entity, COUNT(*) AS remaining
FROM listings WHERE user_id = @old_seed_user_id AND @old_seed_user_id IS NOT NULL
UNION ALL
SELECT 'new_test_listings', COUNT(*)
FROM listings WHERE user_id LIKE '00000000-test-%'
UNION ALL
SELECT 'test_users', COUNT(*)
FROM users WHERE user_id LIKE '00000000-test-%';

SELECT CONCAT('Total addresses in DB: ', (SELECT COUNT(*) FROM addresses)) AS address_total;
SELECT CONCAT('Total listings in DB: ', (SELECT COUNT(*) FROM listings)) AS listing_total;
SELECT CONCAT('Total media in DB: ', (SELECT COUNT(*) FROM media)) AS media_total;

SELECT 'CLEANUP COMPLETE. Safe to run populate scripts.' AS status;