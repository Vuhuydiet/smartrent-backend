-- Migration V76: Add phonetic_title column to listings
-- ============================================================================
-- Root cause: PR #232 ("enhance search suggestion") added the field
--   @Column(name = "phonetic_title", length = 256) String phoneticTitle;
-- to com.smartrent.infra.repository.entity.Listing — but the corresponding
-- ALTER TABLE migration was missing. Every Hibernate SELECT against listings
-- since the merge has failed with:
--   SQL Error: 1054, SQLState: 42S22
--   Unknown column 'l1_0.phonetic_title' in 'field list'
-- so /v1/listings/search and most other listing reads have been 500-ing in
-- production for ~22h.
--
-- The value is computed in @PrePersist/@PreUpdate from `title` via
-- DoubleMetaphone (see Listing.java ~line 360-382), so the column itself
-- only needs to be added — Hibernate will backfill on the next save of
-- each row, and the search WHERE-clause that uses it
-- (ListingSpecification.java:981, LIKE '%phoneticKeyword%') only joins
-- when criteria.getPhoneticKeyword() is set, so existing NULL rows are
-- harmless to current search behaviour.
-- ============================================================================

ALTER TABLE listings
  ADD COLUMN phonetic_title VARCHAR(256) NULL
  COMMENT 'Double-Metaphone phonetic form of title for typo-tolerant search. Auto-populated on insert/update by Listing.@PrePersist via DoubleMetaphone over the title words.';

-- No index: ListingSpecification searches with `LIKE '%kw%'` (leading
-- wildcard), which would not benefit from a B-tree index. If a FULLTEXT
-- search is added later, declare the index in a separate migration.
