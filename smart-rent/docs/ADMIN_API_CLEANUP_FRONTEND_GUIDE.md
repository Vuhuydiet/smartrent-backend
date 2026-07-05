# Admin API Cleanup — Frontend Integration Guide

> Companion to the FE audit "Rà soát API Admin — Filter / Sort / Field dư thừa". This doc covers what
> was actually implemented on the backend, exactly as shipped — including a few places where the
> implementation intentionally diverges from the original audit (see **§0 What was NOT changed**).

**Backend base URL**: `http://localhost:8080` (local) — all endpoints below require `Authorization: Bearer <admin_access_token>` unless noted.

---

## 0. What was NOT changed, and why

The audit asked to cut several fields flagged as "dư thừa". Before cutting anything, each field's response DTO was checked for reuse outside the admin panel — **fields on a DTO shared with a public/customer-facing endpoint were left alone**, since removing them would break that other caller. Additions (new fields) were still made where safe, since additive changes don't break existing consumers.

| Domain | Asked to cut | Why skipped |
|---|---|---|
| Users list | `brokerVerificationStatus` | Flagged as "cần xác nhận" in the audit itself. Confirmed it's a real, correctly-owned field on `User` (not a stray/wrong field) — kept since it's actually useful, e.g. for future broker-status columns. `idDocument` and `contactPhoneVerified` were removed as originally requested. |
| Listing detail | `user.email`, `user.contactPhoneVerified`, `user.avatarUrl` | These live on `UserCreationResponse`, which is shared with the actual user-registration response (`POST /v1/users`) and other user-facing endpoints — not admin-exclusive. Left untouched. |
| Listing detail | `moderationStatus`, `revisionCount`, `lastModerationReasonCode`, `lastModerationReasonText` | These ARE still on the response — turned out they're populated by the **moderate/verify action** response (`PUT /v1/admin/listings/{id}/status`), not just the plain detail GET. Removing them would have broken that action's response. Instead, the DTO now uses `@JsonInclude(NON_NULL)`, so these fields simply don't appear in the plain `GET /v1/admin/listings/{id}` response (where they're never set) but still appear after a moderate/verify action (where they are set). |
| News list | any field on `NewsSummaryResponse` | Shared with the public `GET /v1/news` and `GET /v1/news/newest`. Not touched. `status` being "optional" turned out to be a false premise — it's a non-nullable enum column, always present. |
| Membership packages | `MembershipPackageBenefitResponse.benefitType`, hypothetical `revenue` field | `MembershipPackageResponse` is shared with the public `GET /v1/memberships/packages`. `revenue` doesn't exist anywhere in the current response — there was nothing to remove. |
| VIP tier | `maxImages`, `maxVideos`, `hasBadge`, `badgeName`, `badgeColor`, `description` | **`GET /v1/vip-tiers/all` is not an admin endpoint** — it's fully public and unauthenticated (listed in the security allowlist), almost certainly used by a public pricing page. Skipped entirely; do not add admin restrictions or trim fields here without confirming with product first. |
| Reports | `resolvedBy`, `updatedAt` | `ListingReportResponse` is also returned by the public `ListingReportController` (unauthenticated report creation + report history). Not touched. `listingTitle`/`listingThumbnailUrl` were still added since additive fields don't break existing callers. |

Also found and fixed as a side effect of touching this code:
- **Bug**: `GET /v1/admin/listings/{id}` (detail) and the list view computed `adminVerification.verificationStatus` differently for the same listing state (`verified=false, isVerify=false` → list said `NOT_SUBMITTED`, detail said `PENDING`). Both now consistently return `NOT_SUBMITTED`.
- Confirmed (not fixed — flagging for awareness): `AdminTransactionController`'s documented "already has sort" is not actually wired server-side (`newestFirst()` hardcodes `createdAt DESC`, no client override). If you build sort UI against Transactions expecting it to work, it currently won't do anything different.
- Confirmed (not fixed): `AdminNewsController` and `AdminListingReportController` have no `@PreAuthorize` role check (any authenticated user, not just admins, can call them) — inconsistent with `AdminBrokerController`/`AdminMembershipController`/`AdminTransactionController`. Left alone since a "CM" (content manager?) role referenced in `AdminFilterRequest`'s docs might legitimately need access and locking it down without confirming could break something. Worth a follow-up conversation with backend, not fixed as part of this pass.

---

## 1. Listings — route + method changed, fields trimmed

### Route change

```
POST /v1/listings/admin/list   →   GET /v1/admin/listings
GET  /v1/listings/admin/{id}   →   GET /v1/admin/listings/{id}
```

