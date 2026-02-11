-- Add vip_type_sort_order column to listings table for optimized sorting
-- This column enables efficient sorting by VIP tier: DIAMOND (1) > GOLD (2) > SILVER (3) > NORMAL (4)

-- Step 1: Add the column with default value
ALTER TABLE listings
ADD COLUMN vip_type_sort_order INT NOT NULL DEFAULT 4;

-- Step 2: Update existing rows based on their vip_type
UPDATE listings
SET vip_type_sort_order = CASE vip_type
    WHEN 'DIAMOND' THEN 1
    WHEN 'GOLD' THEN 2
    WHEN 'SILVER' THEN 3
    WHEN 'NORMAL' THEN 4
    ELSE 4
END;

-- Step 3: Create composite index for optimized sorting
CREATE INDEX idx_listings_sort_order
ON listings(vip_type_sort_order ASC, updated_at DESC);
