# VIP Tier Image / Video Limit — Frontend Guide

> Audience: frontend devs wiring up the **create-listing** and **edit-listing**
> screens, plus anywhere the **Membership / Package Benefits** panel is rendered.
> This doc is a copy-paste handbook — every endpoint, every response field, and
> every error code is here so you don't have to dig in Swagger.

---

## 1. What this feature does

Every VIP tier (`NORMAL`, `SILVER`, `GOLD`, `DIAMOND`) has hard caps on how many
images and videos a listing can carry. The backend now enforces those caps in
**all** listing create/update flows:

| Tier      | `maxImages` | `maxVideos` |
| --------- | ----------- | ----------- |
| NORMAL    | 5           | 1           |
| SILVER    | 10          | 2           |
| GOLD      | 15          | 3           |
| DIAMOND   | 20          | 5           |

> The values above are the seeded defaults — **always** read the live numbers
> from the API (`GET /v1/vip-tiers/{tierCode}/media-limits`). Admins can change
> them per tier without a frontend release.

If the user tries to attach more media than the tier allows, the API rejects the
request with HTTP 400 and one of the new domain codes
(`21001` for images, `21002` for videos). See [§4 Errors](#4-error-handling).

---

## 2. New / changed endpoints

### 2.1 `GET /v1/vip-tiers/{tierCode}/media-limits`

**Auth:** public (no token required)
**When to call:** on the create-listing screen, as soon as the user has picked
(or you have inferred) a tier. Cache the result per session.

```http
GET /v1/vip-tiers/SILVER/media-limits
```

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "tierCode": "SILVER",
    "maxImages": 10,
    "maxVideos": 2,
    "currentImages": null,
    "currentVideos": null,
    "remainingImages": null,
    "remainingVideos": null
  }
}
```

`currentImages` / `currentVideos` / `remaining*` are **always `null`** for this
endpoint — there is no listing yet, so "current usage" is undefined.

### 2.2 `GET /v1/listings/{id}/media-limits`

**Auth:** public (consistent with `GET /v1/listings/{id}`)
**When to call:** on the edit-listing screen, after the listing detail loads.

```http
GET /v1/listings/1234/media-limits
```

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "tierCode": "SILVER",
    "maxImages": 10,
    "maxVideos": 2,
    "currentImages": 3,
    "currentVideos": 1,
    "remainingImages": 7,
    "remainingVideos": 1
  }
}
```

