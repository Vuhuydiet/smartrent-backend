# VNPay-Only Payment Flow Implementation Summary

## Overview
This document summarizes the implementation of the VNPay-only payment flow for SmartRent, based on the business requirements in `charge-features-biz.md`.

## Key Changes

### 1. Database Schema Updates (V14 Migration) ✅

**File:** `smart-rent/src/main/resources/db/migration/V14__Update_for_vnpay_only_payment_flow.sql`

#### Listings Table Updates:
- Added `post_source` ENUM('QUOTA', 'DIRECT_PAYMENT') - tracks how listing was created
- Added `transaction_id` VARCHAR(36) - links pay-per-post listings to transactions
- Added foreign key constraint to transactions table
- Added indexes for performance

#### Transactions Table Updates:
- **Removed** `balance_before` and `balance_after` columns (no wallet functionality)
- Updated `transaction_type` ENUM to remove 'WALLET_TOPUP'
- Made `payment_provider` required (NOT NULL, DEFAULT 'VNPAY')

#### Push_History Table Updates:
- Added `transaction_id` VARCHAR(36) - links pay-per-boost to transactions
- Added foreign key constraint to transactions table
- Updated `push_source` ENUM: changed 'DIRECT_PURCHASE' to 'DIRECT_PAYMENT'

### 2. New Enums ✅

#### PostSource Enum
**File:** `smart-rent/src/main/java/com/smartrent/enums/PostSource.java`
- `QUOTA` - Listing created using membership quota
- `DIRECT_PAYMENT` - Listing created by paying per-post via VNPay

#### Updated PushSource Enum
**File:** `smart-rent/src/main/java/com/smartrent/enums/PushSource.java`
- Changed `DIRECT_PURCHASE` to `DIRECT_PAYMENT` for consistency
- Added comprehensive documentation

### 3. Entity Updates ✅

#### Listing Entity
**File:** `smart-rent/src/main/java/com/smartrent/infra/repository/entity/Listing.java`
- Added `postSource` field (PostSource enum)
- Added `transactionId` field (String)
- Added helper methods:
  - `isCreatedWithQuota()`
  - `isCreatedWithDirectPayment()`
  - `hasLinkedTransaction()`

#### Transaction Entity
**File:** `smart-rent/src/main/java/com/smartrent/infra/repository/entity/Transaction.java`
- Removed `balanceBefore` field
- Removed `balanceAfter` field
- Removed `isWalletTopup()` helper method

#### PushHistory Entity
**File:** `smart-rent/src/main/java/com/smartrent/infra/repository/entity/PushHistory.java`
- Added `transactionId` field (String)
- Updated `isFromDirectPurchase()` to `isFromDirectPayment()`
- Added `hasLinkedTransaction()` helper method

### 4. Pricing Constants ✅

**File:** `smart-rent/src/main/java/com/smartrent/constants/PricingConstants.java`

Defined all pricing according to business requirements:
- `NORMAL_POST_30_DAYS` = 90,000 VND
- `VIP_POST_30_DAYS` = 600,000 VND
- `PREMIUM_POST_30_DAYS` = 1,800,000 VND
- `BOOST_PER_TIME` = 40,000 VND

Also includes:
- Daily pricing calculations
- Helper methods for price calculations
- Media limits for each listing type

### 5. Payment DTOs ✅

#### Request DTOs:
1. **PaymentRequest** - `smart-rent/src/main/java/com/smartrent/dto/request/PaymentRequest.java`
   - amount, orderInfo, returnUrl, ipAddress, locale

2. **VNPayCallbackRequest** - `smart-rent/src/main/java/com/smartrent/dto/request/VNPayCallbackRequest.java`
   - All VNPay callback parameters (vnp_*)

#### Response DTOs:
1. **PaymentResponse** - `smart-rent/src/main/java/com/smartrent/dto/response/PaymentResponse.java`
   - paymentUrl, transactionId, orderInfo, amount

2. **PaymentStatusResponse** - `smart-rent/src/main/java/com/smartrent/dto/response/PaymentStatusResponse.java`
   - transactionId, status, amount, orderInfo, providerTransactionId, success, message

### 6. VNPay Configuration ✅

#### VNPayConfig Class
**File:** `smart-rent/src/main/java/com/smartrent/config/VNPayConfig.java`
- Configuration properties for VNPay integration
- Loads from application.yml

#### Application Configuration
**File:** `smart-rent/src/main/resources/application.yml`
- Added VNPay configuration section:
  ```yaml
  vnpay:
    tmnCode: ${VNPAY_TMN_CODE:DEMO}
    hashSecret: ${VNPAY_HASH_SECRET:DEMOSECRETKEY}
    url: ${VNPAY_URL:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}
    returnUrl: ${VNPAY_RETURN_URL:http://localhost:8080/v1/payments/callback}
    version: "2.1.0"
  ```

### 7. VNPay Service ✅

#### VNPayService Interface
**File:** `smart-rent/src/main/java/com/smartrent/service/payment/VNPayService.java`

#### VNPayServiceImpl
**File:** `smart-rent/src/main/java/com/smartrent/service/payment/impl/VNPayServiceImpl.java`

Features:
- `createPaymentUrl()` - Generates VNPay payment URL with proper signature
- `verifyPaymentCallback()` - Verifies VNPay callback with HMAC-SHA512
- `queryTransaction()` - Query transaction status (placeholder)
- `verifySignature()` - Signature verification helper
- Proper HMAC-SHA512 signature generation
- URL encoding and parameter sorting

### 8. Transaction Service ✅

#### TransactionService Interface
**File:** `smart-rent/src/main/java/com/smartrent/service/transaction/TransactionService.java`

