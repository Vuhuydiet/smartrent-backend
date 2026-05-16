# SmartRent Advanced Search and Smart Suggestions

This guide documents the graduation-project-friendly search design implemented across:

- Spring Boot backend: source of truth, filtering, ranking, Redis caching, MySQL querying
- MySQL: listing data, structured filters, FULLTEXT indexes
- Redis: hot suggestion/search caches and telemetry-derived popularity
- AI server: natural-language intent parsing and suggestion normalization only
- React frontend: debounced autocomplete and search execution

The important rule is simple: AI never generates SQL and never searches the database. AI only turns natural text into structured intent or autocomplete phrases. Spring Boot controls every query.

## Current Support

Supported now:

- AI free-text search via `POST /api/v1/advanced-search`
- Public structured search via `POST /v1/listings/search`
- Smart suggestions via `GET /v1/listings/search-suggestions`
- Click telemetry via `POST /v1/listings/search-suggestions/click`
- MySQL FULLTEXT search over normalized listing text/title
- Redis cache for suggestions through Spring Cache
- Abbreviation normalization: `q1`, `dhqg`, `5tr`, `canho`, `phongtro`, `may lan`
- Typo-tolerant local suggestions using Levenshtein distance
- Phonetic suggestions using Apache Commons Codec `DoubleMetaphone`
- AI fallback suggestions through `POST /api/v1/search/suggestions` on the AI server

Not included intentionally:

- Elasticsearch/OpenSearch/Solr
- vector database/RAG search
- Kafka or a search indexing pipeline
- AI-generated SQL

## User Flow

Suggestion flow:

1. User types into the React search box.
2. Frontend waits for debounce, usually `250-350ms`.
3. Frontend calls `GET /v1/listings/search-suggestions?q=...&limit=8`.
4. Backend normalizes Vietnamese text and abbreviations.
5. Backend checks Redis/Spring cache.
6. Backend gathers local suggestions:
   - MySQL title FULLTEXT
   - location matches
   - popular clicked searches
   - typo dictionary
   - phonetic title/canonical phrase matches
7. If the query is long/complex and confidence is low, backend calls AI server for intent-style suggestion phrases.
8. Backend merges, deduplicates, ranks, caches, and returns suggestions.
9. Frontend renders suggestions.
10. If user clicks a suggestion, frontend posts click telemetry.

Search flow:

1. User submits text like `tìm phòng trọ quận 1 dưới 5 triệu có máy lạnh`.
2. Frontend calls backend natural-language search.
3. Backend asks AI server to parse intent.
4. AI returns fields such as `propertyType`, `district`, `maxPrice`, `amenities`, `keyword`.
5. Backend builds a JPA Specification and queries MySQL.
6. Backend returns listings.

## Frontend API

### Smart Suggestions

`GET /v1/listings/search-suggestions`

Query params:

| Param | Type | Required | Notes |
|---|---:|---:|---|
| `q` | string | yes | Raw user input |
| `limit` | number | no | Suggested `8`, max backend clamp is `20` |
| `provinceId` | string | no | Optional location scope |
| `categoryId` | number | no | Optional category scope |

Headers:

| Header | Required | Notes |
|---|---:|---|
| `X-Session-Id` | no | Stable anonymous session id for telemetry |

Example:

```http
GET /v1/listings/search-suggestions?q=phong%20q1%205tr&limit=8
X-Session-Id: sess_abc123
```

Example response:

```json
{
  "code": 1000,
  "data": {
    "suggestions": [
      {
        "type": "AI_INTENT",
        "text": "phòng trọ quận 1 dưới 5 triệu",
        "listingId": null,
        "score": 1.15,
        "metadata": { "matchType": "AI_INTENT" }
      },
      {
        "type": "TYPO_CORRECTION",
        "text": "căn hộ quận 1 giá dưới 5 triệu",
        "listingId": null,
        "score": 1.1,
        "metadata": { "matchType": "LOCAL_DICTIONARY" }
      }
    ],
    "queryNorm": "phong quan 1 5 trieu",
    "impressionId": 9876
  }
}
```

Suggestion `type` values:

- `TITLE`: real listing title
- `LOCATION`: province/district/ward
- `POPULAR_QUERY`: popular clicked query
- `TYPO_CORRECTION`: local typo/abbreviation dictionary
- `PHONETIC`: similar-sounding match
- `AI_INTENT`: AI-normalized intent phrase

### Suggestion Click Telemetry

`POST /v1/listings/search-suggestions/click`

Send this only when the user chooses a suggestion.

```json
{
  "impressionId": 9876,
  "type": "AI_INTENT",
  "text": "phòng trọ quận 1 dưới 5 triệu",
  "listingId": null,
  "rank": 0
}
```

This improves `POPULAR_QUERY` ranking over time.

### Natural-Language Search

`POST /api/v1/advanced-search?page=0&size=20`

```json
{
  "query": "tìm phòng trọ quận 1 dưới 5 triệu có máy lạnh"
}
```

Backend calls AI server:

