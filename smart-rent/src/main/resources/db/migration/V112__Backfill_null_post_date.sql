-- buildListingFromVipRequest (VIP listing creation, including SePay-paid VIP
-- listings) never set post_date, unlike the mapper path used for NORMAL
-- listings. computeListingStatus() doesn't check post_date for the
-- moderation-status branch, so these rows still displayed fine — but
-- buildStatusPredicate(REJECTED) requires post_date IS NOT NULL, so a VIP
-- listing that got rejected silently vanished from any REJECTED filter/search
-- while still showing up in the unfiltered list. Backfill existing rows with
-- their creation time, the closest available approximation of when they were
-- actually posted.
UPDATE listings
SET post_date = created_at
WHERE post_date IS NULL
  AND created_at IS NOT NULL;
