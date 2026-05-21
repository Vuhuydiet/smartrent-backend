-- Migration V82: Complete-coverage indexes for the search/filter API surface
-- ============================================================================
-- V81 closed the address / pricing-history / my-listings gaps. V82 covers the
-- remaining hot paths after another round of EXPLAIN on the same 100k-row
-- prod DB (measured 2026-05-21):
--
--   • POST /v1/listings/search (no category, sort=DEFAULT) : ~1.6s  (filesort)
--   • POST /v1/listings/search (no category, sort=PRICE)   : ~1.3s  (filesort)
--   • POST /v1/listings/search (productType only filter)   : ~0.9s  (no usable idx)
--   • POST /v1/listings/search (hasMedia=true)             : ~0.7s  (media scan)
--   • POST /v1/listings/search (isBroker=false)            : ~1.1s  (users full scan)
--
-- Why these specifically — the gaps left after V80/V81:
--   • V80 only handles category-scoped queries (idx_listings_cat_mod_filter,
--     idx_listings_cat_sort). The "no-category" public search path — homepage
--     hero, /v1/listings/search with only price/location filters, and every
--     POST /v1/listings/search where the FE doesn't pin a category — has
--     nothing to anchor on, so the planner picks idx_status (verified, ...)
--     and filters moderation_status in memory.
--   • The isBroker=false branch in ListingSpecification (line 652) is a
--     NON-correlated NOT IN subquery, not a per-row PK lookup like
--     ownerPhoneVerified — so it full-scans users every time. ~30k user rows
--     today, growing.
--   • media has idx_listing_id and idx_status as separate single-column
--     indexes; the hasMedia / minMediaCount EXISTS subqueries filter on
--     BOTH at once and end up index-merging two scans per outer row.
--
-- Idempotent via information_schema check, matching V78/V80/V81 style.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. listings(moderation_status, is_shadow, is_draft, expired, vip_type_sort_order, updated_at)
--    The global public-search index for the DEFAULT sort path (vip-first,
--    then newest). Leading 4 cols are fixed equality filters in the public
--    spec; trailing 2 give the planner an index-ordered scan for LIMIT.
--    Mirrors idx_listings_cat_sort (V80) for the no-category branch.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_public_default_sort');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_public_default_sort ON listings (moderation_status, is_shadow, is_draft, expired, vip_type_sort_order, updated_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. listings(moderation_status, is_shadow, is_draft, expired, price)
--    Same public-search prefix, swapping the sort tail for `price` to back
--    sortBy=PRICE_ASC / PRICE_DESC. Without this the planner picks
--    idx_price_type (price, listing_type), which is sort-friendly but skips
--    the moderation gate — every row gets a post-filter read.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_public_price_sort');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_public_price_sort ON listings (moderation_status, is_shadow, is_draft, expired, price)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. listings(product_type, listing_type)
--    Mirrors idx_category_type for the productType filter when no category
--    is selected (e.g. "Tất cả phòng trọ trên toàn quốc"). Without it the
--    planner sees no usable index for `product_type = ?` alone and chooses
--    a full scan.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'listings'
                     AND index_name = 'idx_listings_product_type');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_listings_product_type ON listings (product_type, listing_type)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4. media(listing_id, status)
--    The hasMedia and minMediaCount EXISTS subqueries in ListingSpecification
--    filter on (listing_id = outer.listing_id AND status = 'ACTIVE'). media
--    today has those as two separate indexes, so InnoDB index-merges per
--    outer row. A composite turns it into a single seek.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'media'
                     AND index_name = 'idx_media_listing_status');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_media_listing_status ON media (listing_id, status)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 5. users(is_broker, broker_verification_status)
--    Backs the isBroker filter. The isBroker=false branch issues a
--    NON-correlated subquery — SELECT user_id FROM users WHERE is_broker=true
--    AND broker_verification_status='APPROVED' — and the outer query NOT INs
--    the result. With no index it full-scans the users table on every
--    search hit. The isBroker=true correlated branch also gets a covering
--    filter from the same index.
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'users'
                     AND index_name = 'idx_users_broker_status');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_users_broker_status ON users (is_broker, broker_verification_status)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
