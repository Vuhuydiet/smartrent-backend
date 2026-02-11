-- =====================================================
-- SMARTRENT VIP TIER SYSTEM UPDATE
-- Version: V15
-- Description: Update VIP tier system from NORMAL/VIP/PREMIUM to NORMAL/SILVER/GOLD/DIAMOND
--              Update BenefitType from VIP_POSTS/PREMIUM_POSTS to POST_SILVER/POST_GOLD/POST_DIAMOND
-- =====================================================

DELIMITER $$

CREATE PROCEDURE migrate_vip_tiers()
BEGIN
    DECLARE column_exists INT;
    DECLARE old_column_exists INT;
    DECLARE vip_type_exists INT;
    DECLARE new_column_exists INT;
    DECLARE benefit_type_exists INT;
    DECLARE index_exists INT;

    -- =====================================================
    -- 1. UPDATE LISTINGS TABLE - VIP TYPE ENUM
    -- =====================================================
    -- Step 1: Add new vip_type_new column with new enum values (if not exists)
    SELECT COUNT(*) INTO column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type_new';

    IF column_exists = 0 THEN
        ALTER TABLE listings ADD COLUMN vip_type_new ENUM('NORMAL', 'SILVER', 'GOLD', 'DIAMOND') NOT NULL DEFAULT 'NORMAL' AFTER vip_type;
    END IF;

    -- Step 2: Migrate existing data (only if old vip_type column still exists)
    SELECT COUNT(*) INTO old_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type'
        AND COLUMN_TYPE LIKE '%VIP%';

    IF old_column_exists > 0 THEN
        UPDATE listings
        SET vip_type_new = CASE
            WHEN vip_type = 'NORMAL' THEN 'NORMAL'
            WHEN vip_type = 'VIP' THEN 'SILVER'
            WHEN vip_type = 'PREMIUM' THEN 'DIAMOND'
            ELSE 'NORMAL'
        END;
    END IF;

    -- Step 3: Drop old column if it still exists with old enum
    SELECT COUNT(*) INTO old_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type'
        AND COLUMN_TYPE LIKE '%VIP%';

    IF old_column_exists > 0 THEN
        ALTER TABLE listings DROP COLUMN vip_type;
    END IF;

    -- Check if both vip_type and vip_type_new exist (shouldn't happen, but handle it)
    SELECT COUNT(*) INTO vip_type_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type';

    SELECT COUNT(*) INTO new_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type_new';

    -- If both exist, drop vip_type first (it's the old one)
    IF vip_type_exists > 0 AND new_column_exists > 0 THEN
        ALTER TABLE listings DROP COLUMN vip_type;
    END IF;

    -- Now rename vip_type_new to vip_type if vip_type_new exists
    SELECT COUNT(*) INTO new_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type_new';

    IF new_column_exists > 0 THEN
        ALTER TABLE listings CHANGE COLUMN vip_type_new vip_type ENUM('NORMAL', 'SILVER', 'GOLD', 'DIAMOND') NOT NULL DEFAULT 'NORMAL';
    END IF;

    -- =====================================================
    -- 2. UPDATE MEMBERSHIP PACKAGE BENEFITS - BENEFIT TYPE ENUM
    -- =====================================================
    -- Step 1: Add new benefit_type_new column with new enum values (if not exists)
    SELECT COUNT(*) INTO column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type_new';

    IF column_exists = 0 THEN
        ALTER TABLE membership_package_benefits ADD COLUMN benefit_type_new ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'BOOST', 'AUTO_APPROVE', 'BADGE') NOT NULL DEFAULT 'BOOST' AFTER benefit_type;
    END IF;

    -- Step 2: Migrate existing data (only if old benefit_type column still has old values)
    SELECT COUNT(*) INTO old_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type'
        AND COLUMN_TYPE LIKE '%VIP_POSTS%';

    IF old_column_exists > 0 THEN
        UPDATE membership_package_benefits
        SET benefit_type_new = CASE
            WHEN benefit_type = 'VIP_POSTS' THEN 'POST_SILVER'
            WHEN benefit_type = 'PREMIUM_POSTS' THEN 'POST_DIAMOND'
            WHEN benefit_type = 'BOOST_QUOTA' THEN 'BOOST'
            WHEN benefit_type = 'AUTO_VERIFY' THEN 'AUTO_APPROVE'
            WHEN benefit_type = 'TRUSTED_BADGE' THEN 'BADGE'
            ELSE 'BOOST'
        END;
    END IF;

    -- Step 3: Drop old column if it still exists with old enum
    SELECT COUNT(*) INTO old_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type'
        AND COLUMN_TYPE LIKE '%VIP_POSTS%';

    IF old_column_exists > 0 THEN
        ALTER TABLE membership_package_benefits DROP COLUMN benefit_type;
    END IF;

    -- Check if both benefit_type and benefit_type_new exist (shouldn't happen, but handle it)
    SELECT COUNT(*) INTO benefit_type_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type';

    SELECT COUNT(*) INTO new_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type_new';

    -- If both exist, drop benefit_type first (it's the old one)
    IF benefit_type_exists > 0 AND new_column_exists > 0 THEN
        ALTER TABLE membership_package_benefits DROP COLUMN benefit_type;
    END IF;

    -- Now rename benefit_type_new to benefit_type if benefit_type_new exists
    SELECT COUNT(*) INTO new_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type_new';

    IF new_column_exists > 0 THEN
        ALTER TABLE membership_package_benefits CHANGE COLUMN benefit_type_new benefit_type ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'BOOST', 'AUTO_APPROVE', 'BADGE') NOT NULL;
    END IF;

    -- =====================================================
    -- 3. UPDATE USER MEMBERSHIP BENEFITS - BENEFIT TYPE ENUM
    -- =====================================================
    -- Step 1: Add new benefit_type_new column with new enum values (if not exists)
    SELECT COUNT(*) INTO column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type_new';

    IF column_exists = 0 THEN
        ALTER TABLE user_membership_benefits ADD COLUMN benefit_type_new ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'BOOST', 'AUTO_APPROVE', 'BADGE') NOT NULL DEFAULT 'BOOST' AFTER benefit_type;
    END IF;

    -- Step 2: Migrate existing data (only if old benefit_type column still has old values)
    SELECT COUNT(*) INTO old_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type'
        AND COLUMN_TYPE LIKE '%VIP_POSTS%';

    IF old_column_exists > 0 THEN
        UPDATE user_membership_benefits
        SET benefit_type_new = CASE
            WHEN benefit_type = 'VIP_POSTS' THEN 'POST_SILVER'
            WHEN benefit_type = 'PREMIUM_POSTS' THEN 'POST_DIAMOND'
            WHEN benefit_type = 'BOOST_QUOTA' THEN 'BOOST'
            WHEN benefit_type = 'AUTO_VERIFY' THEN 'AUTO_APPROVE'
            WHEN benefit_type = 'TRUSTED_BADGE' THEN 'BADGE'
            ELSE 'BOOST'
        END;
    END IF;

    -- Step 3: Drop old column if it still exists with old enum
    SELECT COUNT(*) INTO old_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type'
        AND COLUMN_TYPE LIKE '%VIP_POSTS%';

    IF old_column_exists > 0 THEN
        ALTER TABLE user_membership_benefits DROP COLUMN benefit_type;
    END IF;

    -- Check if both benefit_type and benefit_type_new exist (shouldn't happen, but handle it)
    SELECT COUNT(*) INTO benefit_type_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type';

    SELECT COUNT(*) INTO new_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type_new';

    -- If both exist, drop benefit_type first (it's the old one)
    IF benefit_type_exists > 0 AND new_column_exists > 0 THEN
        ALTER TABLE user_membership_benefits DROP COLUMN benefit_type;
    END IF;

    -- Now rename benefit_type_new to benefit_type if benefit_type_new exists
    SELECT COUNT(*) INTO new_column_exists FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type_new';

    IF new_column_exists > 0 THEN
        ALTER TABLE user_membership_benefits CHANGE COLUMN benefit_type_new benefit_type ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'BOOST', 'AUTO_APPROVE', 'BADGE') NOT NULL;
    END IF;

    -- =====================================================
    -- 4. UPDATE MEMBERSHIP PACKAGE BENEFITS DATA
    -- =====================================================
    -- Update benefit display names to reflect new tier system
    UPDATE membership_package_benefits
    SET benefit_name_display = CASE
        WHEN benefit_type = 'POST_SILVER' AND membership_id = 1 THEN '5 tin VIP Bạc miễn phí'
        WHEN benefit_type = 'POST_SILVER' AND membership_id = 2 THEN '10 tin VIP Bạc miễn phí'
        WHEN benefit_type = 'POST_SILVER' AND membership_id = 3 THEN '15 tin VIP Bạc miễn phí'
        WHEN benefit_type = 'POST_GOLD' AND membership_id = 2 THEN '5 tin VIP Vàng miễn phí'
        WHEN benefit_type = 'POST_GOLD' AND membership_id = 3 THEN '10 tin VIP Vàng miễn phí'
        WHEN benefit_type = 'POST_DIAMOND' AND membership_id = 2 THEN '2 tin VIP Kim Cương miễn phí'
        WHEN benefit_type = 'POST_DIAMOND' AND membership_id = 3 THEN '5 tin VIP Kim Cương miễn phí'
        ELSE benefit_name_display
    END;

    -- =====================================================
    -- 5. ADD NEW GOLD TIER BENEFITS TO STANDARD PACKAGE
    -- =====================================================
    -- Add POST_GOLD benefit to STANDARD package (membership_id = 2)
    -- First, check if it doesn't already exist
    INSERT INTO membership_package_benefits (membership_id, benefit_type, benefit_name_display, quantity_per_month)
    SELECT 2, 'POST_GOLD', '5 tin VIP Vàng miễn phí', 5
    WHERE NOT EXISTS (
        SELECT 1 FROM membership_package_benefits
        WHERE membership_id = 2 AND benefit_type = 'POST_GOLD'
    );

    -- Add POST_GOLD benefit to ADVANCED package (membership_id = 3)
    INSERT INTO membership_package_benefits (membership_id, benefit_type, benefit_name_display, quantity_per_month)
    SELECT 3, 'POST_GOLD', '10 tin VIP Vàng miễn phí', 10
    WHERE NOT EXISTS (
        SELECT 1 FROM membership_package_benefits
        WHERE membership_id = 3 AND benefit_type = 'POST_GOLD'
    );

    -- =====================================================
    -- 6. UPDATE EXISTING BENEFITS QUANTITIES TO MATCH NEW BUSINESS LOGIC
    -- =====================================================
    -- Update STANDARD package (membership_id = 2)
    UPDATE membership_package_benefits
    SET quantity_per_month = CASE
        WHEN benefit_type = 'POST_SILVER' THEN 10
        WHEN benefit_type = 'POST_GOLD' THEN 5
        WHEN benefit_type = 'POST_DIAMOND' THEN 2
        WHEN benefit_type = 'BOOST' THEN 20
        ELSE quantity_per_month
    END
    WHERE membership_id = 2;

    -- Update ADVANCED package (membership_id = 3)
    UPDATE membership_package_benefits
    SET quantity_per_month = CASE
        WHEN benefit_type = 'POST_SILVER' THEN 15
        WHEN benefit_type = 'POST_GOLD' THEN 10
        WHEN benefit_type = 'POST_DIAMOND' THEN 5
        WHEN benefit_type = 'BOOST' THEN 40
        ELSE quantity_per_month
    END
    WHERE membership_id = 3;

    -- =====================================================
    -- 7. RECREATE INDEXES FOR OPTIMIZED QUERIES
    -- =====================================================
    -- Drop and recreate index on benefit_type in membership_package_benefits (if exists)
    SELECT COUNT(*) INTO index_exists FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND INDEX_NAME = 'idx_benefit_type';

    IF index_exists > 0 THEN
        ALTER TABLE membership_package_benefits DROP INDEX idx_benefit_type;
    END IF;

    -- Create index if it doesn't exist
    SELECT COUNT(*) INTO index_exists FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND INDEX_NAME = 'idx_benefit_type';

    IF index_exists = 0 THEN
        CREATE INDEX idx_benefit_type ON membership_package_benefits (benefit_type);
    END IF;

    -- Drop and recreate index on benefit_type in user_membership_benefits (if exists)
    SELECT COUNT(*) INTO index_exists FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND INDEX_NAME = 'idx_benefit_type';

    IF index_exists > 0 THEN
        ALTER TABLE user_membership_benefits DROP INDEX idx_benefit_type;
    END IF;

    -- Create index if it doesn't exist
    SELECT COUNT(*) INTO index_exists FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND INDEX_NAME = 'idx_benefit_type';

    IF index_exists = 0 THEN
        CREATE INDEX idx_benefit_type ON user_membership_benefits (benefit_type);
    END IF;

    -- Drop and recreate composite index (if exists)
    SELECT COUNT(*) INTO index_exists FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND INDEX_NAME = 'idx_user_benefit_status';

    IF index_exists > 0 THEN
        ALTER TABLE user_membership_benefits DROP INDEX idx_user_benefit_status;
    END IF;

    -- Create composite index if it doesn't exist
    SELECT COUNT(*) INTO index_exists FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND INDEX_NAME = 'idx_user_benefit_status';

    IF index_exists = 0 THEN
        CREATE INDEX idx_user_benefit_status ON user_membership_benefits (user_id, benefit_type, status);
    END IF;

END$$

DELIMITER ;

-- Execute the migration
CALL migrate_vip_tiers();

-- Drop the procedure after execution
DROP PROCEDURE IF EXISTS migrate_vip_tiers;

-- =====================================================
-- END OF MIGRATION V15
-- =====================================================

