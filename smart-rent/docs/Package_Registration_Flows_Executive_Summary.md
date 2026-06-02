# SmartRent Package Registration Flows - Executive Summary

## Document Overview

This summary provides a high-level overview of all package registration flows in the SmartRent system.

## System Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                     SmartRent Backend                       │
├─────────────────────────────────────────────────────────────┤
│  Controllers                                                │
│  - MembershipController (/v1/memberships)                  │
│  - ListingController (/v1/listings)                        │
│  - PushController (/v1/pushes)                             │
│  - PaymentController (/v1/payments)                        │
├─────────────────────────────────────────────────────────────┤
│  Services                                                   │
│  - MembershipService (package purchase, quota management)  │
│  - ListingService (VIP post creation)                      │
│  - PushService (listing push/boost)                        │
│  - PaymentService (VNPay integration)                      │
│  - TransactionService (transaction lifecycle)              │
│  - QuotaService (quota checking & consumption)             │
├─────────────────────────────────────────────────────────────┤
│  Repositories                                               │
│  - MembershipPackageRepository                             │
│  - UserMembershipRepository                                │
│  - UserMembershipBenefitRepository                         │
│  - TransactionRepository                                   │
│  - ListingRepository                                       │
│  - PushHistoryRepository                                   │
│  - PushScheduleRepository                                  │
├─────────────────────────────────────────────────────────────┤
│  Database (MySQL)                                          │
│  - membership_packages                                     │
│  - membership_package_benefits                             │
│  - user_memberships                                        │
│  - user_membership_benefits                                │
│  - transactions                                            │
│  - listings                                                │
│  - push_history                                            │
│  - push_schedule                                           │
│  - vip_tier_details                                        │
│  - push_details                                            │
└─────────────────────────────────────────────────────────────┘
         │                                    │
         │                                    │
         ↓                                    ↓
