# VNPay Payment Integration - Quick Start Guide

## Overview
This guide helps you understand and use the VNPay-only payment system implemented for SmartRent.

## Key Concepts

### 1. No Wallet System
- Users **DO NOT** have a balance/wallet
- Every payment goes directly through VNPay
- No "top-up" functionality

### 2. Two Ways to Post/Boost

#### Option A: Use Membership Quota (Free)
- User has active membership with available quota
- Post/boost using quota (no payment required)
- `postSource = QUOTA` or `pushSource = MEMBERSHIP_QUOTA`

#### Option B: Pay Per Action (VNPay)
- User has no quota or chooses not to use it
- Pay directly via VNPay for that specific action
- `postSource = DIRECT_PAYMENT` or `pushSource = DIRECT_PAYMENT`

## Pricing (from PricingConstants.java)

```java
// Listing Prices (30 days)
NORMAL_POST_30_DAYS   = 90,000 VND
VIP_POST_30_DAYS      = 600,000 VND
PREMIUM_POST_30_DAYS  = 1,800,000 VND

// Boost Price
BOOST_PER_TIME        = 40,000 VND
```

## Payment Flow Examples

### Example 1: User Buys Membership

```java
// 1. Create transaction
String txId = transactionService.createMembershipTransaction(
    userId, 
    membershipId, 
    new BigDecimal("1400000"), 
    "VNPAY"
);

// 2. Generate VNPay payment URL
PaymentRequest paymentRequest = PaymentRequest.builder()
    .amount(new BigDecimal("1400000"))
    .orderInfo("Membership Standard Package")
    .returnUrl("http://localhost:3000/payment/result")
    .ipAddress(request.getRemoteAddr())
    .build();

PaymentResponse paymentResponse = vnPayService.createPaymentUrl(txId, paymentRequest);

// 3. Redirect user to paymentResponse.getPaymentUrl()

// 4. Handle callback (when user returns from VNPay)
PaymentStatusResponse status = vnPayService.verifyPaymentCallback(callbackRequest);

if (status.getSuccess()) {
    // 5. Complete transaction
    transactionService.completeTransaction(txId, status.getProviderTransactionId());
    
    // 6. Activate membership and grant quotas
    membershipService.activateMembership(userId, membershipId);
}
```

### Example 2: User Posts VIP Listing (Has Quota)

```java
// 1. Check if user has Silver quota
QuotaStatusResponse quota = quotaService.checkQuotaAvailability(
    userId,
    BenefitType.POST_SILVER
);

if (quota.getTotalAvailable() > 0) {
    // 2. Create listing using quota
    Listing listing = Listing.builder()
        .title("Beautiful apartment")
        .vipType(VipType.SILVER)
        .postSource(PostSource.QUOTA)  // Using quota
        .transactionId(null)            // No transaction needed
        .build();

    // 3. Consume quota
    quotaService.consumeQuota(userId, BenefitType.POST_SILVER, 1);

    // 4. Save listing
    listingRepository.save(listing);
}
```

### Example 3: User Posts Silver Listing (No Quota - Pay Per Post)

```java
// 1. Check quota - user has 0 Silver posts available
QuotaStatusResponse quota = quotaService.checkQuotaAvailability(
    userId,
    BenefitType.POST_SILVER
);

if (quota.getTotalAvailable() == 0) {
    // 2. Create POST_FEE transaction
    String txId = transactionService.createPostFeeTransaction(
        userId,
        null,  // Listing not created yet
        PricingConstants.SILVER_POST_30_DAYS,  // 1,222,500 VND
        "VNPAY",
        "Silver Listing - 30 days"
    );
    
    // 3. Generate VNPay payment URL
    PaymentRequest paymentRequest = PaymentRequest.builder()
        .amount(PricingConstants.VIP_POST_30_DAYS)
        .orderInfo("VIP Listing - 30 days")
        .returnUrl("http://localhost:3000/payment/result")
        .build();
    
    PaymentResponse paymentResponse = vnPayService.createPaymentUrl(txId, paymentRequest);
    
    // 4. Return payment URL to frontend
    return paymentResponse;
    
    // 5. After payment success (in callback handler):
    if (paymentSuccess) {
        transactionService.completeTransaction(txId, providerTxId);
        
        // 6. Create listing with DIRECT_PAYMENT source
        Listing listing = Listing.builder()
            .title("Beautiful apartment")
            .vipType(VipType.VIP)
            .postSource(PostSource.DIRECT_PAYMENT)  // Paid directly
            .transactionId(txId)                     // Link to transaction
            .build();
        
        listingRepository.save(listing);
    }
}
```

