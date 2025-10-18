# SmartRent API - Swagger Documentation Overview

## Accessing the API Documentation

Once the SmartRent backend is running, you can access the interactive Swagger UI documentation at:

**Swagger UI**: `http://localhost:8080/swagger-ui.html`
**OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

---

## API Groups

The SmartRent API is organized into **12 logical groups** for easy navigation:

### 1. SmartRent Complete API
**Group**: `smartrent-api`
**Description**: All endpoints across the entire platform
**Paths**: `/v1/**`

### 2. Authentication & Verification
**Group**: `authentication`
**Description**: User and admin authentication, token management, and email verification
**Paths**: `/v1/auth/**`, `/v1/verification/**`

**Key Endpoints**:
- User login/logout
- Admin login/logout
- Token refresh and validation
- Email verification code management

### 3. User Management
**Group**: `user-management`
**Description**: User account creation and profile management
**Paths**: `/v1/users/**`

**Key Endpoints**:
- Create user account
- Get user profile

### 4. Admin Management & Roles
**Group**: `admin-management`
**Description**: Administrator account management and role operations
**Paths**: `/v1/admins/**`, `/v1/auth/admin/**`, `/v1/roles/**`

**Key Endpoints**:
- Create admin account
- Get admin profile
- List all roles

### 5. Property Listings
**Group**: `listings`
**Description**: CRUD operations for property listings
**Paths**: `/v1/listings/**`

**Key Endpoints**:
- Create listing
- Get listing by ID
- List listings (paginated or by IDs)
- Update listing
- Delete listing

### 6. Address Management
**Group**: `addresses`
**Description**: Administrative address endpoints for provinces, districts, wards, and streets
**Paths**: `/v1/addresses/**`

**Key Endpoints**:
- List/search provinces
- List/search districts by province
- List/search wards by district
- List/search streets by ward
- Create and search addresses
- Find nearby addresses

### 7. File Upload
**Group**: `file-upload`
**Description**: Image and video upload to cloud storage
**Paths**: `/v1/upload/**`

**Key Endpoints**:
- Upload image
- Upload video

### 8. Pricing & Price History
**Group**: `pricing`
**Description**: Listing price management and historical price tracking
**Paths**: `/v1/listings/*/price`, `/v1/listings/*/pricing-history`, etc.

**Key Endpoints**:
- Update listing price
- Get pricing history
- Get current price
- Get price statistics
- Find listings with recent price changes

### 9. Membership Management
**Group**: `membership`
**Description**: Premium membership packages and subscription management
**Paths**: `/v1/memberships/**`

**Key Endpoints**:
- Get all membership packages
- Get package by ID
- Purchase membership
- Get active membership
- Get membership history
- Check quota (VIP posts, Premium posts, Boosts)
- Cancel membership

### 10. Boost & Promotion
**Group**: `boost`
**Description**: Listing boost features to increase visibility
**Paths**: `/v1/boosts/**`

**Key Endpoints**:
- Boost listing immediately
- Schedule automatic boosts
- Get boost history (by listing or user)
- Cancel scheduled boost

### 11. Saved Listings
**Group**: `saved-listings`
**Description**: User favorite/saved listings management
**Paths**: `/v1/saved-listings/**`

**Key Endpoints**:
- Save a listing
- Remove saved listing
- Get my saved listings
- Check if listing is saved
- Get saved listings count

### 12. VNPay Payments & Transactions
**Group**: `payments`
**Description**: Payment processing and transaction management
**Paths**: `/v1/payments/**`

**Key Endpoints**:
- Initiate membership payment
- Initiate post payment
- Initiate boost payment
- Payment callback handling
- Get transaction history

### 13. Quota Management
**Group**: `quotas`
**Description**: User quota tracking and management
**Paths**: `/v1/quotas/**`

**Key Endpoints**:
- Check all quotas
- Check specific quota by benefit type

---

## Authentication

The API uses **JWT (JSON Web Token)** for authentication.

### Authentication Flow

1. **User Registration**:
   - `POST /v1/users` - Create account
   - `POST /v1/verification/code?email={email}` - Send verification code
   - `POST /v1/verification` - Verify email with code

2. **User Login**:
   - `POST /v1/auth` - Login with email/password
   - Receive `accessToken` and `refreshToken`

3. **Using Protected Endpoints**:
   - Add header: `Authorization: Bearer {accessToken}`
   - Some endpoints also require: `user-id: {userId}` header

