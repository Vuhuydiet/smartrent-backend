# SmartRent Membership & Push API Documentation

## Base URL
```
http://localhost:8080/v1
```

## Authentication
All endpoints (except package browsing) require authentication via `user-id` header.

```http
Headers:
  user-id: {userId}
```

---

## 📦 Membership Management APIs

### 1. Get All Membership Packages

**Endpoint:** `GET /memberships/packages`

**Description:** Returns all active membership packages with benefits and pricing.

**Response:**
```json
{
  "data": [
    {
      "membershipId": 1,
      "packageCode": "BASIC_1M",
      "packageName": "Gói Cơ Bản 1 Tháng",
      "packageLevel": "BASIC",
      "durationMonths": 1,
      "originalPrice": 1000000,
      "salePrice": 700000,
      "discountPercentage": 30.00,
      "isActive": true,
      "description": "Gói cơ bản cho người dùng mới bắt đầu",
      "benefits": [
        {
          "benefitId": 1,
          "benefitType": "VIP_POSTS",
          "benefitNameDisplay": "5 tin VIP miễn phí",
          "quantityPerMonth": 5
        },
        {
          "benefitId": 2,
          "benefitType": "PUSH_QUOTA",
          "benefitNameDisplay": "10 lượt đẩy tin miễn phí",
          "quantityPerMonth": 10
        }
      ]
    }
  ]
}
```

---

### 2. Get Package by ID

**Endpoint:** `GET /memberships/packages/{membershipId}`

**Parameters:**
- `membershipId` (path) - Package ID

**Response:** Same as single package object above

---

### 3. Purchase Membership

**Endpoint:** `POST /memberships/purchase`

**Headers:** `user-id: {userId}`

**Request Body:**
```json
{
  "membershipId": 2,
  "paymentProvider": "VNPAY",
  "returnUrl": "https://app.com/payment/callback"
}
```

**Response:**
```json
{
  "data": {
    "userMembershipId": 1,
    "userId": "user-123",
    "membershipId": 2,
    "packageName": "Gói Tiêu Chuẩn 1 Tháng",
    "packageLevel": "STANDARD",
    "startDate": "2025-01-01T10:00:00",
    "endDate": "2025-02-01T10:00:00",
    "durationDays": 30,
    "daysRemaining": 30,
    "status": "ACTIVE",
    "totalPaid": 1400000,
    "benefits": [
      {
        "userBenefitId": 1,
        "benefitType": "VIP_POSTS",
        "benefitNameDisplay": "10 tin VIP miễn phí",
        "grantedAt": "2025-01-01T10:00:00",
        "expiresAt": "2025-02-01T10:00:00",
        "totalQuantity": 10,
        "quantityUsed": 0,
        "quantityRemaining": 10,
        "status": "ACTIVE"
      },
      {
        "userBenefitId": 2,
        "benefitType": "PREMIUM_POSTS",
        "benefitNameDisplay": "5 tin Premium miễn phí",
        "totalQuantity": 5,
        "quantityUsed": 0,
        "quantityRemaining": 5,
        "status": "ACTIVE"
      },
      {
        "userBenefitId": 3,
        "benefitType": "PUSH_QUOTA",
        "benefitNameDisplay": "20 lượt đẩy tin miễn phí",
        "totalQuantity": 20,
        "quantityUsed": 0,
        "quantityRemaining": 20,
        "status": "ACTIVE"
      }
    ]
  }
}
```

---

### 4. Get My Active Membership

**Endpoint:** `GET /memberships/my-membership`

**Headers:** `user-id: {userId}`

**Response:** Same as purchase response

---

### 5. Get Membership History

**Endpoint:** `GET /memberships/history`

**Headers:** `user-id: {userId}`

**Response:**
```json
{
  "data": [
    {
      "userMembershipId": 1,
      "status": "ACTIVE",
      ...
    },
    {
      "userMembershipId": 2,
      "status": "EXPIRED",
      ...
    }
  ]
}
```

---

### 6. Check VIP Posts Quota

**Endpoint:** `GET /memberships/quota/vip-posts`

**Headers:** `user-id: {userId}`

**Response:**
```json
{
  "data": {
    "benefitType": "VIP_POSTS",
    "totalAvailable": 10,
    "hasActiveMembership": true,
    "message": "You have 10 VIP_POSTS quota available"
  }
}
```

---

### 7. Check Premium Posts Quota

**Endpoint:** `GET /memberships/quota/premium-posts`

**Headers:** `user-id: {userId}`

**Response:** Same structure as VIP quota

---

### 8. Check Push Quota

**Endpoint:** `GET /memberships/quota/pushes`

**Headers:** `user-id: {userId}`

**Response:** Same structure as VIP quota

---

### 9. Cancel Membership

**Endpoint:** `DELETE /memberships/{userMembershipId}`

**Headers:** `user-id: {userId}`

**Parameters:**
- `userMembershipId` (path) - User membership ID

**Response:**
```json
{
  "message": "Membership cancelled successfully"
}
```

---

## 🚀 Push Management APIs

### 1. Push Listing (Immediate)

**Endpoint:** `POST /pushes/boost`

**Headers:** `user-id: {userId}`

