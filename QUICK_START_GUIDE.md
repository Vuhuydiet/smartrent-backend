# Quick Start Guide - CREATE_LISTING_QUOTA Feature

## 🚀 For Developers

### 1. Database Setup
```bash
# Migration will run automatically on next deployment
# V25__Create_listing_duration_plans_and_update_listings.sql
# Creates: listing_duration_plans table + 5 default plans
# Adds: transaction_id column to listings table
```

### 2. Test the Public APIs

**Get Available Duration Plans**
```bash
curl -X GET http://localhost:8080/api/v1/listings/duration-plans \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Response: Array of plans with pricing for all tiers
[
  {
    "planId": 1,
    "durationDays": 5,
    "isActive": true,
    "pricing": {
      "NORMAL": { "basePricePerDay": 2700, "totalPrice": 13500, "discount": "0%", "savings": "0" },
      "SILVER": { "basePricePerDay": 50000, "totalPrice": 250000, ... },
      ...
    }
  },
  ...
]
```

**Calculate Listing Price**
```bash
curl -X GET "http://localhost:8080/api/v1/listings/calculate-price?listingType=NORMAL&durationDays=15" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Response:
{
  "basePricePerDay": 2700,
  "durationDays": 15,
  "totalBeforeDiscount": 40500,
  "discountPercentage": 0.11,
  "discountAmount": 4455,
  "finalPrice": 36045,
  "currency": "VND",
  "discountDescription": "11.0% off",
  "savingsDescription": "Save 4455 VND (11.0%)"
}
```

**Create NORMAL Listing with Payment**
```bash
curl -X POST http://localhost:8080/api/v1/listings \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "durationPlanId": 3,
    "useMembershipQuota": false,
    "paymentProvider": "VNPAY",
    "address": {
      "provinceId": 1,
      "districtId": 2,
      "wardId": 3,
      "street": "123 Test St",
      "houseNumber": "45A"
    },
    "title": "Beautiful Apartment",
    "description": "2BR apartment in city center",
    "area": 80.5,
    "price": 5000000,
    "deposit": 10000000,
    "propertyType": "APARTMENT",
    "rentalType": "MONTHLY"
  }'

# Response: Payment URL for VNPay
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?..."
}
```

### 3. Test Admin APIs

**Get All Plans (Including Inactive)**
```bash
curl -X GET http://localhost:8080/api/v1/admin/duration-plans \
  -H "X-Admin-Id: admin123"
```

**Create New Duration Plan**
```bash
curl -X POST http://localhost:8080/api/v1/admin/duration-plans \
  -H "X-Admin-Id: admin123" \
  -H "Content-Type: application/json" \
  -d '{
    "durationDays": 60,
    "isActive": true
  }'

# Response:
{
  "planId": 6,
  "durationDays": 60,
  "isActive": true,
  "pricing": { ... }
}
```

**Update Duration Plan**
```bash
curl -X PUT http://localhost:8080/api/v1/admin/duration-plans/6 \
  -H "X-Admin-Id: admin123" \
  -H "Content-Type: application/json" \
  -d '{
    "durationDays": 45,
    "isActive": true
  }'
```

**Deactivate Plan**
```bash
curl -X PATCH http://localhost:8080/api/v1/admin/duration-plans/6/deactivate \
  -H "X-Admin-Id: admin123"

# Error if last active plan:
{
  "code": "11003",
  "message": "Cannot deactivate the last active duration plan"
}
```

**Delete Plan**
```bash
curl -X DELETE http://localhost:8080/api/v1/admin/duration-plans/6 \
  -H "X-Admin-Id: admin123"
```

---

## 🧪 For QA Testing

### Test Scenario 1: Normal Listing Payment Flow
1. Call GET /duration-plans → Get plan IDs
2. Call POST /listings with durationPlanId=3 (10 days)
3. System returns VNPay payment URL
4. Complete payment on VNPay sandbox
5. VNPay sends IPN callback
6. Verify listing created in database with transaction_id

**Expected**: Listing created with PostSource.DIRECT_PAYMENT

### Test Scenario 2: Price Calculation
1. Call GET /calculate-price?listingType=NORMAL&durationDays=5
   - Expected: 13,500 VND (no discount)
2. Call GET /calculate-price?listingType=NORMAL&durationDays=15
   - Expected: 36,045 VND (11% discount)
3. Call GET /calculate-price?listingType=DIAMOND&durationDays=30
   - Expected: 6,846,000 VND (18.5% discount)

### Test Scenario 3: Admin Plan Management
1. Call POST /admin/duration-plans with durationDays=20
   - Expected: Plan created successfully
2. Call POST /admin/duration-plans with durationDays=20 again
   - Expected: 409 CONFLICT error (duplicate)
3. Call DELETE /admin/duration-plans/{lastActivePlanId}
   - Expected: 400 BAD_REQUEST (cannot delete last active)
4. Activate another plan first, then delete
   - Expected: Success

