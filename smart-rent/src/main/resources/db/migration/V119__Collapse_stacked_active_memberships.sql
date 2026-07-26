-- V119: one user, one current membership.
--
-- Until completeMembershipPurchase started retiring whatever was already active, a
-- redelivered payment webhook (or any purchase that slipped past the "already has a
-- membership" guard) created ANOTHER ACTIVE user_memberships row overlapping the old
-- one. The quota reads summed every overlapping slot, so a STANDARD seller whose
-- package grants 20 pushes/month was told they had 200 — 10 stacked slots' worth —
-- and each push decremented that inflated pool instead of their real 20.
--
-- The service layer no longer sums across slots (it reads the single current slot), but
-- the duplicate rows themselves are still there: they keep every ACTIVE-membership
-- count, admin dashboard and history view wrong, and they make "which slot is current"
-- depend on query ordering. Collapse them here so the data says what the code now
-- assumes.
--
-- Scope: only slots whose window contains NOW (start_date <= now < end_date). Queued
-- slots (start_date in the future, created by a chained renewal) are legitimate and are
-- left alone. Per user the slot ending LAST wins — same ordering as
-- findActiveUserMembership (end_date DESC, user_membership_id DESC) — so the row this
-- keeps is the row the application was already going to serve. The forfeited quota on
-- the losing slots is intentional: it was never sold.

-- 1. Retire every currently-active slot that isn't the user's latest-ending one.
UPDATE user_memberships um
JOIN (
    SELECT a.user_id, MAX(a.user_membership_id) AS keep_id
    FROM user_memberships a
    JOIN (
        SELECT user_id, MAX(end_date) AS max_end
        FROM user_memberships
        WHERE status = 'ACTIVE' AND start_date <= NOW() AND end_date > NOW()
        GROUP BY user_id
        HAVING COUNT(*) > 1
    ) latest ON latest.user_id = a.user_id AND latest.max_end = a.end_date
    WHERE a.status = 'ACTIVE' AND a.start_date <= NOW() AND a.end_date > NOW()
    GROUP BY a.user_id
) keeper ON keeper.user_id = um.user_id
SET um.status = 'EXPIRED'
WHERE um.status = 'ACTIVE'
  AND um.start_date <= NOW()
  AND um.end_date > NOW()
  AND um.user_membership_id <> keeper.keep_id;

-- 2. Expire the benefit rows hanging off any membership that is no longer ACTIVE —
--    the slots just retired above, plus any older orphans left behind by paths that
--    used to expire the membership without cascading to its benefits. A benefit whose
--    membership is gone must never read as ACTIVE.
UPDATE user_membership_benefits umb
JOIN user_memberships um ON um.user_membership_id = umb.user_membership_id
SET umb.status = 'EXPIRED'
WHERE umb.status = 'ACTIVE'
  AND um.status <> 'ACTIVE';