┌──────────────────┐              ┌──────────────────┐
│  VNPay Gateway   │              │  Frontend App    │
│  (Payment)       │              │  (React/Vue)     │
└──────────────────┘              └──────────────────┘
```

---

## Three Main Package Types

### 1. Membership Packages

**Purpose**: Monthly subscription with quotas for VIP posts and pushes

**Available Packages**:
- **BASIC** (700,000 VND/month): 5 Silver posts + 10 pushes
- **STANDARD** (1,400,000 VND/month): 10 Silver posts + 5 Gold posts + 20 pushes + Auto-verify
- **ADVANCED** (2,800,000 VND/month): 15 Silver posts + 10 Gold posts + 40 pushes + Auto-verify + Badge

**Key Features**:
- One-time benefit grant (no monthly renewal)
- All quotas granted at purchase: total_quantity = quantity_per_month × duration_months
- No rollover of unused quotas
- Expires after duration_months
- Managed by MembershipService and QuotaService

**Flow**: Browse → Select → Pay via VNPay → Membership activated → Quotas granted

### 2. Post Packages

**Purpose**: Create VIP listings with enhanced features

**Available Types**:
- **SILVER VIP** (1,222,500 VND/30 days): Auto-verify, 10 images, 2 videos, no ads, blue badge, priority display
- **GOLD VIP** (2,689,500 VND/30 days): All SILVER + higher priority, blue "VIP VÀNG" badge
- **DIAMOND VIP** (6,846,000 VND/30 days): All GOLD + red badge, HIGHEST priority, 15 images/3 videos, shadow listing, top placement

**Pricing with Duration Discounts**:
- 10 days: Base price (no discount)
- 15 days: 11% discount
- 30 days: 18.5% discount

**Two Ways to Create**:
1. **With Quota**: Use membership quota (FREE if available) - POST_SILVER, POST_GOLD, or POST_DIAMOND
2. **Direct Payment**: Pay per post via VNPay

**Key Features**:
- Shadow NORMAL listing for DIAMOND (doubles visibility)
- Auto-verification if user has AUTO_APPROVE benefit
- post_source: QUOTA or DIRECT_PAYMENT
- Managed by ListingService

**Flow**:
- With Quota: Check quota → Create listing → Quota consumed
- Direct Payment: Create listing → Pay via VNPay → Listing published

### 3. Push Packages

**Purpose**: Push listings to top of search results

**Pricing**: 40,000 VND per push

**Two Ways to Push**:
1. **With Quota**: Use membership PUSH quota (FREE if available)
2. **Direct Payment**: Pay per push via VNPay

**Additional Features**:
- **Scheduled Push**: Auto-push daily at specific time (consumes quota daily)
- **Shadow Push**: GOLD/DIAMOND listings also push their shadow listing (FREE)
- push_source: MEMBERSHIP_QUOTA, DIRECT_PAYMENT, or SCHEDULED
- Managed by PushService

**Flow**:
- Instant Push: Select listing → Push → Listing pushed to top
- Scheduled Push: Set time → Schedule created → Cron job executes hourly

---

## Payment Integration (VNPay)

### Payment Flow

```
1. User initiates action (buy membership, create VIP post, boost)
2. Backend creates PENDING transaction
3. Backend generates VNPay payment URL with HMAC-SHA512 signature
4. User redirected to VNPay portal
5. User completes payment
6. VNPay sends callback to backend
7. Backend validates signature
8. Backend updates transaction to COMPLETED
9. Backend executes business logic (activate membership, create listing, boost)
10. User redirected to success page
```

### Security

- **Signature Algorithm**: HMAC-SHA512
- **Parameter Validation**: All VNPay parameters validated
- **Idempotency**: Duplicate transactions prevented
- **Timeout**: 15 minutes payment window

### Transaction Types

| Type | Purpose | Amount | Reference Type |
|------|---------|--------|----------------|
| MEMBERSHIP_PURCHASE | Buy membership package | 700K - 2.8M VND | MEMBERSHIP |
| POST_FEE | Create VIP listing | 1.2M - 6.8M VND | LISTING |
| PUSH_FEE | Push listing | 40K VND | PUSH |
| WALLET_TOPUP | Top up wallet | Variable | WALLET |
| REFUND | Refund transaction | Variable | Various |

---

## Quota Management System

### How Quotas Work

1. **Grant**: When user purchases membership, all quotas granted immediately (MembershipService.grantBenefits)
    - Formula: `total_quantity = quantity_per_month × duration_months`
    - Example: STANDARD (1 month) = 10 Silver posts, 5 Gold posts, 20 pushes
    - Creates UserMembershipBenefit records with status ACTIVE

2. **Check**: Before using quota, system checks availability (QuotaService.checkQuotaAvailability)
    - Query: `SELECT SUM(total_quantity - quantity_used) WHERE status='ACTIVE' AND expires_at > NOW()`
    - Returns: totalAvailable, totalUsed, totalGranted

3. **Consume**: When user uses quota, quantity_used incremented (QuotaService.consumeQuota)
    - Update: `quantity_used = quantity_used + 1`
    - If `quantity_used >= total_quantity`, status changes to FULLY_USED
    - Creates audit trail in push_history or listing record

4. **Expire**: When membership expires, all quotas expire
    - No rollover to next membership
    - Unused quotas are lost
    - Scheduled job checks expiry_date daily

### Benefit Types

| Type | Description | Consumable | Enum Value |
|------|-------------|------------|------------|
| POST_SILVER | Silver VIP posts | ✅ Yes | BenefitType.POST_SILVER |
| POST_GOLD | Gold VIP posts | ✅ Yes | BenefitType.POST_GOLD |
| POST_DIAMOND | Diamond VIP posts | ✅ Yes | BenefitType.POST_DIAMOND |
| PUSH | Listing pushes | ✅ Yes | BenefitType.PUSH |
| AUTO_APPROVE | Auto-verification | ❌ No (flag) | BenefitType.AUTO_APPROVE |
| BADGE | Trusted badge | ❌ No (flag) | BenefitType.BADGE |

---

## Key API Endpoints

### Membership APIs

```
GET    /v1/memberships/packages              - List all packages
GET    /v1/memberships/packages/{id}         - Get package details
POST   /v1/memberships/initiate-purchase     - Start purchase (returns VNPay URL)
GET    /v1/memberships/my-membership         - Get active membership
GET    /v1/memberships/quota/{benefitType}   - Check specific quota
GET    /v1/memberships/quota/all             - Check all quotas
```

### Listing APIs

```
POST   /v1/listings                          - Create listing (NORMAL/SILVER/GOLD/DIAMOND)
                                               (returns listing or payment URL)
