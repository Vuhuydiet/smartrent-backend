# Index Audit — `listings` + `addresses` tables, pre-200k scale

**Date**: 2026-05-13
**Goal**: Verify the index coverage is sufficient before seeding the
listings table from ~10k → 200k rows for the thesis defense
performance benchmark. Identify slow query paths and propose
targeted index migrations.
**Related**: `smartrent-ai/docs/architecture/` (architecture diagrams),
`d:/DEV/datn/THESIS_PLAN.md` task #1.

## Methodology

1. Inventory current indexes by reading every `V*.sql` Flyway migration.
2. Walk through `ListingSpecification.java` + `ListingServiceImpl.searchListings`
   to enumerate the WHERE/JOIN/ORDER BY columns produced for each user-visible
   filter combination.
3. For each common query pattern (Q1-Q9 below), check whether an existing
   index covers it or full-scans are likely.
4. Propose the smallest set of new indexes that closes the worst gaps.

## Current indexes — `listings`

| Index | Columns | Migration | Used by |
|---|---|---|---|
| `PRIMARY` | `listing_id` | V7 | PK |
| `idx_user_created` | `(user_id, created_at)` | V7 | Owner's listings sorted by creation |
| `idx_category_type` | `(category_id, listing_type)` | V7 | Category + type filter |
| `idx_address` | `(address_id)` | V7 | FK join to addresses |
| `idx_price_type` | `(price, listing_type)` | V7 | Price filter |
| `idx_status` | `(verified, expired, vip_type)` | V7 | Status checks |
| `idx_expiry_date` | `(expiry_date)` | V7 | Expiry tracking |
| `idx_post_date` | `(post_date)` | V7 | postedWithinDays range |
| `idx_is_shadow` | `(is_shadow)` | V13 | Shadow filter |
| `idx_is_draft` | `(is_draft, user_id)` | V30 | Owner drafts |
| `idx_listings_sort_order` | `(vip_type_sort_order ASC, updated_at DESC)` | V51 | **Default sort** |
| `idx_listings_user_drafts` | `(user_id, is_draft, updated_at)` | V53 | Owner drafts with sort |
| `idx_listings_search_text` | `(search_text(128))` | V53 | (rarely used; FULLTEXT preferred) |
| `idx_listings_title_norm` | `(title_norm(64))` | V53 | Autocomplete prefix |
| `idx_listings_public_search` | `(is_shadow, is_draft, verified, expired, vip_type_sort_order, updated_at)` | V53 | **Public default-search baseline** |
| `ft_listings_search_text` | FULLTEXT `(search_text)` | V57 | Keyword search |
| `idx_listings_public_filter` | `(is_shadow, is_draft, verified, expired, listing_type, category_id, price)` | V57 | Public search + type/category/price |
| `idx_listings_user_filter` | `(user_id, is_shadow, updated_at)` | V59 | My-listings with sort |
| `ft_listings_title_norm` | FULLTEXT `(title_norm)` | V73 | Title suggestions |

## Current indexes — `addresses`

| Index | Columns | Migration |
|---|---|---|
| `PRIMARY` | `address_id` | V7 |
| `idx_legacy_province` | `(legacy_province_id)` | V45 |
| `idx_legacy_district` | `(legacy_district_id)` | V45 |
| `idx_legacy_ward` | `(legacy_ward_id)` | V45 |
| `idx_legacy_location` | `(legacy_province_id, legacy_district_id, legacy_ward_id)` | V45 |
| `idx_new_province` | `(new_province_code)` | V45 |
| `idx_new_ward` | `(new_ward_code)` | V45 |
| `idx_new_location` | `(new_province_code, new_ward_code)` | V45 |
| `idx_address_type` | `(address_type)` | V45 |
| `idx_addresses_lat_lng` | `(latitude, longitude)` | V59 |

## Query patterns audited

| # | Pattern | Frequency |
|---|---|---|
| Q1 | Default public listing (`verified=1 AND is_draft=0 AND is_shadow=0 AND expired=0` + default sort) | every request |
| Q2 | Q1 + province filter (legacy or new) | most chat queries |
| Q3 | Q1 + district filter (`legacy_district_id`) | sprint-v2 default chat flow |
| Q4 | Q1 + price range `BETWEEN` | common UI filter |
| Q5 | Q1 ordered by `price ASC/DESC` | UI sort dropdown |
| Q6 | Q1 + `product_type = ?` (5-value enum) | tool-call from search_listings |
| Q7 | Q1 + FULLTEXT `MATCH(search_text)` keyword | text search |
| Q8 | Q1 + EXISTS on amenities | facets |
| Q9 | Q1 + `post_date >=` range | "tin mới đăng" |

## Gap analysis

### 🔴 Critical — will hurt at 200k

