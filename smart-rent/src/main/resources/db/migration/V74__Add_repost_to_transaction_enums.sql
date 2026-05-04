-- Migration V74: Add REPOST_FEE / REPOST to transaction enums
-- Supports the new "đăng lại" (repost) feature — re-publishing an
-- expired listing via membership quota or direct VNPay/ZaloPay payment.

-- =====================================================
-- 1. ADD REPOST_FEE TO TRANSACTION_TYPE ENUM
-- =====================================================
ALTER TABLE transactions
MODIFY COLUMN transaction_type ENUM('MEMBERSHIP_PURCHASE', 'MEMBERSHIP_UPGRADE', 'POST_FEE', 'PUSH_FEE', 'REPOST_FEE', 'WALLET_TOPUP', 'REFUND') NOT NULL
COMMENT 'MEMBERSHIP_PURCHASE: Buy membership package, MEMBERSHIP_UPGRADE: Upgrade membership, POST_FEE: Pay-per-post, PUSH_FEE: Pay-per-push, REPOST_FEE: Pay-per-repost (re-publish expired listing)';

-- =====================================================
-- 2. ADD REPOST TO REFERENCE_TYPE ENUM
-- =====================================================
ALTER TABLE transactions
MODIFY COLUMN reference_type ENUM('MEMBERSHIP', 'LISTING', 'PUSH', 'REPOST', 'WALLET') NOT NULL
COMMENT 'Reference type for the transaction. REPOST: links the transaction to the listing being re-published.';
