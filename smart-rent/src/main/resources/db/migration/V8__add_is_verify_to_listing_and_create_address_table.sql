-- V8__add_is_verify_to_listing_and_create_address_table.sql

-- Add is_verify column to listings table (idempotent - only if column doesn't exist)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = 'smartrent' 
                   AND TABLE_NAME = 'listings' 
                   AND COLUMN_NAME = 'is_verify');

SET @sql = CASE 
    WHEN @col_exists = 0 THEN 'ALTER TABLE listings ADD COLUMN is_verify BOOLEAN NOT NULL DEFAULT FALSE'
    ELSE 'SELECT "Column is_verify already exists" AS message'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- (Removed duplicate CREATE TABLE addresses and index statements. The addresses table and its indexes are already created in V7.)
