-- Index the owner "chủ tin" phone/email lookup used by the admin content/posts
-- filter (POST /v1/listings/admin/list -> ownerSearch, ListingSpecification).
--
-- ownerSearch now matches firstName / lastName / contactPhoneNumber /
-- phoneNumber / email (email added alongside this migration). email already
-- has idx_users_email (V1) and the legacy phone_number column already has
-- idx_users_phone_number (V107). contact_phone_number — the field admins
-- actually search on for a user's current contact number — had no index.
-- Uses the conditional pattern (check INFORMATION_SCHEMA first) since MySQL
-- CREATE INDEX has no IF NOT EXISTS, matching V107's style.

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_contact_phone_number');
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_users_contact_phone_number ON users (contact_phone_number)',
    'SELECT ''Index idx_users_contact_phone_number already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
