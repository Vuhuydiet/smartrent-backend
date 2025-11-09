# SmartRent Package Flows - Visual Guide

## Quick Reference Diagrams

### 1. Membership Purchase Flow

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │ 1. GET /v1/memberships/packages
       ↓
┌──────────────┐
│   Backend    │ → Returns: [BASIC, STANDARD, ADVANCED]
└──────┬───────┘
       │
       │ User selects package
       │
       │ 2. POST /v1/memberships/initiate-purchase
       │    Body: { membershipId: 2, paymentProvider: "VNPAY", returnUrl: "..." }
       ↓
┌──────────────┐
│   Backend    │
│              │ → Create PENDING transaction (TransactionService)
│              │ → Generate VNPay URL with HMAC-SHA512 signature
└──────┬───────┘
       │
       │ Returns: { paymentUrl: "https://vnpay...", transactionRef: "uuid", amount: 1400000 }
       ↓
┌──────────────┐
│   Frontend   │ → Redirect user to VNPay
└──────┬───────┘
       │
       ↓
┌──────────────┐
│    VNPay     │ → User completes payment
└──────┬───────┘
       │
       │ 3. GET /v1/payments/callback/VNPAY?vnp_TxnRef=...&vnp_SecureHash=...
       ↓
┌──────────────┐
│   Backend    │
│              │ → Validate signature (HMAC-SHA512)
│              │ → Update transaction: PENDING → COMPLETED
│              │ → Call completeMembershipPurchase(transactionId)
│              │ → Create UserMembership (status: ACTIVE)
│              │ → Grant benefits (total_quantity = qty_per_month × months)
│              │ → Create UserMembershipBenefit records
└──────┬───────┘
       │
       │ Redirect to: https://app.com/payment/result?success=true&transactionRef=...
       ↓
┌──────────────┐
│   Frontend   │ → Show success message
│              │ → Display active membership & quotas
└──────────────┘
```

### 2. VIP Listing Creation - With Quota

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │ 1. GET /v1/memberships/quota/POST_SILVER
       ↓
┌──────────────┐
│   Backend    │ → Returns: { totalAvailable: 8, totalUsed: 2, totalGranted: 10 }
└──────┬───────┘
       │
       │ User has quota available
       │
       │ 2. POST /v1/listings/vip
       │    Body: { vipType: "SILVER", useMembershipQuota: true, durationDays: 30, ... }
       ↓
┌──────────────┐
│   Backend    │
│              │ → QuotaService.checkQuotaAvailability(userId, POST_SILVER)
│              │ → QuotaService.consumeQuota(userId, POST_SILVER, 1)
│              │ → Create listing:
│              │    - vip_type: SILVER
│              │    - post_source: QUOTA
│              │    - transaction_id: null
│              │    - status: ACTIVE (if AUTO_APPROVE benefit exists)
│              │    - expiry_date: now + durationDays
└──────┬───────┘
       │
       │ Returns: { listingId: 123, vipType: "SILVER", postSource: "QUOTA", status: "ACTIVE" }
       ↓
┌──────────────┐
│   Frontend   │ → Show success: "Listing created using quota"
└──────────────┘
```

### 3. VIP Listing Creation - Direct Payment

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │ 1. POST /v1/listings/vip
       │    Body: { vipType: "DIAMOND", useMembershipQuota: false, durationDays: 30,
       │            paymentProvider: "VNPAY", returnUrl: "...", ... }
       ↓
┌──────────────┐
│   Backend    │
│              │ → No quota or user chose payment
│              │ → Calculate price: DIAMOND 30 days = 6,846,000 VND (with 18.5% discount)
│              │ → TransactionService.createPostFeeTransaction(userId, amount, "DIAMOND", 30, "VNPAY")
│              │ → Store listing data in transaction.additionalInfo (JSON)
│              │ → Generate VNPay payment URL
└──────┬───────┘
       │
       │ Returns: { paymentUrl: "https://vnpay...", transactionRef: "uuid", amount: 6846000 }
       ↓
┌──────────────┐
│   Frontend   │ → Redirect to VNPay
└──────┬───────┘
       │
       ↓
┌──────────────┐
│    VNPay     │ → User pays
└──────┬───────┘
       │
       │ 2. GET /v1/payments/callback/VNPAY?vnp_TxnRef=...&vnp_ResponseCode=00
       ↓
┌──────────────┐
│   Backend    │
│              │ → Validate signature (HMAC-SHA512)
│              │ → Complete transaction (PENDING → COMPLETED)
│              │ → Retrieve listing data from transaction.additionalInfo
│              │ → Create listing:
│              │    - vip_type: DIAMOND
│              │    - post_source: DIRECT_PAYMENT
│              │    - transaction_id: uuid
│              │    - status: ACTIVE (auto-verify for paid VIP)
│              │    - expiry_date: now + 30 days
│              │ → Create shadow NORMAL listing (for DIAMOND):
│              │    - is_shadow: true
│              │    - parent_listing_id: diamond_listing_id
│              │    - vip_type: NORMAL
└──────┬───────┘
       │
       │ Redirect to: https://app.com/payment/result?success=true
       ↓
