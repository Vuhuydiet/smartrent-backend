# Postman Testing Guide - Listing API with Pricing History

## Base Configuration

**Base URL**: `http://localhost:8080`

**Required Headers** (for authenticated endpoints):
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

---

## Complete Test Flow

### Step 1: Authentication (Get JWT Token)

First, you need to authenticate to get a JWT token.

**Endpoint**: `POST /v1/auth/login`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "YourPassword123!"
}
```

**Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Save the `accessToken`** and use it in the Authorization header for all subsequent requests.

---

## Listing API Test Cases

### Test Case 1: Create a New Listing (Initial Pricing)

**Endpoint**: `POST /v1/listings`

**Headers**:
```
Authorization: Bearer <your_access_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "title": "Căn hộ 2PN thoáng mát quận 1",
  "description": "Căn hộ 2 phòng ngủ rộng rãi, có ban công và tầm nhìn đẹp ra thành phố. Gần chợ Bến Thành, tiện ích đầy đủ.",
  "userId": "user-123e4567-e89b-12d3-a456-426614174000",
  "expiryDate": "2025-12-31T23:59:59",
  "listingType": "RENT",
  "verified": false,
  "isVerify": false,
  "expired": false,
  "vipType": "NORMAL",
  "categoryId": 10,
  "productType": "APARTMENT",
  "price": 15000000.00,
  "priceUnit": "MONTH",
  "addressId": 501,
  "area": 78.5,
  "bedrooms": 2,
  "bathrooms": 1,
  "direction": "NORTHEAST",
  "furnishing": "SEMI_FURNISHED",
  "propertyType": "APARTMENT",
  "roomCapacity": 4,
  "amenityIds": [1, 3, 5]
}
```

**Expected Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "listingId": 123,
    "status": "CREATED"
  }
}
```

**What Happens Behind the Scenes**:
- Listing is created with price: 15,000,000 VND/month
- **INITIAL pricing history** entry is automatically created
- Change type: `INITIAL`
- `isCurrent`: `true`

---

### Test Case 2: Get Listing by ID (View Pricing History)

**Endpoint**: `GET /v1/listings/{id}`

**Example**: `GET /v1/listings/123`

**Headers**:
```
Authorization: Bearer <your_access_token>
```

**Expected Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "listingId": 123,
    "title": "Căn hộ 2PN thoáng mát quận 1",
    "description": "Căn hộ 2 phòng ngủ rộng rãi...",
    "userId": "user-123e4567-e89b-12d3-a456-426614174000",
    "postDate": "2025-10-04T10:30:00",
    "expiryDate": "2025-12-31T23:59:59",
    "listingType": "RENT",
    "verified": false,
    "isVerify": false,
    "expired": false,
    "vipType": "NORMAL",
    "status": "PENDING",
    "categoryId": 10,
    "productType": "APARTMENT",
    "price": 15000000.00,
    "priceUnit": "MONTH",
    "addressId": 501,
    "area": 78.5,
    "bedrooms": 2,
    "bathrooms": 1,
    "direction": "NORTHEAST",
    "furnishing": "SEMI_FURNISHED",
    "propertyType": "APARTMENT",
    "roomCapacity": 4,
    "amenities": [
      {
        "amenityId": 1,
        "name": "WiFi",
        "icon": "wifi",
        "description": "Internet không dây tốc độ cao",
        "category": "connectivity",
        "isActive": true
      }
    ],
    "currentPricing": {
      "id": 1,
      "listingId": 123,
      "oldPrice": null,
      "newPrice": 15000000.00,
      "oldPriceUnit": null,
      "newPriceUnit": "MONTH",
      "changeType": "INITIAL",
      "changePercentage": null,
      "changeAmount": null,
      "isCurrent": true,
      "changedBy": "user-123e4567-e89b-12d3-a456-426614174000",
      "changeReason": "Initial listing price",
      "changedAt": "2025-10-04T10:30:00"
    },
    "priceHistory": [
      {
        "id": 1,
        "listingId": 123,
        "oldPrice": null,
        "newPrice": 15000000.00,
        "oldPriceUnit": null,
        "newPriceUnit": "MONTH",
        "changeType": "INITIAL",
        "changePercentage": null,
        "changeAmount": null,
        "isCurrent": true,
        "changedBy": "user-123e4567-e89b-12d3-a456-426614174000",
        "changeReason": "Initial listing price",
        "changedAt": "2025-10-04T10:30:00"
      }
    ],
    "createdAt": "2025-10-04T10:30:00",
    "updatedAt": "2025-10-04T10:30:00"
  }
}
```

**Key Fields to Check**:
- `currentPricing` - Shows the active price
- `priceHistory` - Array of all price changes (currently only INITIAL)

---

### Test Case 3: Update Price (Price Increase)

**Endpoint**: `PUT /v1/listings/{id}`

**Example**: `PUT /v1/listings/123`

**Headers**:
```
Authorization: Bearer <your_access_token>
Content-Type: application/json
```

**Request Body** (Increase price from 15M to 17M):
```json
{
  "title": "Căn hộ 2PN thoáng mát quận 1",
  "description": "Căn hộ 2 phòng ngủ rộng rãi, có ban công và tầm nhìn đẹp ra thành phố. Gần chợ Bến Thành, tiện ích đầy đủ.",
  "userId": 42,
  "expiryDate": "2025-12-31T23:59:59",
  "listingType": "RENT",
  "vipType": "NORMAL",
  "categoryId": 10,
  "productType": "APARTMENT",
  "price": 17000000.00,
  "priceUnit": "MONTH",
  "addressId": 501,
  "area": 78.5,
  "bedrooms": 2,
  "bathrooms": 1,
  "direction": "NORTHEAST",
  "furnishing": "SEMI_FURNISHED",
  "propertyType": "APARTMENT",
  "roomCapacity": 4,
  "amenityIds": [1, 3, 5]
}
```

**Expected Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "listingId": 123,
    "price": 17000000.00,
    "priceUnit": "MONTH",
    "currentPricing": {
      "id": 2,
      "listingId": 123,
      "oldPrice": 15000000.00,
      "newPrice": 17000000.00,
      "oldPriceUnit": "MONTH",
      "newPriceUnit": "MONTH",
      "changeType": "INCREASE",
      "changePercentage": 13.33,
      "changeAmount": 2000000.00,
      "isCurrent": true,
      "changedBy": "user-123e4567-e89b-12d3-a456-426614174000",
      "changeReason": "Price updated via listing update",
      "changedAt": "2025-10-04T11:00:00"
    },
    "priceHistory": [
      {
        "id": 2,
        "listingId": 123,
        "oldPrice": 15000000.00,
        "newPrice": 17000000.00,
        "oldPriceUnit": "MONTH",
        "newPriceUnit": "MONTH",
        "changeType": "INCREASE",
        "changePercentage": 13.33,
        "changeAmount": 2000000.00,
        "isCurrent": true,
        "changedBy": "user-123e4567-e89b-12d3-a456-426614174000",
        "changeReason": "Price updated via listing update",
        "changedAt": "2025-10-04T11:00:00"
      },
      {
        "id": 1,
        "listingId": 123,
        "oldPrice": null,
        "newPrice": 15000000.00,
        "oldPriceUnit": null,
        "newPriceUnit": "MONTH",
        "changeType": "INITIAL",
        "changePercentage": null,
        "changeAmount": null,
        "isCurrent": false,
        "changedBy": "user-123e4567-e89b-12d3-a456-426614174000",
        "changeReason": "Initial listing price",
        "changedAt": "2025-10-04T10:30:00"
      }
    ]
  }
}
```

