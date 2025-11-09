-- Rename zalo_phone_number to contact_phone_number
ALTER TABLE users
CHANGE COLUMN zalo_phone_number contact_phone_number VARCHAR(10) COMMENT 'Contact phone number for Zalo or other messaging apps';

-- Rename zalo_phone_verified to contact_phone_verified
ALTER TABLE users
CHANGE COLUMN zalo_phone_verified contact_phone_verified BOOLEAN DEFAULT FALSE COMMENT 'Whether the contact phone number has been verified';

