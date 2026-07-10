-- Admin can now permanently remove a listing when resolving a report as a
-- confirmed severe violation (see ListingModerationServiceImpl.handleReportResolutionRemoval).
-- The listing is set to moderation_status=SUSPENDED (already excluded from all
-- public/search queries), but unlike an ordinary SUSPEND the owner must NOT be
-- able to resubmit it -- this flag records that distinction so
-- resubmitForReview()/updateAndResubmitForReview() can block it permanently.

ALTER TABLE listings
ADD COLUMN permanently_removed BOOLEAN NOT NULL DEFAULT FALSE;
