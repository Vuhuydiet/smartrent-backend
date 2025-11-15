# Phone Click Detail Tracking API Documentation

## Overview

The Phone Click Detail Tracking feature allows the SmartRent platform to track user interest in property listings by recording when users click on phone numbers in listing detail pages. This provides valuable analytics for property owners (renters) to see who is interested in their listings.

## Features

- ✅ Track phone number clicks with user details, timestamp, IP address, and user agent
- ✅ Prompt users to update their contact phone if not provided
- ✅ View all users who clicked on a specific listing's phone number
- ✅ View user's own click history
- ✅ Get statistics (total clicks, unique users) for listings
- ✅ Renter dashboard: See all interested users across all owned listings

## Base URL

```
http://localhost:8080/v1/phone-click-details
```

## Authentication

All endpoints require JWT Bearer token authentication.

**Header:**
```
Authorization: Bearer <your-jwt-token>
```

---

## API Endpoints

### 1. Track Phone Number Click

Records when a user clicks on a phone number in a listing detail page.

**Endpoint:** `POST /v1/phone-click-details`

**Authentication:** Required

**Request Body:**
```json
{
  "listingId": 123
}
```

**Request Schema:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| listingId | Long | Yes | ID of the listing whose phone number was clicked |

**Response (200 OK):**
```json
{
  "code": "999999",
  "data": {
    "id": 1,
    "listingId": 123,
    "userId": "user-123e4567-e89b-12d3-a456-426614174000",
    "userFirstName": "John",
    "userLastName": "Doe",
    "userEmail": "john.doe@example.com",
    "userContactPhone": "0912345678",
    "userContactPhoneVerified": true,
    "clickedAt": "2024-01-15T10:30:00",
    "ipAddress": "192.168.1.1"
  }
}
```

**Error Responses:**
- `401 Unauthorized` - User not authenticated
- `404 Not Found` - Listing not found

**Behavior:**
- Requires user to be logged in
- If user doesn't have contact phone, frontend should prompt them to update it first
- Records click with timestamp, IP address (supports X-Forwarded-For, X-Real-IP headers), and user agent
- Multiple clicks by the same user are tracked separately (allows tracking repeated interest)

**Use Case:**
User views a listing detail page → Clicks on the masked phone number → System tracks the interest

---

### 2. Get Users Who Clicked on Listing's Phone Number

Retrieves all users who clicked on a specific listing's phone number (unique users only).

**Endpoint:** `GET /v1/phone-click-details/listing/{listingId}`

**Authentication:** Required

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| listingId | Long | Yes | ID of the listing |

**Example Request:**
```
GET /v1/phone-click-details/listing/123
```

**Response (200 OK):**
```json
{
  "code": "999999",
  "data": [
    {
      "id": 1,
      "listingId": 123,
      "userId": "user-123e4567-e89b-12d3-a456-426614174000",
      "userFirstName": "John",
      "userLastName": "Doe",
      "userEmail": "john.doe@example.com",
      "userContactPhone": "0912345678",
      "userContactPhoneVerified": true,
      "clickedAt": "2024-01-15T10:30:00",
      "ipAddress": "192.168.1.1"
    },
    {
      "id": 5,
      "listingId": 123,
      "userId": "user-987e6543-e21b-43d2-b654-321098765432",
      "userFirstName": "Jane",
      "userLastName": "Smith",
      "userEmail": "jane.smith@example.com",
      "userContactPhone": "0987654321",
      "userContactPhoneVerified": false,
      "clickedAt": "2024-01-15T14:20:00",
      "ipAddress": "192.168.1.2"
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` - User not authenticated
- `404 Not Found` - Listing not found

**Use Case:**
Property owner views their listing management page → Sees which users are interested in this specific listing → Can contact them directly

---

### 3. Get My Phone Click History

Retrieves all listings the authenticated user has clicked phone numbers on.

**Endpoint:** `GET /v1/phone-click-details/my-clicks`

**Authentication:** Required

**Example Request:**
```
GET /v1/phone-click-details/my-clicks
```

