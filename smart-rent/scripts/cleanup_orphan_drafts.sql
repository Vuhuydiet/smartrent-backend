-- Clean up drafts that were already published.
--
-- Background
-- ----------
-- A draft's media_ids is a CSV of media rows. Publishing a draft creates a listing
-- and stamps media.listing_id, so a media row can only ever belong to one listing.
-- The draft is then supposed to be deleted.
--
-- Until the fix that ships with this script, the payment path lost the draft id on
-- the way to the gateway (the client read `listingId` off a response that returns
-- `draftId`), so the draft survived a successful payment. Reopening such a draft and
-- publishing it again fails with:
--
--     Media <id> is already linked to listing <id>
--
-- Those drafts are dead weight: the listing they describe already exists. This script
-- finds and removes them.
--
-- Usage: run STEP 1, eyeball the rows, then run STEP 2 in the same session.
--   mysql --default-character-set=utf8mb4 -u <user> -p <db> < cleanup_orphan_drafts.sql
-- (the charset flag matters on Windows, where the client defaults to cp850)


-- STEP 1 — inspect. Every row here is a draft whose media already belongs to a
-- published listing, i.e. a draft that was published and never cleaned up.
SELECT d.draft_id,
       d.user_id,
       d.title,
       d.updated_at,
       GROUP_CONCAT(DISTINCT m.listing_id ORDER BY m.listing_id) AS published_as_listing_ids,
       COUNT(DISTINCT m.media_id)                                AS consumed_media_count
FROM listing_drafts d
         JOIN media m ON FIND_IN_SET(m.media_id, d.media_ids) > 0
WHERE d.media_ids IS NOT NULL
  AND d.media_ids <> ''
  AND m.listing_id IS NOT NULL
GROUP BY d.draft_id, d.user_id, d.title, d.updated_at
ORDER BY d.updated_at DESC;


-- STEP 2 — delete. Same predicate as STEP 1, so it removes exactly the rows above.
-- Guarded by an explicit transaction: check the row count before committing.
START TRANSACTION;

DELETE d
FROM listing_drafts d
WHERE d.media_ids IS NOT NULL
  AND d.media_ids <> ''
  AND EXISTS (SELECT 1
              FROM media m
              WHERE FIND_IN_SET(m.media_id, d.media_ids) > 0
                AND m.listing_id IS NOT NULL);

-- Expect the same count as STEP 1 returned. If it differs, ROLLBACK instead.
COMMIT;
