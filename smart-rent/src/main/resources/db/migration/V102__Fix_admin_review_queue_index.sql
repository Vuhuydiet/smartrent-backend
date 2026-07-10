-- Migration V102: fix idx_listings_admin_review_queue (V101 shipped an
-- incomplete shape that made the admin review-queue query SLOWER, not faster)
-- ============================================================================
-- V101's index was (is_shadow, is_verify, verified, moderation_status, expired)
-- with NO trailing sort columns. EXPLAIN on the real query showed MySQL now
-- chose this index (type=ref_or_null, key_len=127, rows=11029, filtered=50%,
-- Extra="Using index condition; Using where; Using filesort") — the missing
-- sort columns meant filesort still ran on every hit, and the query went from
-- ~13s (before V100/V101) to 20s+ (after), i.e. the new index actively misled
-- the optimizer into a worse plan than whatever it picked before.
--
-- Two real problems with the V101 shape, both fixed here:
--
-- 1. verified and expired are BOOLEAN NOT NULL (V7 listings DDL, never
--    altered) and is_verify is likewise NOT NULL. ListingSpecification's old
--    predicates carried "OR column IS NULL" branches on these columns anyway
--    (dead code — that branch can never match), which forces MySQL's
--    ref_or_null access method — a materially more expensive lookup than a
--    plain ref/equality seek — for zero behavioral benefit. Those dead
--    branches were removed in ListingSpecification.java (buildStatusPredicate
--    and the moderationStatus PENDING_REVIEW/RESUBMITTED blocks), so
--    is_shadow/is_verify/verified/expired are now pure equality predicates.
--
-- 2. moderation_status (genuinely nullable — legacy rows predate the
--    moderation feature, see V79) and expiry_date (a NOW()-relative range)
--    are the only real non-equality conditions left. Neither was in the V101
--    index, so MySQL had to fetch the FULL row for every one of the ~11k
--    equality-matching candidates just to evaluate them, wasting a row fetch
--    for the ~50% that turn out expired. Adding them to the index lets
--    InnoDB's Index Condition Pushdown evaluate both directly from index
--    data, cutting full-row fetches roughly in half.
--
-- Corrected shape: 4 pure-equality columns, then the 2 residual/nullable
-- conditions (still ICP-filterable even though they can't provide index
-- order), then the sort columns so MySQL has the best possible shot at
-- avoiding filesort for whatever prefix of the scan it can satisfy by index
-- order, and a bounded number of ranges to merge-sort otherwise:
--
--     (is_shadow, is_verify, verified, expired, moderation_status,
--      expiry_date, vip_type_sort_order, updated_at)
--
-- Idempotent via information_schema check, matching the V78-V101 style.
-- ============================================================================

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_admin_review_queue');
SET @sql = IF(@idx_exists > 0,
    'DROP INDEX idx_listings_admin_review_queue ON listings',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE INDEX idx_listings_admin_review_queue
    ON listings (is_shadow, is_verify, verified, expired, moderation_status, expiry_date, vip_type_sort_order, updated_at);
