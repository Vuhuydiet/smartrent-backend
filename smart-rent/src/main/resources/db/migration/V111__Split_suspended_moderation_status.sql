-- Split the overloaded moderation_status='SUSPENDED' into three distinct states.
--
-- SUSPENDED used to encode three unrelated outcomes, disambiguated only by the
-- permanently_removed flag and the presence of a listing_owner_actions row:
--   1. Admin rejected the listing in the review queue  -> now REJECTED
--   2. Admin temporarily hid it while a report is open  -> stays SUSPENDED
--   3. Report resolution permanently removed it          -> now REMOVED
--
-- Order matters: REMOVED is claimed first (permanently_removed wins), then the
-- reject case (identified by a LISTING_REJECTED owner action — matched on the
-- action's existence, not its current status, so already-actioned rejects are
-- still reclassified), and whatever SUSPENDED remains is a genuine report hide.

-- 3) Permanently removed via report resolution.
UPDATE listings
SET moderation_status = 'REMOVED'
WHERE moderation_status = 'SUSPENDED'
  AND permanently_removed = TRUE;

-- 1) Rejected in the review queue (created a LISTING_REJECTED owner action).
UPDATE listings l
SET l.moderation_status = 'REJECTED'
WHERE l.moderation_status = 'SUSPENDED'
  AND l.permanently_removed = FALSE
  AND EXISTS (
      SELECT 1
      FROM listing_owner_actions oa
      WHERE oa.listing_id = l.listing_id
        AND oa.trigger_type = 'LISTING_REJECTED'
  );

-- 2) Everything still SUSPENDED is a genuine "temporarily hidden under report" case.
