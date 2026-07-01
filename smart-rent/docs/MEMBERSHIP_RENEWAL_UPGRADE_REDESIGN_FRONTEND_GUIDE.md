# Membership Renewal & Upgrade (Redesign) — Frontend Integration Guide

> **Version:** 2.0 — replaces `MEMBERSHIP_EXPIRY_AND_RENEWAL_FRONTEND_GUIDE.md` and `MEMBERSHIP_UPGRADE_FRONTEND_GUIDE.md`.
>
> **Key design principle:** No new status enum value is introduced.
> "Queued" memberships are ordinary `ACTIVE` memberships whose `startDate` is in the future.
> The backend derives the distinction at query time using `startDate <= NOW()`.

---

## Table of Contents

- [1. Key Concepts](#1-key-concepts)
- [2. Membership Lifecycle](#2-membership-lifecycle)
- [3. API Overview](#3-api-overview)
- [4. GET /my-membership (Breaking Change)](#4-get-my-membership-breaking-change)
- [5. Renewal Flow](#5-renewal-flow)
- [6. Upgrade Flow](#6-upgrade-flow)
- [7. Full Decision Trees](#7-full-decision-trees)
- [8. TypeScript Types](#8-typescript-types)
- [9. React/Next.js Code Examples](#9-reactnextjs-code-examples)
- [10. Error Codes](#10-error-codes)
- [11. UI/UX Checklist](#11-uiux-checklist)

---

## 1. Key Concepts

### Two-slot model — no new status needed

A user can hold **at most two memberships at any time**:

| Slot | How it is stored | Derived meaning |
|------|-----------------|-----------------|
| Current | `status = ACTIVE, startDate <= NOW()` | The membership the user is using right now |
| Queued | `status = ACTIVE, startDate > NOW()` | The next membership, waiting for the current one to end |

The `status` column itself never gets a new value. The `startDate` and `endDate` columns already encode the full lifecycle:

```
startDate <= NOW() AND endDate > NOW()   →  currently active (benefits usable)
startDate >  NOW()                       →  queued (benefits not yet granted)
endDate   <= NOW()  (cron sets EXPIRED)  →  expired
```

### Renewal = enqueue the next period

Renewal does **not** replace the running membership. It stores a new `ACTIVE` membership record with `startDate = current.endDate`. That record silently waits — neither benefits nor any status change is needed until the daily cron job processes it.

### Upgrade — two cases, same endpoint

- **Case A (has queued):** the upgrade targets the queued slot — active slot is untouched.
- **Case B (no queued):** the upgrade applies to the active slot immediately (same behavior as before — immediate replacement with pro-rata discount).

---

## 2. Membership Lifecycle

```
[No membership]
       │
  POST /initiate-purchase  (payment OK)
       │
       ▼
   ACTIVE  startDate = NOW()
   endDate = NOW() + 1 month
       │
       ├──── POST /initiate-renewal  (no queued slot exists)  ──▶  ACTIVE  startDate = active.endDate
       │                                                               endDate = active.endDate + 1 month
       │                                                               (silently queued)
       │
       ├──── POST /initiate-upgrade  ── Case A: queued exists
       │         price = targetPrice − queued.totalPaid
       │         → queued record: UPGRADED
       │         → new ACTIVE record: startDate = old_queued.startDate  (same window, higher tier)
       │
       ├──── POST /initiate-upgrade  ── Case B: no queued
       │         price = pro-rata discount
       │         → current ACTIVE: UPGRADED
       │         → new ACTIVE record: startDate = NOW()  (immediate)
       │
       └──── Daily cron (midnight):
               IF endDate <= NOW():
                 set this → EXPIRED
                 expire benefits
                 IF user has queued record with startDate <= NOW():
                   grant benefits to queued record
                   (queued is now the current — no status change needed)

Status values (unchanged): ACTIVE | EXPIRED | CANCELLED | UPGRADED
```

---

## 3. API Overview

| Endpoint | Method | Change in v2 |
|----------|--------|--------------|
| `/v1/memberships/my-membership` | GET | **Breaking** — returns `{ current, queued }` |
| `/v1/memberships/initiate-renewal` | POST | Creates queued slot; rejects if queued already exists |
| `/v1/memberships/available-upgrades` | GET | Auto-detects Case A vs B; returns `upgradeContext` |
| `/v1/memberships/upgrade-preview/{id}` | GET | Reflects Case A vs B |
| `/v1/memberships/initiate-upgrade` | POST | Auto-detects Case A vs B; pricing differs |
| `/v1/memberships/packages` | GET | Unchanged |
| `/v1/memberships/initiate-purchase` | POST | Unchanged (only when no active AND no queued) |

---

## 4. GET /my-membership (Breaking Change)

### Endpoint

```
GET /v1/memberships/my-membership
Authorization: Bearer <jwt_token>
```

### Response shape (v2)

`data` now contains two named slots. Both can be `null`.

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "current": {
      "userMembershipId": 42,
      "userId": "user-uuid",
      "membershipPackageId": 2,
      "packageName": "Gold Monthly",
      "packageLevel": "STANDARD",
      "startDate": "2026-07-01T00:00:00",
      "endDate": "2026-07-31T23:59:59",
      "durationDays": 30,
      "daysRemaining": 29,
      "status": "ACTIVE",
      "totalPaid": 299000,
      "benefits": [
        {
          "benefitId": 101,
          "benefitType": "POST_GOLD",
          "totalQuantity": 10,
          "quantityUsed": 3,
          "quantityRemaining": 7,
          "status": "ACTIVE",
          "expiresAt": "2026-07-31T23:59:59"
        }
      ],
      "createdAt": "2026-07-01T08:00:00"
    },
    "queued": {
      "userMembershipId": 45,
      "userId": "user-uuid",
      "membershipPackageId": 2,
      "packageName": "Gold Monthly",
      "packageLevel": "STANDARD",
      "startDate": "2026-07-31T23:59:59",
      "endDate": "2026-08-31T23:59:59",
      "durationDays": 30,
      "daysRemaining": null,
      "status": "ACTIVE",
      "totalPaid": 299000,
      "benefits": [],
      "createdAt": "2026-07-21T10:00:00"
    }
  }
}
```

**Notes:**
- `data.queued` is `null` when no queued membership exists.
- `data.current` is `null` when the user has no active membership.
- `queued.benefits` is always `[]` — benefits are only granted by the cron job when the queued slot becomes current.
- `queued.daysRemaining` is `null` — it hasn't started yet.
- Both slots have `status = "ACTIVE"` at the stored level; the distinction is purely `startDate` vs `NOW()`.

### Response when user has no membership

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "current": null,
    "queued": null
  }
}
```

---

## 5. Renewal Flow

### When can the user renew?

| Condition | Can Renew? |
|-----------|-----------|
| Has current active, no queued slot | ✅ Yes |
| Has current active, queued slot already exists | ❌ No — error `14009` |
| Current expired < 7 days ago, no queued slot | ✅ Yes |
| Current expired > 7 days ago | ❌ No |
| No membership at all | ❌ No — use Purchase instead |

### POST /initiate-renewal

```
POST /v1/memberships/initiate-renewal
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request body:**
```json
{
  "paymentProvider": "SEPAY"
}
```

**Response (200):**
```json
{
  "code": "999999",
  "data": {
    "paymentUrl": "https://qr.sepay.vn/img?acc=...&amount=299000&des=SR...",
    "transactionRef": "txn-uuid-here",
    "amount": 299000,
    "provider": "SEPAY",
    "queuedStartDate": "2026-07-31T23:59:59",
    "queuedEndDate": "2026-08-31T23:59:59",
    "packageName": "Gold Monthly"
  }
}
```

After payment webhook fires, the backend creates:
```
UserMembership {
  status    = ACTIVE
  startDate = current.endDate        ← future date
  endDate   = current.endDate + 1 month
  benefits  = []                     ← granted by cron when startDate arrives
}
```

### Complete renewal flow

```
User clicks "Gia hạn"
        │
POST /initiate-renewal
        │
    paymentUrl
        │
   Show SePay QR
        │
  User pays
        │
  Webhook → backend creates queued membership
        │
GET /payments/transactions/{ref}
  until status === "COMPLETED"
        │
Reload GET /my-membership
  → data.queued is now populated
        │
Show: "Gói chờ: Gold Monthly — kích hoạt 01/08/2026"
```

### Renewal error codes

| HTTP | Code | Meaning | Frontend action |
|------|------|---------|----------------|
| 400 | `14008` | No eligible membership to renew | Redirect to Purchase page |
| 400 | `14009` | Queued slot already taken | Show: "Bạn đã có gói chờ" |
| 500 | — | Package deactivated by admin | Show error + contact support |

---

## 6. Upgrade Flow

### Case A — Upgrading the queued slot (user has current + queued)

The current slot is **not touched**. Only the queued slot is replaced.

```
Current: Gold   (Jul 1 → Jul 31)      ← unchanged
Queued:  Gold   (Aug 1 → Aug 31)  totalPaid = 299,000

User upgrades queued to Platinum

Cost = Platinum.price − queued.totalPaid
     = 599,000 − 299,000 = 300,000 VND

Result:
Current: Gold       (Jul 1 → Jul 31)   ← unchanged
Queued:  Platinum   (Aug 1 → Aug 31)   ← new queued (same window, higher tier)
Old queued: UPGRADED
```

- No forfeited benefits (the queued slot has none yet).
- `discountAmount` = what was already paid for the old queued slot.

### Case B — Upgrading the current slot (no queued exists)

Same pro-rata discount behavior as before — immediate replacement.

```
Current: Gold (Jul 1 → Jul 31, 15 days remaining)

Cost = Platinum.price − pro_rata_remaining_gold_value

Result:
Old current: UPGRADED
New current: Platinum (startDate = NOW(), 30-day fresh period)
```

### GET /available-upgrades (updated)

```
GET /v1/memberships/available-upgrades
Authorization: Bearer <jwt_token>
```

**Response — Case A (user has queued):**
```json
{
  "code": "999999",
  "data": {
    "upgradeContext": "QUEUED",
    "upgradingMembership": {
      "userMembershipId": 45,
      "packageName": "Gold Monthly",
      "packageLevel": "STANDARD",
      "status": "ACTIVE",
      "totalPaid": 299000,
      "startDate": "2026-07-31T23:59:59",
      "endDate": "2026-08-31T23:59:59"
    },
    "options": [
      {
        "targetMembershipPackageId": 3,
        "targetPackageName": "Platinum Monthly",
        "targetPackageLevel": "ADVANCED",
        "targetDurationDays": 30,
        "targetPackagePrice": 599000,
        "discountAmount": 299000,
        "finalPrice": 300000,
        "discountPercentage": 49.9,
        "forfeitedBenefits": [],
        "newBenefits": [
          {
            "benefitType": "POST_DIAMOND",
            "benefitName": "VIP Diamond Posts",
            "quantity": 20,
            "description": "Đăng tin với nhãn VIP Diamond"
          }
        ],
        "eligible": true,
        "ineligibilityReason": null
      }
    ]
  }
}
```

**Response — Case B (no queued, upgrading current):**
```json
{
  "data": {
    "upgradeContext": "CURRENT",
    "upgradingMembership": {
      "userMembershipId": 42,
      "packageName": "Gold Monthly",
      "packageLevel": "STANDARD",
      "status": "ACTIVE",
      "totalPaid": 299000,
      "daysRemaining": 15,
      "startDate": "2026-07-01T00:00:00",
      "endDate": "2026-07-31T23:59:59"
    },
    "options": [
      {
        "targetMembershipPackageId": 3,
        "targetPackageName": "Platinum Monthly",
        "targetPackageLevel": "ADVANCED",
        "targetPackagePrice": 599000,
        "discountAmount": 149500,
        "finalPrice": 449500,
        "discountPercentage": 24.9,
        "forfeitedBenefits": [
          {
            "benefitType": "POST_GOLD",
            "benefitName": "VIP Gold Posts",
            "totalQuantity": 10,
            "usedQuantity": 3,
            "remainingQuantity": 7,
            "estimatedValue": 209650
          }
        ],
        "newBenefits": [...],
        "eligible": true,
        "ineligibilityReason": null
      }
    ]
  }
}
```

**`upgradeContext` values:**

| Value | Meaning |
|-------|---------|
| `"CURRENT"` | No queued slot — upgrade applies immediately to current (Case B) |
| `"QUEUED"` | Has queued slot — upgrade targets the queued slot (Case A) |

### POST /initiate-upgrade (updated)

```
POST /v1/memberships/initiate-upgrade
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request body (unchanged):**
```json
{
  "targetMembershipId": 3,
  "paymentProvider": "SEPAY"
}
```

**Response — Case A:**
```json
{
  "data": {
    "upgradeContext": "QUEUED",
    "transactionRef": "txn-uuid",
    "paymentUrl": "https://qr.sepay.vn/...",
    "previousMembershipId": 45,
    "newMembershipPackageId": 3,
    "newPackageName": "Platinum Monthly",
    "newPackageLevel": "ADVANCED",
    "originalPrice": 599000,
    "discountAmount": 299000,
    "finalAmount": 300000,
    "activationDate": "2026-07-31T23:59:59",
    "status": "PENDING_PAYMENT",
    "message": "Nâng cấp gói chờ lên Platinum Monthly. Kích hoạt từ 01/08/2026."
  }
}
```

**Response — Case B:**
```json
{
  "data": {
    "upgradeContext": "CURRENT",
    "transactionRef": "txn-uuid",
    "paymentUrl": "https://qr.sepay.vn/...",
    "previousMembershipId": 42,
    "newMembershipPackageId": 3,
    "newPackageName": "Platinum Monthly",
    "newPackageLevel": "ADVANCED",
    "originalPrice": 599000,
    "discountAmount": 149500,
    "finalAmount": 449500,
    "activationDate": null,
    "status": "PENDING_PAYMENT",
    "message": "Nâng cấp ngay lên Platinum Monthly."
  }
}
```

`activationDate` is non-null only for Case A — shows the frontend when the upgraded tier actually starts.

### Upgrade error codes

| HTTP | Code | Meaning | Frontend action |
|------|------|---------|----------------|
| 400 | `14001` | No active membership | Show Purchase page |
| 400 | `14002` | Cannot downgrade | Disable that option |
| 400 | `14003` | Same tier as current or queued | Disable that option |
| 400 | `14004` | Upgrade transaction in progress | Show "Giao dịch đang xử lý" |
| 400 | `14005` | Upgrade failed (internal) | Show error + retry |
| 400 | `14006` | Target package inactive | Hide that option |
| 400 | `14007` | Tried purchase while active exists | Redirect to Upgrade |
| 400 | `14009` | Tried renewal while queued exists | Show queued slot info |
| 400 | `14010` | Tried purchase while queued exists | Show queued slot info |

---

## 7. Full Decision Trees

### Determining which buttons to show

```
GET /my-membership
       │
  ┌────┴─────────────────────────────────────────┐
  │                                              │
data.current != null                      data.current == null
  │                                              │
  ├── data.queued != null?                 data.queued != null?
  │     YES → show queued info card              │
  │           show "Nâng cấp gói chờ" (if        YES → show queued info only
  │               not highest tier)              │     (current activating soon)
  │           hide "Gia hạn" (already queued)   │
  │           hide "Nâng cấp ngay"              NO → show "Mua gói mới"
  │
  └── data.queued == null?
        YES → daysRemaining <= 7?
                YES → show "Gia hạn" (prominent)
                NO  → show "Gia hạn" (subtle)
              show "Nâng cấp ngay" (if not highest tier)
```

### Renewal decision tree

```
User clicks "Gia hạn"
       │
POST /initiate-renewal
       │
  ┌────┴──────────────────┐
  │ error 14009           │ 200 OK
  │ (queued exists)       │
  ▼                       ▼
"Bạn đã có gói chờ"   Show SePay QR
                           │
                      User pays
                           │
                 Webhook → queued record created
                           │
              Poll GET /payments/transactions/{ref}
              until status === "COMPLETED"
                           │
              Reload GET /my-membership
              → data.queued populated
                           │
              Show: "Gói chờ kích hoạt [date]"
```

### Upgrade decision tree

```
User clicks "Nâng cấp" (or "Nâng cấp gói chờ")
       │
GET /available-upgrades
       │
  upgradeContext === "QUEUED"?
       │                    │
      YES                  NO (CURRENT)
       │                    │
  Show banner:          Show banner:
  "Nâng cấp gói chờ    "Nâng cấp áp dụng
  — gói hiện tại       ngay — gói hiện
  không bị ảnh hưởng"  tại sẽ bị huỷ"
       │                    │
       └────────┬───────────┘
                │
       User picks target package
                │
       POST /initiate-upgrade
                │
          finalAmount > 0?
                │
        ┌───────┴────────┐
        │ YES            │ NO (free)
        │                │
   Show payment QR   Complete immediately
        │                │
   User pays         Reload /my-membership
        │
   Poll until COMPLETED
        │
   Reload /my-membership
```

---

## 8. TypeScript Types

```typescript
// Status values — UNCHANGED, no new values
type MembershipStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'UPGRADED';

type PackageLevel = 'BASIC' | 'STANDARD' | 'ADVANCED';

// upgradeContext tells frontend which case applies
type UpgradeContext = 'CURRENT' | 'QUEUED';

interface MembershipBenefit {
  benefitId: number;
  benefitType: 'POST_SILVER' | 'POST_GOLD' | 'POST_DIAMOND' | 'PUSH';
  totalQuantity: number;
  quantityUsed: number;
  quantityRemaining: number;
  status: 'ACTIVE' | 'FULLY_USED' | 'EXPIRED';
  expiresAt: string;
}

interface UserMembershipSlot {
  userMembershipId: number;
  userId: string;
  membershipPackageId: number;
  packageName: string;
  packageLevel: PackageLevel;
  startDate: string;           // ISO 8601
  endDate: string;             // ISO 8601
  durationDays: number;
  daysRemaining: number | null;  // null on queued slot
  status: MembershipStatus;    // always "ACTIVE" for current/queued slots
  totalPaid: number;
  benefits: MembershipBenefit[];  // always [] on queued slot
  createdAt: string;
}

// v2 GET /my-membership shape
interface MyMembershipResponse {
  current: UserMembershipSlot | null;
  queued:  UserMembershipSlot | null;
}

// Renewal
interface MembershipRenewalRequest {
  paymentProvider?: 'SEPAY' | 'VNPAY';
}

interface MembershipRenewalResponse {
  paymentUrl: string;
  transactionRef: string;
  amount: number;
  provider: string;
  queuedStartDate: string;  // ISO 8601 — when queued slot will activate
  queuedEndDate: string;
  packageName: string;
}

// Upgrade
interface ForfeitedBenefit {
  benefitType: string;
  benefitName: string;
  totalQuantity: number;
  usedQuantity: number;
  remainingQuantity: number;
  estimatedValue: number;
}

interface NewBenefit {
  benefitType: string;
  benefitName: string;
  quantity: number;
  description: string;
}

interface UpgradeOption {
  targetMembershipPackageId: number;
  targetPackageName: string;
  targetPackageLevel: PackageLevel;
  targetDurationDays: number;
  targetPackagePrice: number;
  discountAmount: number;
  finalPrice: number;
  discountPercentage: number;
  forfeitedBenefits: ForfeitedBenefit[];  // always [] in QUEUED context
  newBenefits: NewBenefit[];
  eligible: boolean;
  ineligibilityReason: string | null;
}

interface AvailableUpgradesResponse {
  upgradeContext: UpgradeContext;
  upgradingMembership: {
    userMembershipId: number;
    packageName: string;
    packageLevel: PackageLevel;
    status: MembershipStatus;
    totalPaid: number;
    daysRemaining?: number;   // present only in CURRENT context
    startDate?: string;
    endDate?: string;
  };
  options: UpgradeOption[];
}

interface MembershipUpgradeResponse {
  upgradeContext: UpgradeContext;
  transactionRef: string;
  paymentUrl: string | null;
  paymentProvider: string;
  previousMembershipId: number;
  newMembershipPackageId: number;
  newPackageName: string;
  newPackageLevel: PackageLevel;
  originalPrice: number;
  discountAmount: number;
  finalAmount: number;
  activationDate: string | null;  // non-null only in QUEUED context
  status: 'PENDING_PAYMENT' | 'COMPLETED';
  message: string;
}
```

---

## 9. React/Next.js Code Examples

### 9.1 Hook: useMembership

```typescript
import { useState, useEffect } from 'react';

interface UseMembershipReturn {
  current: UserMembershipSlot | null;
  queued:  UserMembershipSlot | null;
  loading: boolean;
  error: string | null;
  canRenew: boolean;
  hasQueued: boolean;
  refetch: () => void;
}

export function useMembership(token: string): UseMembershipReturn {
  const [current, setCurrent] = useState<UserMembershipSlot | null>(null);
  const [queued,  setQueued]  = useState<UserMembershipSlot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);

  async function fetchMembership() {
    setLoading(true);
    setError(null);
    try {
      const res  = await fetch('/v1/memberships/my-membership', {
        headers: { Authorization: `Bearer ${token}` },
      });
      const json = await res.json();
      setCurrent(json.data?.current ?? null);
      setQueued(json.data?.queued   ?? null);
    } catch {
      setError('Không thể tải thông tin gói thành viên.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { fetchMembership(); }, [token]);

  // Can renew when: active exists, no queued yet, and nearing expiry (or already expired < 7d)
  const canRenew = (() => {
    if (queued) return false;  // queued slot is taken
    if (current?.status === 'ACTIVE' && (current.daysRemaining ?? 99) <= 7) return true;
    if (current?.status === 'EXPIRED') {
      const msSinceExpiry = Date.now() - new Date(current.endDate).getTime();
      return msSinceExpiry <= 7 * 24 * 60 * 60 * 1000;
    }
    return false;
  })();

  return { current, queued, loading, error, canRenew, hasQueued: !!queued, refetch: fetchMembership };
}
```

### 9.2 MembershipCard

```typescript
interface MembershipCardProps {
  current:    UserMembershipSlot | null;
  queued:     UserMembershipSlot | null;
  canRenew:   boolean;
  onRenew:    () => void;
  onUpgrade:  () => void;
  onPurchase: () => void;
}

export function MembershipCard({
  current, queued, canRenew, onRenew, onUpgrade, onPurchase,
}: MembershipCardProps) {
  if (!current && !queued) {
    return (
      <div className="membership-card empty">
        <p>Bạn chưa có gói thành viên.</p>
        <button onClick={onPurchase} className="btn-primary">Mua gói thành viên</button>
      </div>
    );
  }

  return (
    <div className="membership-card">
      {/* Current slot */}
      {current && (
        <div className="slot current-slot">
          <span className="badge active">Đang hoạt động</span>
          <h3>{current.packageName}</h3>
          <p>Hết hạn: {formatDate(current.endDate)}</p>
          {(current.daysRemaining ?? 99) <= 7 && (
            <ExpiryBadge daysRemaining={current.daysRemaining ?? 0} />
          )}
          <BenefitsList benefits={current.benefits} />
        </div>
      )}

      {/* Queued slot */}
      {queued && (
        <div className="slot queued-slot">
          <span className="badge queued">Đang chờ kích hoạt</span>
          <h3>{queued.packageName}</h3>
          <p>Kích hoạt: {formatDate(queued.startDate)}</p>
          <p className="note">Quyền lợi được cấp khi gói kích hoạt.</p>
        </div>
      )}

      {/* Action buttons */}
      <div className="actions">
        {canRenew && (
          <button onClick={onRenew} className="btn-primary">Gia hạn gói</button>
        )}
        {queued && !canRenew && (
          <p className="info-text">Đã có gói chờ. Không thể gia hạn thêm.</p>
        )}
        {queued ? (
          <button onClick={onUpgrade} className="btn-secondary">Nâng cấp gói chờ</button>
        ) : current ? (
          <button onClick={onUpgrade} className="btn-secondary">Nâng cấp ngay</button>
        ) : null}
      </div>
    </div>
  );
}
```

### 9.3 UpgradeFlow (handles both cases)

```typescript
export function UpgradeFlow({ token, onComplete }: { token: string; onComplete: () => void }) {
  const [data,     setData]     = useState<AvailableUpgradesResponse | null>(null);
  const [selected, setSelected] = useState<UpgradeOption | null>(null);
  const [loading,  setLoading]  = useState(true);

  useEffect(() => {
    fetch('/v1/memberships/available-upgrades', {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(r => r.json())
      .then(j => setData(j.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spinner />;
  if (!data || data.options.length === 0) return <p>Không có gói nâng cấp khả dụng.</p>;

  const isQueuedContext = data.upgradeContext === 'QUEUED';

  return (
    <div>
      {isQueuedContext ? (
        <div className="context-banner queued">
          Bạn đang nâng cấp <strong>gói chờ</strong> ({data.upgradingMembership.packageName}).
          Gói hiện tại không bị ảnh hưởng.
          Kích hoạt từ: <strong>{formatDate(data.upgradingMembership.startDate)}</strong>
        </div>
      ) : (
        <div className="context-banner immediate">
          Nâng cấp sẽ <strong>áp dụng ngay</strong>.
          Gói hiện tại sẽ bị huỷ — bạn nhận chiết khấu thời gian còn lại.
        </div>
      )}

      {data.options.map(option => (
        <UpgradeOptionCard
          key={option.targetMembershipPackageId}
          option={option}
          context={data.upgradeContext}
          onSelect={() => setSelected(option)}
        />
      ))}

      {selected && (
        <UpgradeConfirmModal
          token={token}
          option={selected}
          context={data.upgradeContext}
          activationDate={data.upgradingMembership.startDate}
          onCancel={() => setSelected(null)}
          onSuccess={() => { setSelected(null); onComplete(); }}
        />
      )}
    </div>
  );
}
```

### 9.4 QueuedMembershipBanner

```typescript
export function QueuedMembershipBanner({ queued }: { queued: UserMembershipSlot }) {
  const startDate = new Date(queued.startDate).toLocaleDateString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });

  return (
    <div className="queued-banner">
      <span className="label">Gói chờ kích hoạt</span>
      <strong>{queued.packageName}</strong>
      <span>— tự động kích hoạt ngày {startDate}</span>
      <span className="note">Quyền lợi được cấp khi gói bắt đầu.</span>
    </div>
  );
}
```

---

## 10. Error Codes

| HTTP | Code | Meaning | Frontend action |
|------|------|---------|----------------|
| 400 | `14001` | No active membership | Show Purchase page |
| 400 | `14002` | Cannot downgrade | Disable that option |
| 400 | `14003` | Target is same tier | Disable that option |
| 400 | `14004` | Transaction in progress | Show "Giao dịch đang xử lý" |
| 400 | `14005` | Upgrade failed (internal) | Show error + retry |
| 400 | `14006` | Target package inactive | Hide that option |
| 400 | `14007` | Tried purchase while active exists | Redirect to Upgrade |
| 400 | `14008` | No eligible membership to renew | Show Purchase page |
| 400 | `14009` | Queued slot already exists (tried renewal) | Show queued slot info |
| 400 | `14010` | Tried purchase while queued slot exists | Show queued slot info |

---

## 11. UI/UX Checklist

### Membership page (`/buyer/memberships`)

**Current slot:**
- [ ] Package name, level badge, start/end date, days remaining
- [ ] Color scale: >7 days → green, 4–7 → yellow, 1–3 → orange, expired → red
- [ ] Benefit bars: used / total per benefit type
- [ ] If all benefits consumed but membership still active: show "Bạn đã dùng hết quyền lợi tháng này. Gói tự động gia hạn vào [date]."

**Queued slot:**
- [ ] Show "Gói chờ kích hoạt" card when `queued !== null`
- [ ] Show package name + activation date (`queued.startDate`)
- [ ] Show "Quyền lợi chưa được cấp — sẽ cấp khi kích hoạt"
- [ ] Do NOT show benefit bars

**Buttons:**
- [ ] "Gia hạn" — when `canRenew === true` AND `queued === null`
- [ ] "Nâng cấp ngay" — when `current` exists AND `queued === null` AND not highest tier
- [ ] "Nâng cấp gói chờ" — when `queued !== null` AND not highest tier
- [ ] "Mua gói mới" — when `current === null` AND `queued === null`

**Post-renewal UI:**
- [ ] Show SePay QR in modal
- [ ] Poll `GET /payments/transactions/{ref}` until `status === "COMPLETED"`
- [ ] On success: close modal, reload membership, show queued section
- [ ] If error 14009: show "Bạn đã có gói chờ — không thể gia hạn thêm"

**Post-upgrade UI (Case A — QUEUED context):**
- [ ] Banner: "Nâng cấp gói chờ — gói hiện tại không bị ảnh hưởng"
- [ ] Show `activationDate`
- [ ] No forfeited benefits warning
- [ ] Show cost = price difference clearly

**Post-upgrade UI (Case B — CURRENT context):**
- [ ] Banner: "Nâng cấp áp dụng ngay — gói hiện tại bị huỷ"
- [ ] Show forfeited benefits warning
- [ ] Show pro-rata discount explanation
- [ ] Require explicit confirmation (checkbox or second button click)

**Notification bell:**
- [ ] `MEMBERSHIP_EXPIRING` (D-7, D-3) → link to membership page
- [ ] `MEMBERSHIP_ACTIVATED` (when queued becomes current, sent by cron) → "Gói [name] đã kích hoạt!"

---

## Appendix: Discount Calculation Reference

### Case B — upgrading CURRENT slot (immediate)

```
time_ratio       = daysRemaining / totalDurationDays
pro_rata_value   = amountPaid × time_ratio
max_discount     = targetPrice − (targetPrice − currentPrice)
discount         = min(pro_rata_value, max_discount)
final_price      = max(targetPrice − currentPrice, targetPrice − discount)
```

User always pays at least the price difference between tiers.

### Case A — upgrading QUEUED slot

```
discount    = queued.totalPaid          (credit for already-paid queued slot)
final_price = targetPrice − queued.totalPaid
```

If `final_price ≤ 0` → free upgrade, completes without payment.

---

## Appendix: Package Level Hierarchy

```
BASIC → STANDARD → ADVANCED

- Upgrades: higher tier only (one-directional)
- Renewal: stays on same tier, same package
- Tier comparison for upgrade eligibility:
    Case A → target tier must be > QUEUED tier
    Case B → target tier must be > CURRENT tier
```

---

## Appendix: Scheduled Job (cron — existing, slightly enhanced)

The backend's existing **daily midnight job** now does two things in sequence:

1. Find all memberships where `status = ACTIVE AND endDate <= NOW()` → mark `EXPIRED`, expire benefits.
2. For each user whose slot just expired → find their queued membership (`status = ACTIVE AND startDate <= NOW()` — this is now the newly-current one) → **grant benefits** (set `grantedAt = NOW()`, `expiresAt = queued.endDate`) → send `MEMBERSHIP_ACTIVATED` notification.

The frontend does **not** trigger this. A fresh call to `GET /my-membership` after the job runs will show the previously-queued slot as `data.current`, with benefits populated.

No PENDING → ACTIVE state transition is ever needed — the job only ever needs to grant benefits and send a notification.
