-- Migration V47: Drop property_type column from listings table
-- This column is a duplicate of product_type and is no longer needed

-- Check if column exists before dropping (MySQL compatible)
SET @preparedStatement = (SELECT IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'listings'
          AND COLUMN_NAME = 'property_type'
    ),
    'ALTER TABLE listings DROP COLUMN property_type;',
    'SELECT "Column property_type does not exist, skipping drop.";'
));

PREPARE alterIfExists FROM @preparedStatement;
EXECUTE alterIfExists;
DEALLOCATE PREPARE alterIfExists;