### Example 4: User Boosts Listing (No Quota)

```java
// 1. Check boost quota
QuotaStatusResponse quota = membershipService.checkQuotaAvailability(
    userId, 
    BenefitType.BOOST_QUOTA
);

if (quota.getTotalAvailable() == 0) {
    // 2. Create BOOST_FEE transaction
    String txId = transactionService.createBoostFeeTransaction(
        userId,
        listingId,
        PricingConstants.BOOST_PER_TIME,  // 40,000 VND
        "VNPAY"
    );
    
    // 3. Generate payment URL
    PaymentRequest paymentRequest = PaymentRequest.builder()
        .amount(PricingConstants.BOOST_PER_TIME)
        .orderInfo("Boost listing #" + listingId)
        .returnUrl("http://localhost:3000/payment/result")
        .build();
    
    PaymentResponse paymentResponse = vnPayService.createPaymentUrl(txId, paymentRequest);
    
    // 4. After payment success:
    if (paymentSuccess) {
        transactionService.completeTransaction(txId, providerTxId);
        
        // 5. Execute boost
        PushHistory pushHistory = PushHistory.builder()
            .listingId(listingId)
            .userId(userId)
            .pushSource(PushSource.DIRECT_PAYMENT)  // Paid directly
            .transactionId(txId)                     // Link to transaction
            .build();
        
        pushHistoryRepository.save(pushHistory);
        
        // Update listing pushed_at timestamp
        listing.setPushedAt(LocalDateTime.now());
        listingRepository.save(listing);
    }
}
```

## VNPay Callback Handling

```java
@GetMapping("/payments/callback")
public ResponseEntity<?> handleVNPayCallback(
    @RequestParam Map<String, String> params
) {
    // 1. Build callback request from params
    VNPayCallbackRequest callback = VNPayCallbackRequest.builder()
        .vnp_TxnRef(params.get("vnp_TxnRef"))
        .vnp_Amount(params.get("vnp_Amount"))
        .vnp_ResponseCode(params.get("vnp_ResponseCode"))
        .vnp_TransactionStatus(params.get("vnp_TransactionStatus"))
        .vnp_TransactionNo(params.get("vnp_TransactionNo"))
        .vnp_SecureHash(params.get("vnp_SecureHash"))
        // ... other params
        .build();
    
    // 2. Verify callback
    PaymentStatusResponse status = vnPayService.verifyPaymentCallback(callback);
    
    if (!status.getSuccess()) {
        // Payment failed
        transactionService.failTransaction(
            callback.getVnp_TxnRef(), 
            "Payment failed: " + callback.getVnp_ResponseCode()
        );
        return ResponseEntity.ok("Payment failed");
    }
    
    // 3. Get transaction to determine what to do next
    TransactionResponse transaction = transactionService.getTransaction(
        callback.getVnp_TxnRef()
    );
    
    // 4. Complete transaction
    transactionService.completeTransaction(
        transaction.getTransactionId(),
        status.getProviderTransactionId()
    );
    
    // 5. Execute business logic based on transaction type
    switch (transaction.getTransactionType()) {
        case MEMBERSHIP_PURCHASE:
            // Activate membership
            membershipService.activateMembership(
                transaction.getUserId(),
                Long.parseLong(transaction.getReferenceId())
            );
            break;
            
        case POST_FEE:
            // Create listing (if not already created)
            // Or update listing status
            break;
            
        case BOOST_FEE:
            // Execute boost
            boostService.executeBoost(
                transaction.getUserId(),
                Long.parseLong(transaction.getReferenceId()),
                transaction.getTransactionId()
            );
            break;
    }
    
    return ResponseEntity.ok("Payment successful");
}
```

