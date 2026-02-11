-- V50: Add new_street column to listing_drafts table
-- This column stores the street name for the new address structure (34 provinces)
-- to support dual address structures (legacy and new)

ALTER TABLE listing_drafts
ADD COLUMN new_street VARCHAR(255) AFTER street