#### TransactionServiceImpl
**File:** `smart-rent/src/main/java/com/smartrent/service/transaction/impl/TransactionServiceImpl.java`

Features:
- `createMembershipTransaction()` - Create transaction for membership purchase
- `createPostFeeTransaction()` - Create transaction for pay-per-post
- `createBoostFeeTransaction()` - Create transaction for pay-per-boost
- `completeTransaction()` - Mark transaction as completed after VNPay confirmation
- `failTransaction()` - Mark transaction as failed
- `getTransaction()` - Get transaction by ID
- `getTransactionHistory()` - Get user's transaction history
- `getTransactionByProviderTxId()` - Find transaction by VNPay transaction ID

## Business Logic Implementation

### Payment Flow (VNPay Only)

1. **No Wallet System**
   - Removed all wallet-related fields from transactions
   - All payments go directly through VNPay
   - No balance tracking

2. **Membership Purchase Flow**
   ```
   User selects package → Create PENDING transaction → 
   Generate VNPay URL → Redirect to VNPay → 
   Payment callback → Complete transaction → Activate membership
   ```

3. **Pay-Per-Post Flow**
   ```
   User wants to post → Check quota → 
   If no quota: Create POST_FEE transaction → 
   Generate VNPay URL → Payment → 
   Create listing with postSource=DIRECT_PAYMENT
   ```

4. **Pay-Per-Boost Flow**
   ```
   User wants to boost → Check quota → 
   If no quota: Create BOOST_FEE transaction → 
   Generate VNPay URL → Payment → 
   Execute boost with pushSource=DIRECT_PAYMENT
   ```

## Pricing Structure (from business requirements)

### Per-Post Pricing (30 days):
- Normal: 90,000 VND
- VIP: 600,000 VND
- Premium: 1,800,000 VND

### Boost Pricing:
- 40,000 VND per boost

### Membership Packages:
- **Basic (700,000 VND)**: 5 VIP + 10 Boost
- **Standard (1,400,000 VND)**: 10 VIP + 5 Premium + 20 Boost
- **Advanced (2,800,000 VND)**: 15 VIP + 10 Premium + 40 Boost

## Files Created/Modified

### Created Files (11):
1. `V14__Update_for_vnpay_only_payment_flow.sql` - Database migration
2. `PostSource.java` - New enum
3. `PricingConstants.java` - Pricing constants
4. `PaymentRequest.java` - Payment request DTO
5. `VNPayCallbackRequest.java` - VNPay callback DTO
6. `PaymentResponse.java` - Payment response DTO
7. `PaymentStatusResponse.java` - Payment status DTO
8. `VNPayConfig.java` - VNPay configuration
9. `VNPayService.java` - VNPay service interface
10. `VNPayServiceImpl.java` - VNPay service implementation
11. `TransactionService.java` + `TransactionServiceImpl.java` - Transaction service

### Modified Files (6):
1. `Listing.java` - Added postSource and transactionId
2. `Transaction.java` - Removed wallet fields
3. `PushHistory.java` - Added transactionId
4. `PushSource.java` - Updated enum values
5. `application.yml` - Added VNPay configuration

## Next Steps (Not Yet Implemented)

### High Priority:
1. **Update MembershipService** - Integrate VNPay payment flow
2. **Implement Pay-Per-Post Logic** - Create VIP/Premium listings with payment
3. **Update BoostService** - Add pay-per-boost logic
4. **Implement Premium Shadow Listing** - Auto-create shadow listings
5. **Create PaymentController** - REST endpoints for payments
6. **Update ListingController** - Add VIP posting endpoints
7. **Add Auto-Verification Logic** - Auto-verify VIP/Premium posts
8. **Update Repository Queries** - Add new query methods
9. **Implement Validators** - Payment and quota validators
10. **Add Error Handling** - Custom exceptions for payment errors
11. **API Documentation** - Swagger docs for payment endpoints
12. **Integration Tests** - Test payment flows

### Medium Priority:
- Scheduled jobs for membership expiration
- Payment retry logic
- Refund handling
- Payment analytics

## Testing Recommendations

1. **Unit Tests:**
   - VNPay signature generation/verification
   - Transaction creation logic
   - Pricing calculations

2. **Integration Tests:**
   - Full payment flow (membership, post, boost)
   - VNPay callback handling
   - Transaction state transitions

3. **Manual Testing:**
   - VNPay sandbox integration
   - Payment success/failure scenarios
   - Quota vs direct payment flows

## Environment Variables Required

Add to `.env` or environment:
```
VNPAY_TMN_CODE=your_terminal_code
VNPAY_HASH_SECRET=your_hash_secret
VNPAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/v1/payments/callback
```

## Migration Instructions

1. **Backup Database** before running V14 migration
2. **Run Migration:** `./gradlew flywayMigrate`
3. **Verify Schema:** Check that new columns exist
4. **Update Environment:** Add VNPay credentials
5. **Test Payment Flow:** Use VNPay sandbox

## Summary

✅ **Completed:**
- Database schema updates (V14 migration)
- Core entities updated (Listing, Transaction, PushHistory)
- New enums (PostSource)
- Pricing constants
- Payment DTOs
- VNPay service with signature generation
- Transaction service
- Configuration setup

⏳ **Remaining:**
- Service layer integration (Membership, Listing, Boost)
- REST controllers
- Validators and error handling
- Auto-verification logic
- Premium shadow listing logic
- API documentation
- Tests

**Progress: ~40% Complete**

The foundation for VNPay-only payment flow is now in place. The next phase involves integrating these components into the existing services and creating the REST API endpoints.

