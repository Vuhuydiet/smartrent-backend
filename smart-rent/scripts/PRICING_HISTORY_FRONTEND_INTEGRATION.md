# Pricing History — Frontend Integration Guide

## Base URL
```
http://localhost:8080
```

---

## Endpoints

### 1. GET full history
```
GET /v1/listings/:listingId/pricing-history
Authorization: not required
```

Response — `ApiResponse<PriceHistory[]>` sorted **oldest → newest** (ASC):
```json
{
  "code": "999999",
  "message": null,
  "data": [
    {
      "id": 1,
      "listingId": 42,
      "oldPrice": null,
      "newPrice": 6500000,
      "oldPriceUnit": null,
      "newPriceUnit": "MONTH",
      "changeType": "INITIAL",
      "changePercentage": 0,
      "changeAmount": 0,
      "changedBy": "00000000-test-0001-0000-000000000001",
      "changeReason": "Initial listing price",
      "changedAt": "2022-03-01T08:00:00",
      "current": false
    },
    {
      "id": 2,
      "listingId": 42,
      "oldPrice": 6500000,
      "newPrice": 7000000,
      "oldPriceUnit": "MONTH",
      "newPriceUnit": "MONTH",
      "changeType": "INCREASE",
      "changePercentage": 7.69,
      "changeAmount": 500000,
      "changedBy": "00000000-test-0001-0000-000000000001",
      "changeReason": "Annual market rate adjustment Q1 2023",
      "changedAt": "2023-01-10T09:00:00",
      "current": true
    }
  ]
}
```

---

### 2. GET history by date range
```
GET /v1/listings/:listingId/pricing-history/date-range?startDate=&endDate=
Authorization: not required
```

Query params — ISO 8601 datetime (no Z suffix needed):
```
startDate=2023-01-01T00:00:00
endDate=2025-12-31T23:59:59
```

Response — same shape as `PriceHistory[]` above, filtered & sorted ASC.

---

### 3. GET current price
```
GET /v1/listings/:listingId/current-price
Authorization: not required
```

Response — `ApiResponse<PriceHistory>` (the single row with `current: true`):
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "id": 5,
    "listingId": 42,
    "oldPrice": 8000000,
    "newPrice": 8500000,
    "oldPriceUnit": "MONTH",
    "newPriceUnit": "MONTH",
    "changeType": "INCREASE",
    "changePercentage": 6.25,
    "changeAmount": 500000,
    "changedBy": "00000000-test-0001-0000-000000000001",
    "changeReason": "Mid-year 2024 adjustment",
    "changedAt": "2024-07-01T08:00:00",
    "current": true
  }
}
```

---

### 4. GET price statistics
```
GET /v1/listings/:listingId/price-statistics
Authorization: not required
```

Response — `ApiResponse<PriceStatistics>`:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "minPrice": 6500000,
    "maxPrice": 8500000,
    "avgPrice": 7480000,
    "totalChanges": 4,
    "priceIncreases": 4,
    "priceDecreases": 0
  }
}
```

> **Note:** `totalChanges` = total records − 1 (excludes the INITIAL row).

---

### 5. PUT update price  *(owner only — requires JWT)*
```
PUT /v1/listings/:listingId/price
Authorization: Bearer <token>
Content-Type: application/json
```

Request body:
```json
{
  "newPrice": 9000000,
  "priceUnit": "MONTH",
  "changeReason": "Q2 market adjustment",
  "effectiveAt": "2025-04-15T10:00:00+07:00"
}
```

- `priceUnit` — optional. If omitted, inherits the listing's current unit.
- `effectiveAt` — optional ISO 8601 (with or without offset). If omitted, uses server time.