Both now require the same `X-Admin-Id` header as before (unchanged auth mechanism — this predates the JWT-role-based admin controllers and wasn't touched). The list endpoint's ~50 filter fields (documented in `ListingFilterRequest`) now bind as **query parameters** instead of a JSON body — same field names, same `..`-range syntax for range filters (`price`, `area`, `bedroomsRange`, `bathroomsRange`, `roomCapacity`, `priceReductionPercent`, `postDate`, `expiryDate`).

```http
GET /v1/admin/listings?verified=false&isVerify=true&page=1&size=20
GET /v1/admin/listings?moderationStatus=PENDING_REVIEW
GET /v1/admin/listings?postDate=2026-03-01..2026-03-31&listingType=RENT&productType=APARTMENT
X-Admin-Id: <admin-id>
```

The legacy alias `GET /v1/listings/{id}/admin` still works (kept for backward compatibility, marked deprecated in Swagger).

### List response (`AdminListingSummary`) — fields removed / added

```ts
interface AdminListingSummary {
  listingId: number;
  title: string;
  user?: { firstName: string; lastName: string; contactPhoneNumber: string };
  postDate?: string;
  listingType: 'RENT' | 'SALE' | 'SHARE';
  expired: boolean;
  listingStatus: string;
  vipType: 'NORMAL' | 'SILVER' | 'GOLD' | 'DIAMOND';
  productType: 'ROOM' | 'APARTMENT' | 'HOUSE' | 'OFFICE' | 'STUDIO';
  price: number;
  priceUnit: string;
  area: number;
  district?: string;        // NEW — legacy-structure addresses only, null for new-structure
  fullAddress?: string;     // NEW
  images: string[];
  adminVerification?: { verificationStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'NOT_SUBMITTED' };
  moderationStatus?: string;
  lastModerationReasonText?: string;
}
```

Removed from the list row: `categoryId`, `revisionCount`, `lastModerationReasonCode`, `adminVerification.verifiedAt`, `verified` (redundant with `listingStatus`/`adminVerification.verificationStatus`), `expiryDate`.

Added: `district`, `fullAddress` — both cheap to compute (the underlying query already eager-loads the address; `district` costs one extra batched lookup per page, only for legacy-structure addresses).

### Detail response (`ListingResponseWithAdmin`) — fields removed

Removed: `categoryId`, `roomCapacity`, `vipType` (only shown in the list table per the audit). `moderationStatus`/`revisionCount`/`lastModerationReasonCode`/`lastModerationReasonText`/`createdAt`/`updatedAt` are still on the type but will be **absent from the plain detail GET** (only appear on the moderate/verify action response — see §0).

`adminVerification` shrank from `{ adminId, adminName, adminEmail, verifiedAt, verificationStatus, verificationNotes }` to just `{ verificationStatus, verificationNotes }` — the admin-identity fields weren't rendered anywhere per the audit.

```ts
interface ListingResponseWithAdmin {
  listingId: number;
  title: string;
  description: string;
  user: UserCreationResponse; // unchanged — full user object
  postDate?: string;
  expiryDate?: string;
  listingType: string;
  verified: boolean;
  expired: boolean;
  listingStatus: string;
  productType: string;
  price: number;
  priceUnit: string;
  addressId: number;
  area: number;
  bedrooms?: number;
  bathrooms?: number;
  direction?: string;
  furnishing?: string;
  waterPrice?: string;
  electricityPrice?: string;
  internetPrice?: string;
  serviceFee?: string;
  amenities: AmenityResponse[];
  media: MediaResponse[];
  adminVerification?: { verificationStatus: string; verificationNotes?: string };
  updatedBy?: number;
  // Only present on PUT /v1/admin/listings/{id}/status (moderate/verify) responses:
  moderationStatus?: string;
  revisionCount?: number;
  lastModerationReasonCode?: string;
  lastModerationReasonText?: string;
  propertyInfo?: { type: string; area: number; district?: string; fullAddress: string };
}
```

---

## 2. Users — new dedicated admin route

```
GET /v1/users/list   →   GET /v1/admin/users
```

`GET /v1/users` (self-profile) is **completely untouched**. The new route is admin-only (`ROLE_SA`/`ROLE_UA`/`ROLE_SPA`), unlike the old `/v1/users/list` which had no role check at all.

```http
GET /v1/admin/users?firstName=John&isBroker=true&createdAt=2026-02-09..2026-03-10&sort=createdAt,desc&page=1&size=20
```

| Param | Notes |
|---|---|
| `firstName`, `lastName`, `email`, `phoneNumber` | contains-match, same as before |
| `isBroker` | boolean, same as before |
| `createdAt` | **new** — single date or `from..to` range, either side optional |
| `sort` | **new** — `field,direction`, e.g. `firstName,asc`. Fields: `firstName`, `lastName`, `createdAt`. Default `createdAt,desc` |
| `page`, `size` | unchanged, 1-based |

Response DTO is now a dedicated `AdminUserSummaryResponse` (not the shared `GetUserResponse` used by self-profile):

```ts
interface AdminUserSummaryResponse {
  userId: string;
  phoneCode?: string;
  phoneNumber?: string;
  email: string;
  firstName: string;
  lastName: string;
  createdAt: string;
  taxNumber?: string;
  contactPhoneNumber?: string;
  avatarUrl?: string;
  avatarMediaId?: number;
  isBroker?: boolean;
  brokerVerificationStatus?: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';
}
```
Removed vs. the old `/v1/users/list` payload: `idDocument`, `contactPhoneVerified`.

---

## 3. Admins — new dedicated admin route

```
GET /v1/admins/list   →   GET /v1/admin/admins
```

`GET /v1/admins` (self-profile) is untouched.

```http
GET /v1/admin/admins?role=SA,UA&createdAt=2026-02-09..&sort=firstName,asc&page=1&size=20
```

| Param | Notes |
|---|---|
| `firstName`, `lastName`, `email`, `phoneNumber` | contains-match, same as before |
| `role` | CSV of role IDs, same as before |
| `createdAt` | **new** — single date or range |
| `sort` | **new** — `field,direction`. Fields: `firstName`, `lastName`, `createdAt`. Default `createdAt,desc` |

```ts
interface AdminAccountSummaryResponse {
  adminId: string;
  phoneCode?: string;
  phoneNumber?: string;
  email: string;
  firstName: string;
  lastName: string;
  createdAt: string;
  roles: string[];
}
```
Removed: `idDocument`, `taxNumber` — these were always `null` (the `Admin` entity never had these columns; they were leftover copy-paste from the User DTO).

---

## 4. Roles — sort added

```http
GET /v1/roles?sort=roleName,asc&page=1&size=10
```

`sort` format `field,direction`. Fields: `roleId`, `roleName`. Default (when `sort` omitted) is `roleName,desc`. No field changes — `GetRoleResponse` (`roleId`, `roleName`) was already minimal.

---

## 5. Brokers pending — sort, search, date filter, fields trimmed

```
GET /v1/admin/users/broker-pending
```

```http
GET /v1/admin/users/broker-pending?search=nguyen&registeredAt=2026-02-09..2026-03-10&sort=firstName,asc&page=1&size=20
```

| Param | Notes |
|---|---|
| `search` | **new** — matches first name, last name, or email |
| `registeredAt` | **new** — single date or range on `brokerRegisteredAt` |
| `sort` | **new** — `field,direction`. Fields: `brokerRegisteredAt`, `firstName`, `lastName`. Default `brokerRegisteredAt,asc` (FIFO, unchanged default behavior) |
| `page`, `size` | unchanged; `size` now hard-capped at 100 server-side |

```ts
interface AdminBrokerUserResponse {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneCode?: string;
  phoneNumber?: string;
  avatarUrl?: string;
  brokerVerificationStatus: string;  // always "PENDING" on this endpoint
  brokerRegisteredAt: string;
  brokerRejectionReason?: string;    // present if this is a re-submission after a prior rejection
  cccdFrontUrl?: string;
  cccdBackUrl?: string;
  certUrl?: string;
}
```
Removed: `isBroker` (always `false` here by definition), `brokerVerifiedAt` (always `null` — not yet verified), `brokerVerifiedByAdminId` (always `null`). `brokerRejectionReason` was kept per the audit's own note — no UI for it yet, but it's cheap to keep for a future "re-submission" indicator.

The verify/reject action (`PATCH /{userId}/broker-verification`) and remove-broker action (`DELETE /{userId}/broker`) responses (`BrokerStatusResponse`) are **unchanged** — that type is shared with the user's own self-service broker status endpoint, not touched.

---

## 6. News — sort + date filter added

```
GET /v1/admin/news
```

```http
GET /v1/admin/news?filter=status:PUBLISHED&createdAt=2026-02-09..2026-03-10&sort=viewCount,desc&page=1&size=20
```

| Param | Notes |
|---|---|
| `filter` | unchanged — `key:value` array, e.g. `filter=title:market&filter=category:BLOG` |
| `createdAt` | **new** — single date or range |
| `sort` | **new** — `field,direction`. Fields: `createdAt`, `publishedAt`, `title`, `viewCount`. Default `createdAt,desc` |

List response (`NewsSummaryResponse`, shared with public `/v1/news`) is **unchanged** — no fields removed.

Detail response `NewsResponse` (`GET /v1/admin/news/{id}`, admin-only, not shared with any public endpoint) removed: `authorId`, `updatedAt`.

---

## 7. Membership packages — pagination already existed; sort + search added

Correction to the audit: `GET /v1/admin/memberships/packages` already had `page`/`size` — that part of the audit's premise was inaccurate.

```http
GET /v1/admin/memberships/packages?search=Basic&sort=originalPrice,asc&page=1&size=10
```

| Param | Notes |
|---|---|
| `search` | **new** — contains-match on `packageName` |
| `sort` | **new** — `field,direction`. Fields: `packageName`, `packageLevel`, `originalPrice`, `salePrice`, `createdAt`. Default `createdAt,desc` |

Response shape (`MembershipPackageResponse`) is **unchanged** — shared with the public `GET /v1/memberships/packages`, and there was no `revenue` field to remove (it never existed).

---

## 8. VIP tier — no changes

`GET /v1/vip-tiers/all` is a **public, unauthenticated** endpoint (confirmed in the security allowlist), not an admin management API despite the URL suggesting otherwise. No fields were trimmed and no admin restriction was added. If you actually need an admin-only VIP tier management view with a trimmed payload, that would need a new dedicated endpoint — flag this separately if still needed.

---

## 9. Reports — sort, search, listingId filter, new listing preview fields

```
GET /v1/admin/reports
```

```http
GET /v1/admin/reports?status=PENDING&search=nguyen&listingId=123&createdAt=2026-02-09..2026-03-10&sort=status,asc&page=1&size=20
```

| Param | Notes |
|---|---|
| `status` | unchanged |
| `search` | **new** — matches reporter name, email, or phone |
| `listingId` | **new** — exact match on the reported listing |
| `createdAt` | **new** — single date or range |
| `sort` | **new** — `field,direction`. Fields: `createdAt`, `status`, `category`. Default `createdAt,desc` |

```ts
interface ListingReportResponse {
  reportId: number;
  listingId: number;
  listingTitle?: string;         // NEW — no more separate getListingDetail() call just for this
  listingThumbnailUrl?: string;  // NEW
  reporterName: string;
  reporterPhone: string;
  reporterEmail: string;
  reportReasons: ReportReasonResponse[];
  otherFeedback?: string;
  category: 'LISTING' | 'MAP';
  status: 'PENDING' | 'RESOLVED' | 'REJECTED';
  resolvedBy?: string;
  resolvedByName?: string;
  resolvedAt?: string;
  adminNotes?: string;
  createdAt: string;
  updatedAt: string;
}
```

`resolvedBy`/`updatedAt` were requested for removal but kept — this DTO is shared with the public report-creation/report-history endpoints (`ListingReportController`), so nothing was cut there. `listingTitle`/`listingThumbnailUrl` are additive — safe on the shared DTO, and mean the "open report modal" flow no longer needs a second `GET /v1/admin/listings/{id}` (or similar) call just to show the reported listing's name/photo.

---

## 10. Quick reference — all touched routes

| Old route | New route | Change type |
|---|---|---|
| `POST /v1/listings/admin/list` | `GET /v1/admin/listings` | method + path + query params + field trim |
| `GET /v1/listings/admin/{id}` | `GET /v1/admin/listings/{id}` | path + field trim (legacy alias still works) |
| `GET /v1/users/list` | `GET /v1/admin/users` | path + new admin-only DTO + sort + createdAt filter |
| `GET /v1/admins/list` | `GET /v1/admin/admins` | path + new admin-only DTO + sort + createdAt filter |
| `GET /v1/roles` | *(same)* | + `sort` param |
| `GET /v1/admin/users/broker-pending` | *(same)* | + `search`/`registeredAt`/`sort` params + field trim |
| `GET /v1/admin/news` | *(same)* | + `createdAt`/`sort` params + detail field trim |
| `GET /v1/admin/memberships/packages` | *(same)* | + `search`/`sort` params |
| `GET /v1/vip-tiers/all` | *(same, unchanged)* | no changes — public endpoint |
| `GET /v1/admin/reports` | *(same)* | + `search`/`listingId`/`createdAt`/`sort` params + 2 new response fields |
