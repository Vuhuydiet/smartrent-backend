-- Add avatar_url column to users table
-- This column stores the URL of the user's profile picture
-- For Google OAuth users, this will be populated from their Google profile picture

ALTER TABLE users ADD COLUMN avatar_url VARCHAR(1024) COMMENT 'URL of the user profile picture (e.g., from Google OAuth or uploaded)';

