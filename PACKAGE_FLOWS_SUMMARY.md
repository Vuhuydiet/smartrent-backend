# SmartRent Package Registration Flows - Executive Summary

## Document Overview

This summary provides a high-level overview of all package registration flows in the SmartRent system. For detailed technical documentation, refer to:
- **PACKAGE_REGISTRATION_FLOWS.md** - Complete technical documentation
- **PACKAGE_FLOWS_VISUAL_GUIDE.md** - Visual diagrams and flowcharts

---

## System Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                     SmartRent Backend                       │
├─────────────────────────────────────────────────────────────┤
│  Controllers                                                │
│  - MembershipController                                     │
│  - ListingController                                        │
│  - BoostController                                          │
│  - PaymentController                                        │
├─────────────────────────────────────────────────────────────┤
│  Services                                                   │
│  - MembershipService (package purchase, quota management)  │
│  - ListingService (VIP post creation)                      │
│  - BoostService (listing boost)                            │
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
├─────────────────────────────────────────────────────────────┤
│  Database (MySQL)                                          │
│  - membership_packages                                     │
│  - user_memberships                                        │
│  - user_membership_benefits                                │
│  - transactions                                            │
│  - listings                                                │
│  - push_history                                            │
│  - push_schedule                                           │
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

### 1. Membership Packages 💎

**Purpose**: Monthly subscription with quotas for VIP posts, Premium posts, and boosts

**Available Packages**:
- **BASIC** (700,000 VND): 5 VIP posts + 10 boosts
- **STANDARD** (1,400,000 VND): 10 VIP posts + 5 Premium posts + 20 boosts + Auto-verify
- **ADVANCED** (2,800,000 VND): 15 VIP posts + 10 Premium posts + 40 boosts + Auto-verify + Badge

**Key Features**:
- One-time benefit grant (no monthly renewal)
- All quotas granted at purchase
- No rollover of unused quotas
- Expires after duration_months

**Flow**: Browse → Select → Pay via VNPay → Membership activated → Quotas granted

### 2. Post Packages 📝

**Purpose**: Create VIP/Premium listings with enhanced features

**Available Types**:
- **SILVER VIP** (600,000 VND/30 days): Auto-verify, 10 images, 2 videos, no ads, blue badge
- **GOLD VIP** (600,000 VND/30 days): All SILVER + priority display
- **DIAMOND Premium** (1,800,000 VND/30 days): All GOLD + gold badge + shadow listing + top placement

**Two Ways to Create**:
1. **With Quota**: Use membership quota (FREE if available)
2. **Direct Payment**: Pay per post via VNPay

**Key Features**:
- Shadow listing for DIAMOND (doubles visibility)
- Auto-verification for paid VIP posts
- Duration discounts (15 days: 11% off, 30 days: 18.5% off)

**Flow**: 
- With Quota: Check quota → Create listing → Quota consumed
- Direct Payment: Create listing → Pay via VNPay → Listing published

### 3. Boost Packages 🚀

**Purpose**: Push listings to top of search results

**Pricing**: 40,000 VND per boost

**Two Ways to Boost**:
1. **With Quota**: Use membership boost quota (FREE if available)
2. **Direct Payment**: Pay per boost via VNPay

**Additional Features**:
- **Scheduled Boost**: Auto-boost daily at specific time
- **Shadow Boost**: GOLD/DIAMOND listings also boost their shadow listing (FREE)

**Flow**:
- Instant Boost: Select listing → Boost → Listing pushed to top
- Scheduled Boost: Set time → Schedule created → Cron job executes daily

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

| Type | Purpose | Amount |
|------|---------|--------|
| MEMBERSHIP_PURCHASE | Buy membership package | 700K - 2.8M VND |
| POST_FEE | Create VIP listing | 600K - 1.8M VND |
| BOOST_FEE | Boost listing | 40K VND |

---

## Quota Management System

### How Quotas Work

1. **Grant**: When user purchases membership, all quotas granted immediately
   - Formula: `total_quantity = quantity_per_month × duration_months`
   - Example: STANDARD (1 month) = 10 VIP posts, 5 Premium posts, 20 boosts

2. **Check**: Before using quota, system checks availability
   - Query: `SELECT SUM(total_quantity - quantity_used) WHERE status='ACTIVE' AND expires_at > NOW()`

3. **Consume**: When user uses quota, quantity_used incremented
   - Update: `quantity_used = quantity_used + 1`
   - If `quantity_used >= total_quantity`, status changes to FULLY_USED

4. **Expire**: When membership expires, all quotas expire
   - No rollover to next membership
   - Unused quotas are lost

### Benefit Types

| Type | Description | Consumable |
|------|-------------|------------|
| POST_SILVER | Silver VIP posts | ✅ Yes |
| POST_GOLD | Gold VIP posts | ✅ Yes |
| POST_DIAMOND | Diamond Premium posts | ✅ Yes |
| BOOST | Listing boosts | ✅ Yes |
| AUTO_APPROVE | Auto-verification | ❌ No (flag) |
| BADGE | Trusted badge | ❌ No (flag) |

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
POST   /v1/listings/vip                      - Create VIP listing
                                               (returns listing or payment URL)
