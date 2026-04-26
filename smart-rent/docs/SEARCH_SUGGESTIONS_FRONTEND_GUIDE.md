# Search Suggestions API — Frontend Integration Guide

> **Backend version**: implemented in SmartRent API v1 (Spring Boot 3.4)
> **Last updated**: 2026-04-26

---

## Table of Contents

1. [Overview](#overview)
2. [Endpoints](#endpoints)
   - [GET /v1/listings/search-suggestions](#get-v1listingssearch-suggestions)
   - [POST /v1/listings/search-suggestions/click](#post-v1listingssearch-suggestionsclick)
3. [Response Schema](#response-schema)
4. [SuggestionType Enum](#suggestiontype-enum)
5. [Metadata Fields by Type](#metadata-fields-by-type)
6. [Integration Cookbook](#integration-cookbook)
   - [React hook with debounce](#react-hook-with-debounce)
   - [Rendering by type](#rendering-by-type)
   - [Click telemetry](#click-telemetry)
7. [Caching Behaviour](#caching-behaviour)
8. [Error Handling](#error-handling)
9. [Example cURL](#example-curl)
10. [Full Response Example](#full-response-example)

---

## Overview

The **Search Suggestions** endpoint (`GET /v1/listings/search-suggestions`) returns a ranked list of up to 20 suggestions sourced from three places:

| Source | Type constant | Description |
|---|---|---|
| Listing title prefix match | `TITLE` | Listing titles that start with the normalized query. Links directly to a listing. |
| Location name match | `LOCATION` | Province, district, or ward names matching the query. Use to pre-fill the location filter. |
| Popular search terms | `POPULAR_QUERY` | Historically popular queries clicked by real users. Grows over time as click data accumulates. |

Results are **pre-sorted by relevance score** (descending).  
The endpoint is **public** — no authentication header required.

> **Tip**: This endpoint is designed for the search input dropdown. The existing `GET /v1/listings/autocomplete` endpoint (title-only, returns full listing cards) remains unchanged for backward compatibility.

---

## Endpoints

### `GET /v1/listings/search-suggestions`

Returns a merged, ranked suggestion list.

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `q` | string | **yes** | — | Raw search query. Minimum 2 characters after normalization. |
| `limit` | integer | no | `8` | Maximum suggestions to return. Clamped to **[1, 20]**. |
| `provinceId` | string | no | `null` | Legacy province ID (numeric string, e.g. `"79"` for TP.HCM) to scope title and location results. |
| `categoryId` | integer | no | `null` | Category ID to scope title results. |

#### Request Headers

| Header | Required | Description |
|---|---|---|
| `X-Session-Id` | no | Opaque session/device token (max 64 chars). Pass the same value across multiple suggestion requests and click events for better analytics correlation. |

#### Response: `200 OK`

```json
{
  "code": 1000,
  "data": {
    "suggestions": [ ... ],
    "queryNorm": "can ho quan 1",
    "impressionId": 9876
  }
}
```

---

### `POST /v1/listings/search-suggestions/click`

Record that the user selected a suggestion from the dropdown.

> **Call this immediately** when the user clicks/taps a suggestion, **before** navigating to the listing or applying the location filter.  
> The request is fire-and-forget — do not await the response before navigating.

#### Request Body (JSON)

```json
{
  "impressionId": 9876,
  "type": "TITLE",
  "text": "Căn hộ 2PN Quận 1 full nội thất",
  "listingId": 12345,
  "rank": 0
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `impressionId` | long | no (send `0` if unknown) | The `impressionId` from the suggestions response. |
| `type` | string | **yes** | One of `TITLE`, `LOCATION`, `POPULAR_QUERY`. |
| `text` | string | **yes** | Exact display text of the clicked suggestion. |
| `listingId` | long | no (null for non-TITLE) | Listing ID for `TITLE` suggestions. |
| `rank` | integer | **yes** | 0-indexed position of the suggestion in the displayed list. |

#### Response: `200 OK`

```json
{ "code": 1000, "data": null }
```

---

## Response Schema

### `SearchSuggestionsResponse`

| Field | Type | Description |
|---|---|---|
| `suggestions` | `SearchSuggestionItem[]` | Ordered list of suggestions (pre-sorted by score desc). |
| `queryNorm` | string | Normalized form of the raw query (useful for debug). |
| `impressionId` | long | Telemetry handle. Pass back in the click event. |

### `SearchSuggestionItem`

| Field | Type | Nullable | Description |
|---|---|---|---|
| `type` | `SuggestionType` | no | Source type: `TITLE`, `LOCATION`, or `POPULAR_QUERY`. |
| `text` | string | no | Human-readable display text (original, not normalized). |
| `listingId` | long | **yes** | Populated **only** for `TITLE` suggestions. |
| `score` | double | no | Relevance score (higher = more relevant). For display transparency only. |
| `metadata` | `object` | **yes** | Type-specific extra data (see below). |

---

## SuggestionType Enum

```typescript
type SuggestionType = 'TITLE' | 'LOCATION' | 'POPULAR_QUERY';
```

| Value | Meaning | Action on select |
|---|---|---|
| `TITLE` | Listing title prefix match | Navigate to listing detail page using `listingId`. |
| `LOCATION` | Province/district/ward name | Pre-fill the location filter and trigger a new search. |
| `POPULAR_QUERY` | Historically popular search term | Fill the search input with `text` and trigger a search. |

---

## Metadata Fields by Type

### `TITLE`

```json
{
  "address": "123 Nguyễn Trãi, Quận 1, TP. Hồ Chí Minh"
}
```

| Key | Type | Description |
|---|---|---|
| `address` | string | Display address of the listing. |

### `LOCATION`

```json
{
  "provinceName": "TP. Hồ Chí Minh",
  "districtName": "Quận 1",
  "wardName": "Phường Bến Nghé"
}
```

| Key | Type | Nullable | Description |
|---|---|---|---|
| `provinceName` | string | no | Province name. |
| `districtName` | string | no | District name. |
| `wardName` | string | **yes** | Ward name (only when the match is at ward level). |

### `POPULAR_QUERY`

```json
{
  "hitCount": 42
}
```

| Key | Type | Description |
|---|---|---|
| `hitCount` | long | Number of times this query was clicked in the last 7 days. |

---

## Integration Cookbook

### React hook with debounce

```tsx
import { useState, useEffect, useRef } from 'react';

interface Suggestion {
  type: 'TITLE' | 'LOCATION' | 'POPULAR_QUERY';
  text: string;
  listingId: number | null;
  score: number;
  metadata: Record<string, unknown> | null;
}

interface SuggestionsResponse {
  suggestions: Suggestion[];
  queryNorm: string;
  impressionId: number;
}

const SESSION_ID = crypto.randomUUID(); // generate once per page load

function useSearchSuggestions(query: string, provinceId?: string) {
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [impressionId, setImpressionId] = useState(0);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    clearTimeout(debounceRef.current);

    if (!query || query.trim().length < 2) {
      setSuggestions([]);
      return;
    }

    debounceRef.current = setTimeout(async () => {
      try {
        const params = new URLSearchParams({ q: query, limit: '8' });
        if (provinceId) params.set('provinceId', provinceId);

        const res = await fetch(
          `/v1/listings/search-suggestions?${params}`,
          { headers: { 'X-Session-Id': SESSION_ID } }
        );
        const body = await res.json();

        const data: SuggestionsResponse = body.data;
        setSuggestions(data.suggestions);
        setImpressionId(data.impressionId);
      } catch (err) {
        console.error('Search suggestions failed:', err);
        setSuggestions([]);
      }
    }, 200); // 200 ms debounce

    return () => clearTimeout(debounceRef.current);
  }, [query, provinceId]);

  return { suggestions, impressionId };
}
```

### Rendering by type

```tsx
function SuggestionIcon({ type }: { type: string }) {
  switch (type) {
    case 'TITLE':         return <span>🏠</span>;
    case 'LOCATION':      return <span>📍</span>;
    case 'POPULAR_QUERY': return <span>🔥</span>;
    default:              return null;
  }
}

function SuggestionList({
  suggestions,
  impressionId,
  onSelect,
}: {
  suggestions: Suggestion[];
  impressionId: number;
  onSelect: (s: Suggestion, rank: number) => void;
}) {
  return (
    <ul role="listbox">
      {suggestions.map((s, idx) => (
        <li
          key={`${s.type}-${idx}`}
          role="option"
          onClick={() => onSelect(s, idx)}
        >
          <SuggestionIcon type={s.type} />
          <span>{s.text}</span>
          {s.metadata?.address && (
            <small>{String(s.metadata.address)}</small>
          )}
        </li>
      ))}
    </ul>
  );
}
```

### Click telemetry

```tsx
async function recordSuggestionClick(
  suggestion: Suggestion,
  rank: number,
  impressionId: number
) {
  // fire-and-forget — do NOT await before navigating
  fetch('/v1/listings/search-suggestions/click', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      impressionId,
      type:      suggestion.type,
      text:      suggestion.text,
      listingId: suggestion.listingId ?? null,
      rank,
    }),
  }).catch(() => {}); // swallow errors — telemetry must never block UX
}

// Usage inside onSelect handler:
function handleSelect(suggestion: Suggestion, rank: number) {
  recordSuggestionClick(suggestion, rank, impressionId); // fire-and-forget

  if (suggestion.type === 'TITLE' && suggestion.listingId) {
    router.push(`/listings/${suggestion.listingId}`);
  } else if (suggestion.type === 'LOCATION') {
    applyLocationFilter(suggestion.metadata);
    triggerSearch();
  } else {
    // POPULAR_QUERY — fill input and search
    setSearchQuery(suggestion.text);
    triggerSearch();
  }
}
```

---

## Caching Behaviour

| Aspect | Detail |
|---|---|
| Cache TTL | **2 minutes** (configurable via `spring.cache.redis.expires.[listing.suggestions]`) |
| Cache key dimensions | Normalized query + `provinceId` + `categoryId` + `limit` |
| Cache invalidation | TTL-based expiry only — no manual eviction |
| Impression recording | Recorded on **every** API call regardless of cache hit |

> Because the response is cached, two users with the same query within 2 minutes receive the same `impressionId`. This is by design — impression granularity is per-query-shape, not per-user-request.

---

## Error Handling

The endpoint always returns `200 OK`.  Empty suggestions are represented as an empty array — **never** a 4xx/5xx.

| Scenario | Backend behaviour | Frontend should… |
|---|---|---|
| Query < 2 chars after normalization | Returns `suggestions: []` | Hide the dropdown |
| No candidates found | Returns `suggestions: []` | Hide the dropdown or show "No results" |
| Redis unavailable (cache miss) | Falls back to live DB query | Nothing special — transparent to frontend |
| Telemetry write fails | Silently swallowed, `impressionId: 0` returned | Nothing special |
| Location DB query too slow | Returns TITLE + POPULAR_QUERY only (location source fails gracefully) | Nothing special |

---

## Example cURL

```bash
# Basic query
curl "https://api.smartrent.io.vn/v1/listings/search-suggestions?q=c%C4%83n+h%E1%BB%99&limit=8"

# Scoped to TP.HCM (provinceId=79) with session ID
curl "https://api.smartrent.io.vn/v1/listings/search-suggestions?q=can+ho&limit=8&provinceId=79" \
  -H "X-Session-Id: sess_abc123"

# Record a click
curl -X POST "https://api.smartrent.io.vn/v1/listings/search-suggestions/click" \
  -H "Content-Type: application/json" \
  -d '{
    "impressionId": 9876,
    "type": "TITLE",
    "text": "Căn hộ 2PN Quận 1 full nội thất",
    "listingId": 12345,
    "rank": 0
  }'
```

---

## Full Response Example

```json
{
  "code": 1000,
  "message": null,
  "data": {
    "suggestions": [
      {
        "type": "TITLE",
        "text": "Căn hộ 2PN Quận 1 full nội thất, view sông",
        "listingId": 12345,
        "score": 1.5,
        "metadata": {
          "address": "123 Nguyễn Trãi, Quận 1, TP. Hồ Chí Minh"
        }
      },
      {
        "type": "TITLE",
        "text": "Căn hộ studio Quận 1 giá rẻ gần trung tâm",
        "listingId": 67890,
        "score": 1.45,
        "metadata": {
          "address": "456 Lê Lai, Quận 1, TP. Hồ Chí Minh"
        }
      },
      {
        "type": "LOCATION",
        "text": "Quận 1, TP. Hồ Chí Minh",
        "listingId": null,
        "score": 1.0,
        "metadata": {
          "provinceName": "TP. Hồ Chí Minh",
          "districtName": "Quận 1"
        }
      },
      {
        "type": "LOCATION",
        "text": "Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
        "listingId": null,
        "score": 0.95,
        "metadata": {
          "provinceName": "TP. Hồ Chí Minh",
          "districtName": "Quận 1",
          "wardName": "Phường Bến Nghé"
        }
      },
      {
        "type": "POPULAR_QUERY",
        "text": "căn hộ 2 phòng ngủ quận 1",
        "listingId": null,
        "score": 0.8,
        "metadata": {
          "hitCount": 42
        }
      }
    ],
    "queryNorm": "can ho quan 1",
    "impressionId": 9876
  }
}
```
