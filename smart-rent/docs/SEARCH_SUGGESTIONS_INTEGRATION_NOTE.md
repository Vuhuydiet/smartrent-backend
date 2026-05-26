# Search Suggestions — Integration Note for Teammates

> **Purpose:** Before extending or rewriting `GET /v1/listings/search-suggestions`, read this. Search infrastructure (FULLTEXT index, normalized columns, legacy-address indexes, Hibernate function, Redis cache) is already in place. The suggestion feature should *reuse* that infrastructure — not redefine it.

---

## 1. What is already built (do not reinvent)

### 1.1 Normalized text columns on `listings` (V53, V55)

| Column | Purpose | Index |
|---|---|---|
| `title_norm` (VARCHAR 256) | Title only, normalized | `idx_listings_title_norm(title_norm(64))` — prefix index |
| `search_text` (VARCHAR 512) | Title + description + address, normalized | `ft_listings_search_text` — **FULLTEXT** (V57) |

Both columns are populated automatically by `Listing.updateSearchFields()` on save via `TextNormalizer.compact(...)`. **Do not write to them yourself.**

### 1.2 FULLTEXT search wired into JPA (V57 + `MatchAgainstFunctionContributor.java`)

- `MATCH(search_text) AGAINST(? IN BOOLEAN MODE)` is registered as Hibernate function `match_against`.
- In Criteria/Specification:
  ```java
  criteriaBuilder.function("match_against",
      Double.class,
      root.get("searchText"),
      criteriaBuilder.literal(query));
  ```
- Search uses the convention `+token1* +token2* ...` for AND-with-prefix semantics. See `ListingSpecification.java:572-592`.

### 1.3 Composite filter indexes (V57, V59)

- `idx_listings_public_filter (is_shadow, is_draft, verified, expired, listing_type, category_id, price)` — public-search visibility filters.
- `idx_listings_public_search (..., vip_type_sort_order, updated_at)` — default sort.
- `idx_listings_user_filter (user_id, is_shadow, updated_at)` — owner views.
- `idx_addresses_lat_lng`, `idx_address_metadata_address_id`, `idx_media_listing_status_sort`, `idx_pricing_history_filter` — supporting joins.

### 1.4 Legacy address indexes (V40)

`legacy_wards`, `legacy_districts`, `legacy_provinces` all have indexes on:

- Normalized keys: `ward_key`, `district_key`, `province_key`
- Codes: `ward_code`, `district_code`, `province_code`
- Composite full-address: `idx_legacy_wards_full_address (province_code, district_code, ward_code)`

**Use these for prefix lookups.**

### 1.5 Search service & cache layer

- `ListingService.searchListings(filter)` — canonical entry point. Supports keyword, location, price, sort, pagination. Cached via `listing.search` (5m TTL).
- `ListingService.autocompleteListings(query, limit)` — already exists at `GET /v1/listings/autocomplete` for lightweight title-prefix suggestions.
- Redis caches in `application.yml`:
  - `listing.search` — 5m
  - `listing.browse` — 3m
  - `listing.detail` — 3m
  - `listing.suggestions` — 2m

### 1.6 Text normalization

- `TextNormalizer.normalize(...)` — strips diacritics, lowercases, collapses whitespace. Vietnamese-safe (đ/Đ handled, fixed in V55).
- **Use this for every comparison key**: DB columns, cache keys, dedup keys.

---

## 2. How the suggestion feature should plug in

> Think of "search suggestion" as a **lightweight projection of the existing search**, not a new search engine.

### 2.1 TITLE suggestions

- ❌ **Don't** use `LIKE 'prefix%'` on `title_norm`. That only matches at the start of the title and produces a dropdown that doesn't agree with what `POST /search` returns when the user hits Enter.
- ✅ **Do** use the FULLTEXT path: `match_against(searchText, '+token*' IN BOOLEAN MODE)`. Reuses `ft_listings_search_text` and gives the same matches as the main search — so the dropdown is a real preview.
- Sort `vip_type_sort_order ASC, pushed_at DESC` (matches search `DEFAULT` sort). LIMIT 8–20.

### 2.2 LOCATION suggestions

- ❌ **Don't** `legacyWardRepository.findAll()` then filter in Java. That throws away the V40 indexes and loads ~10k rows on every cache miss.
- ✅ **Do** use a native query with `LIKE CONCAT(:q,'%')` against `ward_key`, `district_key`, `province_key`. Those columns are indexed (V40).
- LIMIT in SQL, don't paginate in Java.