GET    /v1/listings/{id}                     - Get listing details
```

### Push APIs

```
POST   /v1/pushes/push                       - Push listing
                                               (returns success or payment URL)
POST   /v1/pushes/schedule                   - Schedule daily push
GET    /v1/pushes/history/{listingId}        - Get push history
GET    /v1/pushes/my-history                 - Get user's push history
DELETE /v1/pushes/schedule/{scheduleId}      - Cancel scheduled push
```

### Payment APIs

```
GET    /v1/payments/callback/VNPAY           - VNPay callback handler
POST   /v1/payments/ipn/VNPAY                - VNPay IPN handler
GET    /v1/payments/transactions/{txnRef}    - Query transaction
```

---

## Database Schema Summary

### Core Tables

1. **membership_packages** - Package definitions (BASIC, STANDARD, ADVANCED)
2. **membership_package_benefits** - Benefits per package
3. **user_memberships** - User's active/expired memberships
4. **user_membership_benefits** - User's quota tracking
5. **transactions** - All payment transactions
6. **listings** - Property listings (with vip_type, post_source)
7. **push_history** - Push history
8. **push_schedule** - Scheduled pushes
9. **vip_tier_details** - VIP tier pricing and features
10. **push_details** - Push pricing details

### Key Relationships

```
membership_packages (1) ←→ (N) membership_package_benefits
        ↓
user_memberships (1) ←→ (N) user_membership_benefits
        ↓
    transactions
        ↓
    listings / push_history
```

---

## Business Rules

### Membership Rules

1. One-time benefit grant at purchase
2. No monthly renewal (user must purchase again)
3. No rollover of unused quotas
4. All benefits expire when membership expires
5. User can have only one active membership at a time (optional)

### Post Rules

1. DIAMOND posts automatically create shadow NORMAL listing
2. Shadow listing syncs with parent listing
3. Paid VIP posts are auto-verified
4. Quota-based posts use membership benefits
5. Duration discounts: 15 days (11% off), 30 days (18.5% off)
6. VIP types: NORMAL, SILVER, GOLD, DIAMOND

### Push Rules

1. Push pushes listing to top (updates post_date)
2. GOLD/DIAMOND push also pushes shadow listing (FREE)
3. Scheduled pushes execute hourly via cron job
4. Scheduled pushes consume quota daily (not upfront)
5. Push history tracked in push_history table
6. Push price: 40,000 VND per push

---

## Error Handling

### Common Errors

| Error | HTTP Status | Description |
|-------|-------------|-------------|
| INSUFFICIENT_QUOTA | 400 | Not enough quota available |
| PAYMENT_FAILED | 402 | VNPay payment failed |
| INVALID_CALLBACK | 400 | Invalid payment signature |
| DUPLICATE_TRANSACTION | 409 | Transaction already processed |
| MEMBERSHIP_NOT_FOUND | 404 | Package doesn't exist |
| MEMBERSHIP_EXPIRED | 400 | User's membership expired |

### VNPay Response Codes

- **00**: Success
- **24**: Cancelled by user
- **51**: Insufficient balance
- **79**: Payment timeout
