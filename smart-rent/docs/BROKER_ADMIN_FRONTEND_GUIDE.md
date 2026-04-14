# Broker Feature – Admin Frontend Integration Guide

> **Audience:** Frontend developer building the admin dashboard.
> User-facing guide is in `BROKER_USER_FRONTEND_GUIDE.md`.

---

## 1. Business Overview

### Admin Workflow
1. Receive in-app notification: **"Yêu cầu đăng ký môi giới mới"**.
2. Navigate to **Broker Review** section in the admin dashboard.
3. Call `GET /v1/admin/users/broker-pending` to list users awaiting review.
4. For each user, manually verify their broker license at:  
   👉 **[https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20](https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20)**
5. Call `PATCH /v1/admin/users/{userId}/broker-verification` to record the decision.
6. User receives an in-app notification automatically.

### Roles with access
All three admin roles can manage broker verification: `ROLE_SA` (Super Admin), `ROLE_UA` (User Admin), `ROLE_SPA` (Support Admin).

---

## 2. State Machine (Admin View)

```
PENDING ──(APPROVE)──▶ APPROVED
PENDING ──(REJECT)──▶  REJECTED
```

The admin can only meaningfully act on `PENDING` users, but the API accepts any state (idempotent).

---

## 3. API Contracts

### 3.1 GET /v1/admin/users/broker-pending

**Auth:** Bearer admin token  
**Roles required:** `ROLE_SA`, `ROLE_UA`, `ROLE_SPA`

**Query params:**

| Param | Default | Description |
|---|---|---|
| `page` | `1` | 1-based page number |
| `size` | `20` | Items per page |

**Response 200:**
```json
{
  "code": "999999",
  "data": {
    "page": 1,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1,
    "data": [
      {
        "userId": "user-abc-123",
        "firstName": "Nguyen",
        "lastName": "Van A",
        "email": "nguyen.vana@example.com",
        "phoneCode": "+84",
        "phoneNumber": "0912345678",
        "isBroker": false,
        "brokerVerificationStatus": "PENDING",
        "brokerRegisteredAt": "2024-01-15T10:30:00",
        "brokerVerifiedAt": null,
        "brokerVerifiedByAdminId": null,
        "brokerRejectionReason": null,
        "cccdFrontUrl": "https://r2.example.com/users/.../broker/....jpg?sig=...",
        "cccdBackUrl": "https://r2.example.com/users/.../broker/....jpg?sig=...",
        "certFrontUrl": "https://r2.example.com/users/.../broker/....jpg?sig=...",
        "certBackUrl": "https://r2.example.com/users/.../broker/....jpg?sig=..."
      }
    ]
  }
}
```

Results are ordered **oldest first** (FIFO) so admins process in submission order.

**Errors:**
| Code | HTTP | Meaning |
|---|---|---|
| `5001` | 401 | JWT missing |
| `6001` | 403 | Insufficient role |

---

### 3.2 PATCH /v1/admin/users/{userId}/broker-verification

**Auth:** Bearer admin token  
**Roles required:** `ROLE_SA`, `ROLE_UA`, `ROLE_SPA`

**Approve request:**
```json
{ "action": "APPROVE" }
```

**Reject request:**
```json
{
  "action": "REJECT",
  "rejectionReason": "Could not find license on external registry"
}
```
> `rejectionReason` is **required** for REJECT. Max 500 characters.

**Response 200 (approve):**
```json
{
  "code": "999999",
  "data": {
    "userId": "user-abc-123",
    "isBroker": true,
    "brokerVerificationStatus": "APPROVED",
    "brokerRegisteredAt": "2024-01-15T10:30:00",
    "brokerVerifiedAt": "2024-01-16T09:00:00",
    "brokerRejectionReason": null,
    "brokerVerificationSource": "https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20"
  }
}
```

