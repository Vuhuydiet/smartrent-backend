-- Add user profile fields
-- This migration adds additional profile fields to the users table

ALTER TABLE users 
ADD COLUMN email VARCHAR(255),
ADD COLUMN first_name VARCHAR(100),
ADD COLUMN last_name VARCHAR(100),
ADD COLUMN phone_number VARCHAR(20),
ADD COLUMN is_active BOOLEAN DEFAULT TRUE,
ADD COLUMN last_login_at TIMESTAMP NULL;

-- Add unique constraint on email
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

-- Add index on email for faster lookups
CREATE INDEX idx_users_email ON users (email);

-- Add index on is_active for filtering active users
CREATE INDEX idx_users_is_active ON users (is_active);