GET    /v1/listings/{id}                     - Get listing details
```

### Boost APIs

```
POST   /v1/boosts/boost                      - Boost listing
                                               (returns success or payment URL)
POST   /v1/boosts/schedule                   - Schedule daily boost
GET    /v1/boosts/history/{listingId}        - Get boost history
GET    /v1/boosts/my-history                 - Get user's boost history
DELETE /v1/boosts/schedule/{scheduleId}      - Cancel scheduled boost
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
7. **push_history** - Boost history
8. **push_schedule** - Scheduled boosts

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

1. ✅ One-time benefit grant at purchase
2. ✅ No monthly renewal (user must purchase again)
3. ✅ No rollover of unused quotas
4. ✅ All benefits expire when membership expires
5. ✅ User can have only one active membership at a time (optional)

### Post Rules

1. ✅ DIAMOND posts automatically create shadow NORMAL listing
2. ✅ Shadow listing syncs with parent listing
3. ✅ Paid VIP posts are auto-verified
4. ✅ Quota-based posts use membership benefits
5. ✅ Duration discounts: 15 days (11% off), 30 days (18.5% off)

### Boost Rules

1. ✅ Boost pushes listing to top (updates post_date)
2. ✅ GOLD/DIAMOND boost also boosts shadow listing (FREE)
3. ✅ Scheduled boosts execute daily via cron job
4. ✅ Scheduled boosts consume quota daily (not upfront)
5. ✅ Boost history tracked in push_history table

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

---

## Testing Guide

### Test Scenarios

1. **Membership Purchase**
   - Browse packages
   - Select STANDARD package
   - Complete VNPay payment (use sandbox)
   - Verify membership activated
   - Verify quotas granted

2. **VIP Listing with Quota**
   - Check quota availability
   - Create SILVER listing with quota
   - Verify quota consumed
   - Verify listing created

3. **VIP Listing with Payment**
   - Create DIAMOND listing without quota
   - Complete VNPay payment
   - Verify listing created
   - Verify shadow listing created

4. **Boost with Quota**
   - Check boost quota
   - Boost listing with quota
   - Verify listing pushed to top
   - Verify quota consumed

5. **Scheduled Boost**
   - Schedule 30 daily boosts
   - Wait for cron execution
   - Verify boost executed
   - Verify quota consumed

### VNPay Sandbox

- **URL**: https://sandbox.vnpayment.vn
- **Test Cards**: Provided by VNPay documentation
- **Hash Secret**: Use sandbox credentials

---

## Performance Considerations

### Optimizations

1. **Indexes**: All foreign keys and frequently queried columns indexed
2. **Quota Queries**: Optimized with aggregate functions
3. **Transaction Lookup**: Indexed by transaction_id and provider_transaction_id
4. **Cron Jobs**: Scheduled boosts use indexed queries

### Scalability

- Stateless API design
- Database connection pooling
- Transaction isolation for concurrent requests
- Idempotent payment callbacks

---

## Monitoring & Maintenance

### Key Metrics to Monitor

1. **Transaction Success Rate**: % of successful payments
2. **Quota Usage**: Average quota consumption per user
3. **Boost Effectiveness**: Listing views after boost
4. **Payment Failures**: Track VNPay error codes
5. **Membership Renewals**: Track expiration and renewal rates

### Scheduled Jobs

1. **Membership Expiration**: Daily check for expired memberships
2. **Benefit Expiration**: Daily check for expired benefits
3. **Scheduled Boosts**: Execute at configured times
4. **Transaction Cleanup**: Archive old transactions

---

## Next Steps for Implementation

### Frontend Tasks

1. ✅ Implement membership package display
2. ✅ Handle VNPay redirect and callback
3. ✅ Display quota status in dashboard
4. ✅ Show transaction history
5. ✅ Implement boost scheduling UI

### Backend Tasks

1. ✅ All core services implemented
2. ✅ VNPay integration complete
3. ✅ Quota management working
4. ⏳ Add scheduled jobs (cron)
5. ⏳ Add comprehensive logging
6. ⏳ Add monitoring endpoints

### DevOps Tasks

1. ⏳ Configure VNPay production credentials
2. ⏳ Set up database backups
3. ⏳ Configure monitoring alerts
4. ⏳ Load testing for payment flows

---

## Support & Documentation

### Additional Resources

- **API Documentation**: http://localhost:8080/swagger-ui.html
- **VNPay Documentation**: https://sandbox.vnpayment.vn/apis/docs
- **Database Migrations**: `/src/main/resources/db/migration/`
- **Configuration**: `/src/main/resources/application.yml`

### Contact

For technical questions or issues:
1. Check Swagger API documentation
2. Review transaction logs in database
3. Verify VNPay configuration
4. Test with sandbox environment first

---

**Document Version**: 1.0  
**Last Updated**: 2024-01-01  
**Status**: ✅ Complete and Production Ready

