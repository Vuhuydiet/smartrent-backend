# Owner App — Update & Resubmit Listing Integration

> **Audience:** Frontend developer working on the **owner/user app**
> **Base URL:** `http://localhost:8080`
> **Auth:** All endpoints require `Authorization: Bearer <token>` (owner token)

---

## Overview

When admin rejects a listing or resolves a report requiring owner action, the listing enters `REJECTED` state. The owner must update the listing content and resubmit for review.

```
REJECTED ──→ owner updates & resubmits ──→ IN_REVIEW ──→ admin approves ──→ DISPLAYING
                                                      ──→ admin rejects  ──→ REJECTED (loop)
```

---

## 1. Detect That Owner Needs to Act

### Option A: Check `listingStatus` on the listing detail

```
GET /v1/listings/{id}
```

In the response, check:
```json
{
  "listingId": 42,
  "listingStatus": "REJECTED",
  "moderationStatus": "REVISION_REQUIRED",
  ...
}
```

**Show the update form when:**
- `moderationStatus` is `REJECTED` or `REVISION_REQUIRED`

### Option B: Get pending owner actions

Check if there are pending `OwnerAction` records linked to the listing. These are created by the backend when admin requests owner action.

```json
// OwnerAction object shape (returned in moderation events or listing detail)
{
  "ownerActionId": 1,
  "listingId": 42,
  "triggerType": "REPORT_RESOLVED",      // or "LISTING_REJECTED"
  "triggerRefId": 5,                      // report ID or event ID
  "requiredAction": "UPDATE_LISTING",     // or "CONTACT_SUPPORT"
  "status": "PENDING_OWNER",             // awaiting owner action
  "deadlineAt": "2026-03-07T00:00:00",   // optional deadline
  "createdAt": "2026-02-28T10:00:00"
}
```

**Owner action statuses:**
| Status | Meaning |
|--------|---------|
| `PENDING_OWNER` | Owner needs to act |
| `SUBMITTED_FOR_REVIEW` | Owner resubmitted, awaiting admin review |
| `COMPLETED` | Admin approved the resubmission |
| `EXPIRED` | Deadline passed without action |

---

## 2. Update & Resubmit (Combined — Recommended)

```
PUT /v1/listings/{id}/update-and-resubmit
Content-Type: application/json
Authorization: Bearer <owner_token>
```

### Request Body

All fields are **optional** — only send fields the owner changed:

```json
{
  "title": "Căn hộ 2PN - đã cập nhật",
  "description": "Mô tả đã chỉnh sửa theo yêu cầu admin",
  "price": 5000000,
  "priceUnit": "MONTH",
  "area": 65.5,
  "bedrooms": 2,
  "bathrooms": 1,
  "direction": "SOUTH",
  "furnishing": "FULLY_FURNISHED",
  "listingType": "RENT",
  "categoryId": 1,
  "productType": "APARTMENT",
  "roomCapacity": 4,
  "waterPrice": "100,000 VND/month",
  "electricityPrice": "3,500 VND/kWh",
  "internetPrice": "200,000 VND/month",
  "serviceFee": "300,000 VND/month",
  "amenityIds": [1, 3, 5],
  "mediaIds": [101, 102, 103],
  "notes": "Đã sửa tiêu đề và thêm ảnh theo yêu cầu"
}
```

### Success Response

```json
{
  "code": "999999",
  "message": "Listing updated and resubmitted for review successfully",
  "data": null
}
```

### Error Responses

| Code | Meaning | UI Action |
|------|---------|-----------|
| `16003` | Listing not in REJECTED/REVISION_REQUIRED state | Show "This listing is not eligible for resubmission" |
| `16004` | Not the listing owner | Show "You don't have permission" |
| `16005` | Listing is SUSPENDED | Show "This listing is suspended. Please contact support" |
| `15001` | Listing not found | Show 404 page |

---

## 3. Alternative: Two-Step Flow

### Step 1 — Update content only

```
PUT /v1/listings/{id}
Content-Type: application/json
```

Same body as `ListingRequest` (your existing create/edit form).

### Step 2 — Resubmit for review

```
POST /v1/listings/{id}/resubmit-for-review
Content-Type: application/json
```

```json
{
  "notes": "Đã cập nhật theo yêu cầu"
}
```

---

## 4. Recommended UI Flow

```
┌─────────────────────────────────────┐
│  My Listings (filter: REJECTED)     │
│                                     │
│  📋 Căn hộ 2PN - Quận 1            │
│     ⚠️ Cần cập nhật                │
│     Admin notes: "Sửa tiêu đề..."  │
│     [Chỉnh sửa & Gửi lại]         │
└─────────────────────────────────────┘
              │ click
              ▼
┌─────────────────────────────────────┐
│  Edit Listing Form                  │
│  (pre-filled with current data)     │
│                                     │
│  Title: [_________________]         │
│  Description: [___________]         │
│  Price: [_________________]         │
│  Photos: [upload/manage]            │
│                                     │
│  Notes to admin: [________]         │
│                                     │
│  [Gửi lại để duyệt]               │
└─────────────────────────────────────┘
              │ submit → PUT /update-and-resubmit
              ▼
┌─────────────────────────────────────┐
│  ✅ Đã gửi lại thành công!         │
│  Tin đăng đang chờ admin duyệt.    │
│  Status: IN_REVIEW                  │
└─────────────────────────────────────┘
```

### Key UI Logic

```javascript
// Determine button visibility
if (listing.moderationStatus === 'REJECTED' || 
    listing.moderationStatus === 'REVISION_REQUIRED') {
  showButton('Chỉnh sửa & Gửi lại');  // → edit form → PUT /update-and-resubmit
}

if (listing.moderationStatus === 'PENDING_REVIEW' || 
    listing.moderationStatus === 'RESUBMITTED') {
  showBadge('Đang chờ duyệt');  // disable editing
}

if (listing.moderationStatus === 'SUSPENDED') {
  showBadge('Đã bị tạm ngưng');
  showButton('Liên hệ hỗ trợ');
}
```
