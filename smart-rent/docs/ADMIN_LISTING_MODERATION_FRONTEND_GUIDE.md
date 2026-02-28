# Admin App — Listing Review & Moderation Integration

> **Audience:** Frontend developer working on the **admin dashboard app**
> **Base URL:** `http://localhost:8080`
> **Auth:** All endpoints require `Authorization: Bearer <admin_token>`
> **Header:** Most admin endpoints require `X-Admin-Id: <admin_uuid>`

---

## Overview

Admin has two review queues:
1. **Listing review queue** — new and resubmitted listings awaiting moderation
2. **Report review queue** — user-submitted reports about listings

```
New listing ──→ PENDING_REVIEW ──→ admin approves ──→ APPROVED
                                ──→ admin rejects  ──→ REJECTED ──→ owner resubmits ──→ PENDING_REVIEW (loop)

Report filed ──→ admin resolves w/ ownerActionRequired ──→ REVISION_REQUIRED ──→ owner resubmits ──→ PENDING_REVIEW
```

---

## 1. Listing Review Queue

### Get listings pending review

```
POST /v1/listings/admin/list
Content-Type: application/json
X-Admin-Id: <admin_uuid>
```

```json
{
  "moderationStatus": "PENDING_REVIEW",
  "page": 1,
  "size": 20
}
```

### Available `moderationStatus` filter values

| Value | What it shows |
|-------|---------------|
| `PENDING_REVIEW` | New + resubmitted listings awaiting review |
| `APPROVED` | Currently live listings |
| `REJECTED` | Listings rejected by admin |
| `REVISION_REQUIRED` | Listings waiting for owner to fix |
| `RESUBMITTED` | Owner has resubmitted (alias for PENDING_REVIEW) |
| `SUSPENDED` | Suspended listings |
| _(empty)_ | All listings regardless of status |

### Response

```json
{
  "code": "999999",
  "data": {
    "listings": [
      {
        "listingId": 42,
        "title": "Căn hộ 2PN - đã cập nhật",
        "moderationStatus": "PENDING_REVIEW",
        "verified": false,
        "isVerify": true,
        "user": {
          "userId": "user-uuid",
          "firstName": "Văn",
          "lastName": "Nguyễn",
          "email": "owner@example.com",
          "phoneNumber": "912345678"
        },
        "adminVerification": {
          "verificationStatus": "PENDING",
          "adminId": null,
          "adminName": null
        }
      }
    ],
    "totalCount": 5,
    "currentPage": 1,
    "pageSize": 20,
    "totalPages": 1,
    "statistics": {
      "pendingVerification": 5,
      "verified": 1200,
      "rejected": 30,
      "expired": 150
    }
  }
}
```

---

## 2. Moderate a Listing (Approve / Reject / Request Revision)

```
PUT /v1/admin/listings/{listingId}/status
Content-Type: application/json
Authorization: Bearer <admin_token>
```

### Approve

```json
{
  "decision": "APPROVE"
}
```
→ Listing becomes `APPROVED` (DISPLAYING)

### Reject (require owner to fix)

```json
{
  "decision": "REJECT",
  "reasonText": "Tiêu đề không chính xác, ảnh không rõ ràng",
  "ownerActionRequired": true,
  "ownerActionDeadlineAt": "2026-03-07T23:59:59"
}
```
→ Listing becomes `REJECTED`, owner gets email notification

### Request Revision

```json
{
  "decision": "REQUEST_REVISION",
  "reasonText": "Vui lòng bổ sung thông tin pháp lý",
  "ownerActionRequired": true
}
```
→ Listing becomes `REVISION_REQUIRED`, owner gets email notification

### Response

```json
{
  "code": "999999",
  "message": "Listing status updated successfully",
  "data": {
    "listingId": 42,
    "title": "Căn hộ 2PN",
    "verified": true,
    "isVerify": true
  }
}
```

---

## 3. Resolve a User Report

### Get all reports

```
GET /v1/admin/reports?status=PENDING&page=1&size=20
X-Admin-Id: <admin_uuid>
```

### Resolve report (require owner fix)

```
PUT /v1/admin/reports/{reportId}/resolve
Content-Type: application/json
X-Admin-Id: <admin_uuid>
```

```json
{
  "status": "RESOLVED",
  "ownerActionRequired": true,
  "adminNotes": "Thông tin giá và diện tích không chính xác. Vui lòng cập nhật.",
  "ownerActionType": "UPDATE_LISTING",
  "ownerActionDeadlineAt": "2026-03-07T23:59:59",
  "listingVisibilityAction": "HIDE_UNTIL_REVIEW"
}
```
→ Listing → `REVISION_REQUIRED`, owner gets email, listing hidden until fixed

### Resolve report (no owner action needed)

```json
{
  "status": "RESOLVED",
  "ownerActionRequired": false,
  "adminNotes": "Đã ghi nhận, không cần thay đổi"
}
```
→ Listing NOT changed

### Reject report (invalid)

```json
{
  "status": "REJECTED",
  "adminNotes": "Báo cáo không hợp lệ"
}
```
→ Listing NOT changed

> [!IMPORTANT]
> **For the owner to be able to update & resubmit**, you MUST:
> - Set `status: "RESOLVED"` (NOT `"REJECTED"`)
> - Set `ownerActionRequired: true`
>
> If you set `REJECTED`, the listing is untouched and the owner **cannot** resubmit.

---

## 4. Recommended Admin UI

### Review Queue Page

```
┌──────────────────────────────────────────────────┐
│  📋 Listing Review Queue                          │
│                                                    │
│  Tabs: [Chờ duyệt (5)] [Đã duyệt] [Đã từ chối]  │
│                                                    │
│  ┌────────────────────────────────────────────┐   │
│  │ #42 Căn hộ 2PN - Quận 1                   │   │
│  │ 👤 Nguyễn Văn U  |  📅 28/02/2026         │   │
│  │ 🏷️ Resubmitted (lần 2)                    │   │
│  │ [Xem chi tiết]  [✅ Duyệt]  [❌ Từ chối] │   │
│  └────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────┘
```

### Tab ↔ API Mapping

| Tab | API call |
|-----|----------|
| Chờ duyệt | `POST /admin/list` with `"moderationStatus": "PENDING_REVIEW"` |
| Đã duyệt | `POST /admin/list` with `"moderationStatus": "APPROVED"` |
| Đã từ chối | `POST /admin/list` with `"moderationStatus": "REJECTED"` |
| Cần sửa | `POST /admin/list` with `"moderationStatus": "REVISION_REQUIRED"` |
| Tạm ngưng | `POST /admin/list` with `"moderationStatus": "SUSPENDED"` |

### Key UI Logic

```javascript
// Quick action buttons on listing card
function renderActions(listing) {
  if (listing.moderationStatus === 'PENDING_REVIEW') {
    return ['Duyệt', 'Từ chối', 'Yêu cầu sửa'];  // → PUT /admin/listings/{id}/status
  }
  if (listing.moderationStatus === 'APPROVED') {
    return ['Tạm ngưng'];  // suspend if needed
  }
  if (listing.moderationStatus === 'REVISION_REQUIRED') {
    return [];  // waiting for owner, no action needed
  }
}
```
