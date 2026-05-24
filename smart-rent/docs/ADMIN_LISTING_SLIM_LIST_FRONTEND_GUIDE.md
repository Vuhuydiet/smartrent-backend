# Admin Listing — Slim List + Detail Endpoint (Frontend Integration Guide)

> **Backend base URL**: `https://dev.api.smartrent.io.vn` or `http://localhost:8080`
>
> **Authentication**: All endpoints require the `X-Admin-Id` header.
>
> **Companion guide**: see [ADMIN_LISTING_MODERATION_FRONTEND_GUIDE.md](ADMIN_LISTING_MODERATION_FRONTEND_GUIDE.md) for the moderation workflow.

---

## 1. What changed

The admin listing list endpoint used to return the **full** listing record per row (description, all media, all amenities, full user object, utility prices, etc.). That payload was too heavy for a table view.

| Endpoint | Before | Now |
|---|---|---|
| `POST /v1/listings/admin/list` | Full record per row (`ListingResponseWithAdmin`) | **Slim summary** per row (`AdminListingSummary`) |
| `GET /v1/listings/admin/{id}` | — | **NEW** — returns the full record for one listing |
| `GET /v1/listings/{id}/admin` | Full record | Same full record (kept as **deprecated legacy alias**) |

**Pattern**: list endpoint returns a slim row, then the frontend calls the detail endpoint when the admin opens a single listing.

---

## 2. Endpoints

### 2.1 List — slim summary per row

```http
POST /v1/listings/admin/list
X-Admin-Id: <admin-id>
Content-Type: application/json

{
  "page": 1,
  "size": 20,
  "moderationStatus": "PENDING_REVIEW"
}
```

