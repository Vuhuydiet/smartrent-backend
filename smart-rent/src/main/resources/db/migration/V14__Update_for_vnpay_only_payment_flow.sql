-- =====================================================
-- SMARTRENT VNPAY-ONLY PAYMENT FLOW UPDATE
-- Version: V14
-- Description: Update schema to support VNPay-only payment flow
--              Remove wallet functionality, add pay-per-post support
-- =====================================================

-- =====================================================
-- 1. UPDATE LISTINGS TABLE - Add pay-per-post support
-- =====================================================

-- Add post_source column to track how listing was created
ALTER TABLE listings
ADD COLUMN post_source ENUM('QUOTA', 'DIRECT_PAYMENT') DEFAULT 'QUOTA' AFTER vip_type;

-- Add transaction_id to link pay-per-post listings to transactions
ALTER TABLE listings
ADD COLUMN transaction_id VARCHAR(36) AFTER post_source;

-- Add foreign key constraint
ALTER TABLE listings
ADD CONSTRAINT fk_listing_transaction
FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE SET NULL;

-- Add index for transaction lookups
ALTER TABLE listings
ADD INDEX idx_transaction_id (transaction_id);

-- Add index for post_source filtering
ALTER TABLE listings
ADD INDEX idx_post_source (post_source);

-- =====================================================
-- 2. UPDATE TRANSACTIONS TABLE - Update enum values
-- =====================================================

-- Note: balance_before and balance_after were never created, so no need to drop them

-- Update transaction_type enum to remove WALLET_TOPUP (if it exists)
ALTER TABLE transactions
MODIFY COLUMN transaction_type ENUM('MEMBERSHIP_PURCHASE', 'POST_FEE', 'BOOST_FEE', 'REFUND') NOT NULL;

-- Make payment_provider required (all payments via VNPay)
ALTER TABLE transactions
MODIFY COLUMN payment_provider ENUM('VNPAY', 'MOMO', 'BANK_TRANSFER', 'CASH') NOT NULL DEFAULT 'VNPAY';

-- =====================================================
-- 3. UPDATE PUSH_HISTORY TABLE - Update enum values
-- =====================================================

-- Note: transaction_id was already created in V13, so no need to add it again

-- Update push_source enum to use DIRECT_PAYMENT instead of DIRECT_PURCHASE
ALTER TABLE push_history
MODIFY COLUMN push_source ENUM('MEMBERSHIP_QUOTA', 'DIRECT_PAYMENT', 'SCHEDULED', 'ADMIN') NOT NULL;

-- =====================================================
-- 4. ADD COMMENTS FOR DOCUMENTATION
-- =====================================================

ALTER TABLE listings
MODIFY COLUMN post_source ENUM('QUOTA', 'DIRECT_PAYMENT') DEFAULT 'QUOTA'
COMMENT 'QUOTA: Created using membership quota, DIRECT_PAYMENT: Paid per-post via VNPay';

ALTER TABLE listings
MODIFY COLUMN transaction_id VARCHAR(36)
COMMENT 'Transaction ID if listing was created via DIRECT_PAYMENT';

ALTER TABLE transactions
MODIFY COLUMN transaction_type ENUM('MEMBERSHIP_PURCHASE', 'POST_FEE', 'BOOST_FEE', 'REFUND') NOT NULL
COMMENT 'MEMBERSHIP_PURCHASE: Buy membership package, POST_FEE: Pay-per-post, BOOST_FEE: Pay-per-boost';

-- =====================================================
-- 5. DATA MIGRATION - Set default values for existing data
-- =====================================================

-- Set post_source to QUOTA for all existing listings
UPDATE listings
SET post_source = 'QUOTA'
WHERE post_source IS NULL;

-- Update existing push_history records to use new enum value
UPDATE push_history
SET push_source = 'DIRECT_PAYMENT'
WHERE push_source = 'DIRECT_PURCHASE';

