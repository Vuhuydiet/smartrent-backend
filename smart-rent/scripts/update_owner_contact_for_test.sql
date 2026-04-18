-- ============================================================
-- Script: Update owner contact info for testing AI chatbot
-- Purpose: Ensure listing owners have contactName & contactPhone
-- ============================================================

-- 1. Check current state: listings whose owners have no contact info
SELECT
    l.listing_id,
    l.title,
    u.user_id,
    u.last_name,
    u.first_name,
    u.contact_phone_number,
    u.contact_phone_verified
FROM listings l
JOIN users u ON u.user_id = l.user_id
WHERE u.contact_phone_number IS NULL
   OR u.first_name = ''
   OR u.last_name = ''
LIMIT 20;

-- 2. Update ALL listing owners who are missing contact phone
UPDATE users u
JOIN listings l ON l.user_id = u.user_id
SET
    u.contact_phone_number = COALESCE(NULLIF(u.contact_phone_number, ''), u.phone_number),
    u.contact_phone_verified = COALESCE(u.contact_phone_verified, FALSE)
WHERE u.contact_phone_number IS NULL OR u.contact_phone_number = '';

-- 3. Update specific test users with sample data (pick listing IDs you want to test)
-- Replace <USER_ID> with actual user_id from step 1

-- UPDATE users
-- SET
--     last_name = 'Nguyễn Văn',
--     first_name = 'A',
--     contact_phone_number = '0901234567',
--     contact_phone_verified = TRUE
-- WHERE user_id = '<USER_ID>';

-- 4. Quick sample: update the owner of a specific listing
-- Replace <LISTING_ID> with the listing you're testing in AI

-- UPDATE users u
-- JOIN listings l ON l.user_id = u.user_id
-- SET
--     u.last_name = 'Trần Thị',
--     u.first_name = 'B',
--     u.contact_phone_number = '0912345678',
--     u.contact_phone_verified = TRUE
-- WHERE l.listing_id = <LISTING_ID>;

-- 5. Verify after update
SELECT
    l.listing_id,
    l.title,
    u.user_id,
    CONCAT(u.last_name, ' ', u.first_name) AS contact_name,
    u.contact_phone_number AS contact_phone,
    u.contact_phone_verified
FROM listings l
JOIN users u ON u.user_id = l.user_id
ORDER BY l.listing_id DESC
LIMIT 20;
