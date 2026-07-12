-- Migration V110: drop redundant / duplicate secondary indexes.
-- ============================================================================
-- A 2026-07-12 index audit found the listings table alone carrying 47 indexes
-- (~484 MB incl. clustered; secondary index_length 298 MB > 181 MB of data),
-- with the same bloat pattern on addresses / phone_clicks / views / media /
-- pricing_histories / users. Two kinds of dead weight:
--
--   1. EXACT DUPLICATES — two indexes on identical column lists. One is pure
--      redundancy (created historically by Hibernate ddl-auto under an entity
--      @Index name, then re-added by a later Flyway migration under a different
--      name — or vice versa).
--   2. PREFIX-REDUNDANT — a single-column index whose column is already the
--      leading column of a wider composite, so the composite serves every query
--      the single-column index could (including FK lookups — each dropped index
--      below still has a same-leading-column composite to back its FK).
--
-- Dropping them speeds up writes (fewer indexes to maintain on the write-heavy
-- views / phone_clicks / listings tables), frees ~115 MB, and shrinks the
-- optimizer's plan search. No read path regresses — every dropped index is
-- covered by a surviving index (noted per line).
--
-- These were already dropped directly on prod during the audit; this migration
-- makes the change reproducible (fresh Flyway builds) and idempotent — each
-- DROP is guarded on information_schema, so it is a no-op where the index is
-- already gone. The matching duplicate @Index annotations were removed from the
-- Address / Media / View / PricingHistory / PhoneClickDetail entities in the
-- same change so ddl-auto=update can't reintroduce them.
--
-- Idempotent guarded-DROP style matches V94–V109 (inline PREPARE, no DELIMITER
-- so Flyway's statement splitter handles it).
-- ============================================================================

-- listings (dup of idx_listings_parent / idx_listings_pushed_at /
--            idx_listings_user_draft_updated respectively)
SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'listings' AND index_name = 'idx_parent_listing');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE listings DROP INDEX idx_parent_listing', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'listings' AND index_name = 'idx_pushed_at');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE listings DROP INDEX idx_pushed_at', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'listings' AND index_name = 'idx_listings_user_drafts');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE listings DROP INDEX idx_listings_user_drafts', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- addresses (dups of idx_addresses_lat_lng / idx_legacy_location /
--            idx_legacy_district / idx_legacy_ward / idx_new_location; and the
--            single idx_legacy_province / idx_new_province are prefixes of
--            idx_legacy_location / idx_new_location)
SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'addresses' AND index_name = 'idx_addresses_geo');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE addresses DROP INDEX idx_addresses_geo', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'addresses' AND index_name = 'idx_addresses_legacy_loc');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE addresses DROP INDEX idx_addresses_legacy_loc', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'addresses' AND index_name = 'idx_addresses_legacy_district');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE addresses DROP INDEX idx_addresses_legacy_district', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'addresses' AND index_name = 'idx_addresses_legacy_ward');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE addresses DROP INDEX idx_addresses_legacy_ward', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'addresses' AND index_name = 'idx_addresses_new_loc');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE addresses DROP INDEX idx_addresses_new_loc', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'addresses' AND index_name = 'idx_legacy_province');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE addresses DROP INDEX idx_legacy_province', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'addresses' AND index_name = 'idx_new_province');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE addresses DROP INDEX idx_new_province', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- users (dup of the unique uk_users_phone on the same (phone_code, phone_number))
SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'users' AND index_name = 'idx_users_phone');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE users DROP INDEX idx_users_phone', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- phone_clicks (idx_listing_id ⊂ idx_listing_clicked_at / idx_listing_user;
--               idx_user_id ⊂ idx_phone_clicks_user_listing)
SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'phone_clicks' AND index_name = 'idx_listing_id');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE phone_clicks DROP INDEX idx_listing_id', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'phone_clicks' AND index_name = 'idx_user_id');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE phone_clicks DROP INDEX idx_user_id', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- views (idx_listing_id ⊂ idx_listing_time)
SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'views' AND index_name = 'idx_listing_id');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE views DROP INDEX idx_listing_id', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- media (idx_listing_id ⊂ idx_listing_sort / idx_media_listing_status_sort;
--        idx_media_listing_status ⊂ idx_media_listing_status_sort)
SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'media' AND index_name = 'idx_listing_id');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE media DROP INDEX idx_listing_id', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'media' AND index_name = 'idx_media_listing_status');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE media DROP INDEX idx_media_listing_status', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- pricing_histories (idx_listing_id ⊂ idx_listing_date;
--                    idx_pricing_history_filter ⊂ idx_pricing_listing_current_type)
SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'pricing_histories' AND index_name = 'idx_listing_id');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE pricing_histories DROP INDEX idx_listing_id', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @cnt = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'pricing_histories' AND index_name = 'idx_pricing_history_filter');
SET @ddl = IF(@cnt > 0, 'ALTER TABLE pricing_histories DROP INDEX idx_pricing_history_filter', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- Refresh optimizer statistics on the affected tables.
ANALYZE TABLE listings, addresses, phone_clicks, views, media, pricing_histories, users;
