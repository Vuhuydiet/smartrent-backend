# Membership Expiry Notification & Renewal — Frontend Integration Guide

Hướng dẫn tích hợp hai tính năng mới:
1. **Thông báo gói thành viên sắp hết hạn** — realtime (WebSocket) + email whitelist
2. **Gia hạn gói thành viên** — cùng gói, thêm 1 tháng, có payment flow

---

## Table of Contents

- [1. Tổng quan luồng](#1-tổng-quan-luồng)
- [2. Expiry Notifications](#2-expiry-notifications)
  - [2.1 WebSocket notification](#21-websocket-notification)
  - [2.2 REST: Lấy danh sách thông báo](#22-rest-lấy-danh-sách-thông-báo)
  - [2.3 Notification payload](#23-notification-payload)
  - [2.4 Hiển thị banner cảnh báo](#24-hiển-thị-banner-cảnh-báo)
- [3. Membership Renewal](#3-membership-renewal)
  - [3.1 Khi nào hiển thị nút Gia hạn?](#31-khi-nào-hiển-thị-nút-gia-hạn)
  - [3.2 GET /my-membership — lấy trạng thái hiện tại](#32-get-my-membership--lấy-trạng-thái-hiện-tại)
  - [3.3 POST /initiate-renewal — khởi tạo thanh toán](#33-post-initiate-renewal--khởi-tạo-thanh-toán)
  - [3.4 Sau khi thanh toán thành công](#34-sau-khi-thanh-toán-thành-công)
  - [3.5 Error codes](#35-error-codes)
- [4. Full Decision Tree](#4-full-decision-tree)
- [5. TypeScript Types](#5-typescript-types)
- [6. React/Next.js Code Examples](#6-reactnextjs-code-examples)
  - [6.1 Hook: useMembership](#61-hook-usemembership)
  - [6.2 ExpiryBanner component](#62-expirybanner-component)
  - [6.3 RenewButton component](#63-renewbutton-component)
  - [6.4 Notification handler](#64-notification-handler)
- [7. UI/UX Checklist](#7-uiux-checklist)

---

## 1. Tổng quan luồng

```
┌────────────────────────────────────────────────────────────────────┐
│                     MEMBERSHIP LIFECYCLE                           │
└────────────────────────────────────────────────────────────────────┘

  Hệ thống chạy job lúc 9:00 SA mỗi ngày
         │
         ▼
  Kiểm tra gói sắp hết hạn
         │
         ├── D-7 (còn 7 ngày) ──▶ Push WebSocket notification
         │                         + Email (nếu có whitelist)
         │
         └── D-3 (còn 3 ngày) ──▶ Push WebSocket notification
                                   + Email (nếu có whitelist)

  User nhận thông báo ──▶ Vào trang Membership ──▶ Bấm "Gia hạn"
         │
         ▼
  POST /v1/memberships/initiate-renewal
         │
         ▼
  Nhận paymentUrl (SePay QR)
         │
         ▼
  User hoàn thành thanh toán
         │
         ▼
  Callback tự động ──▶ completeMembershipRenewal
         │
         ▼
  Gói mới bắt đầu ngay từ endDate cũ (không mất ngày)
```

---

## 2. Expiry Notifications

### 2.1 WebSocket notification

Kết nối WebSocket để nhận thông báo realtime (xem thêm `REALTIME_NOTIFICATION_USER_FRONTEND_GUIDE.md`):

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

function connectNotifications(userId, onNotification) {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${process.env.NEXT_PUBLIC_API_URL}/ws`),
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/notifications/${userId}`, (message) => {
        const notification = JSON.parse(message.body);
        onNotification(notification);
      });
    },
  });
  client.activate();
  return client;
}
```

Khi nhận notification có `type === "MEMBERSHIP_EXPIRING"` → hiển thị banner/toast cảnh báo.

---

### 2.2 REST: Lấy danh sách thông báo

Dùng API này để load lại thông báo khi user mở app (không phụ thuộc WebSocket):

```
GET /v1/notifications?page=1&size=20
Authorization: Bearer <jwt_token>
```

Filter phía client những thông báo có `type === "MEMBERSHIP_EXPIRING"`.

---

### 2.3 Notification payload

```typescript
interface MembershipExpiryNotification {
  id: number;
  type: "MEMBERSHIP_EXPIRING";
  title: string;        // "Gói thành viên Basic Monthly sắp hết hạn"
  message: string;      // "Gói thành viên Basic Monthly của bạn còn 7 ngày nữa sẽ hết hạn..."
  referenceId: null;
  referenceType: "MEMBERSHIP_DAILY_SUMMARY";
  isRead: boolean;
  createdAt: string;    // ISO 8601
}
```

**Ví dụ message thực tế:**

| daysRemaining | message |
|---|---|
| 7 | `"Gói thành viên Basic Monthly của bạn còn 7 ngày nữa sẽ hết hạn. Hãy gia hạn sớm để không bị gián đoạn dịch vụ."` |
| 3 | `"Gói thành viên Basic Monthly của bạn còn 3 ngày nữa sẽ hết hạn. Hãy gia hạn sớm để không bị gián đoạn dịch vụ."` |

---

### 2.4 Hiển thị banner cảnh báo

Gợi ý logic hiển thị:

```
daysRemaining > 7   → Không hiển thị banner
daysRemaining = 7   → Banner màu vàng (warning)  🟡
daysRemaining = 3   → Banner màu cam (danger)     🟠
daysRemaining <= 0  → Banner màu đỏ (urgent)      🔴
```

Bấm banner → điều hướng đến trang gia hạn: `/buyer/memberships` (hoặc route tương đương trong FE).

---

## 3. Membership Renewal

### 3.1 Khi nào hiển thị nút Gia hạn?

| Trạng thái gói | `status` | Hiển thị nút Gia hạn? |
|---|---|---|
| Đang hoạt động, còn > 7 ngày | `ACTIVE` | Ẩn (hoặc hiện nhưng mờ) |
| Đang hoạt động, còn ≤ 7 ngày | `ACTIVE` | **Hiển thị nổi bật** |
| Đã hết hạn (trong vòng 7 ngày) | `EXPIRED` | **Hiển thị nổi bật** |
| Đã hết hạn (> 7 ngày trước) | `EXPIRED` | Ẩn → hiển thị nút Mua mới |
| Đã huỷ | `CANCELLED` | Ẩn → hiển thị nút Mua mới |

> Backend cho phép gia hạn khi gói còn active **hoặc** đã expired trong vòng **7 ngày** gần nhất.

---

### 3.2 GET /my-membership — lấy trạng thái hiện tại

```
GET /v1/memberships/my-membership
Authorization: Bearer <jwt_token>
```

**Response (gói đang active):**
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "userMembershipId": 42,
    "userId": "user-uuid-here",
    "membershipId": 1,
    "packageName": "Basic Monthly",
    "packageLevel": "BASIC",
    "startDate": "2025-06-01T00:00:00",
    "endDate": "2025-07-01T00:00:00",
    "durationDays": 30,
    "daysRemaining": 2,
    "status": "ACTIVE",
    "totalPaid": 99000,
    "benefits": [...],
    "createdAt": "2025-06-01T08:00:00",
    "updatedAt": "2025-06-01T08:00:00"
  }
}
```

**Response (không có gói active — 404):**
```json
{
  "code": "...",
  "message": "No active membership found for user: ..."
}
```

> Nếu 404 → kiểm tra history xem gói có expired gần đây không để quyết định hiện nút "Gia hạn" hay "Mua mới".

---

### 3.3 POST /initiate-renewal — khởi tạo thanh toán

**Endpoint:** `POST /v1/memberships/initiate-renewal`

**Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request body** (optional — body có thể để trống hoặc `{}`):
```json
{
  "paymentProvider": "SEPAY"
}
```

**Response thành công (200):**
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "paymentUrl": "https://qr.sepay.vn/img?acc=...&bank=MBBank&amount=99000&des=SR...",
    "transactionRef": "txn-uuid-here",
    "amount": 99000,
    "provider": "SEPAY"
  }
}
```

**Ví dụ fetch:**
```javascript
async function initiateRenewal(token) {
  const res = await fetch('/v1/memberships/initiate-renewal', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ paymentProvider: 'SEPAY' }),
  });
  return res.json();
}
```

---

### 3.4 Sau khi thanh toán thành công

Giống hệt flow purchase/upgrade — backend callback tự xử lý, FE chỉ cần:

1. Redirect user đến trang kết quả sau khi SePay callback về FE
2. Call `GET /v1/payments/transactions/{transactionRef}` để kiểm tra trạng thái
3. Nếu `status === "COMPLETED"` → hiển thị thông báo thành công + reload membership info

```
SePay QR ──▶ User quét & chuyển khoản
                    │
                    ▼
            Webhook Backend
                    │
                    ▼
      completeMembershipRenewal (tự động)
                    │
                    ▼
      Gói mới được tạo với:
        startDate = endDate cũ (nếu chưa hết hạn)
                  = now       (nếu đã hết hạn)
        endDate   = startDate + 1 tháng
```

**Kiểm tra kết quả:**
```javascript
async function checkTransaction(token, transactionRef) {
  const res = await fetch(`/v1/payments/transactions/${transactionRef}`, {
    headers: { 'Authorization': `Bearer ${token}` },
  });
  const data = await res.json();
  // data.data.status: "PENDING" | "COMPLETED" | "FAILED"
  return data.data;
}
```

---

### 3.5 Error codes

| HTTP | Code | Ý nghĩa | Xử lý FE |
|---|---|---|---|
| 400 | `14008` | Không có gói active hoặc gói hết hạn đã quá 7 ngày | Redirect sang trang Mua gói mới |
| 500 | — | Gói package đã bị deactivate bởi admin | Hiển thị lỗi + contact support |

---

## 4. Full Decision Tree

```
┌────────────────────────────────────────────────────────────────────────────┐
│                     User vào trang Membership                              │
└────────────────────────────────────────────────────────────────────────────┘
                                │
                GET /my-membership
                                │
              ┌─────────────────┴──────────────────┐
              │                                    │
           200 OK                               404 Not Found
              │                                    │
     daysRemaining <= 7?              GET /membership/history?page=1&size=1
              │                                    │
         ┌────┴────┐                    Gói expired < 7 ngày?
         YES       NO                      │           │
         │         │                      YES          NO
         ▼         ▼                      │            │
    Show        Show                      ▼            ▼
  "Gia hạn"   membership           Show "Gia hạn"  Show "Mua gói"
   button      info only            button          button
         │                               │
         └──────────────┬────────────────┘
                        │
              POST /initiate-renewal
                        │
                   paymentUrl
                        │
              ┌─────────┴──────────┐
              │                    │
         Show QR code          Open payment URL
         (SePay)               (redirect)
                        │
              User hoàn thành thanh toán
                        │
              Backend tự hoàn tất (webhook)
                        │
              GET /payments/transactions/{ref}
                        │
              status === "COMPLETED"?
                        │
                   ┌────┴────┐
                   YES       NO
                   │         │
            Show success  Show pending
            + reload       / retry
            membership
```

---

## 5. TypeScript Types

```typescript
// Membership status
type MembershipStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'UPGRADED';

// User's current membership
interface UserMembership {
  userMembershipId: number;
  userId: string;
  membershipId: number;
  packageName: string;
  packageLevel: 'BASIC' | 'STANDARD' | 'ADVANCED';
  startDate: string;       // ISO 8601
  endDate: string;         // ISO 8601
  durationDays: number;
  daysRemaining: number;   // 0 khi expired
  status: MembershipStatus;
  totalPaid: number;
  benefits: UserMembershipBenefit[];
  createdAt: string;
  updatedAt: string;
}

// Renewal request
interface MembershipRenewalRequest {
  paymentProvider?: 'SEPAY' | 'VNPAY';  // default: SEPAY
}

// Payment response (dùng chung cho purchase / upgrade / renewal)
interface PaymentResponse {
  paymentUrl: string;
  transactionRef: string;
  amount: number;
  provider: string;
  providerData?: Record<string, unknown>; // SePay form fields nếu cần
}

// Notification
interface MembershipExpiryNotification {
  id: number;
  type: 'MEMBERSHIP_EXPIRING';
  title: string;
  message: string;
  referenceId: null;
  referenceType: 'MEMBERSHIP_DAILY_SUMMARY';
  isRead: boolean;
  createdAt: string;
}

// Utility: tính trạng thái hết hạn từ daysRemaining
type ExpiryUrgency = 'none' | 'warning' | 'danger' | 'critical';

function getExpiryUrgency(daysRemaining: number, status: MembershipStatus): ExpiryUrgency {
  if (status !== 'ACTIVE') return 'none';
  if (daysRemaining > 7) return 'none';
  if (daysRemaining > 3) return 'warning';
  if (daysRemaining > 0) return 'danger';
  return 'critical';
}
```

---

## 6. React/Next.js Code Examples

### 6.1 Hook: useMembership

```typescript
import { useState, useEffect } from 'react';

interface UseMembershipReturn {
  membership: UserMembership | null;
  loading: boolean;
  error: string | null;
  canRenew: boolean;    // true khi active <= 7 ngày hoặc expired <= 7 ngày
  refetch: () => void;
}

export function useMembership(token: string): UseMembershipReturn {
  const [membership, setMembership] = useState<UserMembership | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function fetchMembership() {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch('/v1/memberships/my-membership', {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.status === 404) {
        setMembership(null);
        return;
      }
      const json = await res.json();
      setMembership(json.data ?? null);
    } catch (e) {
      setError('Không thể tải thông tin gói thành viên.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { fetchMembership(); }, [token]);

  const canRenew = (() => {
    if (!membership) return false;
    if (membership.status === 'ACTIVE' && membership.daysRemaining <= 7) return true;
    // Cho phép gia hạn nếu expired trong vòng 7 ngày
    if (membership.status === 'EXPIRED') {
      const expiredAgo = Date.now() - new Date(membership.endDate).getTime();
      const sevenDaysMs = 7 * 24 * 60 * 60 * 1000;
      return expiredAgo <= sevenDaysMs;
    }
    return false;
  })();

  return { membership, loading, error, canRenew, refetch: fetchMembership };
}
```

---

### 6.2 ExpiryBanner component

```typescript
import { useMembership } from './useMembership';

const URGENCY_STYLES: Record<ExpiryUrgency, string> = {
  none:     'hidden',
  warning:  'bg-yellow-50 border-yellow-400 text-yellow-800',
  danger:   'bg-orange-50 border-orange-400 text-orange-800',
  critical: 'bg-red-50  border-red-400   text-red-800',
};

const URGENCY_ICON: Record<ExpiryUrgency, string> = {
  none: '', warning: '⚠️', danger: '🔔', critical: '🚨',
};

interface ExpiryBannerProps {
  membership: UserMembership;
  onRenewClick: () => void;
}

export function ExpiryBanner({ membership, onRenewClick }: ExpiryBannerProps) {
  const urgency = getExpiryUrgency(membership.daysRemaining, membership.status);
  if (urgency === 'none') return null;

  const daysText = membership.daysRemaining <= 0
    ? 'hết hạn trong vòng 24 giờ tới'
    : `còn ${membership.daysRemaining} ngày`;

  return (
    <div className={`flex items-center justify-between border rounded-lg p-4 mb-4 ${URGENCY_STYLES[urgency]}`}>
      <span>
        {URGENCY_ICON[urgency]}{' '}
        Gói <strong>{membership.packageName}</strong> của bạn {daysText}.
      </span>
      <button
        onClick={onRenewClick}
        className="ml-4 px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-semibold hover:bg-blue-700"
      >
        Gia hạn ngay
      </button>
    </div>
  );
}
```

---

### 6.3 RenewButton component

```typescript
import { useState } from 'react';

interface RenewButtonProps {
  token: string;
  onSuccess?: (transactionRef: string, paymentUrl: string) => void;
  onError?: (message: string) => void;
}

export function RenewButton({ token, onSuccess, onError }: RenewButtonProps) {
  const [loading, setLoading] = useState(false);

  async function handleRenew() {
    setLoading(true);
    try {
      const res = await fetch('/v1/memberships/initiate-renewal', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ paymentProvider: 'SEPAY' }),
      });

      const json = await res.json();

      if (!res.ok) {
        // code 14008: không đủ điều kiện gia hạn
        onError?.(json.message ?? 'Không thể gia hạn lúc này.');
        return;
      }

      const { paymentUrl, transactionRef } = json.data;
      onSuccess?.(transactionRef, paymentUrl);

      // Mở QR thanh toán (tuỳ chọn: open new tab hoặc render modal)
      window.open(paymentUrl, '_blank');
    } catch {
      onError?.('Lỗi kết nối. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <button
      onClick={handleRenew}
      disabled={loading}
      className="px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 disabled:opacity-50"
    >
      {loading ? 'Đang xử lý...' : 'Gia hạn gói thành viên'}
    </button>
  );
}
```

---

### 6.4 Notification handler

Xử lý notification nhận qua WebSocket:

```typescript
function handleIncomingNotification(notification: MembershipExpiryNotification | any) {
  if (notification.type === 'MEMBERSHIP_EXPIRING') {
    // Tuỳ chọn 1: toast popup
    showToast({
      type: 'warning',
      title: notification.title,
      message: notification.message,
      action: {
        label: 'Gia hạn ngay',
        onClick: () => router.push('/buyer/memberships'),
      },
    });

    // Tuỳ chọn 2: cập nhật badge unread count
    setUnreadCount((prev) => prev + 1);

    // Tuỳ chọn 3: hiển thị banner trên header
    setExpiryAlert(notification.message);
  }
}
```

---

## 7. UI/UX Checklist

### Trang Membership (`/buyer/memberships`)

- [ ] Hiển thị `daysRemaining` với màu sắc phù hợp (xanh → vàng → đỏ khi gần hết)
- [ ] Hiển thị `endDate` dạng `dd/MM/yyyy HH:mm`
- [ ] Nút **"Gia hạn"** hiển thị khi `daysRemaining <= 7` hoặc gói expired < 7 ngày
- [ ] Nút **"Mua gói mới"** hiển thị khi không đủ điều kiện gia hạn
- [ ] Nút **"Nâng cấp"** hiển thị song song nếu có gói cao hơn (xem `MEMBERSHIP_UPGRADE_FRONTEND_GUIDE.md`)
- [ ] Sau khi bấm gia hạn: hiển thị QR SePay hoặc link thanh toán
- [ ] Sau khi thanh toán: tự reload membership info (polling hoặc callback redirect)

### Notification bell (header)

- [ ] Badge đếm số thông báo chưa đọc (bao gồm `MEMBERSHIP_EXPIRING`)
- [ ] Click vào notification `MEMBERSHIP_EXPIRING` → điều hướng đến `/buyer/memberships`
- [ ] Đánh dấu đã đọc khi user click: `PATCH /v1/notifications/{id}/read`

### Banner toàn trang

- [ ] Khi user có gói sắp hết hạn (`daysRemaining <= 7`), hiển thị banner cố định trên đầu trang (sticky)
- [ ] Banner tự ẩn sau khi user dismiss hoặc sau khi gia hạn thành công

---

## Phân biệt Purchase / Upgrade / Renewal

| Tính năng | Endpoint | Điều kiện | Kết quả |
|---|---|---|---|
| **Mua mới** | `POST /initiate-purchase` | Chưa có gói active | Tạo gói từ bây giờ |
| **Nâng cấp** | `POST /initiate-upgrade` | Có gói active, muốn lên tier cao hơn | Huỷ gói cũ, tạo gói mới (tier cao hơn), giảm giá theo thời gian còn lại |
| **Gia hạn** | `POST /initiate-renewal` | Gói active (≤ 7 ngày) hoặc expired (< 7 ngày) | Tạo gói mới cùng tier, nối tiếp từ `endDate` cũ |

> **Lưu ý:** Gia hạn không có discount. User trả đúng `salePrice` của gói đó.
