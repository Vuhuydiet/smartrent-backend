-- =====================================================
-- SMARTRENT RENAME BOOST TO PUSH
-- Version: V17
-- Description: Rename all "BOOST" references to "PUSH" in database
--              Update enum values in transactions, membership_package_benefits, user_membership_benefits
-- =====================================================

-- =====================================================
-- 1. UPDATE TRANSACTIONS TABLE - TRANSACTION_TYPE ENUM
-- =====================================================
-- First, add PUSH_FEE to the enum (keeping BOOST_FEE temporarily)
ALTER TABLE transactions
MODIFY COLUMN transaction_type ENUM('MEMBERSHIP_PURCHASE', 'POST_FEE', 'BOOST_FEE', 'PUSH_FEE', 'WALLET_TOPUP', 'REFUND') NOT NULL;

-- Update existing data from BOOST_FEE to PUSH_FEE
UPDATE transactions
SET transaction_type = 'PUSH_FEE'
WHERE transaction_type = 'BOOST_FEE';

-- Remove BOOST_FEE from enum
ALTER TABLE transactions
MODIFY COLUMN transaction_type ENUM('MEMBERSHIP_PURCHASE', 'POST_FEE', 'PUSH_FEE', 'WALLET_TOPUP', 'REFUND') NOT NULL;

-- =====================================================
-- 2. UPDATE TRANSACTIONS TABLE - REFERENCE_TYPE ENUM
-- =====================================================
-- First, add PUSH to the enum (keeping BOOST temporarily)
ALTER TABLE transactions
MODIFY COLUMN reference_type ENUM('MEMBERSHIP', 'LISTING', 'BOOST', 'PUSH', 'WALLET') NOT NULL;

-- Update existing data from BOOST to PUSH
UPDATE transactions
SET reference_type = 'PUSH'
WHERE reference_type = 'BOOST';

-- Remove BOOST from enum
ALTER TABLE transactions
MODIFY COLUMN reference_type ENUM('MEMBERSHIP', 'LISTING', 'PUSH', 'WALLET') NOT NULL;

-- =====================================================
-- 3. UPDATE MEMBERSHIP_PACKAGE_BENEFITS TABLE - BENEFIT_TYPE ENUM
-- =====================================================
-- First, add PUSH to the enum (keeping BOOST temporarily)
ALTER TABLE membership_package_benefits
MODIFY COLUMN benefit_type ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'BOOST', 'PUSH', 'AUTO_APPROVE', 'BADGE') NOT NULL;

-- Update existing data from BOOST to PUSH
UPDATE membership_package_benefits
SET benefit_type = 'PUSH'
WHERE benefit_type = 'BOOST';

-- Remove BOOST from enum
ALTER TABLE membership_package_benefits
MODIFY COLUMN benefit_type ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'PUSH', 'AUTO_APPROVE', 'BADGE') NOT NULL;

-- =====================================================
-- 4. UPDATE USER_MEMBERSHIP_BENEFITS TABLE - BENEFIT_TYPE ENUM
-- =====================================================
-- First, add PUSH to the enum (keeping BOOST temporarily)
ALTER TABLE user_membership_benefits
MODIFY COLUMN benefit_type ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'BOOST', 'PUSH', 'AUTO_APPROVE', 'BADGE') NOT NULL;

-- Update existing data from BOOST to PUSH
UPDATE user_membership_benefits
SET benefit_type = 'PUSH'
WHERE benefit_type = 'BOOST';

-- Remove BOOST from enum
ALTER TABLE user_membership_benefits
MODIFY COLUMN benefit_type ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'PUSH', 'AUTO_APPROVE', 'BADGE') NOT NULL;

-- =====================================================
-- 5. ADD COMMENTS FOR DOCUMENTATION
-- =====================================================
ALTER TABLE transactions
MODIFY COLUMN transaction_type ENUM('MEMBERSHIP_PURCHASE', 'POST_FEE', 'PUSH_FEE', 'WALLET_TOPUP', 'REFUND') NOT NULL
COMMENT 'MEMBERSHIP_PURCHASE: Buy membership package, POST_FEE: Pay-per-post, PUSH_FEE: Pay-per-push';

ALTER TABLE transactions
MODIFY COLUMN reference_type ENUM('MEMBERSHIP', 'LISTING', 'PUSH', 'WALLET') NOT NULL
COMMENT 'MEMBERSHIP: Membership package, LISTING: Listing post, PUSH: Listing push, WALLET: Wallet transaction';

-- =====================================================
-- END OF MIGRATION V17
-- =====================================================

