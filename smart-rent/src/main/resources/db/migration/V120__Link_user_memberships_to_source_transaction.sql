-- V120: one paid transaction can create at most one membership.
--
-- Payment completion is reachable from two IPN endpoints (the generic provider callback
-- and the SePay webhook), and SePay retries until it gets a 200 — so every
-- complete*Membership* method must expect to run more than once for the same
-- transaction. Each one guarded itself differently, and one didn't guard at all:
--
--   purchase — "is there an ACTIVE membership created within 5 minutes of this
--              transaction?", which is a heuristic, not an identity check: it also
--              matches an unrelated membership that happens to fall in the window,
--              and misses the retry that arrives after it.
--   upgrade  — keyed off upgraded_from_membership_id. Correct, but only because
--              upgrades happen to record their predecessor.
--   renewal  — nothing. A redelivered webhook created a second renewed membership
--              chained from the same slot and, when the renewal started immediately,
--              granted its post/push benefits a second time. That is the quota
--              accumulation seen on /quotas/check.
--
-- Record which transaction produced a membership and make the database enforce
-- uniqueness, so the guard is the same fact in all three flows and a concurrent
-- double-delivery can't slip between a check and an insert.
--
-- Nullable + non-unique-safe for history: existing rows keep NULL, and MySQL allows
-- many NULLs in a UNIQUE index. Memberships created by the lifecycle job (a queued slot
-- being promoted) legitimately have no transaction of their own.

ALTER TABLE user_memberships
ADD COLUMN created_from_transaction_id VARCHAR(36) NULL
COMMENT 'Transaction that created this membership; NULL for rows predating V120 or created by the lifecycle job';

CREATE UNIQUE INDEX uk_user_membership_source_transaction
ON user_memberships(created_from_transaction_id);