**What Happened**:
- Price increased from 15M → 17M (+2M, +13.33%)
- Change type: `INCREASE`
- Previous pricing history entry marked `isCurrent: false`
- New pricing history entry marked `isCurrent: true`

---

### Test Case 4: Update Price (Price Decrease)

**Endpoint**: `PUT /v1/listings/123`

**Request Body** (Decrease price from 17M to 14M):
```json
{
  "title": "Căn hộ 2PN thoáng mát quận 1",
  "userId": 42,
  "categoryId": 10,
  "productType": "APARTMENT",
  "price": 14000000.00,
  "priceUnit": "MONTH",
  "addressId": 501
}
```

**Expected Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "listingId": 123,
    "price": 14000000.00,
    "currentPricing": {
      "id": 3,
      "listingId": 123,
      "oldPrice": 17000000.00,
      "newPrice": 14000000.00,
      "oldPriceUnit": "MONTH",
      "newPriceUnit": "MONTH",
      "changeType": "DECREASE",
      "changePercentage": -17.65,
      "changeAmount": -3000000.00,
      "isCurrent": true,
      "changedBy": "user-123e4567-e89b-12d3-a456-426614174000",
      "changeReason": "Price updated via listing update",
      "changedAt": "2025-10-04T11:30:00"
    }
  }
}
```

**What Happened**:
- Price decreased from 17M → 14M (-3M, -17.65%)
- Change type: `DECREASE`
- Negative percentage and amount

---

### Test Case 5: Change Price Unit

**Endpoint**: `PUT /v1/listings/123`

**Request Body** (Change from MONTH to DAY):
```json
{
  "title": "Căn hộ 2PN thoáng mát quận 1",
  "userId": 42,
  "categoryId": 10,
  "productType": "APARTMENT",
  "price": 500000.00,
  "priceUnit": "DAY",
  "addressId": 501
}
```

**Expected Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "listingId": 123,
    "price": 500000.00,
    "priceUnit": "DAY",
    "currentPricing": {
      "id": 4,
      "listingId": 123,
      "oldPrice": 14000000.00,
      "newPrice": 500000.00,
      "oldPriceUnit": "MONTH",
      "newPriceUnit": "DAY",
      "changeType": "UNIT_CHANGE",
      "changePercentage": null,
      "changeAmount": null,
      "isCurrent": true,
      "changedBy": "user-123e4567-e89b-12d3-a456-426614174000",
      "changeReason": "Price updated via listing update",
      "changedAt": "2025-10-04T12:00:00"
    }
  }
}
```

