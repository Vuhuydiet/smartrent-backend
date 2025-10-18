-- Fix Flyway Schema History
-- Remove the failed migration record for V13
DELETE FROM flyway_schema_history WHERE version = '13';

-- Optionally, check remaining migrations
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
