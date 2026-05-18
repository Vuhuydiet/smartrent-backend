-- ============================================================
-- V79: Backfill NULL moderation_status
-- V58 backfilled moderation_status for all rows that existed at
-- that time, and ListingMapperImpl.toEntity stamps PENDING_REVIEW
-- on the standard create path. But the VIP builder
-- (buildListingFromVipRequest) and the shadow-listing builder
-- (createShadowListing) never set moderation_status, so paid
-- (SILVER/GOLD/DIAMOND) and Diamond-shadow listings created after
-- V58 were saved with moderation_status = NULL. Such a listing
-- still computes as IN_REVIEW but is invisible on the seller's
-- IN_REVIEW > PENDING_REVIEW tab and absent from the admin review
-- queue (SQL: NULL = 'PENDING_REVIEW' is never true).
--
-- This repairs the stragglers using the same verified/is_verify
-- mapping as V58. Guarded by moderation_status IS NULL so it is
-- idempotent and never overwrites a real moderation decision.
-- The code-level fixes (VIP/shadow builders) prevent new NULLs.
-- ============================================================

UPDATE listings SET moderation_status = 'APPROVED'
    WHERE moderation_status IS NULL AND verified = true;

UPDATE listings SET moderation_status = 'PENDING_REVIEW'
    WHERE moderation_status IS NULL AND verified = false AND is_verify = true;

UPDATE listings SET moderation_status = 'REJECTED'
    WHERE moderation_status IS NULL AND verified = false AND is_verify = false
      AND is_draft = false AND post_date IS NOT NULL;