**Response (200 OK):**
```json
{
  "code": "999999",
  "data": [
    {
      "id": 1,
      "listingId": 123,
      "userId": "user-123e4567-e89b-12d3-a456-426614174000",
      "userFirstName": "John",
      "userLastName": "Doe",
      "userEmail": "john.doe@example.com",
      "userContactPhone": "0912345678",
      "userContactPhoneVerified": true,
      "clickedAt": "2024-01-15T10:30:00",
      "ipAddress": "192.168.1.1"
    },
    {
      "id": 3,
      "listingId": 456,
      "userId": "user-123e4567-e89b-12d3-a456-426614174000",
      "userFirstName": "John",
      "userLastName": "Doe",
      "userEmail": "john.doe@example.com",
      "userContactPhone": "0912345678",
      "userContactPhoneVerified": true,
      "clickedAt": "2024-01-16T09:15:00",
      "ipAddress": "192.168.1.1"
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` - User not authenticated

**Use Case:**
User views their browsing history → Sees which listings they showed interest in → Can revisit those listings

---

### 4. Get Phone Click Statistics for a Listing

Retrieves statistics about phone clicks for a specific listing.

**Endpoint:** `GET /v1/phone-click-details/listing/{listingId}/stats`

**Authentication:** Required

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| listingId | Long | Yes | ID of the listing |

**Example Request:**
```
GET /v1/phone-click-details/listing/123/stats
```

**Response (200 OK):**
```json
{
  "code": "999999",
  "data": {
    "listingId": 123,
    "totalClicks": 25,
    "uniqueUsers": 18
  }
}
```

**Response Schema:**
| Field | Type | Description |
|-------|------|-------------|
| listingId | Long | ID of the listing |
| totalClicks | Long | Total number of phone clicks (including repeated clicks) |
| uniqueUsers | Long | Number of unique users who clicked |

**Error Responses:**
- `401 Unauthorized` - User not authenticated
- `404 Not Found` - Listing not found

**Use Case:**
Property owner views listing analytics → Tracks engagement metrics → Understands listing popularity

---

### 5. Get Phone Clicks for My Listings

Retrieves all phone clicks for all listings owned by the authenticated user. This is the primary endpoint for the renter's listing management dashboard.

**Endpoint:** `GET /v1/phone-click-details/my-listings`

**Authentication:** Required

**Example Request:**
```
GET /v1/phone-click-details/my-listings
```

**Response (200 OK):**
```json
{
  "code": "999999",
  "data": [
    {
      "id": 1,
      "listingId": 123,
      "userId": "user-123e4567-e89b-12d3-a456-426614174000",
      "userFirstName": "John",
      "userLastName": "Doe",
      "userEmail": "john.doe@example.com",
      "userContactPhone": "0912345678",
      "userContactPhoneVerified": true,
      "clickedAt": "2024-01-15T10:30:00",
      "ipAddress": "192.168.1.1"
    },
    {
      "id": 2,
      "listingId": 456,
      "userId": "user-987e6543-e21b-43d2-b654-321098765432",
      "userFirstName": "Jane",
      "userLastName": "Smith",
      "userEmail": "jane.smith@example.com",
      "userContactPhone": "0987654321",
      "userContactPhoneVerified": false,
      "clickedAt": "2024-01-15T11:45:00",
      "ipAddress": "192.168.1.2"
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` - User not authenticated

**Use Case:**
Property owner opens listing management dashboard → Sees all users who clicked on any of their listings → Can contact interested users → Manages leads effectively

---

## Related API: Update Contact Phone Number

Before tracking phone clicks, users may need to update their contact phone number.

**Endpoint:** `PATCH /v1/users/contact-phone`

**Authentication:** Required

**Request Body:**
```json
{
  "contactPhoneNumber": "0912345678"
}
```

**Request Schema:**
| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| contactPhoneNumber | String | Yes | Vietnam phone pattern | Contact phone number (09xxxxxxxx, 03xxxxxxxx, 07xxxxxxxx, 08xxxxxxxx, 05xxxxxxxx) |

