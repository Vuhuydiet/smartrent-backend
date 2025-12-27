# API Authentication Guide

## Overview

This document explains how to authenticate API requests in the SmartRent system. The application uses **JWT (JSON Web Token)** with Bearer authentication scheme.

---

## Important: User ID Format

**User IDs in this system are UUID strings, NOT numeric Long values.**

The JWT token's `sub` claim contains the user's UUID (e.g., `"550e8400-e29b-41d4-a716-446655440000"`), which is automatically extracted by the authentication system. Controllers and services use this UUID string directly - there is no conversion to numeric IDs.

**Token Structure:**
```
Header.Payload.Signature

Payload (decoded):
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",  // User UUID
  "exp": 1730589600,
  "iat": 1730586000,
  "jti": "uuid-xxx",
  "scope": "ROLE_USER"
}
```

**What was fixed:** Earlier versions incorrectly attempted to parse the UUID string as a Long integer, causing "Invalid user ID format in authentication" errors. This has been corrected - the system now properly handles UUID strings throughout.

---

## Error Code 5001: UNAUTHENTICATED

**Error Response:**
```json
{
  "code": "5001",
  "message": "Unauthenticated"
}
```

**Meaning:** Your request lacks valid authentication credentials or the provided token is invalid.

---

## Authentication Flow

### Step 1: Obtain Access Token

**Endpoint:** `POST /v1/auth/login`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "yourPassword123"
}
```

**Response:**
```json
{
  "code": "0000",
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "expiresIn": 3600
  }
}
```

**Token Types:**
- **Access Token**: Used for API requests (short-lived)
- **Refresh Token**: Used to get new access tokens (long-lived)

---

### Step 2: Use Access Token in Requests

**Required Header Format:**
```
Authorization: Bearer <your_access_token>
```

**Example cURL Request:**
```bash
curl -X POST https://api.smartrent.com/v1/media/upload \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/image.jpg"
```

**Example HTTP Request:**
```http
POST /v1/media/upload HTTP/1.1
Host: api.smartrent.com
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
Content-Type: multipart/form-data

