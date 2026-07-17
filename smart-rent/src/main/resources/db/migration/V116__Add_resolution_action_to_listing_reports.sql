-- Record the concrete action an admin took when resolving a report.
--
-- listing_reports.status only distinguishes PENDING / RESOLVED / REJECTED, and
-- both "suspend the listing" and "request the owner to revise it" resolve to
-- RESOLVED. The admin console previously had to guess the outcome from the
-- listing's live moderation_status, which drifts once the owner resubmits.
-- This column records the action at resolution time so it stays accurate.

ALTER TABLE listing_reports
    ADD COLUMN resolution_action VARCHAR(32) NULL AFTER admin_notes;

-- ── Backfill existing reports from durable signals ──

-- Dismissed: report rejected, listing untouched.
UPDATE listing_reports
SET resolution_action = 'DISMISSED'
WHERE status = 'REJECTED'
  AND resolution_action IS NULL;

-- Suspended/removed: report resolution permanently removed the listing
-- (handleReportResolutionRemoval sets permanently_removed = TRUE).
UPDATE listing_reports r
SET r.resolution_action = 'SUSPENDED'
WHERE r.status = 'RESOLVED'
  AND r.resolution_action IS NULL
  AND EXISTS (
      SELECT 1 FROM listings l
      WHERE l.listing_id = r.listing_id
        AND l.permanently_removed = TRUE
  );

-- Revision requested: report resolution created a REPORT_RESOLVED owner action
-- pointing back at this report (handleReportResolutionOwnerAction).
UPDATE listing_reports r
SET r.resolution_action = 'REVISION_REQUESTED'
WHERE r.status = 'RESOLVED'
  AND r.resolution_action IS NULL
  AND EXISTS (
      SELECT 1 FROM listing_owner_actions oa
      WHERE oa.listing_id = r.listing_id
        AND oa.trigger_type = 'REPORT_RESOLVED'
        AND oa.trigger_ref_id = r.report_id
  );

-- Remaining RESOLVED rows have no reliable historical signal of the exact action
-- and stay NULL; the admin console falls back to deriving the outcome from the
-- listing's current moderation status for those.
