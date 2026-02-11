# Listing Disabled Feature — FE Integration Guide

## Overview

When an admin **resolves** a listing report (status = `RESOLVED`), the reported listing is automatically **disabled**. The listing owner receives an email notification. Disabled listings are hidden from public views but remain visible to the **owner** and **admin**.

---

## New Listing Status: `DISABLED`

A new value `"DISABLED"` is added to the `listingStatus` field in listing responses.

**All possible `listingStatus` values:**
```
EXPIRED | EXPIRING_SOON | DISPLAYING | IN_REVIEW | PENDING_PAYMENT | REJECTED | VERIFIED | DISABLED
```

---

## API Behavior Changes

### 1. Public Endpoints — Disabled listings are HIDDEN

These endpoints automatically **exclude** disabled listings. No FE changes needed for filtering.

| Endpoint | Method | Description |
|---|---|---|
| `POST /v1/listings/search` | POST | Public search — disabled listings excluded |
| `GET /v1/listings` | GET | Public list — disabled listings filtered out |
| `GET /v1/listings/{id}` | GET | Returns **404** if listing is disabled |
| `POST /v1/listings/map-bounds` | POST | Map view — disabled listings excluded |

### 2. Owner Endpoints — Disabled listings are VISIBLE

Owners can see their own disabled listings with status `"DISABLED"`.

| Endpoint | Method | Description |
|---|---|---|
| `POST /v1/listings/my-listings` | POST | Owner's listings — includes disabled with `listingStatus: "DISABLED"` |
| `GET /v1/listings/{id}/my-detail` | GET | Owner detail — includes `disabledAt`, `disabledReason` |

### 3. Admin Endpoints — Full disabled info

Admins see all listings including disabled ones with full audit fields.

| Endpoint | Method | Description |
|---|---|---|
| `POST /v1/listings/admin/list` | POST | Admin list — includes all disabled fields |
| `GET /v1/listings/{id}/admin` | GET | Admin detail — includes all disabled fields |
| `PUT /v1/admin/reports/{reportId}/resolve` | PUT | Resolving with `RESOLVED` disables the listing |

---

## New Response Fields

### `ListingResponse` (public + owner base)

```json
{
  "listingId": 123,
  "listingStatus": "DISABLED",
  "disabled": true,
  ...
}
```

| Field | Type | Description |
|---|---|---|
| `disabled` | `Boolean` | `true` if listing is disabled by admin |
| `listingStatus` | `String` | Will be `"DISABLED"` when listing is disabled |

### `ListingResponseForOwner` (owner detail — `GET /v1/listings/{id}/my-detail`)

```json
{
  "listingId": 123,
  "listingStatus": "DISABLED",
  "disabled": true,
  "disabledAt": "2026-02-11T10:30:00",
  "disabledReason": "Listing contains misleading information about the property",
  ...
}
```

| Field | Type | Description |
|---|---|---|
| `disabledAt` | `DateTime` | When the listing was disabled |
| `disabledReason` | `String` | Admin's reason/notes for disabling |

### `ListingResponseWithAdmin` (admin — `GET /v1/listings/{id}/admin`, `POST /v1/listings/admin/list`)

```json
{
  "listingId": 123,
  "listingStatus": "DISABLED",
  "disabled": true,
  "disabledAt": "2026-02-11T10:30:00",
  "disabledBy": "admin-uuid-123",
  "disabledReason": "Listing contains misleading information about the property",
  ...
}
```

| Field | Type | Description |
|---|---|---|
| `disabled` | `Boolean` | `true` if disabled |
| `disabledAt` | `DateTime` | When disabled |
| `disabledBy` | `String` | Admin ID who disabled |
| `disabledReason` | `String` | Reason for disabling |

---

## Resolve Report Endpoint (triggers disable)

**`PUT /v1/admin/reports/{reportId}/resolve`**

**Request:**
```json
{
  "status": "RESOLVED",
  "adminNotes": "Listing contains misleading information"
}
```

- `status`: `"RESOLVED"` → disables listing + sends email to owner. `"REJECTED"` → no action on listing.
- `adminNotes`: Becomes the `disabledReason` on the listing.

**Response:** `ListingReportResponse` (unchanged shape)

---

## Filter by DISABLED Status

### Owner: filter own disabled listings
```json
POST /v1/listings/my-listings
{
  "listingStatus": "DISABLED",
  "page": 1,
  "size": 20
}
```

### Admin: filter all disabled listings
```json
POST /v1/listings/admin/list
X-Admin-Id: admin-uuid
{
  "listingStatus": "DISABLED",
  "page": 1,
  "size": 20
}
```

---

## FE Implementation Checklist

### Owner Dashboard
- [ ] Show `"DISABLED"` badge/tag when `listingStatus === "DISABLED"` (use red/warning color)
- [ ] Display `disabledAt` and `disabledReason` on listing detail page
- [ ] Add "Disabled" tab/filter option in my-listings page using `listingStatus: "DISABLED"`
- [ ] Show informational message: listing was disabled due to a report violation

### Admin Dashboard
- [ ] Show disabled listings in admin listing list with `disabled: true` indicator
- [ ] Display full audit info: `disabledAt`, `disabledBy`, `disabledReason`
- [ ] When resolving a report with `RESOLVED`, show confirmation that the listing will be disabled
- [ ] Add "Disabled" filter option in admin listing management using `listingStatus: "DISABLED"`

### Public Pages
- [ ] Handle 404 gracefully when accessing a disabled listing by ID (show "Listing not found" page)
- [ ] No other changes needed — disabled listings are automatically excluded from search/list/map