`current*` counts only **ACTIVE** media currently linked to the listing
(`PENDING` uploads and unlinked media don't count). Use `remaining*` directly to
gate the upload button.

### 2.3 `GET /v1/vip-tiers` and `GET /v1/vip-tiers/{tierCode}` (unchanged shape)

These already returned `maxImages` and `maxVideos` on `VipTierDetailResponse`.
Nothing to change on the frontend if you're already reading those fields — but
prefer the lighter `/media-limits` endpoint when all you need are the caps.

### 2.4 Package benefit responses now include tier limits

`MembershipPackageBenefitResponse` and `UserMembershipBenefitResponse` gained
three fields so the Membership / Package screens can show "Silver post — up to
10 images, 2 videos" without a second API call:

| Field           | Type      | Notes                                                                 |
| --------------- | --------- | --------------------------------------------------------------------- |
| `vipTierCode`   | string?   | `SILVER`/`GOLD`/`DIAMOND` for post benefits, `null` for `PUSH` etc.   |
| `maxImages`     | int?      | Mirrors the tier's `maxImages`. `null` when `vipTierCode` is `null`.  |
| `maxVideos`     | int?      | Mirrors the tier's `maxVideos`. `null` when `vipTierCode` is `null`.  |

Endpoints affected (no URL changes — just new fields):
- `GET /v1/memberships/packages` (and its single-package variant)
- `GET /v1/memberships/my-membership` (`benefits[]`)
- `GET /v1/memberships/upgrade/preview` (`newBenefits[]`)

---

## 3. Recommended UX flows

### 3.1 Create-listing screen

```
1. User picks duration + tier (or selects a membership benefit).
2. FE calls GET /v1/vip-tiers/{tierCode}/media-limits   ← cache it
3. Render uploader with two counters:
       "Ảnh:   {selectedImages.length} / {maxImages}"
       "Video: {selectedVideos.length} / {maxVideos}"
4. Disable the "Thêm ảnh / Thêm video" button when the counter hits the cap.
5. On submit, the BE re-validates. If you respected the cap client-side this
   path is unreachable — but still surface a toast on error 21001/21002.
```

If the user is creating with a **membership quota** (`useMembershipQuota=true`):
- You already have `maxImages`/`maxVideos` inside each
  `UserMembershipBenefitResponse` from `GET /v1/memberships/my-membership`.
- When the user picks a `benefitId`, read its `maxImages`/`maxVideos` directly
  — no extra API call needed.

### 3.2 Edit-listing screen

```
1. FE calls GET /v1/listings/{id}            (existing)
2. FE calls GET /v1/listings/{id}/media-limits
3. Render counters using current* + remaining*:
       "Ảnh:   {currentImages} / {maxImages}    (còn {remainingImages} chỗ)"
       "Video: {currentVideos} / {maxVideos}    (còn {remainingVideos} chỗ)"
4. Disable the upload button when remaining* === 0.
5. Allow remove → re-fetch /media-limits to refresh counters (or compute locally).
```

> The edit endpoint (`PUT /v1/listings/{id}`) **replaces** the media set: every
> mediaId you send becomes the new set, anything missing is unlinked. So the
> cap applies to the size of the `mediaIds` array you submit, not to the delta.

### 3.3 Membership / Package screens

The benefit DTOs now carry `vipTierCode`, `maxImages`, `maxVideos`. Render them
in the benefit card so the user knows what they're buying:

```
[ POST SILVER × 5 / month ]
   → Mỗi tin tối đa 10 ảnh, 2 video
```

When `vipTierCode === null` (e.g. `PUSH` benefits), simply hide the media-limit
line.

---

## 4. Error handling

The backend uses the standard `ApiResponse` envelope. On limit-exceeded the
HTTP status is `400` and the body looks like:

```json
{
  "code": "21001",
  "message": "Số lượng ảnh vượt quá giới hạn của gói SILVER. Tối đa 10 ảnh, bạn đang muốn đăng 12 ảnh.",
  "data": null
}
```

| `code`  | HTTP | Meaning                                                                                              |
| ------- | ---- | ---------------------------------------------------------------------------------------------------- |
| `21001` | 400  | `VIP_TIER_IMAGE_LIMIT_EXCEEDED` — too many images for the chosen tier.                               |
| `21002` | 400  | `VIP_TIER_VIDEO_LIMIT_EXCEEDED` — too many videos for the chosen tier.                               |
| `21003` | 500  | `VIP_TIER_NOT_CONFIGURED` — the tier code is missing from `vip_tier_details`. Should not happen in prod; report to backend. |

The `message` field is already user-facing Vietnamese — surface it via toast.
If you want to localize, parse `code` and `data`-less templates yourself.

### 4.1 Where the validation fires

| Endpoint                                | Fires when                                                                 |
| --------------------------------------- | -------------------------------------------------------------------------- |
| `POST /v1/listings` (quota path)        | Before quota is consumed.                                                  |
| `POST /v1/listings` (payment path)      | Before the VNPay transaction is created — user is **not** charged on fail. |
| `POST /v1/listings/vip` *(deprecated)*  | Before quota/payment work.                                                 |
| `PUT /v1/listings/{id}`                 | Before existing media is unlinked — old media stays on rejection.          |
| Payment callback (cache → listing)      | Inside `linkMediaToListing` as a defense-in-depth check.                   |

The payment callback case is the only one where the user has already paid; the
fail-fast checks on `POST /v1/listings` make it effectively unreachable, but
clients should still handle it for safety.

---

## 5. Quick reference — payloads that trigger the cap

### Create with payment
```http
POST /v1/listings
Content-Type: application/json
Authorization: Bearer <token>
```
```json
{
  "title": "...",
  "vipType": "SILVER",
  "durationDays": 30,
  "useMembershipQuota": false,
  "paymentProvider": "VNPAY",
  "mediaIds": [1,2,3,4,5,6,7,8,9,10,11]   // ← 11 images on SILVER → 400 / 21001
}
```

### Create with membership quota
```json
{
  "title": "...",
  "useMembershipQuota": true,
  "benefitIds": [42],                       // POST_GOLD benefit
  "mediaIds": [...]                         // count must be ≤ GOLD.maxImages + maxVideos
}
```

### Update
```http
PUT /v1/listings/1234
```
```json
{
  "mediaIds": [101, 102, 103]               // becomes the full new set
}
```

---

## 6. TL;DR for vibe coding

```ts
// On create screen, after user picks tier:
const { data: limits } = await api.get(`/v1/vip-tiers/${tierCode}/media-limits`);
// → use limits.maxImages, limits.maxVideos to gate the uploader

// On edit screen:
const { data: usage } = await api.get(`/v1/listings/${listingId}/media-limits`);
// → usage.remainingImages, usage.remainingVideos

// On error:
if (err.response?.data?.code === '21001') toast.error(err.response.data.message);
if (err.response?.data?.code === '21002') toast.error(err.response.data.message);
```

That's it. Cap the uploader client-side using the live numbers, surface the
Vietnamese error message on the rare server-side rejection, and read the new
`maxImages`/`maxVideos` fields on benefit responses to enrich the membership
cards.
