# Smart Search & Auto-Applied Filters

How free-text queries like **`trọ tân bình dưới 5tr`** become structured filters,
and how the frontend consumes them. No Elasticsearch, no AI on every keystroke.

---

## 1. The two endpoints

| Endpoint | Purpose | AI? |
|---|---|---|
| `GET /v1/listings/search-suggestions?q=...` | Autocomplete dropdown while typing | Local first, AI only as enrichment |
| `POST /api/v1/advanced-search` | Run a full natural-language search | AI parse, **local parser fallback** |
| `POST /v1/listings/search` | The actual filtered listing query | None (pure JPA) |

The dropdown does **not** run the search. It returns suggestions **plus** an
`appliedFilters` object the frontend uses to call `POST /v1/listings/search`
with filters already filled in.

---

## 2. Local query parser (`SearchQueryParser`)

Every query is parsed in-process — regex + small dictionaries, no AI:

```
"trọ tân bình dưới 5tr"
   → normalize + expand abbreviations  → "tro tan binh duoi 5 trieu"
   → price       : "duoi 5 trieu"      → maxPrice = 5_000_000
   → propertyType: "tro"               → ROOM ("phòng trọ")
   → location    : leftover            → "tan binh"
   → result      : { productType:ROOM, maxPrice:5000000, locationText:"tan binh" }
```

Handled normalizations:

- Abbreviations: `q1 → quận 1`, `dhqg → đại học quốc gia`, `5tr → 5 triệu`,
  `hcm → hồ chí minh`, `full nt → full nội thất`.
- Price: `dưới X`, `trên X`, `từ A đến B`, lone `X triệu`, units `tr / triệu / k / nghìn / tỷ`.
- Property type, listing type (`thuê/bán/ở ghép`), and a small amenity dictionary.

This is the fix for the original bug: previously the **whole sentence** was
matched as one string against title FULLTEXT and location names, so a query
mixing type + city + price matched nothing. Now the location token is isolated
before matching.

---

## 3. Suggestion response contract

`GET /v1/listings/search-suggestions?q=trọ tân bình dưới 5tr`

```json
{
  "code": 1000,
  "data": {
    "suggestions": [
      {
        "type": "AI_INTENT",
        "text": "phòng trọ Tân Bình, TP. Hồ Chí Minh dưới 5 triệu",
        "score": 2.5,
        "metadata": {
          "matchType": "PARSED_QUERY",
          "appliedFilters": {
            "productType": "ROOM",
            "maxPrice": 5000000,
            "locationText": "tan binh",
            "legacyProvinceId": 79,
            "legacyDistrictId": 766
          }
        }
      },
      { "type": "LOCATION", "text": "Tân Bình, TP. Hồ Chí Minh", "metadata": { "legacyDistrictId": 766 } }
    ],
    "queryNorm": "phong tro tan binh duoi 5 trieu",
    "impressionId": 9876,
    "appliedFilters": {
      "productType": "ROOM",
      "maxPrice": 5000000,
      "locationText": "tan binh",
      "legacyProvinceId": 79,
      "legacyDistrictId": 766
    }
  }
}
```

- The **first** suggestion (`matchType: PARSED_QUERY`) is the synthesized
  "click to search with these filters" item. It is always present when the
  query has any recognisable filter intent, even if the AI server is down.
- Top-level `data.appliedFilters` mirrors it for convenience. `null` when the
  query has no filter intent.
- When a real location matched, `legacyProvinceId` / `legacyDistrictId` /
  `legacyWardId` are filled so the frontend can filter exactly.

---

## 4. Frontend integration

### Debounce
- Min query length: **2 chars** (server returns `[]` below this).
- Debounce keystrokes **250–300 ms**. Do not call on every keystroke.
- Cancel the in-flight request when the user keeps typing.

### Applying filters on submit

When the user picks the `PARSED_QUERY` suggestion (or presses Enter):

```js
const f = data.appliedFilters || {};
await fetch('/v1/listings/search', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    productType: f.productType,                 // "ROOM"
    maxPrice:    f.maxPrice,                     // 5000000
    minPrice:    f.minPrice,
    provinceId:  f.legacyProvinceId?.toString(), // "79"
    districtId:  f.legacyDistrictId,             // 766
    keyword:     f.legacyDistrictId ? undefined : f.locationText,
    page: 0, size: 20, sortBy: 'DEFAULT'
  })
});
```

Rule of thumb: if `legacyDistrictId`/`legacyProvinceId` is present, filter by
id; otherwise fall back to `keyword = locationText`.

When the user picks a plain `LOCATION` suggestion, use its `metadata` ids the
same way (no productType/price in that case).

---

## 5. Fallback behaviour (AI unavailable)

| State | Dropdown | Auto-apply |
|---|---|---|
| AI server up + LLM configured | Local sources + AI enrichment | ✅ via `appliedFilters` |
| AI server up, no LLM key | Local sources + synthesized query | ✅ (local parser) |
| AI server down | Local sources + synthesized query | ✅ (local parser) |
| `POST /api/v1/advanced-search`, AI down | — | ✅ local parser fills criteria (no longer dumps raw query into FULLTEXT) |

The system degrades gracefully: filters are always derived locally, the AI
only improves phrasing/recall when reachable.

---

## 6. Config

The search AI client (`AiServerClient`) and the rest of the AI integration now
read the same base URL:

```
AI_SERVICE_BASE_URL=http://your-ai-host:8000
```

Previously the search client read an undefined `ai.server.url` and silently
defaulted to `localhost:8000`, which is why the AI fallback never worked in
deployed environments.

---

## 7. Known follow-ups (not blocking)

- `ListingSpecification.matchesCriteria` matches a numeric district from the AI
  (`"1"`) with `LIKE '%1%'`, which also matches Quận 10/11/12. Prefer mapping
  numeric districts to `"quận N"` or resolving to `legacyDistrictId`.
- `locationText` is matched as a district by the no-AI advanced-search
  fallback. Province/ward-only queries are best-effort there; the suggestion
  endpoint resolves the correct tier and exposes the ids.