## Database Queries

### Check if listing was paid or used quota
```java
Listing listing = listingRepository.findById(listingId).get();

if (listing.isCreatedWithQuota()) {
    // Listing used membership quota
    System.out.println("Free listing (quota)");
} else if (listing.isCreatedWithDirectPayment()) {
    // Listing was paid per-post
    System.out.println("Paid listing: " + listing.getTransactionId());
}
```

### Get all paid listings for a user
```java
@Query("SELECT l FROM listings l WHERE l.userId = :userId AND l.postSource = 'DIRECT_PAYMENT'")
List<Listing> findPaidListingsByUser(@Param("userId") String userId);
```

### Get transaction history
```java
List<TransactionResponse> history = transactionService.getTransactionHistory(
    userId, 
    0,  // page
    20  // size
);
```

## Environment Setup

### 1. Add to `.env` or environment variables:
```bash
VNPAY_TMN_CODE=your_terminal_code
VNPAY_HASH_SECRET=your_hash_secret
VNPAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/v1/payments/callback
```

### 2. For testing, use VNPay sandbox:
- Terminal Code: Get from VNPay sandbox
- Hash Secret: Get from VNPay sandbox
- Test cards: Use VNPay provided test card numbers

## Common Patterns

### Pattern 1: Check Quota Before Action
```java
public void performAction(String userId, BenefitType benefitType) {
    QuotaStatusResponse quota = membershipService.checkQuotaAvailability(
        userId, 
        benefitType
    );
    
    if (quota.getTotalAvailable() > 0) {
        // Use quota (free)
        useQuota(userId, benefitType);
    } else {
        // Require payment
        initiatePayment(userId, benefitType);
    }
}
```

### Pattern 2: Transaction State Machine
```
PENDING → (payment success) → COMPLETED
PENDING → (payment failed) → FAILED
PENDING → (user cancels) → CANCELLED
COMPLETED → (refund) → REFUNDED
```

### Pattern 3: Link Entities to Transactions
```java
// Always link paid actions to transactions
if (postSource == PostSource.DIRECT_PAYMENT) {
    listing.setTransactionId(transactionId);
}

if (pushSource == PushSource.DIRECT_PAYMENT) {
    pushHistory.setTransactionId(transactionId);
}
```

## Testing Checklist

- [ ] VNPay signature generation works correctly
- [ ] VNPay callback verification works
- [ ] Membership purchase flow (quota → payment → callback → activation)
- [ ] Pay-per-post flow (no quota → payment → callback → create listing)
- [ ] Pay-per-boost flow (no quota → payment → callback → boost)
- [ ] Transaction state transitions
- [ ] Payment failure handling
- [ ] Duplicate payment prevention

## Troubleshooting

### Issue: Invalid signature
- Check `VNPAY_HASH_SECRET` is correct
- Verify parameter sorting (alphabetical)
- Check URL encoding

### Issue: Transaction not found
- Verify `vnp_TxnRef` matches internal transaction ID
- Check transaction was created before payment

### Issue: Payment success but action not executed
- Check callback handler logic
- Verify transaction type switch statement
- Check logs for errors

## Next Steps

After understanding this guide:
1. Review the business requirements in `charge-features-biz.md`
2. Check the implementation summary in `VNPAY_IMPLEMENTATION_SUMMARY.md`
3. Implement the remaining services (MembershipService updates, PaymentController, etc.)
4. Write integration tests
5. Test with VNPay sandbox

