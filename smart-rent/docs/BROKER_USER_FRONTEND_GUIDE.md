# Broker Feature – User-Facing Frontend Integration Guide

> **Audience:** Frontend developer building the user-facing (customer) application.
> Admin-facing guide is in `BROKER_ADMIN_FRONTEND_GUIDE.md`.

---

## 1. Business Overview

### User Journey
1. **Normal user** opens their profile page.
2. Clicks **"Register as Broker"** CTA → `POST /v1/users/broker/register`.
3. Status becomes **`PENDING`** → show "Verification in Progress" badge.
4. Admin manually verifies at [nangluchdxd.gov.vn](https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20).
5. **Approved** → `isBroker=true`, status `APPROVED` → show "Verified Broker ✓" badge everywhere.
6. **Rejected** → status `REJECTED` + rejection reason → show rejection message + "Re-apply" CTA.

### What changes for the user once approved
- Profile page shows green "Verified Broker ✓" badge.
- All listing cards posted by this user show a "Broker" badge next to the owner name.
- Other users can filter listings with `isBroker=true` to find broker listings.

---

## 2. State Machine

```
NONE ──(register)──▶ PENDING ──(admin approves)──▶ APPROVED
                        │
                   (admin rejects)
                        │
                        ▼
                    REJECTED ──(re-register)──▶ PENDING
```

| `brokerVerificationStatus` | `isBroker` | Meaning |
|---|---|---|
| `NONE` | `false` | Never applied |
| `PENDING` | `false` | Waiting for admin review |
| `APPROVED` | `true` | Verified broker |
| `REJECTED` | `false` | Application rejected; can re-apply |

---

## 3. API Contracts

### 3.1 POST /v1/users/broker/register

**Auth:** Bearer token (logged-in user)  
**Body:** None

**Response 200:**
```json
{
  "code": "999999",
  "data": {
    "userId": "user-abc-123",
    "isBroker": false,
    "brokerVerificationStatus": "PENDING",
    "brokerRegisteredAt": "2024-01-15T10:30:00",
    "brokerVerifiedAt": null,
    "brokerRejectionReason": null,
    "brokerVerificationSource": null
  }
}
```

**Idempotent behavior:** Calling this when status is already `PENDING` or `APPROVED` returns the current state unchanged — no error thrown.

**Errors:**
| Code | HTTP | Meaning |
|---|---|---|
| `5001` | 401 | JWT missing or expired |
| `4001` | 404 | User not found |

---

### 3.2 GET /v1/users/broker/status

**Auth:** Bearer token  
**Body:** None

**Response 200:** Same shape as register response above.

**When to call:** On profile page load and after registering to get the latest state.

---

### 3.3 POST /v1/listings/search — broker filter

Add `isBroker` to filter only broker-owned listings:

```json
{
  "isBroker": true,
  "page": 1,
  "size": 20
}
```

| Value | Effect |
|---|---|
| `true` | Only listings from approved brokers |
| `false` | Only listings from non-brokers |
| `null` / omitted | No filter — all listings |

---

### 3.4 GET /v1/users — profile endpoint

After broker approval, the user's profile response includes:
```json
{
  "userId": "user-abc-123",
  "firstName": "Nguyen",
  "isBroker": true,
  "brokerVerificationStatus": "APPROVED"
}
```

---

## 4. TypeScript Data Model

```typescript
export type BrokerVerificationStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';

export interface BrokerStatusResponse {
  userId: string;
  isBroker: boolean;
  brokerVerificationStatus: BrokerVerificationStatus;
  brokerRegisteredAt?: string | null;
  brokerVerifiedAt?: string | null;
  brokerRejectionReason?: string | null;
  brokerVerificationSource?: string | null;
}

export interface User {
  userId: string;
  firstName: string;
  lastName: string;
  email?: string;
  phoneNumber?: string;
  avatarUrl?: string;
  // Present in broker-enabled backend versions; may be absent in older deployments
  isBroker?: boolean | null;
  brokerVerificationStatus?: BrokerVerificationStatus | null;
}

export interface ListingFilterPayload {
  page?: number;
  size?: number;
  isBroker?: boolean | null;   // null = no filter
  provinceCode?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  keyword?: string;
  // ... other filters
}
```

---

## 5. UI Requirements

### Profile Page

| Status | What to show |
|---|---|
| `NONE` | "Register as Broker" button |
| `PENDING` | "🕐 Verification in Progress" info badge; button disabled |
| `APPROVED` | "✓ Verified Broker" green badge; no CTA |
| `REJECTED` | Warning with `brokerRejectionReason`; "Re-apply" button |
| Loading | Skeleton / spinner |
| Error | "Failed to load. Retry?" with retry button |

### Listing Card Broker Badge

Show only when **both** conditions are true:
```typescript
const showBrokerBadge = user?.isBroker === true
  && user?.brokerVerificationStatus === 'APPROVED';
```

### Listing Filter Panel

```
[ ] Broker listings only
```
Maps to `isBroker: true` in payload when checked; omit `isBroker` when unchecked.