**G1. Province filter forces an address-side scan after the listings filter.**
The orchestrator joins via `idx_address` (`listings.address_id`) and then
filters addresses by `legacy_province_id`. The current `idx_legacy_province`
on addresses works for direct lookups but not for `(address_id, province)`
covering after a join — MySQL falls back to ref scan + filter. At 200k
listings × ~30k addresses, the per-province scan dominates Q2/Q3 latency.

→ Add a composite `(address_id, legacy_province_id)` and a parallel
`(address_id, new_province_code)` so the optimiser can pin the post-join
filter.

**G2. `PRICE_ASC` / `PRICE_DESC` sort spills into an in-memory sort.**
`ListingQueryService:125-128` falls back to JVM sorting when the sortBy
field isn't covered by a composite index. At 200k rows, this is a
network-bound full-table fetch into the application heap. Adding
`(price, vip_type_sort_order, updated_at)` lets MySQL produce sorted
output directly from the index.

**G3. No index on `phonetic_title` — and that's correct.** The current
search is `LIKE '%keyword%'` (leading wildcard) which a B-tree index
cannot serve anyway. Leave it. If typo-tolerant search graduates to a
real feature, switch to FULLTEXT + drop the LIKE then.

### 🟡 Worth adding (optional)

- Map-bounds queries already use `idx_addresses_lat_lng`. Could add a
  covering `(latitude, longitude, address_id)` if maps becomes hot;
  not needed for defense workload.
- `postDate + verified` for "recent verified" lists — not in current
  `ListingSpecification`, skip until a use case appears.

### 🟢 Existing coverage is fine

- Default sort: `idx_listings_sort_order` is a perfect match.
- FULLTEXT keyword: `ft_listings_search_text`.
- Owner queries (my-listings, my-drafts): `idx_listings_user_filter` +
  `idx_listings_user_drafts`.
- District + ward nested lookup: `idx_legacy_location`,
  `idx_new_location`.
- Amenity EXISTS: `listing_amenities` PK is already
  `(listing_id, amenity_id)`.
- `product_type` filter: 5-value enum, not worth a dedicated index;
  covered by `idx_listings_public_filter` already.

### ⚠️ Anti-patterns observed (defer cleanup)

- `idx_listings_search_text` (BTREE prefix on `search_text(128)`) is
  almost certainly unused — the FULLTEXT version handles all search
  workloads. Dropping it would save ~5% write cost but breaks
  zero-search-experience users with one quirky query. Leave it.
- `idx_post_date` on a single column is partially redundant with
  whatever composite covers `postDate + sort`. Not aggressive enough
  to remove.

## Proposed migration — V78

Three indexes targeting G1 + G2. Idempotent (skips if already exists)
following the project's existing migration style. See
`V78__Scale_optimization_indexes.sql` in this branch.

### Sizing

For 200k listings each ~1KB:
- `idx_address_legacy_province` on addresses (~30k rows estimated):
  ~30k × 12 bytes ≈ **360 KB**
- `idx_address_new_province` on addresses:
  ~30k × 14 bytes ≈ **420 KB**
- `idx_listings_price_sort` on listings (200k rows):
  200k × 24 bytes ≈ **4.8 MB**

Total: < 6 MB extra index space. Write amplification negligible (1 BTREE
update on insert/price change). Acceptable.

## Test plan after applying

Run `EXPLAIN FORMAT=JSON` on each query before / after V78 and capture
the `query_cost`. Target: every query under 50ms p95 at 200k rows.

```sql
-- Q2: province filter (top user query)
EXPLAIN SELECT l.*
FROM listings l INNER JOIN addresses a ON l.address_id = a.address_id
WHERE l.verified = 1 AND l.is_draft = 0 AND l.is_shadow = 0 AND l.expired = 0
  AND a.legacy_province_id = 79
ORDER BY l.vip_type_sort_order ASC, l.updated_at DESC
LIMIT 20;

-- Q5: price sort
EXPLAIN SELECT l.*
FROM listings l
WHERE l.verified = 1 AND l.is_draft = 0 AND l.is_shadow = 0 AND l.expired = 0
ORDER BY l.price ASC, l.vip_type_sort_order ASC, l.updated_at DESC
LIMIT 20;
```

Compare:
- `EXPLAIN` chosen index name (should be the new index)
- `rows examined` count (should be ≤ a few hundred, not 200k)
- p95 latency on a warm cache (~5ms vs current likely 200ms+)

## Next steps in THESIS_PLAN.md task #1

1. Apply V78 to dev DB. Run EXPLAIN suite above and record before/after
   numbers in this doc as "Results" appendix.
2. Run the seed script (decide between approach A/B/C per
   THESIS_PLAN.md) on dev to load 200k listings.
3. Re-run EXPLAIN + measure live `/v1/listings/search` p95 via k6 or
   simple curl loop. Add results to the appendix.
4. Merge V78 to main (CI builds backend image → Droplet auto-deploys
   → Flyway runs the migration on prod schema during container start).
5. Reseed prod DB only after dev results confirm zero regression.