┌──────────────┐
│   Frontend   │ → Show success: "Listing created and published"
└──────────────┘
```

### 4. Push Listing - With Quota

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │ 1. GET /v1/memberships/quota/PUSH
       ↓
┌──────────────┐
│   Backend    │ → Returns: { totalAvailable: 18, totalUsed: 2, totalGranted: 20 }
└──────┬───────┘
       │
       │ 2. POST /v1/pushes/push
       │    Body: { listingId: 123, useMembershipQuota: true }
       ↓
┌──────────────┐
│   Backend    │
│              │ → QuotaService.checkQuotaAvailability(userId, PUSH)
│              │ → QuotaService.consumeQuota(userId, PUSH, 1)
│              │ → Create PushHistory:
│              │    - push_source: MEMBERSHIP_QUOTA
│              │    - transaction_id: null
│              │    - user_benefit_id: benefit_id
│              │ → Update listing:
│              │    - pushed_at: now()
│              │    - post_date: now() (pushes to top)
│              │ → If GOLD/DIAMOND: Push shadow listing too (FREE)
└──────┬───────┘
       │
       │ Returns: { listingId: 123, pushSource: "MEMBERSHIP_QUOTA", pushedAt: "2024-01-01T10:00:00" }
       ↓
┌──────────────┐
│   Frontend   │ → Show success: "Listing pushed to top"
└──────────────┘
```

### 5. Push Listing - Direct Payment

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │ 1. POST /v1/pushes/push
       │    Body: { listingId: 123, useMembershipQuota: false,
       │            paymentProvider: "VNPAY", returnUrl: "..." }
       ↓
┌──────────────┐
│   Backend    │
│              │ → No quota or user chose payment
│              │ → TransactionService.createPushFeeTransaction(userId, listingId, 40000, "VNPAY")
│              │ → Generate VNPay payment URL
└──────┬───────┘
       │
       │ Returns: { paymentUrl: "https://vnpay...", transactionRef: "uuid", amount: 40000 }
       ↓
┌──────────────┐
│   Frontend   │ → Redirect to VNPay
└──────┬───────┘
       │
       ↓
┌──────────────┐
│    VNPay     │ → User pays 40,000 VND
└──────┬───────┘
       │
       │ 2. GET /v1/payments/callback/VNPAY?vnp_TxnRef=...&vnp_ResponseCode=00
       ↓
┌──────────────┐
│   Backend    │
│              │ → Validate signature (HMAC-SHA512)
│              │ → Complete transaction (PENDING → COMPLETED)
│              │ → PushService.completePushAfterPayment(transactionId)
│              │ → Create PushHistory:
│              │    - push_source: DIRECT_PAYMENT
│              │    - transaction_id: uuid
│              │ → Update listing:
│              │    - pushed_at: now()
│              │    - post_date: now()
│              │ → If GOLD/DIAMOND: Push shadow listing (FREE)
└──────┬───────┘
       │
       │ Redirect to: https://app.com/payment/result?success=true
       ↓
┌──────────────┐
│   Frontend   │ → Show success: "Listing pushed to top"
└──────────────┘
```

### 6. Scheduled Push Flow

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │ 1. POST /v1/pushes/schedule
       │    Body: { listingId: 123, scheduledTime: "08:00:00", totalPushes: 30, useMembershipQuota: true }
       ↓
┌──────────────┐
│   Backend    │
│              │ → QuotaService.hasSufficientQuota(userId, PUSH, 30)
│              │ → Create PushSchedule:
│              │    - scheduled_time: 08:00:00
│              │    - total_pushes: 30
│              │    - used_pushes: 0
│              │    - status: ACTIVE
│              │    - source: MEMBERSHIP
│              │    - source_id: user_membership_id
│              │ → Does NOT consume quota yet (consumed daily)
└──────┬───────┘
       │
       │ Returns: { scheduleId: 456, listingId: 123, scheduledTime: "08:00:00",
       │            totalPushes: 30, usedPushes: 0, status: "ACTIVE" }
       ↓
┌──────────────┐
│   Frontend   │ → Show: "Scheduled 30 daily pushes at 8:00 AM"
└──────────────┘
       │
       │ ... Next day at 8:00 AM ...
       │
       ↓
┌──────────────┐
│  Cron Job    │ @Scheduled(cron = "0 0 * * * *") // Every hour
└──────┬───────┘
       │
       │ PushService.executeScheduledPushes()
       ↓
┌──────────────┐
│   Backend    │
│              │ → Find all ACTIVE schedules at current hour
│              │ → For each schedule:
│              │    - QuotaService.consumeQuota(userId, PUSH, 1)
│              │    - Create PushHistory (push_source: SCHEDULED, schedule_id: 456)
│              │    - Update listing (pushed_at: now(), post_date: now())
│              │    - Increment schedule.used_pushes
│              │    - If used_pushes >= total_pushes: status = COMPLETED
│              │    - If GOLD/DIAMOND: Push shadow listing (FREE)
└──────────────┘
```