4. **Token Refresh**:
   - `POST /v1/auth/refresh` - Get new tokens using refresh token

5. **Logout**:
   - `POST /v1/auth/logout` - Invalidate token

### Admin Authentication

Similar flow but using `/v1/auth/admin/**` endpoints.

---

## Response Format

All API responses follow a consistent format:

### Success Response
```json
{
  "code": "999999",
  "message": null,
  "data": { /* response payload */ }
}
```

### Error Response
```json
{
  "code": "400001",
  "message": "INVALID_INPUT",
  "data": null
}
```

### Error Code Categories
- **1xxx**: Internal server errors
- **2xxx**: Client input validation errors
- **3xxx**: Resource conflict errors (already exists)
- **4xxx**: Resource not found errors
- **5xxx**: Authentication errors (unauthenticated)

---

## Key Features Documented

### 1. Membership System
- Multiple membership tiers (BASIC, STANDARD, PREMIUM, VIP)
- Quota-based benefits:
  - VIP Posts
  - Premium Posts
  - Boost Quota
  - Priority Support
- Automatic benefit allocation upon purchase
- Membership history tracking
- Real-time quota checking

### 2. Boost System
- **Instant Boost**: Push listing to top immediately
- **Scheduled Boost**: Automatic daily boosts at specific times
- **Payment Options**: Use membership quota or direct purchase
- **History Tracking**: View all boost activities
- **Source Tracking**: Track boost source (membership vs. direct purchase)

### 3. Dynamic Pricing
- Update listing prices with effective dates
- Track complete price history
- Calculate price statistics (min, max, average)
- Identify listings with recent price changes
- Date range filtering for historical data

### 4. Saved Listings
- Save favorite listings for later
- Quick check if listing is already saved
- Count total saved listings
- Remove from saved list

### 5. Address Hierarchy
- Complete Vietnam address system
- Province → District → Ward → Street hierarchy
- Search functionality at each level
- Nearby address discovery
- Coordinate-based location services

---

## Testing with Swagger UI

### Step 1: Start the Application
```bash
cd smart-rent
./gradlew bootRun
```

### Step 2: Open Swagger UI
Navigate to: `http://localhost:8080/swagger-ui.html`

### Step 3: Authenticate
1. Create a user account via `POST /v1/users`
2. Verify email via verification endpoints
3. Login via `POST /v1/auth`
4. Click **Authorize** button in Swagger UI
5. Enter: `Bearer {your-access-token}`
6. Click **Authorize**

### Step 4: Test Endpoints
- Select an API group from the dropdown
- Expand an endpoint
- Click **Try it out**
- Fill in parameters
- Click **Execute**
- View response

---

## API Statistics

- **Total API Groups**: 13
- **Total Controllers**: 15
- **Authentication Methods**: JWT Bearer Token
- **Supported Operations**: CRUD + Custom operations
- **Response Format**: JSON
- **API Version**: v1

---

## Recent Updates

### New API Groups Added:
1. **Membership Management** - Complete membership package system
2. **Boost & Promotion** - Listing visibility enhancement
3. **Saved Listings** - User favorites management
4. **Pricing & Price History** - Dynamic pricing with history
5. **VNPay Payments & Transactions** - Payment processing
6. **Quota Management** - User quota tracking

### Enhanced Documentation:
- Added comprehensive descriptions for all endpoints
- Included request/response examples
- Added error response documentation
- Organized into logical API groups
- Added authentication flow documentation

---

## Support

For API documentation issues or questions:
- **Email**: api-support@smartrent.com
- **Documentation**: https://docs.smartrent.com
- **Swagger UI**: http://localhost:8080/swagger-ui.html

---

## Quick Start Guide

1. **Start the backend**:
   ```bash
   ./gradlew bootRun
   ```

2. **Access Swagger UI**:
   ```
   http://localhost:8080/swagger-ui.html
   ```

3. **Select an API group** from the dropdown menu

4. **Authenticate** using the Authorize button

5. **Test endpoints** using the "Try it out" feature

6. **View detailed documentation** for each endpoint including:
   - Request parameters
   - Request body schemas
   - Response schemas
   - Example requests/responses
   - Error codes and descriptions

---

## Notes

- All timestamps are in UTC format
- Sensitive data (passwords, tokens) are masked in logs
- Null fields are excluded from JSON responses
- All string fields support UTF-8 encoding
- Rate limiting is applied to prevent abuse
- Circuit breaker pattern implemented for email services

