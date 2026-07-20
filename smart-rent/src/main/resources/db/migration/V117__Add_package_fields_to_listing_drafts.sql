-- V117: keep the package/quota selection on a draft.
--
-- listing_drafts stored vip_type but none of the three fields that decide HOW a
-- listing gets paid for. Reopening a draft therefore lost them: the create-post
-- form re-defaulted duration to 10 days from today, and with benefit_ids and
-- use_membership_quota gone the UI flipped the CTA from "Đăng tin" to "Thanh
-- toán" — asking the user to pay for a listing their membership already covers.
--
-- benefit_ids is a comma-separated list, matching the existing amenity_ids /
-- media_ids columns on this table.

ALTER TABLE listing_drafts
    ADD COLUMN duration_days INT NULL AFTER vip_type,
    ADD COLUMN use_membership_quota BOOLEAN NOT NULL DEFAULT FALSE AFTER duration_days,
    ADD COLUMN benefit_ids VARCHAR(500) NULL AFTER use_membership_quota;