Request body, filters, statistics, and pagination wrapper are **unchanged** — only the shape of items inside `data.listings[]` changed. See [§3.1](#31-admin-only-filter-fields) for the admin-only filters (title, owner search, date ranges).

### 2.2 Detail — full record (NEW)

```http
GET /v1/listings/admin/{id}
X-Admin-Id: <admin-id>
```

Returns the existing `ListingResponseWithAdmin` payload (description, media[], amenities[], full user object, address, moderation context, admin verification info).

### 2.3 Legacy detail alias (deprecated)

```http
GET /v1/listings/{id}/admin
X-Admin-Id: <admin-id>
```

Same response as 2.2 — kept for backward compatibility. **New code should use 2.2.**

---

## 3. Filter fields

### 3.1 Filter fields

The `POST /v1/listings/admin/list` request body accepts all of these. They mirror what's used by the public search and owner views; the admin table simply uses more of them.

#### 3.1.a Text and exact-match filters

| Field | Type | Description |
|-------|------|-------------|
| `title` | `string` | Case-insensitive substring match on the listing title (not description). Use this for the "search by title" input. |
| `ownerSearch` | `string` | Matches the owner's `firstName`, `lastName`, `contactPhoneNumber`, OR `phoneNumber` (case-insensitive contains). Use a single textbox — accepts a name fragment or a phone number. |
| `verified` | `boolean` | `true` / `false` / omit. |
| `listingType` | `string` | `RENT` / `SALE` / `SHARE`. |
| `productType` | `string` | `ROOM` / `APARTMENT` / `HOUSE` / `OFFICE` / `STUDIO`. |
| `vipType` | `string` | `NORMAL` / `SILVER` / `GOLD` / `DIAMOND`. |
| `moderationStatus` | `string` | `PENDING_REVIEW` / `APPROVED` / `REJECTED` / `REVISION_REQUIRED` / `RESUBMITTED` / `SUSPENDED`. |
| `categoryId` | `number` | Exact match. |
| `bedrooms` | `number` | Exact match. Use `bedroomsRange` for a range. |
| `bathrooms` | `number` | Exact match. Use `bathroomsRange` for a range. |

#### 3.1.b Range filters — single field with `..` separator

**All range filters use the same `from..to` format.** Either side may be omitted for an open-ended range. Empty string / `null` / field omitted = no filter on that bound.

| Field | Value type | Example |
|-------|-----------|---------|
| `price` | VND amount | `"5000000..15000000"` |
| `area` | m² (decimal) | `"30..60"` |
| `bedroomsRange` | integer | `"2..4"` |
| `bathroomsRange` | integer | `"1..3"` |
| `roomCapacity` | integer | `"2..6"` |
| `priceReductionPercent` | percent (decimal) | `"10..50"` |
| `postDate` | `YYYY-MM-DD` | `"2026-03-01..2026-03-31"` |
| `expiryDate` | `YYYY-MM-DD` | `"2026-08-01..2026-08-31"` |

> **Examples of the `..` syntax:**
> - `"5000000..15000000"` — between 5M and 15M VND (inclusive)
> - `"5000000.."` — ≥ 5M VND (no upper bound)
> - `"..15000000"` — ≤ 15M VND (no lower bound)
>
> Dates: server pads lower → start-of-day, upper → end-of-day automatically. **You only ever send `YYYY-MM-DD`, never times.**

#### Request examples

```jsonc
// Title search
{ "page": 1, "size": 20, "title": "Tân Bình" }

// Owner search — by phone or name fragment
{ "page": 1, "size": 20, "ownerSearch": "0367919024" }
{ "page": 1, "size": 20, "ownerSearch": "Phú" }

// Numeric ranges — single field each
{
  "page": 1, "size": 20,
  "price":         "5000000..15000000",
  "area":          "30..60",
  "bedroomsRange": "2..4",
  "bathroomsRange": "1..3"
}

// Open-ended ranges (numeric)
{ "page": 1, "size": 20, "price": "5000000.." }   // ≥ 5M VND
{ "page": 1, "size": 20, "area":  "..60" }         // ≤ 60 m²

// Date ranges
{
  "page": 1, "size": 20,
  "postDate":   "2026-03-01..2026-03-31",
  "expiryDate": "2026-08-01..2026-08-31"
}

// Open-ended date ranges
{ "page": 1, "size": 20, "postDate":   "2026-03-01.." }   // posted on/after Mar 1
{ "page": 1, "size": 20, "expiryDate": "..2026-08-31" }   // expires on/before Aug 31

// Combined
{
  "page": 1, "size": 20,
  "verified": true,
  "listingType": "RENT",
  "productType": "APARTMENT",
  "price":    "5000000..15000000",
  "area":     "30..60",
  "postDate": "2026-04-01..2026-04-30"
}
```

#### TypeScript type

```ts
interface AdminListingFilterFields {
  // Text / exact-match
  title?: string;
  ownerSearch?: string;
  verified?: boolean;
  listingType?: 'RENT' | 'SALE' | 'SHARE';
  productType?: 'ROOM' | 'APARTMENT' | 'HOUSE' | 'OFFICE' | 'STUDIO';
  vipType?: 'NORMAL' | 'SILVER' | 'GOLD' | 'DIAMOND';
  moderationStatus?: 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED'
                   | 'REVISION_REQUIRED' | 'RESUBMITTED' | 'SUSPENDED';
  categoryId?: number;
  bedrooms?: number;         // exact match — use bedroomsRange for a range
  bathrooms?: number;        // exact match — use bathroomsRange for a range

  // Range filters — all use `"from..to"`. Either side may be omitted.
  price?: string;            // "5000000..15000000"
  area?: string;             // "30..60"
  bedroomsRange?: string;    // "2..4"
  bathroomsRange?: string;   // "1..3"
  roomCapacity?: string;     // "2..6"
  priceReductionPercent?: string; // "10..50"
  postDate?: string;         // "2026-03-01..2026-03-31" (YYYY-MM-DD)
  expiryDate?: string;       // "2026-08-01..2026-08-31" (YYYY-MM-DD)

  // Pagination
  page?: number;             // 1-based, default 1
  size?: number;             // default 20, max 100
}
```

#### UI helper — build the range string

Same helper for every range filter (numeric, date — doesn't matter).

```ts
// Returns undefined when both ends are empty so you can spread it conditionally.
function buildRange(from?: string | number, to?: string | number): string | undefined {
  const f = from === undefined || from === null || from === '' ? '' : String(from);
  const t = to   === undefined || to   === null || to   === '' ? '' : String(to);
  if (!f && !t) return undefined;
  return `${f}..${t}`;
}

// Examples
buildRange(5_000_000, 15_000_000);          // "5000000..15000000"
buildRange(5_000_000);                       // "5000000.."
buildRange(undefined, 15_000_000);           // "..15000000"
buildRange('2026-03-01', '2026-03-31');      // "2026-03-01..2026-03-31"
buildRange();                                // undefined
```

---

## 4. Slim list — response shape

See [§5](#5-fields-removed-from-the-list-now-detail-only) for fields that moved to the detail endpoint.


```jsonc
{
  "code": "999999",
  "data": {
    "listings": [
      {
        "listingId": 667419,
        "title": "Cho thuê căn hộ tầng cao, view thoáng Quận Tân Bình - 34m²",
        "user": {
          "firstName": "Trương",
          "lastName": "Quốc Phú",
          "contactPhoneNumber": "0367919024"
        },
        "postDate": "2026-03-24T16:01:34",
        "expiryDate": "2026-08-10T16:17:02",
        "listingType": "SHARE",
        "verified": false,
        "expired": false,
        "listingStatus": "IN_REVIEW",
        "vipType": "DIAMOND",
        "categoryId": 2,
        "productType": "APARTMENT",
        "price": 14200000,
        "priceUnit": "MONTH",
        "area": 34.1,
        "adminVerification": {
          "verifiedAt": "2026-05-24T04:08:12",
          "verificationStatus": "PENDING"
        },
        "moderationStatus": "PENDING_REVIEW",
        "revisionCount": 0,
        "lastModerationReasonCode": null,
        "lastModerationReasonText": null
      }
    ],
    "totalCount": 150,
    "currentPage": 1,
    "pageSize": 20,
    "totalPages": 8,
    "filterCriteria": { /* echoed back */ },
    "statistics": {
      "totalListings": 101810,
      "pendingVerification": 36998,
      "verified": 59164,
      "expired": 1806,
      "rejected": 3848,
      "drafts": 0,
      "shadows": 0,
      "normalListings": 68902,
      "silverListings": 15487,
      "goldListings": 10016,
      "diamondListings": 5605
    }
  }
}
```

### TypeScript interface

```ts
interface OwnerSummary {
  firstName: string | null;
  lastName: string | null;
  contactPhoneNumber: string | null;
}

interface AdminVerificationSummary {
  verifiedAt: string | null;          // "2026-05-24T04:08:12"
  verificationStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'NOT_SUBMITTED';
}

interface AdminListingSummary {
  listingId: number;
  title: string;
  user: OwnerSummary | null;
  postDate: string | null;
  expiryDate: string | null;
  listingType: 'RENT' | 'SALE' | 'SHARE' | null;
  verified: boolean | null;
  expired: boolean | null;
  listingStatus: 'EXPIRED' | 'EXPIRING_SOON' | 'DISPLAYING' | 'IN_REVIEW'
               | 'PENDING_PAYMENT' | 'REJECTED' | 'VERIFIED' | 'RESUBMITTED';
  vipType: 'NORMAL' | 'SILVER' | 'GOLD' | 'DIAMOND' | null;
  categoryId: number | null;
  productType: 'ROOM' | 'APARTMENT' | 'HOUSE' | 'OFFICE' | 'STUDIO' | null;
  price: number | null;
  priceUnit: string | null;
  area: number | null;
  adminVerification: AdminVerificationSummary;
  moderationStatus: 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED'
                  | 'REVISION_REQUIRED' | 'RESUBMITTED' | 'SUSPENDED' | null;
  revisionCount: number | null;
  lastModerationReasonCode: string | null;
  lastModerationReasonText: string | null;
}

interface AdminStatistics {
  totalListings: number;        // total in system (all states) — useful for the top KPI card
  pendingVerification: number;
  verified: number;
  expired: number;
  rejected: number;
  drafts: number;
  shadows: number;
  normalListings: number;
  silverListings: number;
  goldListings: number;
  diamondListings: number;
}

interface AdminListingListResponse {
  listings: AdminListingSummary[];
  totalCount: number;           // count matching the active filter (for pagination)
  currentPage: number;
  pageSize: number;
  totalPages: number;
  filterCriteria: unknown;
  statistics: AdminStatistics;  // global, NOT filtered
}
```

> **Note**: `statistics.totalListings` is the global total across the system, **not** the filtered count. Use `totalCount` (top-level) for the row count matching the current filter, and `statistics.totalListings` for KPI cards.

---

## 5. Fields removed from the list (now detail-only)

If your table was binding to any of these, switch to the detail endpoint when the row is opened:

| Field | Where to get it now |
|-------|---------------------|
| `description` | Detail endpoint |
| `media[]` (full list) | Detail endpoint |
| `amenities[]` | Detail endpoint |
| `user.userId`, `user.phoneCode`, `user.phoneNumber`, `user.email`, `user.isBroker`, `user.brokerVerificationStatus`, `user.contactPhoneVerified` | Detail endpoint |
| `addressId`, `propertyInfo` (full address) | Detail endpoint |
| `bedrooms`, `bathrooms`, `direction`, `furnishing`, `roomCapacity` | Detail endpoint |
| `waterPrice`, `electricityPrice`, `internetPrice`, `serviceFee` | Detail endpoint |
| `createdAt`, `updatedAt`, `updatedBy` | Detail endpoint |
| `adminVerification.adminId`, `adminName`, `adminEmail`, `verificationNotes` | Detail endpoint |

---

## 6. Detail — full response shape

```http
GET /v1/listings/admin/667419
X-Admin-Id: <admin-id>
```

```jsonc
{
  "code": "999999",
  "data": {
    "listingId": 667419,
    "title": "...",
    "description": "...full description...",
    "user": {
      "userId": "00000000-test-0015-0000-000000000015",
      "phoneCode": "+84",
      "phoneNumber": "0367919024",
      "email": "quocphu.truong89@gmail.com",
      "firstName": "Trương",
      "lastName": "Quốc Phú",
      "contactPhoneNumber": "0367919024",
      "contactPhoneVerified": true,
      "isBroker": true,
      "brokerVerificationStatus": "APPROVED"
    },
    "postDate": "...",
    "expiryDate": "...",
    "listingType": "SHARE",
    "verified": false,
    "expired": false,
    "listingStatus": "IN_REVIEW",
    "vipType": "DIAMOND",
    "categoryId": 2,
    "productType": "APARTMENT",
    "price": 14200000,
    "priceUnit": "MONTH",
    "addressId": 492845,
    "area": 34.1,
    "bedrooms": 4,
    "bathrooms": 1,
    "direction": "WEST",
    "furnishing": "UNFURNISHED",
    "roomCapacity": null,
    "waterPrice": "80000",
    "electricityPrice": "3500/kWh",
    "internetPrice": "80000",
    "serviceFee": "500000",
    "amenities": [ /* ... */ ],
    "media": [ /* ... */ ],
    "adminVerification": {
      "adminId": null,
      "adminName": null,
      "adminEmail": null,
      "verifiedAt": "2026-05-24T04:08:12",
      "verificationStatus": "PENDING",
      "verificationNotes": null
    },
    "createdAt": "2026-03-24T16:01:34",
    "updatedAt": "2026-05-24T04:08:12",
    "updatedBy": null,
    "moderationStatus": "PENDING_REVIEW",
    "revisionCount": 0,
    "lastModerationReasonCode": null,
    "lastModerationReasonText": null,
    "propertyInfo": {
      "type": "APARTMENT",
      "area": 34.1,
      "district": null,
      "fullAddress": "Số 164 Đường Điện Biên Phủ, Phường 14, Quận Tân Bình, Thành phố Hồ Chí Minh"
    }
  }
}
```

---

## 7. Fetch helpers

`AdminListingFilterFields` is the type defined in [§3.1](#31-admin-only-filter-fields) — covers all admin filters including the new ones.

```ts
const API = process.env.NEXT_PUBLIC_API_URL;

export async function fetchAdminListings(
  adminId: string,
  filter: AdminListingFilterFields,
): Promise<AdminListingListResponse> {
  const res = await fetch(`${API}/v1/listings/admin/list`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Admin-Id': adminId,
    },
    body: JSON.stringify(filter),
  });
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}

export async function fetchAdminListingDetail(
  adminId: string,
  listingId: number,
): Promise<ListingResponseWithAdmin> {
  const res = await fetch(`${API}/v1/listings/admin/${listingId}`, {
    headers: { 'X-Admin-Id': adminId },
  });
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}
```

---

## 8. Recommended UI pattern

A filter toolbar drives `AdminListingFilterFields`; the table re-fetches on filter change; clicking a row opens the detail drawer.

```tsx
'use client';

import { useEffect, useState } from 'react';

export function AdminListingsTable({ adminId }: { adminId: string }) {
  // ── Filter state ──
  const [title, setTitle] = useState('');
  const [ownerSearch, setOwnerSearch] = useState('');

  // Range inputs: keep each end in its own piece of state, combine at request time.
  const [priceFrom, setPriceFrom] = useState('');
  const [priceTo,   setPriceTo]   = useState('');
  const [areaFrom,  setAreaFrom]  = useState('');
  const [areaTo,    setAreaTo]    = useState('');
  const [bedFrom,   setBedFrom]   = useState('');
  const [bedTo,     setBedTo]     = useState('');
  const [postFrom,  setPostFrom]  = useState('');   // "YYYY-MM-DD"
  const [postTo,    setPostTo]    = useState('');
  const [expFrom,   setExpFrom]   = useState('');
  const [expTo,     setExpTo]     = useState('');

  const [listingType, setListingType] = useState<'RENT' | 'SALE' | 'SHARE' | ''>('');
  const [productType, setProductType] = useState<'ROOM' | 'APARTMENT' | 'HOUSE' | 'OFFICE' | 'STUDIO' | ''>('');
  const [verified,    setVerified]    = useState<'true' | 'false' | ''>('');
  const [pageNum, setPageNum] = useState(1);

  // ── Data state ──
  const [page, setPage] = useState<AdminListingListResponse | null>(null);
  const [openId, setOpenId] = useState<number | null>(null);
  const [detail, setDetail] = useState<ListingResponseWithAdmin | null>(null);

  // Build filter from inputs (omit empty values so the server doesn't see them).
  function buildFilter(): AdminListingFilterFields {
    const price         = buildRange(priceFrom, priceTo);
    const area          = buildRange(areaFrom,  areaTo);
    const bedroomsRange = buildRange(bedFrom,   bedTo);
    const postDate      = buildRange(postFrom,  postTo);
    const expiryDate    = buildRange(expFrom,   expTo);
    return {
      page: pageNum,
      size: 20,
      ...(title.trim()       && { title: title.trim() }),
      ...(ownerSearch.trim() && { ownerSearch: ownerSearch.trim() }),
      ...(price              && { price }),
      ...(area               && { area }),
      ...(bedroomsRange      && { bedroomsRange }),
      ...(postDate           && { postDate }),
      ...(expiryDate         && { expiryDate }),
      ...(listingType        && { listingType }),
      ...(productType        && { productType }),
      ...(verified !== ''    && { verified: verified === 'true' }),
    };
  }

  // List — refetch whenever filters or page change.
  // Debounce the text inputs in production; omitted here for clarity.
  useEffect(() => {
    fetchAdminListings(adminId, buildFilter()).then(setPage);
  }, [adminId, pageNum, title, ownerSearch,
      priceFrom, priceTo, areaFrom, areaTo, bedFrom, bedTo,
      postFrom, postTo, expFrom, expTo,
      listingType, productType, verified]);

  // Detail — fetched only when a row is opened.
  useEffect(() => {
    if (openId == null) { setDetail(null); return; }
    setDetail(null);
    fetchAdminListingDetail(adminId, openId).then(setDetail);
  }, [adminId, openId]);

  return (
    <>
      {/* ── Filter toolbar ── */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
        <input placeholder="Tìm theo tiêu đề"
               value={title} onChange={(e) => { setTitle(e.target.value); setPageNum(1); }} />
        <input placeholder="Tên hoặc SĐT chủ tin"
               value={ownerSearch} onChange={(e) => { setOwnerSearch(e.target.value); setPageNum(1); }} />

        <label>Giá từ <input type="number" min={0} value={priceFrom}
               onChange={(e) => { setPriceFrom(e.target.value); setPageNum(1); }} /></label>
        <label>đến    <input type="number" min={0} value={priceTo}
               onChange={(e) => { setPriceTo(e.target.value);   setPageNum(1); }} /></label>

        <label>Diện tích (m²) từ <input type="number" min={0} step={0.5} value={areaFrom}
               onChange={(e) => { setAreaFrom(e.target.value); setPageNum(1); }} /></label>
        <label>đến                 <input type="number" min={0} step={0.5} value={areaTo}
               onChange={(e) => { setAreaTo(e.target.value);   setPageNum(1); }} /></label>

        <label>Phòng ngủ từ <input type="number" min={0} value={bedFrom}
               onChange={(e) => { setBedFrom(e.target.value); setPageNum(1); }} /></label>
        <label>đến           <input type="number" min={0} value={bedTo}
               onChange={(e) => { setBedTo(e.target.value);   setPageNum(1); }} /></label>

        <label>Đăng từ <input type="date" value={postFrom} onChange={(e) => { setPostFrom(e.target.value); setPageNum(1); }} /></label>
        <label>đến    <input type="date" value={postTo}   onChange={(e) => { setPostTo(e.target.value);   setPageNum(1); }} /></label>

        <label>Hết hạn từ <input type="date" value={expFrom} onChange={(e) => { setExpFrom(e.target.value); setPageNum(1); }} /></label>
        <label>đến       <input type="date" value={expTo}   onChange={(e) => { setExpTo(e.target.value);   setPageNum(1); }} /></label>

        <select value={listingType} onChange={(e) => { setListingType(e.target.value as any); setPageNum(1); }}>
          <option value="">Loại tin (tất cả)</option>
          <option value="RENT">Cho thuê</option>
          <option value="SALE">Bán</option>
          <option value="SHARE">Chia sẻ</option>
        </select>

        <select value={productType} onChange={(e) => { setProductType(e.target.value as any); setPageNum(1); }}>
          <option value="">Loại BĐS (tất cả)</option>
          <option value="ROOM">Phòng trọ</option>
          <option value="APARTMENT">Căn hộ</option>
          <option value="HOUSE">Nhà</option>
          <option value="OFFICE">Văn phòng</option>
          <option value="STUDIO">Studio</option>
        </select>

        <select value={verified} onChange={(e) => { setVerified(e.target.value as any); setPageNum(1); }}>
          <option value="">Xác minh (tất cả)</option>
          <option value="true">Đã xác minh</option>
          <option value="false">Chưa xác minh</option>
        </select>
      </div>

      {/* ── Table ── */}
      {!page ? <div>Loading...</div> : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Title</th>
              <th>Owner</th>
              <th>Status</th>
              <th>Moderation</th>
              <th>VIP</th>
              <th>Price</th>
              <th>Posted</th>
            </tr>
          </thead>
          <tbody>
            {page.listings.map((row) => (
              <tr key={row.listingId} onClick={() => setOpenId(row.listingId)}>
                <td>{row.listingId}</td>
                <td>{row.title}</td>
                <td>
                  {[row.user?.firstName, row.user?.lastName].filter(Boolean).join(' ')}<br />
                  <small>{row.user?.contactPhoneNumber}</small>
                </td>
                <td>{row.listingStatus}</td>
                <td>{row.moderationStatus} {row.revisionCount ? `(rev ${row.revisionCount})` : ''}</td>
                <td>{row.vipType}</td>
                <td>{row.price?.toLocaleString('vi-VN')} {row.priceUnit && `/ ${row.priceUnit}`}</td>
                <td>{row.postDate?.slice(0, 10)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {openId != null && (
        <DetailDrawer
          listingId={openId}
          detail={detail}
          onClose={() => setOpenId(null)}
        />
      )}
    </>
  );
}
```

> **Production tip**: debounce the `title` and `ownerSearch` inputs (~300ms) so each keystroke doesn't fire a request.

---

## 9. Migration checklist for frontend

- [ ] Audit table column bindings — any field listed in §5 must be sourced from detail, not list.
- [ ] Replace any inline media/amenity preview on the list row with a thumbnail-less row or a detail-on-click pattern. The list no longer ships media URLs at all.
- [ ] If the previous list row showed `user.email` or `user.isBroker` — drop the column or fetch the detail.
- [ ] Switch admin detail fetches from `GET /v1/listings/{id}/admin` to `GET /v1/listings/admin/{id}` (the old route still works but is deprecated).
- [ ] Verify the row-click drawer/page now actually fires the detail call.
- [ ] Wire the new admin filters from §3.1: title search, owner-name/phone search, post/expiry date range.
- [ ] **BREAKING — update every range filter call site** (public search, owner my-listings, mobile, admin). The legacy `minPrice`/`maxPrice`/`minArea`/`maxArea`/`minBedrooms`/`maxBedrooms`/`minBathrooms`/`maxBathrooms`/`minRoomCapacity`/`maxRoomCapacity`/`minPriceReductionPercent`/`maxPriceReductionPercent` fields are **gone**. Replace each pair with the corresponding single string field from §3.1.b (`price`, `area`, `bedroomsRange`, `bathroomsRange`, `roomCapacity`, `priceReductionPercent`).
  - The `bedrooms` and `bathrooms` exact-match fields are unchanged — only the range pairs were renamed.

---

## 10. Why this is faster

The list endpoint no longer:

- Prefetches amenities for the page (`findByIdsWithAmenities`).
- Batch-loads the `Admin` entities for `updatedBy`.
- Serializes the full `user{}` object, media list, amenity list, full address, or description for each row.

For a typical 20-row page, this drops **2 SQL queries** and shrinks the JSON payload by ~70–90% depending on description and media counts.

---

## 11. Quick reference

| Action | Endpoint | Method | Body | Returns |
|--------|----------|--------|------|---------|
| List rows | `/v1/listings/admin/list` | POST | `AdminListingFilterFields` (see [§3.1](#31-admin-only-filter-fields)) | `AdminListingListResponse` with `AdminListingSummary[]` |
| Open one row | `/v1/listings/admin/{id}` | GET | — | `ListingResponseWithAdmin` (full) |
| Legacy detail (deprecated) | `/v1/listings/{id}/admin` | GET | — | Same as above |

### Admin filter capabilities on `POST /v1/listings/admin/list`

| Capability | Filter field(s) |
|------------|-----------------|
| Title text search | `title` |
| Owner name or phone | `ownerSearch` |
| **Price range** | `price` (`from..to`) |
| **Area range (m²)** | `area` (`from..to`) |
| **Bedrooms range** | `bedroomsRange` (`from..to`) — or `bedrooms` for exact match |
| **Bathrooms range** | `bathroomsRange` (`from..to`) — or `bathrooms` for exact match |
| **Room capacity range** | `roomCapacity` (`from..to`) |
| **Price reduction % range** | `priceReductionPercent` (`from..to`) |
| **Posted in date range** | `postDate` (`YYYY-MM-DD..YYYY-MM-DD`) |
| **Expires in date range** | `expiryDate` (same `..` format) |
| Listing type | `listingType` (`RENT` / `SALE` / `SHARE`) |
| Verified flag | `verified` (boolean) |
| Product type | `productType` (`ROOM` / `APARTMENT` / `HOUSE` / `OFFICE` / `STUDIO`) |
| VIP tier | `vipType` (`NORMAL` / `SILVER` / `GOLD` / `DIAMOND`) |
| Moderation state | `moderationStatus` |
| Category | `categoryId` |
| Province / district | `provinceId`, `districtId`, `provinceCode`, `provinceCodes`, … |

> **All range filters use the same `from..to` format.** Either side may be omitted (e.g. `"5000000.."`, `"..60"`).

All endpoints require `X-Admin-Id` header.