**What Happened**:
- Price unit changed: MONTH → DAY
- Change type: `UNIT_CHANGE`
- Price amount also changed (500K/day)
- No percentage calculated (different units)

---

### Test Case 6: Get All Listings (Paginated)

**Endpoint**: `GET /v1/listings?page=0&size=20`

**Headers**:
```
Authorization: Bearer <your_access_token>
```

**Expected Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": [
    {
      "listingId": 123,
      "title": "Căn hộ 2PN thoáng mát quận 1",
      "price": 500000.00,
      "priceUnit": "DAY",
      "currentPricing": {
        "newPrice": 500000.00,
        "newPriceUnit": "DAY",
        "changeType": "UNIT_CHANGE",
        "isCurrent": true
      },
      "priceHistory": [ /* full history */ ]
    },
    {
      "listingId": 124,
      "title": "Studio hiện đại gần công viên",
      "price": 8500000.00,
      "priceUnit": "MONTH",
      "currentPricing": { /* ... */ }
    }
  ]
}
```

---

### Test Case 7: Get Listings by IDs

**Endpoint**: `GET /v1/listings?ids=123,124,125`

**Headers**:
```
Authorization: Bearer <your_access_token>
```

**Expected Response**: Returns only listings with IDs 123, 124, 125

---

### Test Case 8: Delete Listing

**Endpoint**: `DELETE /v1/listings/123`

**Headers**:
```
Authorization: Bearer <your_access_token>
```

**Expected Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": null
}
```

**What Happens**:
- Listing is deleted
- All associated pricing history entries are also deleted (cascade delete)

---

## Validation Test Cases

### Test Case 9: Invalid Price (Negative)

**Endpoint**: `POST /v1/listings`

**Request Body**:
```json
{
  "title": "Test Listing",
  "userId": "user-123",
  "categoryId": 10,
  "productType": "APARTMENT",
  "price": -1000.00,
  "priceUnit": "MONTH",
  "addressId": 501
}
```

**Expected Response**:
```json
{
  "code": "2001",
  "message": "EMPTY_INPUT or validation error",
  "data": null
}
```

---

### Test Case 10: Missing Required Fields

**Endpoint**: `POST /v1/listings`

**Request Body**:
```json
{
  "title": "Test Listing"
}
```

**Expected Response**:
```json
{
  "code": "2001",
  "message": "EMPTY_INPUT",
  "data": null
}
```

---

## Pricing History Analysis Scenarios

### Scenario 1: Track Price Fluctuations

1. Create listing at 15M/month
2. Update to 17M/month (INCREASE +13.33%)
3. Update to 14M/month (DECREASE -17.65%)
4. Update to 16M/month (INCREASE +14.29%)

**Check `priceHistory` array** - should show all 5 entries (1 INITIAL + 4 changes)

### Scenario 2: Price Unit Changes

1. Create listing at 15M/MONTH
2. Change to 500K/DAY (UNIT_CHANGE)
3. Change back to 16M/MONTH (UNIT_CHANGE)

**Check**: `changeType` should be `UNIT_CHANGE` for unit changes

### Scenario 3: View Historical Trends

**GET** `/v1/listings/123` and analyze:
- `currentPricing.changePercentage` - latest change %
- `priceHistory[0]` - most recent change
- `priceHistory[last]` - initial price
- Compare `oldPrice` vs `newPrice` across history

---

## Postman Collection Tips

### Environment Variables

Create a Postman environment with:
```
base_url: http://localhost:8080
access_token: <set after login>
listing_id: <set after create>
```

### Pre-request Script (Auto-login)

```javascript
// Auto-refresh token if expired
if (!pm.environment.get("access_token")) {
    pm.sendRequest({
        url: pm.environment.get("base_url") + "/v1/auth/login",
        method: 'POST',
        header: 'Content-Type: application/json',
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                email: "user@example.com",
                password: "YourPassword123!"
            })
        }
    }, function (err, res) {
        var token = res.json().data.accessToken;
        pm.environment.set("access_token", token);
    });
}
```

### Test Script (Save Listing ID)

```javascript
// After creating a listing, save the ID
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("listing_id", jsonData.data.listingId);

    // Test that pricing history was created
    pm.test("Listing created with ID", function () {
        pm.expect(jsonData.data.listingId).to.be.a('number');
    });
}
```

---

## Expected Pricing History Behavior Summary

| Action | Change Type | Percentage Calculated | Amount Calculated |
|--------|-------------|----------------------|-------------------|
| Create listing | INITIAL | ❌ No | ❌ No |
| Increase price | INCREASE | ✅ Yes | ✅ Yes |
| Decrease price | DECREASE | ✅ Yes (negative) | ✅ Yes (negative) |
| Change unit | UNIT_CHANGE | ❌ No | ❌ No |
| Same price | CORRECTION | ❌ No | ❌ No |

**Notes**:
- Only **one** pricing entry has `isCurrent: true` at any time
- All previous entries are marked `isCurrent: false`
- History is ordered by `changedAt` descending (newest first)
- Price changes are tracked per listing
- Cascade delete: deleting listing removes all pricing history