## Decision Trees

### When to Use Quota vs Payment?

```
User wants to create VIP listing
    │
    ├─ Has active membership?
    │   │
    │   ├─ YES → Check quota for vip_type
    │   │   │
    │   │   ├─ Quota available?
    │   │   │   │
    │   │   │   ├─ YES → Use quota (FREE)
    │   │   │   │         - QuotaService.consumeQuota()
    │   │   │   │         - Create listing immediately
    │   │   │   │         - post_source: QUOTA
    │   │   │   │         - transaction_id: null
    │   │   │   │
    │   │   │   └─ NO → Require payment
    │   │   │             - TransactionService.createPostFeeTransaction()
    │   │   │             - Redirect to VNPay
    │   │   │             - post_source: DIRECT_PAYMENT
    │   │   │
    │   │   └─ User prefers payment → Require payment
    │   │
    │   └─ NO → Require payment
    │
    └─ Create listing after payment success
```

### VIP Type Selection

```
User selects VIP type
    │
    ├─ SILVER (1,222,500 VND/30 days with 18.5% discount)
    │   - Auto-verify (if AUTO_APPROVE benefit)
    │   - 10 images, 2 videos
    │   - No ads
    │   - Blue "VIP BẠC" badge
    │   - Priority display
    │   - Requires: POST_SILVER quota or payment
    │
    ├─ GOLD (2,689,500 VND/30 days with 18.5% discount)
    │   - All SILVER features
    │   - Higher priority display
    │   - Blue "VIP VÀNG" badge
    │   - Requires: POST_GOLD quota or payment
    │
    └─ DIAMOND (6,846,000 VND/30 days with 18.5% discount)
        - All GOLD features
        - Red "VIP KIM CƯƠNG" badge
        - HIGHEST priority display
        - Top homepage placement
        - 15 images, 3 videos
        - Shadow NORMAL listing (FREE)
        - Push DIAMOND → Shadow also pushed (FREE)
        - Requires: POST_DIAMOND quota or payment
```

## State Diagrams

### Transaction States

```
┌─────────┐
│ PENDING │ ← Transaction created
└────┬────┘
     │
     ├─ Payment successful → ┌───────────┐
     │                       │ COMPLETED │
     │                       └───────────┘
     │
     ├─ Payment failed ────→ ┌────────┐
     │                       │ FAILED │
     │                       └────────┘
     │
     └─ User cancelled ────→ ┌───────────┐
       or timeout            │ CANCELLED │
                             └───────────┘
```

### Membership Status

```
┌────────┐
│ ACTIVE │ ← Membership purchased
└───┬────┘
    │
    ├─ end_date reached ──→ ┌─────────┐
    │                       │ EXPIRED │
    │                       └─────────┘
    │
    └─ User cancels ──────→ ┌───────────┐
                            │ CANCELLED │
                            └───────────┘
```

### Benefit Status

```
┌────────┐
│ ACTIVE │ ← Benefit granted (UserMembershipBenefit)
└───┬────┘
    │
    ├─ quantity_used >= total_quantity → ┌─────────────┐
    │                                    │ FULLY_USED  │
    │                                    └─────────────┘
    │
    └─ expires_at reached ─────────────→ ┌─────────┐
                                         │ EXPIRED │
                                         └─────────┘

Note: Quota consumption updates quantity_used and status automatically
```

## Sequence Diagrams

### Complete Membership Purchase Sequence

```
User    Frontend    Backend    VNPay    Database
 │         │          │          │          │
 │ Browse  │          │          │          │
 │────────>│          │          │          │
 │         │ GET /packages       │          │
 │         │─────────>│          │          │
 │         │          │ Query    │          │
 │         │          │─────────────────────>│
 │         │          │<─────────────────────│
 │         │<─────────│          │          │
 │<────────│          │          │          │
 │         │          │          │          │
 │ Select  │          │          │          │
 │────────>│          │          │          │
 │         │ POST /initiate      │          │
 │         │─────────>│          │          │
 │         │          │ Create TX│          │
 │         │          │─────────────────────>│
 │         │          │ Generate URL        │
 │         │<─────────│          │          │
 │<────────│          │          │          │
 │         │          │          │          │
 │ Redirect to VNPay  │          │          │
 │────────────────────────────────>│        │
 │         │          │          │          │
 │ Pay     │          │          │          │
 │────────────────────────────────>│        │
 │         │          │          │          │
 │         │          │ Callback │          │
 │         │          │<─────────│          │
 │         │          │ Validate │          │
 │         │          │ Complete TX         │
 │         │          │─────────────────────>│
 │         │          │ Create Membership   │
 │         │          │─────────────────────>│
 │         │          │ Grant Benefits      │
 │         │          │─────────────────────>│
 │         │          │          │          │
 │ Redirect to success│          │          │
 │<───────────────────│          │          │
 │         │          │          │          │
```