**Response (200 OK):**
```json
{
  "code": "999999",
  "data": {
    "userId": "user-123e4567-e89b-12d3-a456-426614174000",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "contactPhoneNumber": "0912345678",
    "contactPhoneVerified": false
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid phone number format
- `401 Unauthorized` - User not authenticated
- `404 Not Found` - User not found

---

## Data Models

### PhoneClickResponse

```typescript
{
  id: number;                      // Phone click record ID
  listingId: number;               // Listing ID
  userId: string;                  // User ID (UUID format)
  userFirstName: string;           // User's first name
  userLastName: string;            // User's last name
  userEmail: string;               // User's email
  userContactPhone: string;        // User's contact phone (nullable)
  userContactPhoneVerified: boolean; // Phone verification status
  clickedAt: string;               // ISO 8601 datetime
  ipAddress: string;               // IP address of the click
}
```

### PhoneClickStatsResponse

```typescript
{
  listingId: number;    // Listing ID
  totalClicks: number;  // Total number of clicks
  uniqueUsers: number;  // Number of unique users
}
```

---

## Database Schema

**Table:** `phone_clicks`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Phone click record ID |
| listing_id | BIGINT | NOT NULL, FOREIGN KEY → listings(listing_id) | Reference to listing |
| user_id | VARCHAR(255) | NOT NULL, FOREIGN KEY → users(user_id) | Reference to user |
| clicked_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When the click occurred |
| ip_address | VARCHAR(45) | NULL | IP address (supports IPv4 and IPv6) |
| user_agent | TEXT | NULL | Browser user agent string |

**Indexes:**
- `idx_listing_id` - Fast lookup by listing
- `idx_user_id` - Fast lookup by user
- `idx_clicked_at` - Time-based queries
- `idx_listing_user` - Composite index for checking if user clicked on listing

**Cascade Rules:**
- ON DELETE CASCADE for both listing_id and user_id (cleanup when listing or user is deleted)

---

## Frontend Integration Guide

### 1. Listing Detail Page Flow

```javascript
// When user clicks on phone number
async function handlePhoneClick(listingId) {
  try {
    // Check if user has contact phone
    const user = await getCurrentUser();
    
    if (!user.contactPhoneNumber) {
      // Prompt user to input phone number
      const phone = await showPhoneInputModal();
      
      // Update contact phone
      await fetch('/v1/users/contact-phone', {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ contactPhoneNumber: phone })
      });
    }
    
    // Track the phone click
    const response = await fetch('/v1/phone-click-details', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ listingId })
    });
    
    if (response.ok) {
      // Show the actual phone number
      showPhoneNumber(listing.ownerPhone);
    }
  } catch (error) {
    console.error('Error tracking phone click:', error);
  }
}
```

### 2. Renter Listing Management Dashboard

```javascript
// Fetch all interested users for owner's listings
async function loadInterestedUsers() {
  try {
    const response = await fetch('/v1/phone-click-details/my-listings', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const result = await response.json();
    const clicks = result.data;
    
    // Group by listing
    const clicksByListing = clicks.reduce((acc, click) => {
      if (!acc[click.listingId]) {
        acc[click.listingId] = [];
      }
      acc[click.listingId].push(click);
      return acc;
    }, {});
    
    // Display in UI
    displayInterestedUsers(clicksByListing);
  } catch (error) {
    console.error('Error loading interested users:', error);
  }
}
```

### 3. Listing Analytics

```javascript
// Get statistics for a specific listing
async function loadListingStats(listingId) {
  try {
    const response = await fetch(`/v1/phone-click-details/listing/${listingId}/stats`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const result = await response.json();
    const stats = result.data;
    
    console.log(`Total clicks: ${stats.totalClicks}`);
    console.log(`Unique users: ${stats.uniqueUsers}`);
    
    // Display in analytics dashboard
    displayStats(stats);
  } catch (error) {
    console.error('Error loading stats:', error);
  }
}
```

---

## Testing with Swagger UI

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

1. Navigate to **"Phone Click Detail Tracking"** group
2. Click **"Authorize"** button and enter your JWT token
3. Test each endpoint with the provided examples

---

## Security Considerations

1. **Authentication Required:** All endpoints require valid JWT token
2. **User Context:** User ID is extracted from JWT token (not from request body)
3. **IP Address Tracking:** Supports proxy headers (X-Forwarded-For, X-Real-IP)
4. **Data Privacy:** Only listing owners can see who clicked on their listings
5. **Phone Validation:** Vietnam phone number format validation

---

## Performance Considerations

1. **Database Indexes:** Optimized queries with proper indexes
2. **Lazy Loading:** Entity relationships use lazy loading
3. **Caching:** User details are cached (cache eviction on phone update)
4. **Pagination:** Consider adding pagination for large result sets in future

---

## Future Enhancements

- [ ] Add pagination for listing click history
- [ ] Add date range filtering
- [ ] Add email notifications when someone clicks
- [ ] Add rate limiting to prevent abuse
- [ ] Add analytics dashboard with charts
- [ ] Add export functionality (CSV, Excel)
- [ ] Add click heatmap by time of day
- [ ] Add geographic analytics based on IP

---

## Support

For API support, contact: api-support@smartrent.com

**Documentation Version:** 1.0.0  
**Last Updated:** 2024-01-15

