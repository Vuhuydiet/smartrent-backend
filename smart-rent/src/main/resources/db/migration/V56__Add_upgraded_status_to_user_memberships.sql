-- Migration V53: Add UPGRADED status to user_memberships table
-- This migration adds the UPGRADED status to track memberships that have been upgraded to a higher tier

-- =====================================================
-- 1. ADD UPGRADED TO USER_MEMBERSHIPS STATUS ENUM
-- =====================================================
ALTER TABLE user_memberships
MODIFY COLUMN status ENUM('ACTIVE', 'EXPIRED', 'CANCELLED', 'UPGRADED') NOT NULL DEFAULT 'ACTIVE'
COMMENT 'ACTIVE: Currently active, EXPIRED: Membership ended, CANCELLED: User cancelled, UPGRADED: Replaced by upgrade to higher tier';

