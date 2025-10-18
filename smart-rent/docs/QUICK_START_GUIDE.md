# SmartRent Membership System - Quick Start Guide

## 🚀 Getting Started

### 1. Run Database Migration

The system will automatically run Flyway migration `V13__Create_membership_and_transaction_system.sql` on startup.

This creates:
- All membership tables
- 3 default packages (BASIC, STANDARD, ADVANCED)
- Default benefits configuration

### 2. API Endpoints

#### Membership Management

**Get All Packages**
```http
GET /v1/memberships/packages
```

**Purchase Membership**
```http
POST /v1/memberships/purchase
Headers: user-id: {userId}
Body:
{
  "membershipId": 2,
  "paymentProvider": "VNPAY",
  "returnUrl": "https://app.com/callback"
}
```

**Get My Active Membership**
```http
GET /v1/memberships/my-membership
Headers: user-id: {userId}
```

**Check VIP Posts Quota**
```http
GET /v1/memberships/quota/vip-posts
Headers: user-id: {userId}
```

**Check Premium Posts Quota**
```http
GET /v1/memberships/quota/premium-posts
Headers: user-id: {userId}
```

**Check Push Quota**
```http
GET /v1/memberships/quota/pushes
Headers: user-id: {userId}
```

#### Push Management

**Push Listing (Immediate)**
```http
POST /v1/pushes/boost
Headers: user-id: {userId}
Body:
{
  "listingId": 101,
  "useMembershipQuota": true
}
```

**Schedule Automatic Pushes**
```http
POST /v1/pushes/schedule
Headers: user-id: {userId}
Body:
{
  "listingId": 101,
  "scheduledTime": "09:00:00",
  "totalPushes": 10,
  "useMembershipQuota": true
}
```

**Get Push History**
```http
GET /v1/pushes/my-history
Headers: user-id: {userId}
```

## 📊 Usage Scenarios

### Scenario 1: User Purchases Standard Package

```java
// 1. User browses packages
GET /v1/memberships/packages

// 2. User purchases STANDARD package (ID: 2)
POST /v1/memberships/purchase
{
  "membershipId": 2,
  "paymentProvider": "VNPAY"
}

// Response: UserMembershipResponse with benefits:
// - 10 VIP posts
// - 5 Premium posts
// - 20 pushes
// - Auto-verify enabled
```

### Scenario 2: User Posts VIP Listing

```java
// 1. Check VIP quota
GET /v1/memberships/quota/vip-posts
// Response: { "totalAvailable": 10, "hasActiveMembership": true }

// 2. Create VIP listing (using quota)
POST /v1/listings/vip
{
  "title": "Cho thuê căn hộ 2PN view sông",
  "vipType": "VIP",
  "useMembershipQuota": true,
  ...
}

// 3. Check quota again
GET /v1/memberships/quota/vip-posts
// Response: { "totalAvailable": 9, ... }
```

### Scenario 3: User Pushes Listing

```java
// 1. Check push quota
GET /v1/memberships/quota/pushes
// Response: { "totalAvailable": 20 }

// 2. Push listing
POST /v1/pushes/boost
{
  "listingId": 101,
  "useMembershipQuota": true
}

// 3. Listing is pushed to top
// 4. If Premium listing, shadow listing also pushed FREE
```

### Scenario 4: User Schedules Daily Pushes

```java
// Schedule 10 automatic pushes at 9 AM daily
POST /v1/pushes/schedule
{
  "listingId": 101,
  "scheduledTime": "09:00:00",
  "totalPushes": 10,
  "useMembershipQuota": true
}

// Cron job will execute push daily at 9 AM
// Consumes 1 quota per execution
```

## 🔧 Configuration

### Pricing Constants

Update these in your configuration or database:

```java
// Listing posting fees (30 days)
NORMAL_POST_FEE = 90,000 VND
VIP_POST_FEE = 600,000 VND
PREMIUM_POST_FEE = 1,800,000 VND

// Push fees
SINGLE_PUSH_FEE = 50,000 VND
PUSH_PACKAGE_3 = 120,000 VND (40,000 each)
```

### VIP Type Limits

