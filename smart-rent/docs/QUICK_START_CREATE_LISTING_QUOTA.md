# Quick Start Guide: CREATE_LISTING_QUOTA Feature

## For Frontend Developers

### 1. Get Available Duration Plans
```javascript
// GET /v1/listings/duration-plans
const response = await fetch('/v1/listings/duration-plans');
const { data: plans } = await response.json();

// Display plans to user
plans.forEach(plan => {
  console.log(`${plan.durationDays} days - ${plan.silverPrice} VND (${plan.discountDescription})`);
});
```

### 2. Calculate Price Preview
```javascript
// GET /v1/listings/calculate-price?vipType=SILVER&durationDays=30
const vipType = 'SILVER';
const durationDays = 30;

const response = await fetch(
  `/v1/listings/calculate-price?vipType=${vipType}&durationDays=${durationDays}`
);
const { data: calculation } = await response.json();

// Show user: "Price: 1,222,500 VND (Save 277,500 VND - 18.5% off)"
console.log(`Price: ${calculation.finalPrice} ${calculation.currency}`);
console.log(`${calculation.savingsDescription}`);
```

### 3. Create NORMAL Listing (Always Paid)
```javascript
// POST /v1/listings
const listingData = {
  title: "Cho thuê căn hộ 2PN",
  description: "Căn hộ đẹp, view sông",
  listingType: "RENT",
  vipType: "NORMAL",
  categoryId: 1,
  productType: "APARTMENT",
  price: 15000000,
  priceUnit: "MONTH",
  address: { /* address data */ },

  // NEW FIELDS for payment flow
  durationPlanId: 5,           // Selected plan (e.g., 30 days)
  useMembershipQuota: false,   // NORMAL never uses quota
  paymentProvider: "VNPAY"
};

const response = await fetch('/v1/listings', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(listingData)
});

const { data: paymentResponse } = await response.json();

// Redirect user to VNPay
window.location.href = paymentResponse.paymentUrl;
```

### 4. Create VIP Listing with Payment
```javascript
// POST /v1/listings/vip
const vipListingData = {
  title: "Cho thuê căn hộ cao cấp",
  vipType: "SILVER",
  categoryId: 1,
  productType: "APARTMENT",
  price: 20000000,
  priceUnit: "MONTH",
  address: { /* address data */ },

  // Payment option 1: Use quota (if available)
  useMembershipQuota: true,
  durationDays: 30,  // or use durationPlanId

  // Payment option 2: Direct payment
  // useMembershipQuota: false,
  // durationPlanId: 5,
  // paymentProvider: "VNPAY"
};

const response = await fetch('/v1/listings/vip', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(vipListingData)
});

const result = await response.json();

// If quota used, returns ListingCreationResponse
if (result.data.listingId) {
  console.log('Listing created with quota:', result.data.listingId);
}

// If payment needed, returns PaymentResponse
if (result.data.paymentUrl) {
  window.location.href = result.data.paymentUrl;
}
```

### 5. Payment Return URL Handling
```javascript
// After user completes payment, VNPay redirects to your return URL
// URL: https://yourapp.com/payment/return?vnp_ResponseCode=00&vnp_TxnRef=TXN123...

const urlParams = new URLSearchParams(window.location.search);
const responseCode = urlParams.get('vnp_ResponseCode');
const txnRef = urlParams.get('vnp_TxnRef');

if (responseCode === '00') {
  // Payment successful
  // Listing will be created automatically via IPN callback
  // You can poll transaction status or show success message
  showSuccess('Payment successful! Your listing is being created...');

  // Optional: Check transaction status
  const status = await fetch(`/v1/payments/transactions/${txnRef}`);
  const { data: transaction } = await status.json();

  if (transaction.status === 'COMPLETED') {
    // Listing should be created now
    navigateToMyListings();
  }
} else {
  showError('Payment failed or cancelled');
}
```

---

## For Backend Developers

### Testing the Flow

#### 1. Database Setup
```bash
# Run migration
./gradlew flywayMigrate

# Verify plans created
mysql> SELECT * FROM listing_duration_plans;
+----------+---------------+-----------+
| plan_id  | duration_days | is_active |
+----------+---------------+-----------+
|        1 |             5 | 1         |
|        2 |             7 | 1         |
|        3 |            10 | 1         |
|        4 |            15 | 1         |
|        5 |            30 | 1         |
+----------+---------------+-----------+
```

#### 2. Test Duration Plans API
```bash
curl -X GET http://localhost:8080/v1/listings/duration-plans
```

Expected response: 5 plans with calculated prices

#### 3. Test Price Calculator
```bash
curl -X GET "http://localhost:8080/v1/listings/calculate-price?vipType=SILVER&durationDays=30"
```

Expected: finalPrice = 1,222,500 VND (18.5% discount)

#### 4. Create Listing with Payment
```bash
curl -X POST http://localhost:8080/v1/listings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "title": "Test Listing",
    "description": "Test",
    "listingType": "RENT",
    "vipType": "NORMAL",
    "categoryId": 1,
    "productType": "APARTMENT",
    "price": 10000000,
    "priceUnit": "MONTH",
    "address": {
      "addressType": "NEW",
      "newProvinceCode": "01",
      "newWardCode": "00001"
    },
    "durationPlanId": 5,
    "useMembershipQuota": false,
    "paymentProvider": "VNPAY"
  }'
```

Expected response: PaymentResponse with paymentUrl