### 2.3 POPULAR_QUERY suggestions

- ❌ **Don't** define yet another telemetry table — the existing `search_query_impressions` table already logs every search-suggestion call with a normalized query.
- ❌ **Don't** count "clicks of type POPULAR_QUERY" — that path can never bootstrap from cold (no popular queries exist until popular queries exist).
- ✅ **Do** mine `search_query_impressions` for popularity:
  ```sql
  SELECT query_norm, COUNT(*) AS hits
  FROM   search_query_impressions
  WHERE  created_at >= :since
  GROUP BY query_norm
  ORDER BY hits DESC
  LIMIT  :n
  ```

### 2.4 Province / category filters

- Reuse the same field names and resolution logic as `ListingFilterRequest` (`provinceId`, `provinceCode`, `provinceCodes`, `isLegacy`).
- ❌ **Don't** add a new "numeric-only legacy province ID" path — it silently breaks for users on the new 34-province structure.

### 2.5 Caching

- Keep using `listing.suggestions` cache name. Cache key already encodes normalized query + provinceId + categoryId + limit (`CacheKeyBuilder.suggestionKey`).
- ⚠️ **Telemetry write must be outside the cached method**, not inside.
  `@Cacheable` returns the cached value and **never enters the method body** — anything inside the body (including `persistImpression`) does not run on cache hits. The current implementation is silently broken on every cache hit.

### 2.6 `@Transactional(REQUIRES_NEW)` self-invocation

- ❌ **Don't** call `@Transactional(REQUIRES_NEW)` methods via `this.persistXxx(...)` from within the same bean — Spring AOP proxies don't intercept self-invocation, so the propagation is ignored.
- ✅ **Do** move telemetry persist to a separate `@Service` bean (e.g. `SearchSuggestionTelemetryService`). Or accept that you don't need `REQUIRES_NEW` since the catch swallows exceptions anyway.

---

## 3. Concrete "do this / don't do this" checklist

| Topic | Don't | Do |
|---|---|---|
| Title match | `WHERE title_norm LIKE 'prefix%'` | `match_against(searchText, '+token*')` via the registered Hibernate function |
| Location match | `findAll()` + Java `.contains()` | Native `LIKE CONCAT(:q,'%')` on indexed `*_key` columns, LIMIT in SQL |
| Popular queries | Aggregate clicks of type `POPULAR_QUERY` (cold-start dead) | Aggregate `search_query_impressions.query_norm` over rolling window |
| Normalization | Custom lowercase / strip in suggestion code | `TextNormalizer.normalize(...)` everywhere |
| Province filter | Numeric-only `Integer.parseInt(provinceId)` | Mirror `ListingFilterRequest` fields (`provinceId`, `provinceCode`, `isLegacy`) |
| Telemetry on cache hit | Write impression inside `@Cacheable` method | Write impression in the controller (or a non-cached wrapper) before/after the cached call |
| `REQUIRES_NEW` | Self-invoke from same bean (no-op) | Separate `@Service` bean |
| New API surface | Add `/search-suggestions/click` as unauthenticated free-write endpoint | Reuse impression IDs as sufficient telemetry; rate-limit + tie click `text` to impression's `query_norm` if you keep it |
| New tables | Add `search_suggestion_clicks` for popularity | Mine `search_query_impressions` (and existing search analytics from commit `5266a21`) |
| New DB indexes | Single-column indexes copied from existing patterns | Composite `(suggestion_type, created_at)` only if you keep the clicks table |

---

## 4. Migration / rollout notes

- Before merging code that reads `title_norm` / `search_text`, **confirm the V55 backfill ran** in every environment (`application.search.backfill.enabled=true`). Without it, both columns are NULL on legacy rows and suggestions silently return empty.
- Before merging code that adds new indexes, run `EXPLAIN` on the existing query first — `idx_listings_title_norm` and `idx_listings_public_filter` may already cover your case.

---

## 5. Database connection infrastructure (the real bottleneck)

The suggestion endpoint is **public, uncacheable on the cold path, and writes telemetry per call** — so it touches DB connections more aggressively than almost any other endpoint. Understanding what's already configured matters.

### 5.1 What is already configured

**HikariCP pool** (bumped from 10 → 30 in commit `80b926f`, `application.yml:83-88`):