```java
// Image/Video limits
NORMAL: max 5 images, 1 video
VIP: max 10 images, 2 videos
PREMIUM: max 15 images, 3 videos
```

## 🔄 Scheduled Jobs

### 1. Membership Expiration Job

```java
@Scheduled(cron = "0 0 0 * * *") // Daily at midnight
public void expireMemberships() {
    membershipService.expireOldMemberships();
}
```

### 2. Scheduled Push Executor

```java
@Scheduled(cron = "0 * * * * *") // Every minute
public void executeScheduledPushes() {
    pushService.executeScheduledPushes();
}
```

## 📝 Business Logic Summary

### One-Time Benefit Grant
- When user purchases membership, ALL benefits granted immediately
- `total_quantity = quantity_per_month × duration_months`
- Example: 1-month STANDARD = 10 VIP + 5 Premium + 20 Pushes

### No Quota Rollover
- Unused quotas are LOST when membership expires
- No refunds for unused benefits

### Premium Shadow Listings
- Premium post automatically creates shadow NORMAL listing
- Shadow listing syncs with parent
- Pushing Premium also pushes shadow FREE

### Auto-Verification
- Users with AUTO_VERIFY benefit get instant listing approval
- No 4-8 hour wait time

## 🧪 Testing

### Test Data

The migration creates 3 packages:

1. **BASIC (ID: 1)** - 700,000 VND
   - 5 VIP posts
   - 10 pushes

2. **STANDARD (ID: 2)** - 1,400,000 VND
   - 10 VIP posts
   - 5 Premium posts
   - 20 pushes
   - Auto-verify

3. **ADVANCED (ID: 3)** - 2,800,000 VND
   - 15 VIP posts
   - 10 Premium posts
   - 40 boosts
   - Auto-verify
   - Trusted badge

### Test Workflow

```bash
# 1. Get packages
curl -X GET http://localhost:8080/v1/memberships/packages

# 2. Purchase STANDARD package
curl -X POST http://localhost:8080/v1/memberships/purchase \
  -H "user-id: test-user-123" \
  -H "Content-Type: application/json" \
  -d '{"membershipId": 2, "paymentProvider": "VNPAY"}'

# 3. Check quota
curl -X GET http://localhost:8080/v1/memberships/quota/vip-posts \
  -H "user-id: test-user-123"

# 4. Push listing
curl -X POST http://localhost:8080/v1/pushes/boost \
  -H "user-id: test-user-123" \
  -H "Content-Type: application/json" \
  -d '{"listingId": 101, "useMembershipQuota": true}'
```

## 🎯 Key Features Implemented

✅ Membership package management
✅ One-time benefit grant system
✅ Quota tracking and consumption
✅ VIP/Premium listing support
✅ Shadow listing for Premium
✅ Manual boost
✅ Scheduled automatic boost
✅ Transaction recording
✅ Membership expiration
✅ Benefit expiration
✅ RESTful APIs with Swagger docs

## 📚 Next Steps

1. **Payment Integration**
   - Implement VNPay callback handler
   - Add payment verification
   - Handle payment failures

2. **Enhanced Listing Service**
   - Add VIP posting logic
   - Create shadow listings
   - Apply auto-verification
   - Enforce image/video limits

3. **Admin Panel**
   - Create/edit packages
   - View user memberships
   - Manual quota adjustments
   - Transaction monitoring

4. **Notifications**
   - Membership expiration warnings
   - Quota low alerts
   - Boost success notifications

5. **Analytics**
   - Membership revenue reports
   - Quota usage statistics
   - Boost effectiveness metrics

## 🐛 Troubleshooting

### Issue: "No quota available"
- Check if user has active membership
- Verify quota hasn't been fully consumed
- Check membership expiration date

### Issue: "Listing not found"
- Verify listing ID exists
- Check if listing belongs to user
- Ensure listing hasn't been deleted

### Issue: "Membership package not active"
- Check package `is_active` flag in database
- Verify package hasn't been disabled

## 📞 Support

For questions or issues:
1. Check the implementation documentation
2. Review the business logic document
3. Examine the database schema
4. Test with Swagger UI at `/swagger-ui.html`

---

**Happy Coding! 🎉**

