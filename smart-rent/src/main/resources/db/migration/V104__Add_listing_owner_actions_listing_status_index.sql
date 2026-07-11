-- The seller "update-post" page now always calls GET /v1/listings/{id}/my-detail
-- (previously only fetched in resubmit mode), which calls
-- ListingModerationServiceImpl.getOwnerPendingAction -> ownerActionRepository
-- .findByListingIdAndStatus(listingId, PENDING_OWNER) on every page load.
--
-- listing_owner_actions only has two single-column indexes (idx_owner_action_listing
-- on listing_id, idx_owner_action_status on status), so MySQL cannot serve the
-- (listing_id, status) equality lookup from a single index range scan. A composite
-- index covers both predicates directly.

SET @idx_exists = (SELECT COUNT(*)
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listing_owner_actions'
                     AND index_name = 'idx_owner_action_listing_status');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_owner_action_listing_status ON listing_owner_actions (listing_id, status)',
    'SELECT ''Index idx_owner_action_listing_status already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
