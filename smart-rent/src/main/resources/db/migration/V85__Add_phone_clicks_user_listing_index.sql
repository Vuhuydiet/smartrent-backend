-- Migration V85: Composite index for the seller "customer management" page
-- ============================================================================
-- Backs PhoneClickDetailServiceImpl.buildUsersWhoClickedResponse, which — for
-- every clicking user on the page — runs:
--
--   SELECT pc.* FROM phone_clicks pc
--   JOIN listings l ON pc.listing_id = l.listing_id
--   WHERE l.user_id = :ownerId AND pc.user_id = :clickingUserId
--   ORDER BY pc.clicked_at DESC
--
-- (repository method findByListingOwnerIdAndClickingUserId). The existing
-- phone_clicks indexes are idx_user_id (user_id) and idx_listing_user
-- (listing_id, user_id) — neither lets MySQL both filter by user_id AND pull
-- listing_id from the index for the join, so it falls back to row lookups.
-- A (user_id, listing_id) composite covers the filter + join in one index
-- read, which matters now that the page issues one such query per row plus
-- the new keyword search path.
--
-- NOTE: the keyword search itself filters users with LOWER(col) LIKE '%kw%'
-- (leading wildcard) on first_name/last_name/email/contact_phone_number, which
-- is not B-tree indexable; the owner scope is already served by
-- listings.idx_user_created and phone_clicks.idx_listing_id, so this migration
-- only closes the per-user join gap. A FULLTEXT index would be the next step
-- if substring user search becomes a hot path.
--
-- Idempotent via information_schema check, matching V78/V80/V81 style.
-- ============================================================================

SET @idx_exists = (SELECT COUNT(*)
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'phone_clicks'
                     AND index_name = 'idx_phone_clicks_user_listing');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_phone_clicks_user_listing ON phone_clicks (user_id, listing_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
