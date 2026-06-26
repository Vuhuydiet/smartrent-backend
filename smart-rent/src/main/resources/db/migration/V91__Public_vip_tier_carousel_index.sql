-- Migration V91: Public VIP-tier carousel index — the homepage analog of V84
-- ============================================================================
-- The homepage fires one query per VIP tier (DIAMOND / GOLD / SILVER / NORMAL),
-- now batched behind POST /v1/listings/search/sections. Each section runs the
-- ordinary ListingSpecification.fromFilterRequest path with verified=true and a
-- pinned vip_type, producing this shape:
--
--     WHERE vip_type = ? AND verified = true
--           AND is_draft = false AND is_shadow = false
--     ORDER BY vip_type_sort_order ASC, updated_at DESC
--     LIMIT 10
--
-- (No moderation_status predicate: the FE sends verified=true, so the spec gates
-- on `verified` and the moderation_status=APPROVED branch is NOT taken. No
-- expired predicate either — the homepage doesn't send expired/excludeExpired.)
--
-- vip_type is pinned by the section, so vip_type_sort_order is constant
-- post-filter → effective sort is `updated_at DESC` (same reasoning as V84).
--
-- Why current indexes don't seek this cleanly:
--   • V84 idx_listings_user_vip_updated (user_id, vip_type, updated_at)
--     — that's the SELLER analog; it leads with user_id, which is null here.
--   • V82 idx_listings_public_default_sort
--     (moderation_status, is_shadow, is_draft, expired, vip_type_sort_order,
--      updated_at) — leads with moderation_status, which is NOT a predicate on
--     the verified=true path → planner can't use it for this query.
--   • idx_status (verified, expired, vip_type) — `expired` sits between the two
--     usable equalities (and isn't constrained here), and it carries no sort
--     column → equality seek on `verified` only, then filesort on the tier sort.
--
-- This index leads with the selective vip_type, matches the three boolean
-- equalities, then exposes updated_at for an ordered LIMIT 10 with no filesort.
--
-- NOTE: modelled on V84's proven shape but NOT yet EXPLAIN-validated on prod
-- data — validate with EXPLAIN on the 100k-row DB before relying on the numbers,
-- as V81-V84 did.
--
-- Idempotent via information_schema check, matching V78-V84 style.
-- ============================================================================

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_public_vip_tier');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_public_vip_tier ON listings (vip_type, verified, is_draft, is_shadow, updated_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