| Setting | Value | Notes |
|---|---|---|
| `maximum-pool-size` | 30 (`HIKARI_MAX_POOL`) | Hard ceiling on concurrent DB connections per node |
| `minimum-idle` | 10 (`HIKARI_MIN_IDLE`) | Always-warm connections |
| `connection-timeout` | 30s (`HIKARI_CONN_TIMEOUT`) | If exceeded → `SQLTransientConnectionException` |
| `idle-timeout` | 10m (`HIKARI_IDLE_TIMEOUT`) | Idle conns above min-idle reaped |
| `max-lifetime` | 30m (`HIKARI_MAX_LIFETIME`) | Forces conn rotation |

**Hibernate connection handling** (`application.yml:108-110`):

```yaml
hibernate:
  connection:
    provider_disables_autocommit: false
    handling_mode: DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION
```

This is a **major** pool-saver: Hibernate does **not** acquire a JDBC connection at the start of a `@Transactional` method — it grabs one only when the first SQL is fired, and releases it the moment the transaction commits. Combined with `openInView: false`, this means a `@Transactional(readOnly = true)` method that reads from cache and skips DB **uses zero connections**.

**Other DB-relevant flags:**

- `default_batch_fetch_size: 20` — automatic batching of lazy associations.
- `batch_size: 25`, `order_inserts: true`, `order_updates: true` — JDBC batch writes.
- `openInView: false` — Hibernate session does not extend into the view layer; lazy access outside `@Transactional` will throw. Forces explicit fetching, prevents accidental N-query waterfalls.

**Redis (Lettuce) pool** — separate from JDBC (`application.yml:123-127`):

| Setting | Value |
|---|---|
| `maxActive` | 16 (`LETTUCE_MAX_ACTIVE`) |
| `maxIdle` | 8 (`LETTUCE_MAX_IDLE`) |
| `minIdle` | 4 (`LETTUCE_MIN_IDLE`) |
| `maxWait` | 10s |

**Cache hits = zero DB connections** because Lettuce uses its own pool. This is the single biggest lever for the suggestion endpoint.

**No async executor** — there is no `@EnableAsync` / `@Async` in this codebase yet. All writes are synchronous on the request thread.

**Single primary, no read-replica routing** — search/suggestion queries compete on the same pool with payment / listing-write traffic.

### 5.2 Why suggestions hit the pool harder than search

The suggestion endpoint, as currently implemented, on every cache-miss does:

| Step | DB cost | Cache layer? |
|---|---|---|
| 1. `findTitleSuggestions` (native FULLTEXT-able query, but currently `LIKE`) | 1 query, 1 connection acquisition | ❌ |
| 2. `fetchLocationSuggestions` → `legacyWardRepository.findAll()` | **1 query streaming ~10k rows** | ❌ |
| 3. `fetchPopularQuerySuggestions` → aggregate over `search_suggestion_clicks` | 1 aggregation query | ❌ |
| 4. `persistImpression` (every uncached request) | 1 INSERT, separate transaction *intended* (broken — see §2.5/2.6) | ❌ |

That's **3 reads + 1 write** per cache miss. With `maximum-pool-size = 30` and `connection-timeout = 30s`, sustained ~10 RPS of cache-miss traffic to the suggestion endpoint will saturate the pool when other endpoints are also active.

The `findAll()` over `legacy_wards` is the single worst offender — it holds a connection for the time it takes MySQL to ship ~10,000 rows, then Hibernate to hydrate them. That can be hundreds of milliseconds of pool occupancy per call.

### 5.3 What to change for the suggestion path

**A. Make every source path use connection-cheap queries.** A native query with `LIMIT` and indexed columns finishes in <5 ms and releases the connection immediately.

| Source | Current | Connection-cost target |
|---|---|---|
| TITLE | Native LIKE prefix, LIMIT N | Keep, ≤5 ms — OK |
| LOCATION | `findAll()` then Java filter | Switch to native `LIKE CONCAT(:q,'%') LIMIT N` against indexed `*_key` columns — see §2.2 |
| POPULAR_QUERY | Aggregation over clicks (uses `idx_ssc_type_text`) | Switch to aggregation over `search_query_impressions` (§2.3); add `(query_norm, created_at)` composite index if QPS is high |

**B. Don't write telemetry on every call from the request thread.** Two acceptable patterns; pick one:

