-- =====================================================
-- SMARTRENT VIP TIER SYSTEM UPDATE
-- Version: V15
-- Description: Update VIP tier system from NORMAL/VIP/PREMIUM to NORMAL/SILVER/GOLD/DIAMOND
--              Update BenefitType from VIP_POSTS/PREMIUM_POSTS to POST_SILVER/POST_GOLD/POST_DIAMOND
-- =====================================================

-- =====================================================
-- 1. UPDATE LISTINGS TABLE - VIP TYPE ENUM
-- =====================================================
-- Step 1: Add new vip_type_new column with new enum values (if not exists)
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type_new');

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE listings ADD COLUMN vip_type_new ENUM(''NORMAL'', ''SILVER'', ''GOLD'', ''DIAMOND'') NOT NULL DEFAULT ''NORMAL'' AFTER vip_type',
    'SELECT "Column vip_type_new already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: Migrate existing data (only if old vip_type column still exists)
SET @old_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type'
    AND COLUMN_TYPE LIKE '%VIP%');

SET @sql = IF(@old_column_exists > 0,
    'UPDATE listings SET vip_type_new = CASE WHEN vip_type = ''NORMAL'' THEN ''NORMAL'' WHEN vip_type = ''VIP'' THEN ''SILVER'' WHEN vip_type = ''PREMIUM'' THEN ''DIAMOND'' ELSE ''NORMAL'' END',
    'SELECT "Data already migrated" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 3: Drop old column and rename new column (if old column still exists)
SET @old_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type'
    AND COLUMN_TYPE LIKE '%VIP%');

SET @sql = IF(@old_column_exists > 0,
    'ALTER TABLE listings DROP COLUMN vip_type',
    'SELECT "Old vip_type column already dropped" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check if both vip_type and vip_type_new exist (shouldn't happen, but handle it)
SET @vip_type_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type');

SET @new_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type_new');

