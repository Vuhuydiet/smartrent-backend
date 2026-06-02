# SmartRent Package Registration Flows Documentation

## Table of Contents
1. [Overview](#overview)
2. [Membership Package Registration Flow](#membership-package-registration-flow)
3. [Post Package Registration Flow](#post-package-registration-flow)
4. [Push Package Registration Flow](#push-package-registration-flow)
5. [Payment Integration](#payment-integration)
6. [Quota Management](#quota-management)
7. [API Endpoints](#api-endpoints)
8. [Database Schema](#database-schema)
9. [Error Handling](#error-handling)

---

## Overview

SmartRent provides three main package types:
1. **Membership Packages** - Monthly subscriptions with quotas for VIP posts and pushes
2. **Post Packages** - Direct payment for creating VIP listings (SILVER, GOLD, DIAMOND)
3. **Push Packages** - Direct payment for pushing listings to increase visibility

All payments are processed through **VNPay** payment gateway.

### Key Concepts

- **Quota-based**: Users with active membership can use their allocated quotas
- **Direct Payment**: Users without quota or who prefer to pay directly use VNPay
- **One-time Grant**: Membership benefits are granted once at purchase (total_quantity = quantity_per_month × duration_months)
- **No Rollover**: Unused quotas expire with membership
- **VIP Tiers**: NORMAL, SILVER, GOLD, DIAMOND with different pricing and features
- **Shadow Listings**: DIAMOND tier automatically creates a NORMAL shadow listing for double visibility

---

## Membership Package Registration Flow

### 1. Available Membership Packages

| Package | Price | Duration | Benefits |
|---------|-------|----------|----------|
| **BASIC** | 700,000 VND | 1 month | 5 Silver posts, 10 pushes |
| **STANDARD** | 1,400,000 VND | 1 month | 10 Silver posts, 5 Gold posts, 20 pushes, Auto-verify |
| **ADVANCED** | 2,800,000 VND | 1 month | 15 Silver posts, 10 Gold posts, 40 pushes, Auto-verify, Trusted badge |

### 2. Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                  MEMBERSHIP PURCHASE FLOW                       │
└─────────────────────────────────────────────────────────────────┘

Step 1: User Browses Packages
   ↓
   GET /v1/memberships/packages
   ← Returns: List of active membership packages with benefits

Step 2: User Initiates Purchase
   ↓
   POST /v1/memberships/initiate-purchase
   Body: {
     "membershipId": 2,
     "paymentProvider": "VNPAY",
     "returnUrl": "https://app.com/payment/callback"
   }
   ↓
   Backend Actions:
   - Validates user exists
   - Validates membership package is active
   - Creates PENDING transaction (TransactionType: MEMBERSHIP_PURCHASE)
   - Generates VNPay payment URL with signature
   ↓
   ← Returns: {
     "paymentUrl": "https://sandbox.vnpayment.vn/...",
     "transactionRef": "uuid-transaction-id",
     "amount": 1400000,
     "expiresAt": "2024-01-01T10:15:00"
   }

Step 3: User Redirected to VNPay
   ↓
   User completes payment on VNPay portal

Step 4: VNPay Callback
   ↓
   GET /v1/payments/callback/VNPAY?vnp_TxnRef=...&vnp_SecureHash=...
   ↓
   Backend Actions:
   - Validates VNPay signature (HMAC-SHA512)
   - Finds transaction by vnp_TxnRef
   - Updates transaction status to COMPLETED
   - Calls completeMembershipPurchase(transactionId)
   ↓
   completeMembershipPurchase:
   - Creates UserMembership record (status: ACTIVE)
   - Calculates start_date (now) and end_date (now + duration_months)
   - Grants ALL benefits immediately:
     * For each benefit in package:
       - total_quantity = quantity_per_month × duration_months
       - Creates UserMembershipBenefit record
       - Sets status: ACTIVE, quantity_used: 0
   ↓
   ← Redirects to: https://app.com/payment/result?success=true&transactionRef=...

Step 5: User Sees Success Page
   ↓
   Frontend can query:
   GET /v1/memberships/my-membership
   ← Returns: Active membership with all benefits and quotas
```

### 3. Implementation Details

#### Service: `MembershipServiceImpl.initiateMembershipPurchase()`

```java
// 1. Validate user and package
User user = userRepository.findById(userId).orElseThrow();
MembershipPackage package = membershipPackageRepository.findById(membershipId).orElseThrow();

// 2. Create PENDING transaction
String transactionId = transactionService.createMembershipTransaction(
    userId, membershipId, package.getSalePrice(), "VNPAY"
);

// 3. Generate VNPay payment URL
PaymentRequest paymentRequest = PaymentRequest.builder()
    .provider(PaymentProvider.VNPAY)
    .amount(package.getSalePrice())
    .orderInfo("Membership: " + package.getPackageName())
    .returnUrl(request.getReturnUrl())
    .build();

PaymentResponse response = paymentService.createPayment(paymentRequest, null);
return response; // Contains paymentUrl
```

#### Service: `MembershipServiceImpl.completeMembershipPurchase()`

```java
// 1. Get transaction and validate
Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();
if (!transaction.isCompleted()) throw new RuntimeException("Transaction not completed");

// 2. Get membership package
MembershipPackage package = membershipPackageRepository.findById(
    Long.parseLong(transaction.getReferenceId())
).orElseThrow();

// 3. Create user membership
LocalDateTime startDate = LocalDateTime.now();
LocalDateTime endDate = startDate.plusMonths(package.getDurationMonths());

UserMembership userMembership = UserMembership.builder()
    .userId(transaction.getUserId())
    .membershipPackage(package)
    .startDate(startDate)
    .endDate(endDate)
    .durationDays(package.getDurationMonths() * 30)
    .status(MembershipStatus.ACTIVE)
    .totalPaid(package.getSalePrice())
    .build();

userMembership = userMembershipRepository.save(userMembership);

// 4. Grant ALL benefits immediately (ONE-TIME GRANT)
List<UserMembershipBenefit> benefits = grantBenefits(userMembership, package);
userBenefitRepository.saveAll(benefits);

return mapToResponse(userMembership);
```

#### Benefit Granting Logic

```java
private List<UserMembershipBenefit> grantBenefits(UserMembership membership, MembershipPackage package) {
    List<MembershipPackageBenefit> packageBenefits = package.getBenefits();
    List<UserMembershipBenefit> userBenefits = new ArrayList<>();
    
    for (MembershipPackageBenefit packageBenefit : packageBenefits) {
        // ONE-TIME GRANT: total = quantity_per_month × duration_months
        int totalQuantity = packageBenefit.getQuantityPerMonth() * package.getDurationMonths();
        
        UserMembershipBenefit userBenefit = UserMembershipBenefit.builder()
            .userMembership(membership)
            .packageBenefit(packageBenefit)
            .userId(membership.getUserId())
            .benefitType(packageBenefit.getBenefitType())
            .grantedAt(LocalDateTime.now())
            .expiresAt(membership.getEndDate())
            .totalQuantity(totalQuantity)
            .quantityUsed(0)
            .status(BenefitStatus.ACTIVE)
            .build();
        
        userBenefits.add(userBenefit);
    }
    
    return userBenefits;
}
```

### 4. Database Changes

**Tables Affected:**
- `transactions` - New record with type MEMBERSHIP_PURCHASE
- `user_memberships` - New active membership
- `user_membership_benefits` - Multiple benefit records with quotas

---

## Post Package Registration Flow

### 1. Post Types and Pricing

| Type | Price (30 days) | Features |
|------|-----------------|----------|
| **NORMAL** | 90,000 VND | Manual verification, 5 images, 1 video, has ads |
| **SILVER (VIP)** | 600,000 VND | Auto-verify, 10 images, 2 videos, no ads, blue badge |
| **GOLD (VIP)** | 600,000 VND | Same as Silver + priority display |
| **DIAMOND (Premium)** | 1,800,000 VND | All VIP features + gold badge + shadow listing + top placement |

### 2. Flow Diagram - With Quota

```
┌─────────────────────────────────────────────────────────────────┐
│              VIP LISTING CREATION (WITH QUOTA)                  │
└─────────────────────────────────────────────────────────────────┘

Step 1: User Checks Quota
   ↓
   GET /v1/memberships/quota/{benefitType}
   Example: GET /v1/memberships/quota/POST_SILVER
   ↓
   ← Returns: {
     "totalAvailable": 8,
     "totalUsed": 2,
     "totalGranted": 10,
     "hasActiveMembership": true
   }

Step 2: User Creates VIP Listing
   ↓
   POST /v1/listings
   Body: {
     "title": "Beautiful apartment",
     "vipType": "SILVER",
     "useMembershipQuota": true,
     "durationDays": 30,
     ... (other listing fields)
   }
   ↓
   Backend Actions:
   - Validates user has active membership
   - Checks quota availability for POST_SILVER
   - Consumes 1 quota from user_membership_benefits
   - Creates listing with:
     * vip_type: SILVER
     * post_source: QUOTA
     * transaction_id: null
     * status: PENDING_VERIFICATION or ACTIVE (if auto-verify)
   ↓
   ← Returns: {
     "listingId": 123,
     "vipType": "SILVER",
     "postSource": "QUOTA",
     "status": "ACTIVE",
     "quotaUsed": true
   }
```

### 3. Flow Diagram - Direct Payment

```
┌─────────────────────────────────────────────────────────────────┐
│            VIP LISTING CREATION (DIRECT PAYMENT)                │
└─────────────────────────────────────────────────────────────────┘

Step 1: User Creates VIP Listing (No Quota)
   ↓
   POST /v1/listings
   Body: {
     "title": "Beautiful apartment",
     "vipType": "DIAMOND",
     "useMembershipQuota": false,
     "durationDays": 30,
     "paymentProvider": "VNPAY",
     ... (other listing fields)
   }
   ↓
   Backend Actions:
   - Calculates price: DIAMOND 30 days = 1,800,000 VND
   - Creates PENDING transaction (TransactionType: POST_FEE)
   - Stores listing data temporarily (in transaction metadata or cache)
   - Generates VNPay payment URL
   ↓
   ← Returns: {
     "paymentUrl": "https://sandbox.vnpayment.vn/...",
     "transactionRef": "uuid-transaction-id",
     "amount": 1800000,
     "orderInfo": "Post DIAMOND listing: Beautiful apartment"
   }

Step 2: User Pays on VNPay
   ↓
   (Same as membership flow)

Step 3: VNPay Callback
   ↓
   GET /v1/payments/callback/VNPAY?vnp_TxnRef=...
   ↓
   Backend Actions:
   - Validates signature
   - Completes transaction
   - Retrieves listing data from cache/metadata
   - Creates listing with:
     * vip_type: DIAMOND
     * post_source: DIRECT_PAYMENT
     * transaction_id: transaction-uuid
     * status: ACTIVE (auto-verify for paid VIP)
   - If DIAMOND: Creates shadow NORMAL listing
   ↓
   ← Redirects to success page

Step 4: Frontend Queries Listing
   ↓
   GET /v1/listings/{listingId}
   ← Returns: Complete listing details
```

### 4. Implementation Details

#### Service: `ListingServiceImpl.createVipListing()`

```java
// Check if user wants to use quota
if (Boolean.TRUE.equals(request.getUseMembershipQuota())) {
    // Determine benefit type based on vipType
    BenefitType benefitType = switch(request.getVipType()) {
        case "SILVER", "GOLD" -> BenefitType.POST_SILVER;
        case "DIAMOND" -> BenefitType.POST_DIAMOND;
        default -> throw new RuntimeException("Invalid VIP type");
    };

    // Check quota availability
    QuotaStatusResponse quotaStatus = quotaService.checkQuotaAvailability(userId, benefitType);

    if (quotaStatus.getTotalAvailable() > 0) {
        // Consume quota
        boolean consumed = quotaService.consumeQuota(userId, benefitType, 1);
        if (!consumed) throw new RuntimeException("Failed to consume quota");

        // Create listing with postSource = QUOTA
        Listing listing = buildListingFromVipRequest(request);
        listing.setPostSource(PostSource.QUOTA);
        listing.setTransactionId(null);

        // Check if user has AUTO_APPROVE benefit
        boolean hasAutoApprove = quotaService.hasBenefit(userId, BenefitType.AUTO_APPROVE);
        listing.setStatus(hasAutoApprove ? ListingStatus.ACTIVE : ListingStatus.PENDING_VERIFICATION);

        Listing saved = listingRepository.save(listing);

        // If Diamond, create shadow NORMAL listing
        if ("DIAMOND".equalsIgnoreCase(request.getVipType())) {
            createShadowListing(saved);
        }

        return listingMapper.toCreationResponse(saved);
    }
}

// No quota - require payment
BigDecimal amount = calculatePostPrice(request.getVipType(), request.getDurationDays());
String transactionId = transactionService.createPostFeeTransaction(
    userId, amount, request.getVipType(), request.getDurationDays(), "VNPAY"
);

// Generate payment URL
PaymentRequest paymentRequest = PaymentRequest.builder()
    .provider(PaymentProvider.VNPAY)
    .amount(amount)
    .orderInfo("Post " + request.getVipType() + " listing: " + request.getTitle())
    .returnUrl(request.getReturnUrl())
    .build();

return paymentService.createPayment(paymentRequest, null);
```

#### Shadow Listing Creation (for DIAMOND posts)

```java
private void createShadowListing(Listing parentListing) {
    Listing shadowListing = Listing.builder()
        .userId(parentListing.getUserId())
        .title(parentListing.getTitle())
        .description(parentListing.getDescription())
        .vipType(VipType.NORMAL)  // Shadow is always NORMAL
        .isShadow(true)
        .parentListingId(parentListing.getListingId())
        .postSource(parentListing.getPostSource())
        .transactionId(parentListing.getTransactionId())
        .status(parentListing.getStatus())
        // ... copy all other fields
        .build();

    listingRepository.save(shadowListing);
    log.info("Created shadow listing for DIAMOND listing {}", parentListing.getListingId());
}
```

### 5. Pricing Calculation

```java
// From PricingConstants.java
public static BigDecimal calculateSilverPostPrice(int durationDays) {
    BigDecimal dailyRate = VIP_POST_30_DAYS.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
    return dailyRate.multiply(BigDecimal.valueOf(durationDays));
}

// Duration discounts
// 10 days: No discount
// 15 days: 11% discount
// 30 days: 18.5% discount (standard pricing)
```

---

## Push Package Registration Flow

### 1. Push Pricing

- **Single Push**: 40,000 VND
- **Push Effect**: Pushes listing to top of search results
- **Duration**: Instant effect, listing appears at top

### 2. Flow Diagram - With Quota

```
┌─────────────────────────────────────────────────────────────────┐
│                  PUSH LISTING (WITH QUOTA)                      │
└─────────────────────────────────────────────────────────────────┘

Step 1: User Checks Push Quota
   ↓
   GET /v1/memberships/quota/PUSH
   ↓
   ← Returns: {
     "totalAvailable": 18,
     "totalUsed": 2,
     "totalGranted": 20
   }

Step 2: User Pushes Listing
   ↓
   POST /v1/pushes/push
   Body: {
     "listingId": 123,
     "useMembershipQuota": true
   }
   ↓
   Backend Actions:
   - Validates listing exists and belongs to user
   - QuotaService.checkQuotaAvailability(userId, PUSH)
   - QuotaService.consumeQuota(userId, PUSH, 1)
   - Creates PushHistory record:
     * push_source: MEMBERSHIP_QUOTA
     * transaction_id: null
     * user_benefit_id: benefit_id
   - Updates listing.pushed_at = now()
   - Updates listing.post_date = now() (to push to top)
   - If listing is GOLD or DIAMOND: Also pushes shadow listing (FREE)
   ↓
   ← Returns: {
     "listingId": 123,
     "userId": "user-123",
     "pushSource": "MEMBERSHIP_QUOTA",
     "pushedAt": "2024-01-01T10:00:00",
     "message": "Listing pushed successfully using quota"
   }
```

### 3. Flow Diagram - Direct Payment

```
┌─────────────────────────────────────────────────────────────────┐
│                PUSH LISTING (DIRECT PAYMENT)                    │
└─────────────────────────────────────────────────────────────────┘

Step 1: User Pushes Listing (No Quota)
   ↓
   POST /v1/pushes/push
   Body: {
     "listingId": 123,
     "useMembershipQuota": false,
     "paymentProvider": "VNPAY",
     "returnUrl": "https://app.com/payment/callback"
   }
   ↓
   Backend Actions:
   - TransactionService.createPushFeeTransaction(userId, listingId, 40000, "VNPAY")
   - Amount: 40,000 VND
   - Generates VNPay payment URL
   ↓
   ← Returns: {
     "paymentUrl": "https://sandbox.vnpayment.vn/...",
     "transactionRef": "uuid-transaction-id",
     "amount": 40000,
     "message": "Payment required"
   }

Step 2: User Pays on VNPay
   ↓
   (Payment flow same as above)

Step 3: VNPay Callback
   ↓
   GET /v1/payments/callback/VNPAY?vnp_TxnRef=...&vnp_ResponseCode=00
   ↓
   Backend Actions:
   - Validates signature (HMAC-SHA512)
   - Completes transaction (PENDING → COMPLETED)
   - Calls pushService.completePushAfterPayment(transactionId)
   ↓
   completePushAfterPayment:
   - Gets listing from transaction.referenceId
   - Creates PushHistory record:
     * push_source: DIRECT_PAYMENT
     * transaction_id: transaction-uuid
   - Updates listing.pushed_at = now()
   - Updates listing.post_date = now()
   - If GOLD/DIAMOND: Pushes shadow listing (FREE)
   ↓
   ← Redirects to success page
```

### 4. Scheduled Push Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    SCHEDULED PUSH FLOW                          │
└─────────────────────────────────────────────────────────────────┘

Step 1: User Schedules Daily Push
   ↓
   POST /v1/pushes/schedule
   Body: {
     "listingId": 123,
     "scheduledTime": "08:00:00",  // Daily at 8 AM
     "totalPushes": 30,             // 30 days
     "useMembershipQuota": true
   }
   ↓
   Backend Actions:
   - QuotaService.hasSufficientQuota(userId, PUSH, 30)
   - Creates PushSchedule record:
     * status: ACTIVE
     * source: MEMBERSHIP
     * source_id: user_membership_id
     * total_pushes: 30
     * used_pushes: 0
   - Does NOT consume quota yet (consumed daily)
   ↓
   ← Returns: {
     "scheduleId": 456,
     "listingId": 123,
     "scheduledTime": "08:00:00",
     "totalPushes": 30,
     "usedPushes": 0,
     "status": "ACTIVE"
   }

Step 2: Cron Job Executes (Hourly)
   ↓
   Scheduled Job: @Scheduled(cron = "0 0 * * * *")
   ↓
   pushService.executeScheduledPushes()
   ↓
   For each ACTIVE schedule at current hour:
   - QuotaService.consumeQuota(userId, PUSH, 1)
   - Creates PushHistory record (push_source: SCHEDULED, schedule_id: 456)
   - Updates listing.pushed_at and post_date
   - Increments schedule.used_pushes
   - If used_pushes >= total_pushes: Set status to COMPLETED
   - If GOLD/DIAMOND: Pushes shadow listing (FREE)
```

### 5. Implementation Details

#### Service: `PushServiceImpl.pushListing()`

```java
// Check if user wants to use quota
boolean useQuota = Boolean.TRUE.equals(request.getUseMembershipQuota());

if (useQuota) {
    // Check quota availability
    QuotaStatusResponse quotaStatus = quotaService.checkQuotaAvailability(userId, BenefitType.PUSH);

    if (quotaStatus.getTotalAvailable() > 0) {
        // Consume quota
        boolean consumed = quotaService.consumeQuota(userId, BenefitType.PUSH, 1);
        if (!consumed) throw new RuntimeException("Failed to consume push quota");

        // Create push history
        PushHistory pushHistory = PushHistory.builder()
            .listingId(listing.getListingId())
            .userId(userId)
            .pushSource(PushSource.MEMBERSHIP_QUOTA)
            .transactionId(null)
            .userBenefitId(benefitId)
            .pushedAt(LocalDateTime.now())
            .build();

        pushHistoryRepository.save(pushHistory);

        // Update listing
        listing.setPushedAt(LocalDateTime.now());
        listing.setPostDate(LocalDateTime.now());
        listingRepository.save(listing);

        // Push shadow listing if Gold or Diamond
        if ((listing.isGold() || listing.isDiamond()) && !listing.isShadowListing()) {
            pushShadowListing(listing, userId);
        }

        return mapToPushResponse(pushHistory, "Listing pushed successfully using quota");
    }
}

// No quota - require payment
String transactionId = transactionService.createPushFeeTransaction(
    userId, request.getListingId(), PricingConstants.PUSH_PER_TIME, "VNPAY"
);

PaymentRequest paymentRequest = PaymentRequest.builder()
    .provider(PaymentProvider.VNPAY)
    .amount(PricingConstants.PUSH_PER_TIME)
    .orderInfo("Push listing #" + request.getListingId())
    .returnUrl(request.getReturnUrl())
    .build();

PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);

return PushResponse.builder()
    .listingId(request.getListingId())
    .userId(userId)
    .pushSource("PAYMENT_REQUIRED")
    .message("Payment required")
    .paymentUrl(paymentResponse.getPaymentUrl())
    .transactionId(paymentResponse.getTransactionRef())
    .build();
```

---

## Payment Integration

### 1. VNPay Payment Flow

All payments in SmartRent are processed through VNPay payment gateway.

#### Payment URL Generation

```java
// VNPayServiceImpl.createPaymentUrl()
public PaymentResponse createPaymentUrl(String transactionId, PaymentRequest request) {
    // 1. Get transaction
    Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();

    // 2. Build VNPay parameters
    Map<String, String> vnpParams = new TreeMap<>();
    vnpParams.put("vnp_Version", vnPayConfig.getVersion());
    vnpParams.put("vnp_Command", "pay");
    vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
    vnpParams.put("vnp_Amount", String.valueOf(transaction.getAmount().multiply(BigDecimal.valueOf(100)).longValue()));
    vnpParams.put("vnp_CurrCode", "VND");
    vnpParams.put("vnp_TxnRef", transaction.getTransactionId());
    vnpParams.put("vnp_OrderInfo", request.getOrderInfo());
    vnpParams.put("vnp_OrderType", "other");
    vnpParams.put("vnp_Locale", "vn");
    vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
    vnpParams.put("vnp_IpAddr", request.getIpAddress());
    vnpParams.put("vnp_CreateDate", formatDateTime(LocalDateTime.now()));

    // 3. Generate secure hash (HMAC-SHA512)
    String queryString = buildQueryString(vnpParams);
    String secureHash = generateSecureHash(queryString, vnPayConfig.getHashSecret());

    // 4. Build payment URL
    String paymentUrl = vnPayConfig.getUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;

    return PaymentResponse.builder()
        .paymentUrl(paymentUrl)
        .transactionRef(transactionId)
        .amount(transaction.getAmount())
        .build();
}
```

#### Callback Verification

```java
// VNPayServiceImpl.verifyPaymentCallback()
public PaymentCallbackResponse processCallback(Map<String, String> params, HttpServletRequest request) {
    String txnRef = params.get("vnp_TxnRef");

    // 1. Validate signature
    String secureHash = params.get("vnp_SecureHash");
    boolean signatureValid = validateSignature(params, secureHash);

    if (!signatureValid) {
        return PaymentCallbackResponse.builder()
            .success(false)
            .signatureValid(false)
            .message("Invalid signature")
            .build();
    }

    // 2. Find transaction
    Transaction transaction = transactionRepository.findById(txnRef).orElseThrow();

    // 3. Update transaction status
    String responseCode = params.get("vnp_ResponseCode");
    boolean success = "00".equals(responseCode);

    if (success) {
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setProviderTransactionId(params.get("vnp_TransactionNo"));
    } else {
        transaction.setStatus(TransactionStatus.FAILED);
    }

    transactionRepository.save(transaction);

    return PaymentCallbackResponse.builder()
        .success(success)
        .signatureValid(true)
        .transactionRef(txnRef)
        .status(transaction.getStatus().name())
        .message(success ? "Payment successful" : "Payment failed")
        .build();
}
```

### 2. Transaction Types

| Type | Description | Reference Type | Reference ID |
|------|-------------|----------------|--------------|
| `MEMBERSHIP_PURCHASE` | Membership package purchase | MEMBERSHIP | membership_id |
| `POST_FEE` | Pay-per-post VIP listing | LISTING | listing_id |
| `PUSH_FEE` | Pay-per-push | PUSH | listing_id |
| `WALLET_TOPUP` | Wallet top-up | WALLET | wallet_id |
| `REFUND` | Refund transaction | Various | original_transaction_id |

### 3. Transaction States

```
PENDING → COMPLETED (payment successful)
        → FAILED (payment failed)
        → CANCELLED (user cancelled or timeout)
```

### 4. Payment Security

- **Signature Algorithm**: HMAC-SHA512
- **Hash Secret**: Provided by VNPay (stored in environment variables)
- **Parameter Sorting**: Alphabetical order before hashing
- **Validation**: Both request and callback signatures are validated

---

## Quota Management

### 1. Quota System Overview

Quotas are granted when a user purchases a membership package. All quotas are granted **once** at purchase time.

#### Quota Calculation

```
total_quantity = quantity_per_month × duration_months
```

Example: STANDARD package (1 month)
- Silver Posts: 10 posts/month × 1 month = 10 total posts
- Gold Posts: 5 posts/month × 1 month = 5 total posts
- Pushes: 20 pushes/month × 1 month = 20 total pushes

### 2. Benefit Types

| Benefit Type | Description | Consumable | Enum Value |
|--------------|-------------|------------|------------|
| `POST_SILVER` | Silver VIP posts | Yes | BenefitType.POST_SILVER |
| `POST_GOLD` | Gold VIP posts | Yes | BenefitType.POST_GOLD |
| `POST_DIAMOND` | Diamond VIP posts | Yes | BenefitType.POST_DIAMOND |
| `PUSH` | Listing pushes | Yes | BenefitType.PUSH |
| `AUTO_APPROVE` | Auto-verification | No (flag) | BenefitType.AUTO_APPROVE |
| `BADGE` | Trusted badge | No (flag) | BenefitType.BADGE |

### 3. Quota Consumption Flow

```java
// QuotaServiceImpl.consumeQuota()
public boolean consumeQuota(String userId, BenefitType benefitType, int quantity) {
    // Find first available benefit with quota
    UserMembershipBenefit benefit = userBenefitRepository
        .findFirstAvailableBenefit(userId, benefitType, LocalDateTime.now())
        .orElse(null);

    if (benefit == null) {
        log.warn("No available quota for user {} and benefit type {}", userId, benefitType);
        return false;
    }

    // Check if sufficient quota
    if (benefit.getQuantityRemaining() < quantity) {
        return false;
    }

    // Consume quota
    benefit.setQuantityUsed(benefit.getQuantityUsed() + quantity);

    // Update status if fully consumed
    if (benefit.getQuantityUsed() >= benefit.getTotalQuantity()) {
        benefit.setStatus(BenefitStatus.FULLY_USED);
    }

    userBenefitRepository.save(benefit);
    return true;
}
```

### 4. Quota Checking

```java
// QuotaServiceImpl.checkQuotaAvailability()
public QuotaStatusResponse checkQuotaAvailability(String userId, BenefitType benefitType) {
    // Get total available quota across all active benefits
    Integer totalAvailable = userBenefitRepository.getTotalAvailableQuota(
        userId, benefitType, LocalDateTime.now()
    );

    // Get total used and granted
    List<UserMembershipBenefit> benefits = userBenefitRepository
        .findByUserIdAndBenefitType(userId, benefitType);

    int totalUsed = benefits.stream()
        .mapToInt(UserMembershipBenefit::getQuantityUsed)
        .sum();

    int totalGranted = benefits.stream()
        .mapToInt(UserMembershipBenefit::getTotalQuantity)
        .sum();

    return QuotaStatusResponse.builder()
        .totalAvailable(totalAvailable != null ? totalAvailable : 0)
        .totalUsed(totalUsed)
        .totalGranted(totalGranted)
        .hasActiveMembership(hasActiveMembership(userId))
        .build();
}
```

### 5. Quota Expiration

Quotas expire when:
1. Membership expires (end_date reached)
2. All quota is consumed (quantity_used >= total_quantity)

**No Rollover**: Unused quotas are lost when membership expires.

### 6. Repository Query

```java
// UserMembershipBenefitRepository
@Query("""
    SELECT COALESCE(SUM(b.totalQuantity - b.quantityUsed), 0)
    FROM user_membership_benefits b
    WHERE b.userId = :userId
    AND b.benefitType = :benefitType
    AND b.status = 'ACTIVE'
    AND b.expiresAt > :now
    """)
Integer getTotalAvailableQuota(
    @Param("userId") String userId,
    @Param("benefitType") BenefitType benefitType,
    @Param("now") LocalDateTime now
);
```

---

## API Endpoints

### 1. Membership Endpoints

#### Get All Packages
```http
GET /v1/memberships/packages
Response: List<MembershipPackageResponse>
```

#### Get Package by ID
```http
GET /v1/memberships/packages/{membershipId}
Response: MembershipPackageResponse
```

#### Initiate Membership Purchase
```http
POST /v1/memberships/initiate-purchase
Headers: user-id: {userId}
Body: {
  "membershipId": 2,
  "paymentProvider": "VNPAY",
  "returnUrl": "https://app.com/payment/callback"
}
Response: PaymentResponse {
  "paymentUrl": "https://sandbox.vnpayment.vn/...",
  "transactionRef": "uuid",
  "amount": 1400000
}
```

#### Get My Membership
```http
GET /v1/memberships/my-membership
Headers: user-id: {userId}
Response: UserMembershipResponse
```

#### Check Quota
```http
GET /v1/memberships/quota/{benefitType}
Headers: user-id: {userId}
Path Params: benefitType = POST_SILVER | POST_GOLD | POST_DIAMOND | PUSH
Response: QuotaStatusResponse {
  "totalAvailable": 8,
  "totalUsed": 2,
  "totalGranted": 10,
  "hasActiveMembership": true
}
```

#### Check All Quotas
```http
GET /v1/memberships/quota/all
Headers: user-id: {userId}
Response: Map<String, QuotaStatusResponse>
```

### 2. Listing Endpoints

#### Create Listing (supports all VIP types)
```http
POST /v1/listings
Authorization: Bearer {accessToken}
Body: {
  "title": "Beautiful apartment",
  "description": "...",
  "vipType": "NORMAL" | "SILVER" | "GOLD" | "DIAMOND",
  "useMembershipQuota": true,
  "durationDays": 30,
  "paymentProvider": "VNPAY",
  "address": { ... },
  "price": 5000000,
  ... (other listing fields)
}
Response:
  - If quota used: ListingCreationResponse
  - If payment required: PaymentResponse
```

### 3. Push Endpoints

#### Push Listing
```http
POST /v1/pushes/push
Headers: user-id: {userId}
Body: {
  "listingId": 123,
  "useMembershipQuota": true,
  "paymentProvider": "VNPAY",
  "returnUrl": "https://app.com/payment/callback"
}
Response:
  - If quota used: PushResponse
  - If payment required: PaymentResponse
```

#### Schedule Push
```http
POST /v1/pushes/schedule
Headers: user-id: {userId}
Body: {
  "listingId": 123,
  "scheduledTime": "08:00:00",
  "totalPushes": 30,
  "useMembershipQuota": true
}
Response: PushResponse
```

#### Get Push History
```http
GET /v1/pushes/history/{listingId}
Response: List<PushResponse>
```

#### Get User Push History
```http
GET /v1/pushes/my-history
Headers: user-id: {userId}
Response: List<PushResponse>
```

#### Cancel Scheduled Push
```http
DELETE /v1/pushes/schedule/{scheduleId}
Headers: user-id: {userId}
Response: ApiResponse
```

### 4. Payment Endpoints

#### Payment Callback (VNPay)
```http
GET /v1/payments/callback/VNPAY
Query Params: vnp_TxnRef, vnp_ResponseCode, vnp_SecureHash, ...
Response: Redirect to frontend with result
```

#### Payment IPN (VNPay)
```http
POST /v1/payments/ipn/VNPAY
Body: VNPay IPN parameters
Response: RspCode=00&Message=OK
```

#### Query Transaction
```http
GET /v1/payments/transactions/{transactionRef}
Response: TransactionResponse
```

### 5. Transaction Endpoints

#### Get Transaction History
```http
GET /v1/transactions/history
Headers: user-id: {userId}
Query Params: page, size, transactionType
Response: Page<TransactionResponse>
```

#### Get Transaction by ID
```http
GET /v1/transactions/{transactionId}
Response: TransactionResponse
```

---

## Database Schema

### 1. Core Tables

#### membership_packages
```sql
CREATE TABLE membership_packages (
    membership_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    package_code VARCHAR(50) UNIQUE NOT NULL,
    package_name VARCHAR(100) NOT NULL,
    package_level ENUM('BASIC', 'STANDARD', 'ADVANCED') NOT NULL,
    duration_months INT NOT NULL DEFAULT 1,
    original_price DECIMAL(15,0) NOT NULL,
    sale_price DECIMAL(15,0) NOT NULL,
    discount_percentage DECIMAL(5,2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### membership_package_benefits
```sql
CREATE TABLE membership_package_benefits (
    benefit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    membership_id BIGINT NOT NULL,
    benefit_type ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'PUSH', 'AUTO_APPROVE', 'BADGE') NOT NULL,
    benefit_name_display VARCHAR(200) NOT NULL,
    quantity_per_month INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (membership_id) REFERENCES membership_packages(membership_id)
);
```

#### user_memberships
```sql
CREATE TABLE user_memberships (
    user_membership_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(36) NOT NULL,
    membership_id BIGINT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    duration_days INT NOT NULL,
    status ENUM('ACTIVE', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    total_paid DECIMAL(15,0) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (membership_id) REFERENCES membership_packages(membership_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_end_date (end_date)
);
```

#### user_membership_benefits
```sql
CREATE TABLE user_membership_benefits (
    user_benefit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_membership_id BIGINT NOT NULL,
    benefit_id BIGINT NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    benefit_type ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'PUSH', 'AUTO_APPROVE', 'BADGE') NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    total_quantity INT NOT NULL,
    quantity_used INT NOT NULL DEFAULT 0,
    status ENUM('ACTIVE', 'FULLY_USED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_membership_id) REFERENCES user_memberships(user_membership_id),
    FOREIGN KEY (benefit_id) REFERENCES membership_package_benefits(benefit_id),
    INDEX idx_user_benefit_status (user_id, benefit_type, status),
    INDEX idx_expires_at (expires_at)
);
```

#### transactions
```sql
CREATE TABLE transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    transaction_type ENUM('MEMBERSHIP_PURCHASE', 'POST_FEE', 'PUSH_FEE', 'WALLET_TOPUP', 'REFUND') NOT NULL,
    amount DECIMAL(15,0) NOT NULL,
    reference_type ENUM('MEMBERSHIP', 'LISTING', 'PUSH', 'WALLET', 'REFUND') NOT NULL,
    reference_id VARCHAR(100) NOT NULL,
    status ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    payment_provider ENUM('VNPAY', 'MOMO', 'ZALOPAY') NOT NULL DEFAULT 'VNPAY',
    provider_transaction_id VARCHAR(100),
    additional_info TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_provider_tx_id (provider_transaction_id)
);
```

#### listings (relevant columns)
```sql
ALTER TABLE listings ADD COLUMN vip_type ENUM('NORMAL', 'SILVER', 'GOLD', 'DIAMOND') DEFAULT 'NORMAL';
ALTER TABLE listings ADD COLUMN post_source ENUM('QUOTA', 'DIRECT_PAYMENT');
ALTER TABLE listings ADD COLUMN transaction_id VARCHAR(36);
ALTER TABLE listings ADD COLUMN is_shadow BOOLEAN DEFAULT FALSE;
ALTER TABLE listings ADD COLUMN parent_listing_id BIGINT;
ALTER TABLE listings ADD COLUMN pushed_at TIMESTAMP;
ALTER TABLE listings ADD FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id);
ALTER TABLE listings ADD FOREIGN KEY (parent_listing_id) REFERENCES listings(listing_id);
```

#### push_history
```sql
CREATE TABLE push_history (
    push_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    listing_id BIGINT NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    push_source ENUM('MEMBERSHIP_QUOTA', 'DIRECT_PAYMENT', 'SCHEDULED') NOT NULL,
    user_benefit_id BIGINT,
    schedule_id BIGINT,
    transaction_id VARCHAR(36),
    pushed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listings(listing_id),
    FOREIGN KEY (user_benefit_id) REFERENCES user_membership_benefits(user_benefit_id),
    FOREIGN KEY (schedule_id) REFERENCES push_schedule(schedule_id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    INDEX idx_listing_id (listing_id),
    INDEX idx_user_id (user_id),
    INDEX idx_pushed_at (pushed_at)
);
```

#### push_schedule
```sql
CREATE TABLE push_schedule (
    schedule_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(36) NOT NULL,
    listing_id BIGINT NOT NULL,
    scheduled_time TIME NOT NULL,
    source ENUM('MEMBERSHIP', 'DIRECT_PURCHASE') NOT NULL,
    source_id BIGINT,
    total_pushes INT NOT NULL,
    used_pushes INT NOT NULL DEFAULT 0,
    status ENUM('ACTIVE', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    transaction_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listings(listing_id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    INDEX idx_user_listing (user_id, listing_id),
    INDEX idx_status_time (status, scheduled_time)
);
```

### 2. Entity Relationships

```
membership_packages (1) ←→ (N) membership_package_benefits
        ↓
user_memberships (1) ←→ (N) user_membership_benefits
        ↓
    transactions
        ↓
    listings / push_history
```

### 3. Key Indexes

- `idx_user_status` on user_memberships - Fast lookup of active memberships
- `idx_user_benefit_status` on user_membership_benefits - Fast quota checks
- `idx_expires_at` on user_membership_benefits - Expiration cleanup
- `idx_provider_tx_id` on transactions - VNPay callback lookup
- `idx_status_time` on push_schedule - Scheduled job execution

---

## Error Handling

### 1. Custom Exceptions

#### InsufficientQuotaException
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientQuotaException extends RuntimeException {
    public InsufficientQuotaException(String benefitType, int required, int available) {
        super(String.format("Insufficient %s quota. Required: %d, Available: %d",
            benefitType, required, available));
    }
}
```

#### PaymentFailedException
```java
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentFailedException extends RuntimeException {
    private final String transactionId;
    private final String responseCode;

    public PaymentFailedException(String transactionId, String responseCode, String message) {
        super(message);
        this.transactionId = transactionId;
        this.responseCode = responseCode;
    }
}
```

#### InvalidPaymentCallbackException
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPaymentCallbackException extends RuntimeException {
    public InvalidPaymentCallbackException(String message) {
        super(message);
    }
}
```

#### DuplicateTransactionException
```java
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String transactionId) {
        super("Transaction already processed: " + transactionId);
    }
}
```

### 2. Global Exception Handler

```java
@RestControllerAdvice
public class GlobalPaymentExceptionHandler {

    @ExceptionHandler(InsufficientQuotaException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientQuota(InsufficientQuotaException ex) {
        return ResponseEntity.badRequest().body(
            ApiResponse.<Void>builder()
                .code("INSUFFICIENT_QUOTA")
                .message(ex.getMessage())
                .build()
        );
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentFailed(PaymentFailedException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(
            ApiResponse.<Void>builder()
                .code("PAYMENT_FAILED")
                .message(ex.getMessage())
                .build()
        );
    }

    @ExceptionHandler(InvalidPaymentCallbackException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCallback(InvalidPaymentCallbackException ex) {
        return ResponseEntity.badRequest().body(
            ApiResponse.<Void>builder()
                .code("INVALID_CALLBACK")
                .message(ex.getMessage())
                .build()
        );
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateTransaction(DuplicateTransactionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiResponse.<Void>builder()
                .code("DUPLICATE_TRANSACTION")
                .message(ex.getMessage())
                .build()
        );
    }
}
```

### 3. Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INSUFFICIENT_QUOTA` | 400 | User doesn't have enough quota |
| `PAYMENT_FAILED` | 402 | Payment failed at VNPay |
| `INVALID_CALLBACK` | 400 | Invalid payment callback signature |
| `DUPLICATE_TRANSACTION` | 409 | Transaction already processed |
| `MEMBERSHIP_NOT_FOUND` | 404 | Membership package not found |
| `LISTING_NOT_FOUND` | 404 | Listing not found |
| `TRANSACTION_NOT_FOUND` | 404 | Transaction not found |
| `INVALID_VIP_TYPE` | 400 | Invalid VIP type specified |
| `MEMBERSHIP_EXPIRED` | 400 | User's membership has expired |
| `INVALID_PAYMENT_AMOUNT` | 400 | Payment amount doesn't match |

### 4. VNPay Response Codes

| Code | Meaning |
|------|---------|
| `00` | Success |
| `07` | Suspicious transaction |
| `09` | Card not registered for internet banking |
| `10` | Incorrect card authentication |
| `11` | Payment timeout |
| `12` | Card locked |
| `13` | Incorrect OTP |
| `24` | Transaction cancelled |
| `51` | Insufficient balance |
| `65` | Transaction limit exceeded |
| `75` | Payment gateway under maintenance |
| `79` | Payment timeout |

### 5. Validation Rules

#### Membership Purchase
- User must exist
- Membership package must be active
- User cannot have multiple active memberships (optional business rule)

#### VIP Listing Creation
- If using quota: User must have active membership with available quota
- If direct payment: Amount must match calculated price
- VIP type must be valid: SILVER, GOLD, or DIAMOND
- Duration must be 10, 15, or 30 days

#### Push
- Listing must exist and belong to user
- If using quota: User must have available push quota
- If direct payment: Amount must be 40,000 VND

---

## Summary

### Key Features Implemented

1. **Membership System**
    - 3 package tiers (BASIC, STANDARD, ADVANCED)
    - One-time benefit grant at purchase
    - VNPay payment integration
    - Automatic quota management

2. **Post Package System**
    - VIP listing creation with quota or payment
    - 3 VIP types (SILVER, GOLD, DIAMOND)
    - Shadow listing for DIAMOND posts
    - Auto-verification for paid VIP posts

3. **Push Package System**
    - Instant push with quota or payment
    - Scheduled daily pushes
    - Automatic shadow listing push for GOLD/DIAMOND
    - Push history tracking

4. **Payment Integration**
    - VNPay payment gateway
    - Secure signature validation (HMAC-SHA512)
    - Transaction lifecycle management
    - Callback and IPN handling

5. **Quota Management**
    - Real-time quota checking
    - Automatic consumption on usage
    - Expiration handling
    - No rollover policy

### Technology Stack

- **Backend**: Spring Boot 3.4.0, Java 17
- **Database**: MySQL 8.0
- **Payment Gateway**: VNPay
- **Security**: HMAC-SHA512 signatures
- **API Documentation**: Swagger/OpenAPI 3