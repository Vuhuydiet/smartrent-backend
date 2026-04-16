-- =====================================================
-- SMARTRENT MEMBERSHIP SYSTEM
-- Version: V71
-- Description: Remove AUTO_APPROVE (Duyệt tin ngay lập tức) and BADGE
--              (Badge đối tác tin cậy / Đối tác đáng tin cậy) benefit types
--              as these features are no longer supported.
-- =====================================================

-- =====================================================
-- 1. DELETE BENEFIT ROWS FROM USER_MEMBERSHIP_BENEFITS
--    (must happen before removing the enum values)
-- =====================================================
DELETE FROM user_membership_benefits
WHERE benefit_type IN ('AUTO_APPROVE', 'BADGE');

-- =====================================================
-- 2. DELETE BENEFIT ROWS FROM MEMBERSHIP_PACKAGE_BENEFITS
-- =====================================================
DELETE FROM membership_package_benefits
WHERE benefit_type IN ('AUTO_APPROVE', 'BADGE');

-- =====================================================
-- 3. NARROW THE ENUM ON membership_package_benefits
--    Old: ENUM('POST_SILVER','POST_GOLD','POST_DIAMOND','PUSH','AUTO_APPROVE','BADGE')
--    New: ENUM('POST_SILVER','POST_GOLD','POST_DIAMOND','PUSH')
-- =====================================================
ALTER TABLE membership_package_benefits
    MODIFY COLUMN benefit_type
        ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'PUSH') NOT NULL;

-- =====================================================
-- 4. NARROW THE ENUM ON user_membership_benefits
--    Old: ENUM('POST_SILVER','POST_GOLD','POST_DIAMOND','PUSH','AUTO_APPROVE','BADGE')
--    New: ENUM('POST_SILVER','POST_GOLD','POST_DIAMOND','PUSH')
-- =====================================================
ALTER TABLE user_membership_benefits
    MODIFY COLUMN benefit_type
        ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'PUSH') NOT NULL;
