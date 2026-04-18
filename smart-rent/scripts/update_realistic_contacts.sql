-- ============================================================================
-- Script: update_realistic_contacts.sql
-- Purpose: Make test user emails & phone numbers look realistic
--          - Email: givenname.familyname[YY]@gmail.com (Vietnamese diacritics stripped)
--          - Phone: random Vietnamese mobile numbers with varied prefixes
-- Usage:   mysql -u <user> -p smartrent < update_realistic_contacts.sql
-- Prerequisites: populate_listings.sql must have been run first (50 test users)
-- ============================================================================

USE smartrent;

-- ============================================================================
-- Step 1: Helper function — strip Vietnamese diacritics to ASCII
-- ============================================================================
DROP FUNCTION IF EXISTS strip_vn_diacritics;

DELIMITER //
CREATE FUNCTION strip_vn_diacritics(input_str VARCHAR(255))
RETURNS VARCHAR(255) DETERMINISTIC
BEGIN
    DECLARE r VARCHAR(255);
    SET r = LOWER(input_str);
    SET r = REPLACE(r, ' ', '');

    -- a variants (a, ă, â)
    SET r = REPLACE(r,'à','a'); SET r = REPLACE(r,'á','a'); SET r = REPLACE(r,'ả','a');
    SET r = REPLACE(r,'ã','a'); SET r = REPLACE(r,'ạ','a');
    SET r = REPLACE(r,'ă','a'); SET r = REPLACE(r,'ằ','a'); SET r = REPLACE(r,'ắ','a');
    SET r = REPLACE(r,'ẳ','a'); SET r = REPLACE(r,'ẵ','a'); SET r = REPLACE(r,'ặ','a');
    SET r = REPLACE(r,'â','a'); SET r = REPLACE(r,'ầ','a'); SET r = REPLACE(r,'ấ','a');
    SET r = REPLACE(r,'ẩ','a'); SET r = REPLACE(r,'ẫ','a'); SET r = REPLACE(r,'ậ','a');
    -- e variants (e, ê)
    SET r = REPLACE(r,'è','e'); SET r = REPLACE(r,'é','e'); SET r = REPLACE(r,'ẻ','e');
    SET r = REPLACE(r,'ẽ','e'); SET r = REPLACE(r,'ẹ','e');
    SET r = REPLACE(r,'ê','e'); SET r = REPLACE(r,'ề','e'); SET r = REPLACE(r,'ế','e');
    SET r = REPLACE(r,'ể','e'); SET r = REPLACE(r,'ễ','e'); SET r = REPLACE(r,'ệ','e');
    -- i variants
    SET r = REPLACE(r,'ì','i'); SET r = REPLACE(r,'í','i'); SET r = REPLACE(r,'ỉ','i');
    SET r = REPLACE(r,'ĩ','i'); SET r = REPLACE(r,'ị','i');
    -- o variants (o, ô, ơ)
    SET r = REPLACE(r,'ò','o'); SET r = REPLACE(r,'ó','o'); SET r = REPLACE(r,'ỏ','o');
    SET r = REPLACE(r,'õ','o'); SET r = REPLACE(r,'ọ','o');
    SET r = REPLACE(r,'ô','o'); SET r = REPLACE(r,'ồ','o'); SET r = REPLACE(r,'ố','o');
    SET r = REPLACE(r,'ổ','o'); SET r = REPLACE(r,'ỗ','o'); SET r = REPLACE(r,'ộ','o');
    SET r = REPLACE(r,'ơ','o'); SET r = REPLACE(r,'ờ','o'); SET r = REPLACE(r,'ớ','o');
    SET r = REPLACE(r,'ở','o'); SET r = REPLACE(r,'ỡ','o'); SET r = REPLACE(r,'ợ','o');
    -- u variants (u, ư)
    SET r = REPLACE(r,'ù','u'); SET r = REPLACE(r,'ú','u'); SET r = REPLACE(r,'ủ','u');
    SET r = REPLACE(r,'ũ','u'); SET r = REPLACE(r,'ụ','u');
    SET r = REPLACE(r,'ư','u'); SET r = REPLACE(r,'ừ','u'); SET r = REPLACE(r,'ứ','u');
    SET r = REPLACE(r,'ử','u'); SET r = REPLACE(r,'ữ','u'); SET r = REPLACE(r,'ự','u');
    -- y variants
    SET r = REPLACE(r,'ỳ','y'); SET r = REPLACE(r,'ý','y'); SET r = REPLACE(r,'ỷ','y');
    SET r = REPLACE(r,'ỹ','y'); SET r = REPLACE(r,'ỵ','y');
    -- đ
    SET r = REPLACE(r,'đ','d');

    RETURN r;
END //
DELIMITER ;

-- ============================================================================
-- Step 2: Update emails — format: givenname.familyname[YY]@gmail.com
--         ~70% of users get a 2-digit birth-year suffix (very common in VN)
-- ============================================================================
UPDATE users
SET email = CONCAT(
    strip_vn_diacritics(last_name),       -- given/middle name (e.g. "Văn An" → "vanan")
    '.',
    strip_vn_diacritics(first_name),      -- family name (e.g. "Nguyễn" → "nguyen")
    CASE WHEN (CRC32(CONCAT(user_id, 'yf')) % 10) < 7
         THEN LPAD(((CRC32(CONCAT(user_id, 'yr')) % 18) + 85) % 100, 2, '0')
         ELSE ''
    END,
    '@gmail.com'
)
WHERE user_id LIKE '00000000-test-%';

SELECT CONCAT('Updated ', ROW_COUNT(), ' emails') AS step_2;

-- ============================================================================
-- Step 3: Update phone numbers — realistic Vietnamese mobile prefixes
--         Uses CRC32 for deterministic (same result each run) but varied output
-- ============================================================================
UPDATE users
SET phone_number = CONCAT(
    ELT(
        (CRC32(CONCAT(user_id, 'px')) % 15) + 1,
        '0903','0912','0356','0978','0868',
        '0936','0789','0918','0376','0988',
        '0852','0384','0708','0367','0932'
    ),
    LPAD(CRC32(CONCAT(user_id, 'pn')) % 1000000, 6, '0')
)
WHERE user_id LIKE '00000000-test-%';

SELECT CONCAT('Updated ', ROW_COUNT(), ' phone numbers') AS step_3;

-- ============================================================================
-- Step 4: Sync contact_phone_number where it was previously set (not NULL)
-- ============================================================================
UPDATE users
SET contact_phone_number = phone_number
WHERE user_id LIKE '00000000-test-%'
  AND contact_phone_number IS NOT NULL;

SELECT CONCAT('Synced ', ROW_COUNT(), ' contact phones') AS step_4;

-- ============================================================================
-- Step 5: Cleanup helper function
-- ============================================================================
DROP FUNCTION IF EXISTS strip_vn_diacritics;

-- ============================================================================
-- Step 6: Verify results
-- ============================================================================
SELECT
    SUBSTRING(user_id, 16, 4)             AS user_num,
    CONCAT(first_name, ' ', last_name)    AS full_name,
    email,
    phone_number,
    contact_phone_number
FROM users
WHERE user_id LIKE '00000000-test-%'
ORDER BY user_id
LIMIT 50;