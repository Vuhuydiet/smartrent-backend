-- Create users table
-- This migration creates the initial users table for user authentication

CREATE TABLE users (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Add unique constraint on username to prevent duplicates
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);

-- Add index on username for faster lookups
CREATE INDEX idx_users_username ON users (username);
