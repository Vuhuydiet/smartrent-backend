-- Migration V52: Add membership upgrade tracking columns
-- This migration adds columns to track membership upgrades

-- =====================================================
-- 1. ADD MEMBERSHIP_UPGRADE TO TRANSACTION_TYPE ENUM
-- =====================================================
ALTER TABLE transactions
MODIFY COLUMN transaction_type ENUM('MEMBERSHIP_PURCHASE', 'MEMBERSHIP_UPGRADE', 'POST_FEE', 'PUSH_FEE', 'WALLET_TOPUP', 'REFUND') NOT NULL
COMMENT 'MEMBERSHIP_PURCHASE: Buy membership package, MEMBERSHIP_UPGRADE: Upgrade membership, POST_FEE: Pay-per-post, PUSH_FEE: Pay-per-push';

-- =====================================================
-- 2. ADD UPGRADE TRACKING COLUMNS TO USER_MEMBERSHIPS
-- =====================================================
-- Add upgraded_from_membership_id column to user_memberships table
-- This tracks which membership was upgraded from (for upgrade history)
ALTER TABLE user_memberships
ADD COLUMN upgraded_from_membership_id BIGINT NULL;

-- Add foreign key constraint (self-referencing)
ALTER TABLE user_memberships
ADD CONSTRAINT fk_upgraded_from_membership
FOREIGN KEY (upgraded_from_membership_id)
REFERENCES user_memberships(user_membership_id)
ON DELETE SET NULL;

-- Add index for faster lookups
CREATE INDEX idx_upgraded_from_membership ON user_memberships(upgraded_from_membership_id);

-- =====================================================
-- 3. ADD UPGRADE TRACKING COLUMNS TO TRANSACTIONS
-- =====================================================
-- Add previous_membership_id column to transactions table
-- This stores the membership being upgraded from in upgrade transactions
ALTER TABLE transactions
ADD COLUMN previous_membership_id BIGINT NULL;

-- Add discount_amount column to transactions table
-- This stores the discount amount applied during upgrade
ALTER TABLE transactions
ADD COLUMN discount_amount DECIMAL(15, 0) NULL;

-- Add index for upgrade transaction lookups
CREATE INDEX idx_transactions_previous_membership ON transactions(previous_membership_id);

