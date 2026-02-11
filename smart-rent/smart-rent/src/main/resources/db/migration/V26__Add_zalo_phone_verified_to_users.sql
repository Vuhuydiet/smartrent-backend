-- Add zalo_phone_verified column to users table
ALTER TABLE users
ADD COLUMN zalo_phone_verified BOOLEAN DEFAULT FALSE COMMENT 'Whether the Zalo phone number has been verified';

-- Set existing users with zalo_phone_number to unverified (false)
UPDATE users
SET zalo_phone_verified = FALSE
WHERE zalo_phone_number IS NOT NULL;