**Errors:**
| Code | HTTP | Meaning | FE action |
|---|---|---|---|
| `17001` | 400 | Rejection reason required | Show inline form error on the reason field |
| `17002` | 400 | Invalid action value | Log + show generic error (shouldn't happen in normal flow) |
| `4001` | 404 | User not found | Show "User not found" toast; remove row from list |
| `6001` | 403 | Insufficient role | Show "Access Denied" |

---

## 4. TypeScript Data Model

```typescript
export type BrokerVerificationStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AdminBrokerUserResponse {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneCode: string;
  phoneNumber: string;
  isBroker: boolean;
  brokerVerificationStatus: BrokerVerificationStatus;
  brokerRegisteredAt: string;           // ISO 8601
  brokerVerifiedAt?: string | null;
  brokerVerifiedByAdminId?: string | null;
  brokerRejectionReason?: string | null;
  // Presigned document URLs (short-lived, regenerated on each fetch)
  cccdFrontUrl?: string | null;
  cccdBackUrl?: string | null;
  certFrontUrl?: string | null;
  certBackUrl?: string | null;
}

export interface BrokerVerificationRequest {
  action: 'APPROVE' | 'REJECT';
  rejectionReason?: string;             // required when action = REJECT
}

export interface BrokerStatusResponse {
  userId: string;
  isBroker: boolean;
  brokerVerificationStatus: BrokerVerificationStatus;
  brokerRegisteredAt?: string | null;
  brokerVerifiedAt?: string | null;
  brokerRejectionReason?: string | null;
  brokerVerificationSource?: string | null;
}

export interface PagedResponse<T> {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  data: T[];
}
```

---

## 5. UI Requirements

### Broker Review Page (`/admin/broker-pending`)

- **Table columns:** Name, Email, Phone, Registration Date, Actions (Approve / Reject)
- **Pagination:** use `page` + `size` params; display total count
- **Loading state:** table skeleton
- **Empty state:** "No pending broker applications." with illustration
- **Error state:** "Failed to load. Retry?" with retry button

### Approve Button
- Single click → call PATCH with `action: APPROVE`
- Show loading spinner on the row during the call
- On success: remove row from list, show success toast "Broker approved"

### Reject Button / Modal
- Opens a modal with a **required** textarea for `rejectionReason` (max 500 chars)
- Inline character counter
- Submit → call PATCH with `action: REJECT`
- On 400 (code 17001): show "Reason is required" under the textarea
- On success: remove row from list, show toast "Broker rejected"

### Document Viewer

Each row in the pending list must expose the 4 identity images for the admin to review **before** making a decision. The URLs are presigned and included directly in the `GET /v1/admin/users/broker-pending` response.

```tsx
function BrokerDocumentViewer({ user }: { user: AdminBrokerUserResponse }) {
  const docs = [
    { label: 'CCCD — Mặt trước', url: user.cccdFrontUrl },
    { label: 'CCCD — Mặt sau',   url: user.cccdBackUrl },
    { label: 'Chứng chỉ — Mặt trước', url: user.certFrontUrl },
    { label: 'Chứng chỉ — Mặt sau',   url: user.certBackUrl },
  ];
  return (
    <div className="broker-docs-grid">
      {docs.map(({ label, url }) =>
        url ? (
          <a key={label} href={url} target="_blank" rel="noopener noreferrer">
            <img src={url} alt={label} className="broker-doc-thumb" />
            <span>{label}</span>
          </a>
        ) : (
          <span key={label} className="broker-doc-missing">{label}: Not submitted</span>
        )
      )}
    </div>
  );
}
```

**Important:** Presigned URLs expire (typically 60 minutes). If an image returns 403, call `GET /v1/admin/users/broker-pending` again to refresh them. Do not cache the URLs on the client beyond the page lifetime.

### Admin Notification Badge
When the admin receives a `BROKER_REGISTRATION_RECEIVED` notification:
- Increment the notification bell counter
- Optionally show a banner: "New broker application from [User Name]"
- Deep-link to `/admin/broker-pending`

---

## 6. Integration Flows (React)

### Fetch pending list

```typescript
// hooks/usePendingBrokers.ts
export function usePendingBrokers(page: number, size = 20) {
  return useQuery({
    queryKey: ['admin', 'broker-pending', page, size],
    queryFn: () =>
      adminApiClient
        .get<ApiResponse<PagedResponse<AdminBrokerUserResponse>>>('/v1/admin/users/broker-pending', {
          params: { page, size },
        })
        .then(r => r.data.data),
  });
}
```

### Approve a broker

```typescript
// hooks/useBrokerApproval.ts
export function useBrokerApproval() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) =>
      adminApiClient
        .patch<ApiResponse<BrokerStatusResponse>>(
          `/v1/admin/users/${userId}/broker-verification`,
          { action: 'APPROVE' }
        )
        .then(r => r.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'broker-pending'] });
    },
  });
}
```

### Reject a broker

```typescript
// hooks/useBrokerRejection.ts
export function useBrokerRejection() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, rejectionReason }: { userId: string; rejectionReason: string }) =>
      adminApiClient
        .patch<ApiResponse<BrokerStatusResponse>>(
          `/v1/admin/users/${userId}/broker-verification`,
          { action: 'REJECT', rejectionReason }
        )
        .then(r => r.data.data),
    onError: (error: any) => {
      const code = error?.response?.data?.code;
      if (code === '17001') {
        // Surface field-level validation error
        return { fieldError: 'rejectionReason', message: 'Rejection reason is required' };
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'broker-pending'] });
    },
  });
}
```

---

## 7. Admin Notification Integration

When backend sends `BROKER_REGISTRATION_RECEIVED` notification to all admins:

```typescript
// In your notification handler / WebSocket message processor
function handleNotification(notification: Notification) {
  if (notification.type === 'BROKER_REGISTRATION_RECEIVED') {
    // 1. Increment notification badge
    incrementUnreadCount();
    // 2. Invalidate pending broker list so it auto-refreshes
    queryClient.invalidateQueries({ queryKey: ['admin', 'broker-pending'] });
    // 3. Optional toast
    showToast(`New broker application from a user. Review now.`);
  }
}
```

---

## 8. QA Checklist

| # | Test | Pass |
|---|---|---|
| 1 | Pending list loads with correct user info | [ ] |
| 2 | Pagination works (page/size params sent correctly) | [ ] |
| 3 | Results ordered oldest-first | [ ] |
| 4 | Approve → row removed, success toast | [ ] |
| 5 | Reject with reason → row removed, success toast | [ ] |
| 6 | Reject without reason → inline error on textarea | [ ] |
| 7 | Approve → user's `isBroker` flips to true when refetched | [ ] |
| 8 | Invalid action → generic error toast | [ ] |
| 9 | User not found → toast + row removed | [ ] |
| 10 | Non-admin JWT → 403, redirect to login | [ ] |
| 11 | `BROKER_REGISTRATION_RECEIVED` notification increments bell | [ ] |
| 12 | External verification URL link opens correctly | [ ] |
| 13 | Document images load in viewer for each pending user | [ ] |
| 14 | Presigned URLs expire gracefully (403 → refetch) | [ ] |
| 15 | "Not submitted" placeholder shown when a doc URL is null | [ ] |

---

## 9. External Verification Link

Every row in the pending list should include a direct link to the external registry:

```tsx
<a
  href="https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20"
  target="_blank"
  rel="noopener noreferrer"
>
  Verify on official registry ↗
</a>
```

This link must open in a new tab and must **not** be tracked as an internal navigation event.

---

## 10. Microcopy (EN / VI)

| Element | English | Vietnamese |
|---|---|---|
| Page title | "Pending Broker Applications" | "Đơn đăng ký môi giới chờ duyệt" |
| Approve button | "Approve" | "Chấp thuận" |
| Reject button | "Reject" | "Từ chối" |
| Reason label | "Rejection Reason" | "Lý do từ chối" |
| Reason placeholder | "Describe why this application cannot be approved…" | "Mô tả lý do không thể chấp thuận đơn này…" |
| Reason required error | "Rejection reason is required" | "Vui lòng nhập lý do từ chối" |
| Approve toast | "Broker approved successfully" | "Đã chấp thuận môi giới" |
| Reject toast | "Broker application rejected" | "Đã từ chối đơn môi giới" |
| Empty state | "No pending applications" | "Không có đơn chờ duyệt" |
| External link label | "Verify on official registry ↗" | "Xác minh trên cổng chính thức ↗" |