---

## 6. Integration Flows (React)

### Flow A: Register as Broker

```typescript
// hooks/useBrokerRegistration.ts
export function useBrokerRegistration() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      apiClient.post<ApiResponse<BrokerStatusResponse>>('/v1/users/broker/register')
        .then(r => r.data.data),
    onSuccess: () => {
      // Refetch broker status so profile updates immediately
      queryClient.invalidateQueries({ queryKey: ['broker-status'] });
    },
  });
}
```

### Flow B: Get / Poll Status

```typescript
// hooks/useBrokerStatus.ts
export function useBrokerStatus() {
  return useQuery({
    queryKey: ['broker-status'],
    queryFn: () =>
      apiClient.get<ApiResponse<BrokerStatusResponse>>('/v1/users/broker/status')
        .then(r => r.data.data),
    // Auto-poll while PENDING; stop when resolved
    refetchInterval: (query) =>
      query.state.data?.brokerVerificationStatus === 'PENDING' ? 30_000 : false,
  });
}
```

### Flow C: Broker Listing Filter

```typescript
// In your listing search service / hook
const searchListings = (filters: ListingFilterPayload) =>
  apiClient.post('/v1/listings/search', {
    ...filters,
    // Send isBroker only when explicitly set; omit when null
    ...(filters.isBroker != null ? { isBroker: filters.isBroker } : {}),
  }).then(r => r.data.data);

// Filter toggle handler
const handleBrokerToggle = (checked: boolean) =>
  setFilters(prev => ({ ...prev, isBroker: checked ? true : null }));
```

---

## 7. UX Rules & Edge Cases

| Scenario | Behavior |
|---|---|
| Register when already PENDING | API 200 with current state; show "Already under review" toast |
| Register when APPROVED | API 200 with current state; show "You are already a verified broker" toast |
| Re-apply after rejection | Clears old rejection data; status → PENDING; show success toast |
| `isBroker` field absent in response | Treat as `false`; hide badge (backward compat) |
| JWT expired mid-flow | 401 → redirect to login, preserve current page in return URL |
| Network error | Show error toast; do not change local state |

---

## 8. Notifications (in-app)

The user receives in-app notifications automatically on status changes:

| Event | Title | Message |
|---|---|---|
| Admin approves | "Đăng ký môi giới được chấp thuận" | Congratulations message |
| Admin rejects | "Đăng ký môi giới bị từ chối" | Includes rejection reason |

Listen for new notifications via your existing WebSocket/polling mechanism and refresh broker status when a `BROKER_APPROVED` or `BROKER_REJECTED` notification arrives.

---

## 9. Microcopy (EN / VI)

| Element | English | Vietnamese |
|---|---|---|
| Register CTA | "Register as Broker" | "Đăng ký làm môi giới" |
| Pending badge | "🕐 Verification in Progress" | "🕐 Đang xác minh" |
| Pending description | "Your application is under review. We'll notify you once processed." | "Đơn của bạn đang được xem xét. Chúng tôi sẽ thông báo khi có kết quả." |
| Approved badge | "✓ Verified Broker" | "✓ Môi giới được xác nhận" |
| Rejected badge | "Registration Rejected" | "Đơn bị từ chối" |
| Rejected hint | "Your application was not approved: {reason}. You may re-apply." | "Đơn chưa được chấp thuận: {reason}. Bạn có thể nộp lại đơn." |
| Re-apply CTA | "Re-apply" | "Nộp lại đơn" |
| Listing filter toggle | "Broker listings only" | "Chỉ hiện tin môi giới" |
| Listing card badge | "Broker" | "Môi giới" |

---

## 10. Backward Compatibility

Fields `isBroker` and `brokerVerificationStatus` are absent in older backend versions. Always guard:

```typescript
function BrokerBadge({ user }: { user: User }) {
  if (!user.isBroker || user.brokerVerificationStatus !== 'APPROVED') return null;
  return <span className="broker-badge">✓ Môi giới</span>;
}
```

---

## 11. QA Checklist

| # | Test | Pass |
|---|---|---|
| 1 | Register (NONE → PENDING) → badge shows "Verification in Progress" | [ ] |
| 2 | Register again (PENDING) → toast "Already under review" | [ ] |
| 3 | Register (APPROVED) → toast "Already a broker" | [ ] |
| 4 | Status endpoint returns correct fields | [ ] |
| 5 | Approved → "✓ Verified Broker" badge on profile | [ ] |
| 6 | Approved → "Broker" badge on listing cards | [ ] |
| 7 | Rejected → rejection reason displayed + Re-apply CTA | [ ] |
| 8 | Re-apply after rejection → back to PENDING | [ ] |
| 9 | Filter `isBroker=true` returns only broker listings | [ ] |
| 10 | Filter cleared → `isBroker` omitted from payload | [ ] |
| 11 | Unauthenticated register → redirected to login | [ ] |
| 12 | `isBroker` field absent → no badge shown | [ ] |