1. **`@Async` + thread pool** — annotate `persistImpression` / `persistClick` on a *separate* bean (so the proxy actually intercepts), enable `@EnableAsync`, configure a small `ThreadPoolTaskExecutor` with its own queue. Telemetry stops blocking request latency and stops competing for the request's JDBC connection. Caveat: each `@Async` write still acquires a connection — under burst you can still saturate the pool. Bound the executor's queue and reject overflow rather than blocking.
2. **Batch / buffered writes** — accumulate impressions in memory, flush every N rows or T seconds via a scheduled task. Single multi-row INSERT, one connection, drastically lower pool pressure. Trade-off: a process crash loses the in-memory buffer (acceptable for telemetry, not for billing).

The second is what high-traffic search systems normally do. For DATN scope, `@Async` with a bounded queue is enough.

**C. Treat the Redis cache as your primary DB-protector.** The current 2-minute TTL is conservative; suggestions can safely stretch to 5 minutes (typing patterns repeat heavily). Every minute of TTL is a multiplier on connection savings.

**D. Mind `@Transactional(readOnly = true)` semantics.** `getSuggestions` is annotated `readOnly = true`, but the inner `persistImpression` is a write. Because the `REQUIRES_NEW` propagation is silently ignored under self-invocation, the write attempts to run inside the read-only transaction. On MySQL this typically *succeeds* (read-only is just a hint by default), but if Spring's `JpaTransactionManager` is configured strictly, you'll get `TransientDataAccessResourceException`. Either move the write to a separate bean, or drop `readOnly = true`.

**E. Don't introduce more sources without considering connection cost.** Each new "source" the team adds (e.g. recommended categories, brand-name fallback) is another query × every cache miss. If a new source is needed, either:
- Roll it into one of the existing 3 queries (UNION ALL with type tag), or
- Materialize a denormalized "suggestion candidates" table that one query can scan.

### 5.4 Connection-cost checklist for any future suggestion change

- [ ] Does this code path execute on **cache hit**? If yes, audit `@Cacheable` placement so it doesn't.
- [ ] Does the query have a hard `LIMIT`?
- [ ] Does every `WHERE` column resolve to an existing index?
- [ ] Is there a write on the request thread? Move to async/batch if yes.
- [ ] Is `@Transactional(REQUIRES_NEW)` invoked from a *different* bean than the caller? (Self-invocation is a no-op.)
- [ ] Does the change increase the per-request query count? If yes, what's the cache hit rate that keeps p99 latency stable?

---

## 6. TL;DR

> Search is already fast. We have FULLTEXT on `search_text`, prefix index on `title_norm`, indexed `*_key` columns on legacy address tables, a Hibernate `match_against` function, normalized columns kept up-to-date by the entity, and a Redis cache stack.
>
> **Suggestions = same query, smaller LIMIT, lighter projection.**
>
> Don't add new tables, new indexes, or new normalization. Don't reach for `LIKE 'x%'` or `findAll()`. Reuse what's there so the dropdown agrees with the search results page.

---

## 6. Reference files

| Concern | File |
|---|---|
| Search Specification | `smart-rent/src/main/java/com/smartrent/infra/repository/specification/ListingSpecification.java` |
| FULLTEXT Hibernate function | `smart-rent/src/main/java/com/smartrent/config/MatchAgainstFunctionContributor.java` |
| Service registration | `smart-rent/src/main/resources/META-INF/services/org.hibernate.boot.model.FunctionContributor` |
| Listing entity (search field auto-update) | `smart-rent/src/main/java/com/smartrent/infra/repository/entity/Listing.java` (`updateSearchFields()`) |
| Text normalizer | `smart-rent/src/main/java/com/smartrent/util/TextNormalizer.java` |
| Cache config | `smart-rent/src/main/resources/application.yml` (`spring.cache.redis.cacheConfigurations`) |
| Cache key builder | `smart-rent/src/main/java/com/smartrent/util/CacheKeyBuilder.java` |
| Search columns + indexes | `V53__Add_listing_search_text.sql` |
| Backfill reset | `V55__Reset_listing_search_text_for_normalization_fix.sql` |
| FULLTEXT + composite filter index | `V57__Add_fulltext_index_for_listing_search.sql` |
| Performance indexes | `V59__Add_performance_indexes.sql` |
| Legacy address indexes | `V40__Create_legacy_indexes.sql` |
| Existing autocomplete endpoint | `ListingSearchController.java` (`GET /v1/listings/autocomplete`) |
| Suggestion telemetry tables | `V72__Create_search_suggestion_telemetry_tables.sql` |
| Suggestion service (current impl) | `SearchSuggestionServiceImpl.java` |