...
```

---

### Step 3: Refresh Expired Token

When access token expires, use refresh token to get new one.

**Endpoint:** `POST /v1/auth/refresh`

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response:**
```json
{
  "code": "0000",
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "expiresIn": 3600
  }
}
```

---

## Protected vs Public Endpoints

### Public Endpoints (No Authentication Required)

**Authentication & Registration:**
- `POST /v1/auth/login`
- `POST /v1/auth/logout`
- `POST /v1/auth/refresh`
- `POST /v1/users` - User registration
- `POST /v1/admins` - Admin registration

**Verification:**
- `POST /v1/verification/**` - Email/Phone verification

**Listings (Read-Only):**
- `GET /v1/listings/**` - View listings
- `POST /v1/listings` - Create listing (MISCONFIGURED - should be protected)
- `POST /v1/listings/**` - Listing operations (MISCONFIGURED - should be protected)

**Public Data:**
- `GET /v1/addresses/**`
- `GET /v1/memberships/packages/**`
- `GET /v1/vip-tiers/**`

**System:**
- `GET /actuator/health`
- Swagger UI documentation

### Protected Endpoints (Authentication Required)

**Media Operations (ALL require authentication):**
- `POST /v1/media/upload-url` - Generate presigned upload URL
- `POST /v1/media/upload` - Direct file upload
- `POST /v1/media/{mediaId}/confirm` - Confirm upload completion
- `GET /v1/media/{mediaId}/download-url` - Get download URL
- `DELETE /v1/media/{mediaId}` - Delete media
- `POST /v1/media/external` - Save external media URL
- `GET /v1/media/listing/{listingId}` - Get listing media
- `GET /v1/media/my-media` - Get current user's media
- `GET /v1/media/{mediaId}` - Get media details

**Listing Operations (should require authentication):**
- `POST /v1/listings` - Create listing (supports NORMAL, SILVER, GOLD, DIAMOND with payment/quota)
- `PUT /v1/listings/{id}` - Update listing
- `DELETE /v1/listings/{id}` - Delete listing
- `GET /v1/listings/quota-check` - Check listing quota
- `GET /v1/listings/{id}/admin` - Admin listing details

**Payments:**
- `POST /v1/payments/**` - Payment operations

**User Profile:**
- `GET /v1/users/me` - Get current user info
- `PUT /v1/users/{id}` - Update user

---

## Token Validation Process

The system performs the following checks on every authenticated request:

### 1. Token Format Validation
- Must be valid JWT format: `header.payload.signature`
- Must use HMAC-SHA512 algorithm
- Must have proper structure

### 2. Signature Verification
- Token signature verified using `ACCESS_SIGNER_KEY`
- Ensures token was issued by this server
- Prevents token tampering

### 3. Expiration Check
- Token expiration time (`exp` claim) must be in future
- Default expiration configured in environment variables

### 4. Invalidation Check
- Checks Redis cache for logged-out tokens
- Token invalidated on logout remains invalid until expiration

### 5. User Context Extraction
- User ID extracted from token subject (`sub` claim)
- Authentication context populated for controllers

---

## Common Authentication Issues

### Issue 1: Missing Authorization Header

**Error:** `5001 UNAUTHENTICATED`

**Cause:** No `Authorization` header in request

**Solution:**
```bash
# Wrong
curl -X POST /v1/media/upload

# Correct
curl -X POST /v1/media/upload \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### Issue 2: Incorrect Header Format

**Error:** `5001 UNAUTHENTICATED`

**Cause:** Malformed header format

**Common Mistakes:**
```bash
# Wrong - Missing "Bearer" prefix
Authorization: eyJhbGciOiJIUzUxMiJ9...

# Wrong - Extra space
Authorization: Bearer  eyJhbGciOiJIUzUxMiJ9...

# Wrong - Lowercase bearer
Authorization: bearer eyJhbGciOiJIUzUxMiJ9...

# Correct
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

---

### Issue 3: Expired Token

**Error:** `5001 UNAUTHENTICATED`

**Cause:** Token expiration time has passed

**Solution:** Use refresh token to get new access token
```bash
curl -X POST /v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "YOUR_REFRESH_TOKEN"}'
```

---

### Issue 4: Token Invalidated (Logged Out)

**Error:** `5001 UNAUTHENTICATED`

**Cause:** Token was invalidated by logout

**Solution:** Login again to get new tokens
```bash
curl -X POST /v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'
```

---

### Issue 5: Invalid Signature

**Error:** `5001 UNAUTHENTICATED`

**Cause:**
- Token signed with different key
- Token modified/tampered
- Wrong environment (dev token in prod)

**Solution:**
- Ensure `ACCESS_SIGNER_KEY` matches between environments
- Get fresh token from correct environment
- Never modify token manually

---

### Issue 6: Configuration Issue with Listing Endpoints

**Problem:** `POST /v1/listings` and `POST /v1/listings/**` are configured as **public endpoints** in `application.yml`

**Location:** `src/main/resources/application.yml` (Lines 133-134)

**Current Configuration:**
```yaml
application:
  authorize-ignored:
    methods:
      post:
        - "/v1/listings"      # Public - anyone can create
        - "/v1/listings/**"   # Public - anyone can modify
```

**Security Risk:** This allows unauthenticated users to create and modify listings, which is likely unintended.

**Recommended Fix:** Remove these lines from the configuration to require authentication:

```yaml
application:
  authorize-ignored:
    methods:
      post:
        - "/v1/auth/**"
        - "/v1/users"
        - "/v1/admins"
        - "/v1/verification/**"
        # REMOVED: - "/v1/listings"
        # REMOVED: - "/v1/listings/**"
        - "/v1/payments/vnpay/ipn"
```

After removing these lines, `POST /v1/listings/**` will require authentication like the media endpoints.

---

## Testing Authentication

### Using cURL

**1. Login:**
```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**2. Save token to variable:**
```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."
```

**3. Test protected endpoint:**
```bash
curl -X POST http://localhost:8080/v1/media/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@image.jpg"
```

---

### Using Postman

**1. Setup:**
- Create new request
- Set URL: `http://localhost:8080/v1/media/upload`
- Set method: `POST`

**2. Configure Authorization:**
- Go to "Authorization" tab
- Select Type: "Bearer Token"
- Paste token in "Token" field

**3. Add Body:**
- Go to "Body" tab
- Select "form-data"
- Add key: `file`, type: `File`
- Choose file to upload

**4. Send Request**

---

### Using Swagger UI

**1. Access Swagger UI:**
```
http://localhost:8080/swagger-ui.html
```

**2. Authorize:**
- Click "Authorize" button (lock icon)
- Enter: `Bearer YOUR_TOKEN`
- Click "Authorize"
- Click "Close"

**3. Try Endpoints:**
- Expand endpoint
- Click "Try it out"
- Fill parameters
- Click "Execute"

---

## Token Lifecycle

```
┌─────────────┐
│   Login     │
│ POST /auth/ │
│    login    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  Access Token   │ ────────────┐
│ (1 hour valid)  │             │
└──────┬──────────┘             │
       │                        │
       ▼                        ▼
┌─────────────────┐      ┌──────────────┐
│  API Requests   │      │Refresh Token │
│ with Bearer     │      │(7 days valid)│
│     Token       │      └──────┬───────┘
└──────┬──────────┘             │
       │                        │
       │ Token Expires          │
       │                        │
       └────────────────────────┘
                  │
                  ▼
         ┌─────────────────┐
         │  POST /auth/    │
         │    refresh      │
         └─────────────────┘
                  │
                  ▼
         ┌─────────────────┐
         │ New Access Token│
         └─────────────────┘
```

---

## Environment Configuration

### Required Environment Variables

**JWT Configuration:**
```bash
# Signing key for access tokens (HMAC-SHA512)
ACCESS_SIGNER_KEY=your-secret-key-at-least-64-chars-long

# Token expiration times (in seconds)
JWT_ACCESS_DURATION=3600          # 1 hour
JWT_REFRESH_DURATION=604800       # 7 days
```

**Redis Configuration (for token invalidation):**
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
```

**Database Configuration:**
```bash
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password
```

---

## Security Best Practices

### For Developers

1. **Never commit tokens** to version control
2. **Use environment variables** for sensitive keys
3. **Rotate keys regularly** in production
4. **Use HTTPS** in production (never HTTP)
5. **Implement rate limiting** on auth endpoints
6. **Log authentication failures** for security monitoring
7. **Set appropriate token expiration** times

### For API Consumers

1. **Store tokens securely** (never in localStorage for web apps)
2. **Refresh tokens proactively** before expiration
3. **Handle 5001 errors** by redirecting to login
4. **Never log tokens** in application logs
5. **Use HTTPS only** for API requests
6. **Implement token refresh logic** automatically
7. **Clear tokens on logout**

---

## Quick Reference

### HTTP Status Codes

| Status | Code | Meaning |
|--------|------|---------|
| 401 | 5001 | UNAUTHENTICATED - Missing or invalid token |
| 403 | 6xxx | FORBIDDEN - Valid token but insufficient permissions |
| 200 | 0000 | SUCCESS - Request completed successfully |

### Header Format

```
Authorization: Bearer <token>
```

### Token Claims

```json
{
  "sub": "123",           // User ID
  "exp": 1234567890,      // Expiration timestamp
  "iat": 1234567000,      // Issued at timestamp
  "jti": "uuid",          // JWT ID (for invalidation)
  "scope": "ROLE_USER"    // User role
}
```

---

## Troubleshooting Commands

### Check Token Expiration

**Decode JWT (online tool or local):**
```bash
# Using jwt.io (don't use for production tokens)
# Paste token at https://jwt.io

# Using jq (if you have the token)
echo "YOUR_TOKEN" | cut -d'.' -f2 | base64 -d | jq
```

### Test Redis Connection

```bash
redis-cli -h localhost -p 6379 ping
# Expected: PONG
```

### Check Application Logs

```bash
# Enable debug logging in application-local.yml
logging:
  level:
    com.smartrent.config.security: DEBUG
    org.springframework.security: DEBUG
```

### Verify Environment Variables

```bash
# Check if ACCESS_SIGNER_KEY is loaded
./gradlew bootRun --args='--spring.profiles.active=local'
# Look for startup logs showing JWT configuration
```

---

## Support

**Documentation:**
- Error Codes: `/docs/ERROR_CODES.md`
- Architecture: `/CLAUDE.md`
- API Reference: `/swagger-ui.html`

**Debugging:**
- Enable debug logging: Set `logging.level.com.smartrent=DEBUG`
- Check Redis: Ensure connection is active
- Verify environment: Check all required env vars are set

**Common Files:**
- Security Config: `src/main/java/com/smartrent/config/security/SecurityConfig.java`
- JWT Decoder: `src/main/java/com/smartrent/config/security/CustomJwtDecoder.java`
- Auth Service: `src/main/java/com/smartrent/service/authentication/impl/AuthenticationServiceImpl.java`
- Public Endpoints: `src/main/resources/application.yml` (lines 108-134)