Response — `ApiResponse<PriceHistory>` (the newly created record):
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "id": 6,
    "listingId": 42,
    "oldPrice": 8500000,
    "newPrice": 9000000,
    "oldPriceUnit": "MONTH",
    "newPriceUnit": "MONTH",
    "changeType": "INCREASE",
    "changePercentage": 5.88,
    "changeAmount": 500000,
    "changedBy": "user-uuid-here",
    "changeReason": "Q2 market adjustment",
    "changedAt": "2025-04-15T10:00:00",
    "current": true
  }
}
```

---

## Field reference

| Field | Type | Notes |
|---|---|---|
| `id` | `number` | Auto-increment PK |
| `listingId` | `number` | FK to listing |
| `oldPrice` | `number \| null` | `null` for INITIAL record |
| `newPrice` | `number` | Always present |
| `oldPriceUnit` | `"MONTH" \| "DAY" \| "YEAR" \| null` | `null` for INITIAL |
| `newPriceUnit` | `"MONTH" \| "DAY" \| "YEAR"` | Always present |
| `changeType` | `string` | See values below |
| `changePercentage` | `number` | `0` for INITIAL / UNIT_CHANGE |
| `changeAmount` | `number` | `newPrice - oldPrice`, `0` for INITIAL |
| `changedBy` | `string` | User UUID |
| `changeReason` | `string \| null` | Free text |
| `changedAt` | `string` | ISO 8601, no timezone (`"2024-07-01T08:00:00"`) |
| `current` | `boolean` | `true` only on the most-recent record |

### `changeType` values
| Value | Meaning |
|---|---|
| `INITIAL` | First price when listing was created |
| `INCREASE` | `newPrice > oldPrice`, same unit |
| `DECREASE` | `newPrice < oldPrice`, same unit |
| `UNIT_CHANGE` | Price unit changed (e.g. DAY → MONTH) |
| `CORRECTION` | Same price, same unit (manual correction) |

---

## Error responses

| HTTP | `code` | When |
|---|---|---|
| 404 / 500 | `"999998"` or Spring default | Listing not found |
| 500 | Spring default | No pricing history exists for listing |
| 400 | Spring validation | `newPrice` missing or ≤ 0 |
| 401 | Spring security | PUT endpoint called without JWT |

> If `price-statistics` returns an error (listing has no history), the frontend chart should fall back to computing stats from the history array directly.

---

## Quick curl tests

```bash
# 1. Full history (replace 1 with a real listingId from your DB)
curl http://localhost:8080/v1/listings/1/pricing-history | jq .

# 2. Date-range filter
curl "http://localhost:8080/v1/listings/1/pricing-history/date-range?startDate=2023-01-01T00:00:00&endDate=2025-12-31T23:59:59" | jq .

# 3. Current price
curl http://localhost:8080/v1/listings/1/current-price | jq .

# 4. Statistics
curl http://localhost:8080/v1/listings/1/price-statistics | jq .

# 5. Update price (needs real JWT)
curl -X PUT http://localhost:8080/v1/listings/1/price \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{"newPrice": 9000000, "changeReason": "Test update"}'
```

---

## How to get listing IDs with seeded pricing data

Run this on your local DB after executing `populate_pricing_history_local.sql`:

```sql
SELECT
    ph.listing_id,
    COUNT(*) AS records,
    MIN(ph.new_price) AS min_price,
    MAX(ph.new_price) AS max_price,
    MAX(ph.changed_at) AS last_changed
FROM pricing_histories ph
GROUP BY ph.listing_id
HAVING COUNT(*) > 1
ORDER BY records DESC
LIMIT 10;
```

---

## `changedAt` timezone note

Backend stores and returns `LocalDateTime` (no timezone). Serialized as `"2024-07-01T08:00:00"` (no `Z`).  
For chart display, treat as local time (UTC+7 / Vietnam time). If you need to parse to JS `Date`:
```ts
new Date(record.changedAt)          // works but assumes local TZ
new Date(record.changedAt + 'Z')    // forces UTC parse — avoid unless backend changes to UTC
new Date(record.changedAt + '+07:00') // explicit Vietnam time — safest for local env
```
