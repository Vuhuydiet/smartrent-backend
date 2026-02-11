-- =====================================================
-- SMARTRENT PAYMENT METADATA UPDATE
-- Version: V16
-- Description: Add order_info and ip_address to transactions table
--              for VNPay payment tracking and query support
-- =====================================================

-- =====================================================
-- 1. ADD PAYMENT METADATA COLUMNS TO TRANSACTIONS TABLE
-- =====================================================

-- Add order_info column to store payment description
ALTER TABLE transactions
ADD COLUMN order_info VARCHAR(500) COMMENT 'Payment order description/info for payment gateway' AFTER additional_info;

-- Add ip_address column to store client IP for payment gateway
ALTER TABLE transactions
ADD COLUMN ip_address VARCHAR(45) COMMENT 'Client IP address when creating payment transaction' AFTER order_info;

-- Add index for order_info searches
ALTER TABLE transactions
ADD INDEX idx_order_info (order_info(255));

-- =====================================================
-- END OF MIGRATION V16
-- =====================================================