```json
{
  "propertyType": "ROOM",
  "listingType": "RENT",
  "district": "1",
  "maxPrice": 5000000,
  "keyword": "máy lạnh",
  "phoneticKeyword": "may lanh"
}
```

Then Spring Boot builds the database query. The frontend does not need to build SQL-like filters for free-text search.

### Structured Search

For filter chips and advanced filter panels, prefer the existing structured endpoint:

`POST /v1/listings/search`

```json
{
  "provinceId": "79",
  "districtId": "760",
  "listingType": "RENT",
  "productType": "ROOM",
  "maxPrice": 5000000,
  "keyword": "máy lạnh",
  "page": 1,
  "size": 20,
  "sortBy": "DEFAULT"
}
```

## React Recommendations

Use a debounce and minimum query length.

```ts
const SUGGESTION_DEBOUNCE_MS = 300;
const MIN_SUGGESTION_LENGTH = 2;
const SUGGESTION_LIMIT = 8;
```

Recommended behavior:

- Call suggestions only when the input length after trim is at least `2`.
- Debounce `250-350ms`.
- Abort previous request with `AbortController`.
- Cache the last few queries in memory for the session.
- Send `X-Session-Id` as a random UUID stored in `localStorage`.
- Do not call AI directly from frontend.
- On submit, use the selected suggestion text or raw input.
- If suggestion type is `LOCATION`, you may apply location filters from `metadata`.
- If suggestion type is `TITLE` and `listingId` exists, you may navigate directly to listing detail or run search with that title.

## Redis Design

Spring Cache already stores smart suggestions:

| Cache | TTL | Purpose |
|---|---:|---|
| `listing.suggestions` | 2 minutes | Full suggestion response for normalized query/scope |

Additional practical Redis key design if you extend this later:

| Key | Type | TTL | Purpose |
|---|---|---:|---|
| `suggest:q:{hash}` | JSON/string | 2m | Suggestion list |
| `ai:parse:{hash}` | JSON/string | 15m-1h | Parsed AI search intent |
| `ai:suggest:{hash}` | JSON/string | 5m-30m | AI suggestion phrases |
| `popular:search:zset` | sorted set | none | Search/click popularity score |
| `hot:keyword:{provinceId}` | sorted set | none | Scoped hot keywords |

Invalidation:

- Suggestion cache can expire naturally.
- Listing create/update does not need immediate cache purge for this project because TTL is short.
- Popularity can be updated from click telemetry and re-read continuously.

## MySQL Indexing

Already present or recommended:

```sql
CREATE FULLTEXT INDEX ft_listings_search_text ON listings(search_text);
CREATE FULLTEXT INDEX ft_listings_title_norm ON listings(title_norm);

CREATE INDEX idx_listing_public_status
ON listings(is_draft, is_shadow, verified, expired);

CREATE INDEX idx_listing_price
ON listings(price);

CREATE INDEX idx_listing_product_type
ON listings(product_type);

CREATE INDEX idx_address_legacy_location
ON addresses(legacy_province_id, legacy_district_id, legacy_ward_id);
```

FULLTEXT is used for keyword/title relevance. B-tree indexes are used for structured filters such as price, product type, status, and location.

## Ranking Strategy

Backend weights:

| Source | Weight | Reason |
|---|---:|---|
| `TITLE` | 1.50 | Real matching listing title is high confidence |
| `AI_INTENT` | 1.15 | Good normalized intent phrase |
| `LOCATION` | 1.00 | Strong filter candidate |
| `TYPO_CORRECTION` | 0.95 | Useful, but slightly less certain |
| `PHONETIC` | 0.90 | Helpful fallback for similar sounds |
| `POPULAR_QUERY` | 0.80 | Good for discovery but less query-specific |

Each source applies a small rank decay so earlier candidates remain stronger.

## AI Prompt Contract

AI parse endpoint:

- Extract only structured filters.
- Return JSON only.
- Never generate SQL.
- Use enum values expected by backend: `ROOM`, `APARTMENT`, `HOUSE`, `STUDIO`, `OFFICE`, `RENT`, `SALE`, `SHARE`.

AI suggestion endpoint:

- Normalize short fragments into complete Vietnamese autocomplete phrases.
- Expand abbreviations.
- Return JSON `{ "suggestions": [...] }`.
- Never query listings.

## Why This Fits A Graduation Project

This design is realistic because it uses tools already in the stack:

- Spring Boot for business logic and query safety
- MySQL FULLTEXT for good-enough search
- Redis for low-latency repeated suggestions
- AI only for small intent extraction/normalization tasks
- No search cluster or background indexing infrastructure

It gives a modern “smart search” demo while staying maintainable and explainable in a thesis defense.

## Roadmap

1. Use `GET /v1/listings/search-suggestions` in the React search box.
2. Post click telemetry when a suggestion is selected.
3. Use `POST /api/v1/advanced-search` for free-text submit.
4. Use `POST /v1/listings/search` for filter panels.
5. Seed or grow popular searches through telemetry.
6. Add more canonical Vietnamese phrases and abbreviations as real users test the app.
7. Add cache metrics and slow-query logging before demo day.

