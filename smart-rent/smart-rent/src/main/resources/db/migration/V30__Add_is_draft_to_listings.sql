-- Add is_draft column to listings table to support draft listings
-- Draft listings are created when users start creating a listing but haven't completed it
-- (e.g., due to network issues, power outage, etc.)

ALTER TABLE listings
ADD COLUMN is_draft BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for querying draft listings efficiently
CREATE INDEX idx_is_draft ON listings(is_draft, user_id);