### Test Scenario 4: Cache Expiration
1. Create listing with payment
2. Wait 31 minutes (cache TTL = 30 min)
3. Send VNPay callback
4. Expected: 404 error "Cache may have expired"

### Test Scenario 5: Idempotency
1. Create listing with payment (transaction_id: TX123)
2. Complete payment successfully
3. Send VNPay callback again with same transaction_id
4. Expected: Returns existing listing, no duplicate creation

---

## 🔍 Debugging Tips

### Check Redis Cache
```bash
# Connect to Redis
redis-cli

# List all listing request keys
KEYS listing:request:*

# Get specific request
GET listing:request:normal:TRANSACTION_ID
GET listing:request:vip:TRANSACTION_ID

# Check TTL
TTL listing:request:normal:TRANSACTION_ID
```

### Check Database
```sql
-- Verify duration plans exist
SELECT * FROM listing_duration_plans WHERE is_active = TRUE;

-- Check listings by transaction ID
SELECT * FROM listings WHERE transaction_id = 'YOUR_TRANSACTION_ID';

-- Check transaction history
SELECT * FROM transactions WHERE transaction_ref = 'YOUR_TRANSACTION_ID';
```

### Common Errors

**"Duration plan not found"**
- Check plan exists: `SELECT * FROM listing_duration_plans WHERE plan_id = X`
- Check plan is active: `is_active = TRUE`

**"Cache expired"**
- Redis TTL = 30 minutes
- User must complete payment within 30 min of creating request
- If expired, user must create new listing request

**"Cannot deactivate last active plan"**
- At least 1 plan must remain active
- Activate another plan first, then deactivate

**"Listing already exists for transaction"**
- This is idempotency protection working correctly
- Transaction was already processed successfully

---

## 📊 Monitoring

### Key Metrics to Track
1. **Cache Hit Rate**: How many callbacks find cached requests
2. **Payment Success Rate**: % of initiated payments that complete
3. **Cache Expiration Rate**: % of requests that expire before payment
4. **Admin Plan Changes**: Frequency of plan CRUD operations

### Log Examples
```
INFO  - User 123 creating NORMAL listing with plan 3 (10 days) - Price: 27000 VND
INFO  - Listing request cached for transaction: VNPAY_TX456 (TTL: 30 min)
INFO  - Payment completed for transaction: VNPAY_TX456
INFO  - Creating NORMAL listing from cache for transaction: VNPAY_TX456
INFO  - Listing created with id: 789 for transaction: VNPAY_TX456
INFO  - Admin admin123 creating new duration plan: 60 days
```

---

## 🎯 API Quick Reference

### Public Endpoints (Require JWT)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/listings/duration-plans` | Get active plans with pricing |
| GET | `/api/v1/listings/calculate-price` | Calculate price for tier + duration |
| POST | `/api/v1/listings` | Create listing (with payment) |

### Admin Endpoints (Require X-Admin-Id)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/admin/duration-plans` | List all plans |
| POST | `/api/v1/admin/duration-plans` | Create plan |
| GET | `/api/v1/admin/duration-plans/{id}` | Get plan by ID |
| PUT | `/api/v1/admin/duration-plans/{id}` | Update plan |
| DELETE | `/api/v1/admin/duration-plans/{id}` | Delete plan |
| PATCH | `/api/v1/admin/duration-plans/{id}/activate` | Activate plan |
| PATCH | `/api/v1/admin/duration-plans/{id}/deactivate` | Deactivate plan |

---

## 🛠️ Configuration

### Application Properties
```yaml
# Redis Configuration (for cache)
spring.data.redis.host=localhost
spring.data.redis.port=6379

# VNPay Configuration
vnpay.tmn_code=YOUR_TMN_CODE
vnpay.hash_secret=YOUR_HASH_SECRET
vnpay.payment_url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return_url=http://localhost:8080/payment/return
vnpay.ipn_url=http://localhost:8080/api/v1/payment-gateway/ipn-handler
```

### Environment Variables
```bash
# Required for Redis cache
REDIS_HOST=localhost
REDIS_PORT=6379

# Required for VNPay integration
VNPAY_TMN_CODE=your_terminal_code
VNPAY_HASH_SECRET=your_hash_secret
```

---

## ✅ Deployment Checklist

- [ ] Migration V25 executed successfully
- [ ] Redis server running and accessible
- [ ] VNPay credentials configured
- [ ] 5 default duration plans exist in database
- [ ] Admin endpoints require proper authentication
- [ ] Payment IPN callback URL whitelisted in VNPay
- [ ] Monitoring/logging configured
- [ ] Error tracking enabled (Sentry/etc)

---

**For more details, see**:
- `CREATE_LISTING_QUOTA_IMPLEMENTATION_SUMMARY.md` - Full technical documentation
- `LISTING_CREATION_PAYMENT_FLOW.md` - Payment flow diagrams
- `ADMIN_DURATION_PLAN_API.md` - Admin API reference
