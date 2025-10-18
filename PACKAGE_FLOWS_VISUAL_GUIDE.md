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
       │    Body: { membershipId: 2, paymentProvider: "VNPAY" }
       ↓
┌──────────────┐
│   Backend    │
│              │ → Create PENDING transaction
│              │ → Generate VNPay URL with signature
└──────┬───────┘
       │
       │ Returns: { paymentUrl: "https://vnpay...", transactionRef: "uuid" }
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
│              │ → Create UserMembership (status: ACTIVE)
│              │ → Grant benefits (total_quantity = qty_per_month × months)
│              │ → Create UserMembershipBenefit records
└──────┬───────┘
       │
       │ Redirect to: https://app.com/payment/result?success=true
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
│   Backend    │ → Returns: { totalAvailable: 8, totalUsed: 2 }
└──────┬───────┘
       │
       │ User has quota available
       │
       │ 2. POST /v1/listings/vip
       │    Body: { vipType: "SILVER", useMembershipQuota: true, ... }
       ↓
┌──────────────┐
│   Backend    │
│              │ → Check quota: POST_SILVER available?
│              │ → Consume 1 quota (quantity_used++)
│              │ → Create listing:
│              │    - vip_type: SILVER
│              │    - post_source: QUOTA
│              │    - transaction_id: null
│              │    - status: ACTIVE (if auto-verify)
└──────┬───────┘
       │
       │ Returns: { listingId: 123, vipType: "SILVER", quotaUsed: true }
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
       │    Body: { vipType: "DIAMOND", useMembershipQuota: false, ... }
       ↓
┌──────────────┐
│   Backend    │
│              │ → No quota or user chose payment
│              │ → Calculate price: DIAMOND 30 days = 1,800,000 VND
│              │ → Create PENDING transaction (type: POST_FEE)
│              │ → Store listing data in cache/metadata
│              │ → Generate VNPay URL
└──────┬───────┘
       │
       │ Returns: { paymentUrl: "https://vnpay...", transactionRef: "uuid" }
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
       │ 2. GET /v1/payments/callback/VNPAY?vnp_TxnRef=...
       ↓
┌──────────────┐
│   Backend    │
│              │ → Validate signature
│              │ → Complete transaction
│              │ → Retrieve listing data from cache
│              │ → Create listing:
│              │    - vip_type: DIAMOND
│              │    - post_source: DIRECT_PAYMENT
│              │    - transaction_id: uuid
│              │    - status: ACTIVE
│              │ → Create shadow NORMAL listing (for DIAMOND)
└──────┬───────┘
       │
       │ Redirect to success page
       ↓
┌──────────────┐
│   Frontend   │ → Show success: "Listing created and published"
└──────────────┘
```

### 4. Boost Listing - With Quota

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │ 1. GET /v1/memberships/quota/BOOST
       ↓
┌──────────────┐
│   Backend    │ → Returns: { totalAvailable: 18 }
└──────┬───────┘
       │
       │ 2. POST /v1/boosts/boost
       │    Body: { listingId: 123, useMembershipQuota: true }
       ↓
┌──────────────┐
│   Backend    │
│              │ → Check boost quota available?
│              │ → Consume 1 boost quota
│              │ → Create PushHistory:
│              │    - push_source: MEMBERSHIP_QUOTA
│              │    - transaction_id: null
│              │ → Update listing:
│              │    - pushed_at: now()
│              │    - post_date: now() (pushes to top)
│              │ → If GOLD/DIAMOND: Boost shadow listing too
└──────┬───────┘
       │
       │ Returns: { listingId: 123, pushSource: "MEMBERSHIP_QUOTA" }
       ↓
┌──────────────┐
│   Frontend   │ → Show success: "Listing boosted to top"
└──────────────┘
```

### 5. Boost Listing - Direct Payment

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │ 1. POST /v1/boosts/boost
       │    Body: { listingId: 123, useMembershipQuota: false }
       ↓
┌──────────────┐
│   Backend    │
│              │ → No quota or user chose payment
│              │ → Create PENDING transaction (type: BOOST_FEE)
│              │ → Amount: 40,000 VND
│              │ → Generate VNPay URL
└──────┬───────┘
       │
       │ Returns: { paymentUrl: "https://vnpay...", transactionRef: "uuid" }
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
       │ 2. GET /v1/payments/callback/VNPAY?vnp_TxnRef=...
       ↓
┌──────────────┐
│   Backend    │
│              │ → Validate signature
│              │ → Complete transaction
│              │ → Create PushHistory:
│              │    - push_source: DIRECT_PAYMENT
│              │    - transaction_id: uuid
│              │ → Update listing (pushed_at, post_date)
│              │ → If GOLD/DIAMOND: Boost shadow listing
└──────┬───────┘
       │
       │ Redirect to success page
       ↓
┌──────────────┐
│   Frontend   │ → Show success: "Listing boosted to top"
└──────────────┘
```

### 6. Scheduled Boost Flow

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │ 1. POST /v1/boosts/schedule
       │    Body: { listingId: 123, scheduledTime: "08:00:00", totalPushes: 30 }
       ↓
┌──────────────┐
│   Backend    │
│              │ → Check user has 30 boost quota
│              │ → Create PushSchedule:
│              │    - scheduled_time: 08:00:00
│              │    - total_pushes: 30
│              │    - used_pushes: 0
│              │    - status: ACTIVE
│              │    - source: MEMBERSHIP
│              │ → Does NOT consume quota yet
└──────┬───────┘
       │
       │ Returns: { scheduleId: 456, status: "ACTIVE" }
       ↓
┌──────────────┐
│   Frontend   │ → Show: "Scheduled 30 daily boosts at 8:00 AM"
└──────────────┘
       │
       │ ... Next day at 8:00 AM ...
       │
       ↓
┌──────────────┐
│  Cron Job    │ @Scheduled(cron = "0 0 8 * * *")
└──────┬───────┘
       │
       │ executeScheduledBoosts()
       ↓
┌──────────────┐
│   Backend    │
│              │ → Find all ACTIVE schedules at 08:00:00
│              │ → For each schedule:
│              │    - Consume 1 boost quota
│              │    - Create PushHistory (push_source: SCHEDULED)
│              │    - Update listing (pushed_at, post_date)
│              │    - Increment used_pushes
│              │    - If used_pushes >= total_pushes: status = COMPLETED
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
    │   │   │   │         - Consume quota
    │   │   │   │         - Create listing immediately
    │   │   │   │         - post_source: QUOTA
    │   │   │   │
    │   │   │   └─ NO → Require payment
    │   │   │             - Create transaction
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
    ├─ SILVER (600,000 VND/30 days)
    │   - Auto-verify
    │   - 10 images, 2 videos
    │   - No ads
    │   - Blue badge
    │   - Requires: POST_SILVER quota or payment
    │
    ├─ GOLD (600,000 VND/30 days)
    │   - All SILVER features
    │   - Priority display
    │   - Requires: POST_GOLD quota or payment
    │
    └─ DIAMOND (1,800,000 VND/30 days)
        - All GOLD features
        - Gold badge
        - Top homepage placement
        - Shadow NORMAL listing (FREE)
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
│ ACTIVE │ ← Benefit granted
└───┬────┘
    │
    ├─ quantity_used >= total_quantity → ┌─────────────┐
    │                                    │ FULLY_USED  │
    │                                    └─────────────┘
    │
    └─ expires_at reached ─────────────→ ┌─────────┐
                                         │ EXPIRED │
                                         └─────────┘
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