-- If both exist, drop vip_type first (it's the old one)
SET @sql = IF(@vip_type_exists > 0 AND @new_column_exists > 0,
    'ALTER TABLE listings DROP COLUMN vip_type',
    'SELECT "No need to drop vip_type before rename" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Now rename vip_type_new to vip_type if vip_type_new exists
SET @new_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'listings' AND COLUMN_NAME = 'vip_type_new');

SET @sql = IF(@new_column_exists > 0,
    'ALTER TABLE listings CHANGE COLUMN vip_type_new vip_type ENUM(''NORMAL'', ''SILVER'', ''GOLD'', ''DIAMOND'') NOT NULL DEFAULT ''NORMAL''',
    'SELECT "Column already renamed" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 2. UPDATE MEMBERSHIP PACKAGE BENEFITS - BENEFIT TYPE ENUM
-- =====================================================
-- Step 1: Add new benefit_type_new column with new enum values (if not exists)
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type_new');

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE membership_package_benefits ADD COLUMN benefit_type_new ENUM(''POST_SILVER'', ''POST_GOLD'', ''POST_DIAMOND'', ''BOOST'', ''AUTO_APPROVE'', ''BADGE'') NOT NULL DEFAULT ''BOOST'' AFTER benefit_type',
    'SELECT "Column benefit_type_new already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: Migrate existing data (only if old benefit_type column still has old values)
SET @old_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type'
    AND COLUMN_TYPE LIKE '%VIP_POSTS%');

SET @sql = IF(@old_column_exists > 0,
    'UPDATE membership_package_benefits SET benefit_type_new = CASE WHEN benefit_type = ''VIP_POSTS'' THEN ''POST_SILVER'' WHEN benefit_type = ''PREMIUM_POSTS'' THEN ''POST_DIAMOND'' WHEN benefit_type = ''BOOST_QUOTA'' THEN ''BOOST'' WHEN benefit_type = ''AUTO_VERIFY'' THEN ''AUTO_APPROVE'' WHEN benefit_type = ''TRUSTED_BADGE'' THEN ''BADGE'' ELSE ''BOOST'' END',
    'SELECT "Data already migrated" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 3: Drop old column and rename new column (if old column still exists with old enum)
SET @old_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type'
    AND COLUMN_TYPE LIKE '%VIP_POSTS%');

SET @sql = IF(@old_column_exists > 0,
    'ALTER TABLE membership_package_benefits DROP COLUMN benefit_type',
    'SELECT "Old benefit_type column already dropped" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check if both benefit_type and benefit_type_new exist (shouldn't happen, but handle it)
SET @benefit_type_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type');

SET @new_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type_new');

-- If both exist, drop benefit_type first (it's the old one)
SET @sql = IF(@benefit_type_exists > 0 AND @new_column_exists > 0,
    'ALTER TABLE membership_package_benefits DROP COLUMN benefit_type',
    'SELECT "No need to drop benefit_type before rename" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Now rename benefit_type_new to benefit_type if benefit_type_new exists
SET @new_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND COLUMN_NAME = 'benefit_type_new');

SET @sql = IF(@new_column_exists > 0,
    'ALTER TABLE membership_package_benefits CHANGE COLUMN benefit_type_new benefit_type ENUM(''POST_SILVER'', ''POST_GOLD'', ''POST_DIAMOND'', ''BOOST'', ''AUTO_APPROVE'', ''BADGE'') NOT NULL',
    'SELECT "Column already renamed" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 3. UPDATE USER MEMBERSHIP BENEFITS - BENEFIT TYPE ENUM
-- =====================================================
-- Step 1: Add new benefit_type_new column with new enum values (if not exists)
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type_new');

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE user_membership_benefits ADD COLUMN benefit_type_new ENUM(''POST_SILVER'', ''POST_GOLD'', ''POST_DIAMOND'', ''BOOST'', ''AUTO_APPROVE'', ''BADGE'') NOT NULL DEFAULT ''BOOST'' AFTER benefit_type',
    'SELECT "Column benefit_type_new already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: Migrate existing data (only if old benefit_type column still has old values)
SET @old_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type'
    AND COLUMN_TYPE LIKE '%VIP_POSTS%');

SET @sql = IF(@old_column_exists > 0,
    'UPDATE user_membership_benefits SET benefit_type_new = CASE WHEN benefit_type = ''VIP_POSTS'' THEN ''POST_SILVER'' WHEN benefit_type = ''PREMIUM_POSTS'' THEN ''POST_DIAMOND'' WHEN benefit_type = ''BOOST_QUOTA'' THEN ''BOOST'' WHEN benefit_type = ''AUTO_VERIFY'' THEN ''AUTO_APPROVE'' WHEN benefit_type = ''TRUSTED_BADGE'' THEN ''BADGE'' ELSE ''BOOST'' END',
    'SELECT "Data already migrated" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 3: Drop old column and rename new column (if old column still exists with old enum)
SET @old_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type'
    AND COLUMN_TYPE LIKE '%VIP_POSTS%');

SET @sql = IF(@old_column_exists > 0,
    'ALTER TABLE user_membership_benefits DROP COLUMN benefit_type',
    'SELECT "Old benefit_type column already dropped" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check if both benefit_type and benefit_type_new exist (shouldn't happen, but handle it)
SET @benefit_type_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type');

SET @new_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type_new');

-- If both exist, drop benefit_type first (it's the old one)
SET @sql = IF(@benefit_type_exists > 0 AND @new_column_exists > 0,
    'ALTER TABLE user_membership_benefits DROP COLUMN benefit_type',
    'SELECT "No need to drop benefit_type before rename" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Now rename benefit_type_new to benefit_type if benefit_type_new exists
SET @new_column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND COLUMN_NAME = 'benefit_type_new');

SET @sql = IF(@new_column_exists > 0,
    'ALTER TABLE user_membership_benefits CHANGE COLUMN benefit_type_new benefit_type ENUM(''POST_SILVER'', ''POST_GOLD'', ''POST_DIAMOND'', ''BOOST'', ''AUTO_APPROVE'', ''BADGE'') NOT NULL',
    'SELECT "Column already renamed" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND INDEX_NAME = 'idx_benefit_type');

SET @sql = IF(@index_exists > 0,
    'ALTER TABLE membership_package_benefits DROP INDEX idx_benefit_type',
    'SELECT "Index idx_benefit_type does not exist on membership_package_benefits" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create index if it doesn't exist
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_package_benefits' AND INDEX_NAME = 'idx_benefit_type');

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_benefit_type ON membership_package_benefits (benefit_type)',
    'SELECT "Index idx_benefit_type already exists on membership_package_benefits" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop and recreate index on benefit_type in user_membership_benefits (if exists)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND INDEX_NAME = 'idx_benefit_type');

SET @sql = IF(@index_exists > 0,
    'ALTER TABLE user_membership_benefits DROP INDEX idx_benefit_type',
    'SELECT "Index idx_benefit_type does not exist on user_membership_benefits" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create index if it doesn't exist
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND INDEX_NAME = 'idx_benefit_type');

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_benefit_type ON user_membership_benefits (benefit_type)',
    'SELECT "Index idx_benefit_type already exists on user_membership_benefits" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop and recreate composite index (if exists)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND INDEX_NAME = 'idx_user_benefit_status');

SET @sql = IF(@index_exists > 0,
    'ALTER TABLE user_membership_benefits DROP INDEX idx_user_benefit_status',
    'SELECT "Index idx_user_benefit_status does not exist" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create composite index if it doesn't exist
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_membership_benefits' AND INDEX_NAME = 'idx_user_benefit_status');

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_user_benefit_status ON user_membership_benefits (user_id, benefit_type, status)',
    'SELECT "Index idx_user_benefit_status already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- END OF MIGRATION V15
-- =====================================================

