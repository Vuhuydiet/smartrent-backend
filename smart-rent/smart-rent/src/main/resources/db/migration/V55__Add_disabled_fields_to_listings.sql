-- Add disabled fields to listings table for admin report resolution
ALTER TABLE listings ADD COLUMN disabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE listings ADD COLUMN disabled_at TIMESTAMP NULL;
ALTER TABLE listings ADD COLUMN disabled_by VARCHAR(36) NULL;
ALTER TABLE listings ADD COLUMN disabled_reason TEXT NULL;

-- Add index for filtering disabled listings
CREATE INDEX idx_listings_disabled ON listings (disabled);