#### 5. Check Redis Cache
```bash
redis-cli

# Check if request cached
> KEYS listing:*
1) "listing:normal:TXN123456789"

# Get cached data
> GET listing:normal:TXN123456789
"{\"title\":\"Test Listing\",\"description\":\"Test\",...}"

# Check TTL
> TTL listing:normal:TXN123456789
(integer) 1795  # ~30 minutes in seconds
```

#### 6. Simulate VNPay IPN Callback
```bash
curl -X POST "http://localhost:8080/v1/payments/ipn/vnpay" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "vnp_TxnRef=TXN123456789&vnp_ResponseCode=00&vnp_Amount=6600000&vnp_SecureHash=..."
```

Expected:
- Transaction status → COMPLETED
- Listing created from cache
- Cache entry removed
- Response: `{"RspCode":"00","Message":"success"}`

#### 7. Verify Listing Created
```bash
mysql> SELECT listing_id, title, transaction_id, post_source
       FROM listings
       WHERE transaction_id = 'TXN123456789';

+------------+--------------+----------------+-----------------+
| listing_id | title        | transaction_id | post_source     |
+------------+--------------+----------------+-----------------+
|       1001 | Test Listing | TXN123456789   | DIRECT_PAYMENT  |
+------------+--------------+----------------+-----------------+
```

#### 8. Test Idempotency (Duplicate IPN)
```bash
# Send same IPN again
curl -X POST "http://localhost:8080/v1/payments/ipn/vnpay" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "vnp_TxnRef=TXN123456789&vnp_ResponseCode=00&..."

# Should return same listing without error
# Check logs: "WARN: Listing already created for transaction TXN123456789"
```

---

## Common Issues & Solutions

### Issue 1: Cache Not Found After Payment
**Error**: "Listing request not found in cache for transaction: TXN123"

**Causes**:
- Payment took > 30 minutes (cache expired)
- Redis connection issue
- Wrong transaction ID

**Solution**:
```bash
# Check Redis connection
redis-cli PING
> PONG

# Check cache exists
redis-cli KEYS listing:*

# If expired, user needs to create listing again
```

### Issue 2: Listing Not Created After IPN
**Symptoms**: Payment successful but listing not in database

**Debug Steps**:
```bash
# 1. Check transaction status
mysql> SELECT * FROM transactions WHERE transaction_id = 'TXN123';

# 2. Check application logs
tail -f logs/application.log | grep TXN123

# 3. Check Redis cache
redis-cli GET listing:normal:TXN123

# 4. Verify IPN callback received
# Look for: "Processing IPN callback from VNPay for transaction: TXN123"
```

### Issue 3: Duplicate Listings Created
**Symptoms**: Same transaction creates multiple listings

**Check**:
```sql
SELECT COUNT(*), transaction_id
FROM listings
GROUP BY transaction_id
HAVING COUNT(*) > 1;
```

**Root Cause**: Idempotency check not working
**Solution**: Verify `findByTransactionId()` called before creation

---

## Performance Tips

### 1. Redis Connection Pooling
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### 2. Cache Monitoring
```java
@Scheduled(fixedRate = 300000) // Every 5 minutes
public void monitorCache() {
    long normalCount = redisTemplate.keys("listing:normal:*").size();
    long vipCount = redisTemplate.keys("listing:vip:*").size();
    log.info("Cached requests - NORMAL: {}, VIP: {}", normalCount, vipCount);
}
```

### 3. Database Indexing
Already added:
- `listing_duration_plans.duration_days` (UNIQUE)
- `listings.transaction_id` (for idempotency)

---

## Monitoring Queries

### Active Cached Requests
```bash
redis-cli

# Count pending NORMAL listings
> KEYS listing:normal:* | wc -l

# Count pending VIP listings
> KEYS listing:vip:* | wc -l

# Get all transaction IDs
> KEYS listing:*
```

### Payment Success Rate
```sql
-- Listings created through payment
SELECT COUNT(*) as payment_listings
FROM listings
WHERE post_source = 'DIRECT_PAYMENT';

-- Transactions completed
SELECT COUNT(*) as completed_payments
FROM transactions
WHERE transaction_type = 'POST_FEE'
  AND status = 'COMPLETED';
```

### Cache Expiration Rate
```sql
-- Transactions COMPLETED but no listing
SELECT t.transaction_id, t.created_at, t.status
FROM transactions t
LEFT JOIN listings l ON l.transaction_id = t.transaction_id
WHERE t.transaction_type = 'POST_FEE'
  AND t.status = 'COMPLETED'
  AND l.listing_id IS NULL;
```

---

## API Reference

### Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/listings/duration-plans` | Get all duration plans |
| GET | `/v1/listings/calculate-price` | Calculate price |
| POST | `/v1/listings` | Create NORMAL listing |
| POST | `/v1/listings/vip` | Create VIP listing |
| POST | `/v1/payments/ipn/vnpay` | VNPay IPN callback |

### Request Examples
See Frontend section above

### Response Codes
| Code | Message | Description |
|------|---------|-------------|
| 200000 | Success | Request successful |
| 400000 | Bad Request | Invalid input |
| 404000 | Not Found | Resource not found |
| 500000 | Internal Error | Server error |

---

## Support

For issues or questions:
1. Check logs: `logs/application.log`
2. Verify Redis: `redis-cli KEYS listing:*`
3. Check database: `SELECT * FROM listing_duration_plans;`
4. Review implementation doc: `CREATE_LISTING_QUOTA_IMPLEMENTATION.md`