**Request Body:**
```json
{
  "listingId": 101,
  "useMembershipQuota": true
}
```

**Parameters:**
- `listingId` - ID of listing to push
- `useMembershipQuota` - true = use quota, false = direct purchase
- `paymentProvider` - Required if useMembershipQuota = false

**Response:**
```json
{
  "data": {
    "pushId": 1,
    "listingId": 101,
    "userId": "user-123",
    "pushSource": "MEMBERSHIP_QUOTA",
    "pushedAt": "2025-01-03T15:00:00",
    "message": "Listing pushed successfully"
  }
}
```

---

### 2. Schedule Automatic Pushes

**Endpoint:** `POST /pushes/schedule`

**Headers:** `user-id: {userId}`

**Request Body:**
```json
{
  "listingId": 101,
  "scheduledTime": "09:00:00",
  "totalPushes": 10,
  "useMembershipQuota": true
}
```

**Parameters:**
- `listingId` - ID of listing to push
- `scheduledTime` - Time to push daily (HH:mm:ss)
- `totalPushes` - Number of times to push
- `useMembershipQuota` - true = use quota, false = direct purchase

**Response:**
```json
{
  "data": {
    "listingId": 101,
    "userId": "user-123",
    "pushSource": "MEMBERSHIP",
    "message": "Push scheduled successfully for 09:00:00"
  }
}
```

---

### 3. Get Listing Push History

**Endpoint:** `GET /pushes/listing/{listingId}/history`

**Parameters:**
- `listingId` (path) - Listing ID

**Response:**
```json
{
  "data": [
    {
      "pushId": 1,
      "listingId": 101,
      "userId": "user-123",
      "pushSource": "MEMBERSHIP_QUOTA",
      "pushedAt": "2025-01-03T15:00:00"
    },
    {
      "pushId": 2,
      "listingId": 101,
      "userId": "user-123",
      "pushSource": "SCHEDULED",
      "pushedAt": "2025-01-04T09:00:00"
    }
  ]
}
```

---

### 4. Get My Push History

**Endpoint:** `GET /pushes/my-history`

**Headers:** `user-id: {userId}`

**Response:** Same as listing push history

---

### 5. Cancel Scheduled Push

**Endpoint:** `DELETE /pushes/schedule/{scheduleId}`

**Headers:** `user-id: {userId}`

**Parameters:**
- `scheduleId` (path) - Schedule ID

**Response:**
```json
{
  "message": "Scheduled push cancelled successfully"
}
```

---

## 📊 Usage Examples

### Example 1: Complete Membership Purchase Flow

```bash
# Step 1: Browse packages
curl -X GET http://localhost:8080/v1/memberships/packages

# Step 2: Purchase STANDARD package
curl -X POST http://localhost:8080/v1/memberships/purchase \
  -H "user-id: user-123" \
  -H "Content-Type: application/json" \
  -d '{
    "membershipId": 2,
    "paymentProvider": "VNPAY"
  }'

# Step 3: Check received quotas
curl -X GET http://localhost:8080/v1/memberships/my-membership \
  -H "user-id: user-123"
```

### Example 2: Push Listing with Quota

```bash
# Step 1: Check push quota
curl -X GET http://localhost:8080/v1/memberships/quota/pushes \
  -H "user-id: user-123"

# Step 2: Push listing
curl -X POST http://localhost:8080/v1/pushes/boost \
  -H "user-id: user-123" \
  -H "Content-Type: application/json" \
  -d '{
    "listingId": 101,
    "useMembershipQuota": true
  }'

# Step 3: View push history
curl -X GET http://localhost:8080/v1/pushes/my-history \
  -H "user-id: user-123"
```

### Example 3: Schedule Daily Boosts

```bash
# Schedule 10 boosts at 9 AM daily
curl -X POST http://localhost:8080/v1/pushes/schedule \
  -H "user-id: user-123" \
  -H "Content-Type: application/json" \
  -d '{
    "listingId": 101,
    "scheduledTime": "09:00:00",
    "totalPushes": 10,
    "useMembershipQuota": true
  }'
```

---

## 🔐 Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid request",
  "message": "Membership package not found: 999"
}
```

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "user-id header is required"
}
```

### 404 Not Found
```json
{
  "error": "Not found",
  "message": "No active membership found for user"
}
```

### 409 Conflict
```json
{
  "error": "Insufficient quota",
  "message": "No push quota available"
}
```

---

## 📝 Business Rules

### Membership Purchase
- User receives ALL benefits immediately
- Total quantity = quantity_per_month × duration_months
- No monthly cycles, one-time grant

### Quota Consumption
- Quotas consumed on first-come-first-served basis
- Oldest benefits consumed first
- No quota rollover when membership expires

### Premium Listings
- Automatically creates shadow NORMAL listing
- Boosting Premium also boosts shadow (FREE)
- Shadow syncs with parent listing

### Boost Scheduling
- Executes daily at specified time
- Consumes quota per execution
- Stops when quota exhausted or total pushes reached

---

## 🧪 Testing with Swagger

Access Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

All endpoints are documented with:
- Request/response schemas
- Example values
- Try-it-out functionality

---

## 📞 Support

For API issues or questions:
1. Check this documentation
2. Review Swagger UI
3. Examine response error messages
4. Check server logs

