-- Add zalo_phone_number column to users table
ALTER TABLE users
ADD COLUMN zalo_phone_number VARCHAR(10) NULL;

-- Add comment to the column
ALTER TABLE users
MODIFY COLUMN zalo_phone_number VARCHAR(10) NULL COMMENT 'Vietnam Zalo phone number for contact (format: 09xxxxxxxx, 03xxxxxxxx, 07xxxxxxxx, 08xxxxxxxx, 05xxxxxxxx)';

