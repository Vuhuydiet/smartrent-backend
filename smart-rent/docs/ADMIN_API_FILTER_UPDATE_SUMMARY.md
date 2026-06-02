# Admin API Filters - Backend Update Summary

## Overview

All admin list endpoints have been updated to remove keyword-based search and replace it with **flexible key:value filtering** (using `AdminFilterRequest`). This allows filtering by any supported field instead of a generic search across multiple fields.

## General Request Format

All admin list endpoints use **GET with filter query parameters** in `key:value` format:

```
GET /v1/admins/list?page=1&size=20&filter=firstName:John&filter=role:SA,UA
GET /v1/users?page=1&size=20&filter=firstName:Jane&filter=isBroker:true
GET /v1/admin/news?page=1&size=20&filter=title:market&filter=status:PUBLISHED
```

### Query Parameters:

- **page**: Page number (1-based, default: 1)
- **size**: Items per page (default: 20)
- **filter[]**: Flexible filters in `key:value` format (multiple allowed, all optional)

---

## 1. Admin List - `GET /v1/admins/list`

### Endpoint Details

- **Method**: GET (query parameters in URL)
- **Authentication**: Required (Bearer token)

### Query Parameters

| Parameter | Type    | Description                                        | Example                        |
| --------- | ------- | -------------------------------------------------- | ------------------------------ |
| `page`    | Integer | Page number (1-based, default: 1)                  | `1`                            |
| `size`    | Integer | Items per page (default: 20)                       | `20`                           |
| `filter`  | String  | Flexible filter in format `key:value` (repeatable) | `firstName:John`, `role:SA,UA` |

### Supported Filter Keys

| Key           | Type   | Matching              | Example                       |
| ------------- | ------ | --------------------- | ----------------------------- |
| `firstName`   | String | Contains search       | `filter=firstName:John`       |
| `lastName`    | String | Contains search       | `filter=lastName:Smith`       |
| `email`       | String | Contains search       | `filter=email:@smartrent.com` |
| `phoneNumber` | String | Contains search       | `filter=phoneNumber:0912345`  |
| `adminId`     | String | Contains search       | `filter=adminId:admin-123`    |
| `role`        | String | CSV (exact match any) | `filter=role:SA,UA,CM`        |

### Example Requests

**Filter by firstName and role:**

```
GET /v1/admins/list?page=1&size=10&filter=firstName:John&filter=role:SA,UA
```

**Filter by email only:**

```
GET /v1/admins/list?page=1&size=20&filter=email:@smartrent.com
```

**All admins (no filters):**

```
GET /v1/admins/list?page=1&size=20
```

---

## 2. User List - `GET /v1/users`

### Endpoint Details

- **Method**: GET (query parameters in URL)
- **Authentication**: Required (Bearer token)

### Query Parameters

| Parameter | Type    | Description                                        | Example                           |
| --------- | ------- | -------------------------------------------------- | --------------------------------- |
| `page`    | Integer | Page number (1-based, default: 1)                  | `1`                               |
| `size`    | Integer | Items per page (default: 20)                       | `20`                              |
| `filter`  | String  | Flexible filter in format `key:value` (repeatable) | `firstName:John`, `isBroker:true` |

### Supported Filter Keys

| Key           | Type    | Matching        | Example                                           |
| ------------- | ------- | --------------- | ------------------------------------------------- |
| `firstName`   | String  | Contains search | `filter=firstName:John`                           |
| `lastName`    | String  | Contains search | `filter=lastName:Smith`                           |
| `email`       | String  | Contains search | `filter=email:@gmail.com`                         |
| `phoneNumber` | String  | Contains search | `filter=phoneNumber:0912345`                      |
| `userId`      | String  | Contains search | `filter=userId:user-123`                          |
| `isBroker`    | Boolean | Exact match     | `filter=isBroker:true` or `filter=isBroker:false` |

### Example Requests

**Filter by firstName and broker status:**

```
GET /v1/users?page=1&size=20&filter=firstName:John&filter=isBroker:true
```

**Filter by email:**

```
GET /v1/users?page=1&size=20&filter=email:@gmail.com
```

**All users (no filters):**

```
GET /v1/users?page=1&size=20
```

---

## 3. News List - `GET /v1/admin/news`

### Endpoint Details

- **Method**: GET (query parameters in URL)
- **Authentication**: Required (Bearer token)

### Query Parameters

| Parameter | Type    | Description                                        | Example                            |
| --------- | ------- | -------------------------------------------------- | ---------------------------------- |
| `page`    | Integer | Page number (1-based, default: 1)                  | `1`                                |
| `size`    | Integer | Items per page (default: 20)                       | `20`                               |
| `filter`  | String  | Flexible filter in format `key:value` (repeatable) | `title:market`, `status:PUBLISHED` |

### Supported Filter Keys

| Key        | Type   | Matching        | Example                        | Expected Values            |
| ---------- | ------ | --------------- | ------------------------------ | -------------------------- |
| `title`    | String | Contains search | `filter=title:market`          | Any string                 |
| `summary`  | String | Contains search | `filter=summary:quarterly`     | Any string                 |
| `category` | String | Exact match     | `filter=category:BLOG`         | BLOG, NEWS, GUIDE          |
| `tag`      | String | CSV matching    | `filter=tag:market,investment` | Any comma-separated tags   |
| `status`   | String | Exact match     | `filter=status:PUBLISHED`      | DRAFT, PUBLISHED, ARCHIVED |

### Example Requests

**Filter by title and status:**

```
GET /v1/admin/news?page=1&size=20&filter=title:market&filter=status:PUBLISHED
```

**Filter by category:**

```
GET /v1/admin/news?page=1&size=20&filter=category:BLOG
```

**Filter by tag:**

```
GET /v1/admin/news?page=1&size=20&filter=tag:market,investment
```

**All news (no filters):**

```
GET /v1/admin/news?page=1&size=20
```

---

## 4. Listing Admin List - `POST /v1/listings/admin/list`

### Endpoint Details

- **Method**: POST (JSON body)
- **Authentication**: Required (Bearer token + X-Admin-Id header)
- **Content-Type**: application/json

### Supported Filters (30+ fields)

| Category         | Fields                     | Expected Values                                                                    |
| ---------------- | -------------------------- | ---------------------------------------------------------------------------------- |
| **Verification** | `verified`: Boolean        | true, false                                                                        |
|                  | `isVerify`: Boolean        | true, false (pending verification)                                                 |
|                  | `moderationStatus`: String | PENDING_REVIEW, APPROVED, REJECTED, REVISION_REQUIRED, RESUBMITTED, SUSPENDED      |
| **VIP & Type**   | `vipType`: String          | NORMAL, SILVER, GOLD, DIAMOND                                                      |
|                  | `listingType`: String      | RENT, SALE, SHARE                                                                  |
|                  | `productType`: String      | ROOM, APARTMENT, HOUSE, OFFICE, STUDIO                                             |
| **Price**        | `minPrice`: Number         | Any valid price in VND                                                             |
|                  | `maxPrice`: Number         | Any valid price in VND                                                             |
|                  | `priceUnit`: String        | MONTH, DAY, YEAR                                                                   |
| **Area & Specs** | `minArea`: Float           | Any valid area in m²                                                               |
|                  | `maxArea`: Float           | Any valid area in m²                                                               |
|                  | `minBedrooms`: Integer     | 1-10                                                                               |
|                  | `maxBedrooms`: Integer     | 1-10                                                                               |
|                  | `minBathrooms`: Integer    | 1-5                                                                                |
|                  | `maxBathrooms`: Integer    | 1-5                                                                                |
| **Furnishing**   | `furnishing`: String       | FULLY_FURNISHED, SEMI_FURNISHED, UNFURNISHED                                       |
| **Direction**    | `direction`: String        | NORTH, SOUTH, EAST, WEST, NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST               |
| **Owner**        | `userId`: String           | User ID of listing owner                                                           |
| **Status**       | `listingStatus`: String    | EXPIRED, EXPIRING_SOON, DISPLAYING, IN_REVIEW, PENDING_PAYMENT, REJECTED, VERIFIED |
|                  | `expired`: Boolean         | true, false                                                                        |
| **Location**     | `provinceId`: String       | Province ID (old structure, 63 provinces)                                          |
|                  | `provinceCode`: String     | Province code (new structure, 34 provinces)                                        |
|                  | `districtId`: Integer      | District ID (old structure)                                                        |
|                  | `wardId`: String           | Ward ID (old structure)                                                            |
|                  | `newWardCode`: String      | Ward code (new structure)                                                          |
|                  | `isLegacy`: Boolean        | true (use old structure), false (use new structure)                                |

### Example Request

```json
{
  "page": 1,
  "size": 20,
  "verified": false,
  "isVerify": true,
  "vipType": "GOLD",
  "minPrice": 5000000,
  "maxPrice": 15000000,
  "priceUnit": "MONTH",
  "categoryId": 1
}
```

---

## Migration Guide for Frontend

### Before (Old Endpoints)

```javascript
// Admin list - with keyword search
GET /v1/admins/list?page=1&size=10&keyword=John&role=SA

// User list - with keyword search
GET /v1/users?page=1&size=10&keyword=John&isBroker=true

// News list - with query params
GET /v1/admin/news?page=1&size=20&keyword=market&category=BLOG&status=PUBLISHED
```

### After (New Format - Unified key:value Filter)

```javascript
// Admin list - unified filter format
GET /v1/admins/list?page=1&size=10&filter=firstName:John&filter=role:SA

// User list - unified filter format
GET /v1/users?page=1&size=10&filter=firstName:John&filter=isBroker:true

// News list - unified filter format
GET /v1/admin/news?page=1&size=20&filter=title:market&filter=status:PUBLISHED
```

---

## Key Changes

1. **Unified Filter Format**: All filtering now uses consistent `filter=key:value` format across all endpoints
2. **Multiple Filters**: Support multiple filter parameters - repeat `&filter=key:value` for each filter
3. **No More Individual Parameters**: Replaced individual parameters (keyword, role, isBroker, etc.) with unified filter format
4. **Flexible Backend**: Internal filtering uses AdminFilterRequest DTO for dynamic, flexible filtering
5. **Case-Insensitive Search**: String filters use contains matching (case-insensitive)
6. **Clean URL Structure**: Single, consistent URL parameter structure across all admin APIs

---

## Filter Behavior

### String Filters (Contains Search)

- Case-insensitive
- Matches anywhere in the field (substring match)
- Example: `filter=firstName:john` matches "John", "JOHN", "john", "jonathan"

### Boolean Filters (Exact Match)

- Must be exactly `true` or `false`
- Example: `filter=isBroker:true`

### Enum Filters (Exact Match)

- Must be exact enum value (case-sensitive)
- Example: `filter=status:PUBLISHED` (not "published")

### CSV Filters (Multiple Values)

- Separate values with commas
- Example: `filter=role:SA,UA,CM` (matches if role is any of SA, UA, or CM)

---

## Response Format

All endpoints return the same response structure:

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "page": 1,
    "size": 10,
    "totalElements": 42,
    "totalPages": 5,
    "data": [
      {
        /* item details */
      }
    ]
  }
}
```

---

## Notes for Frontend Development

1. **Filter Format**: Always use `filter=key:value` format
2. **Multiple Filters**: Repeat the `filter` parameter for each filter (e.g., `&filter=firstName:John&filter=role:SA`)
3. **URL Encoding**: Remember to URL-encode special characters (spaces become `%20`, colons are safe)
4. **Combined Filters**: Multiple filters are combined with AND logic (must match all specified filters)
5. **Optional Filters**: All filter parameters are optional - omit to skip that filter
6. **Pagination**: Always include `page` and `size` parameters
7. **No Spaces**: Avoid spaces around the colon in key:value pairs (use `firstName:John` not `firstName: John`)
8. **CSV Values**: For multi-select filters like role, use comma separation (e.g., `filter=role:SA,UA,CM`)

---

## Backend Files Modified

1. **DTOs**:
   - Created: `AdminFilterRequest.java` (new generic filter DTO)

2. **Admin Module**:
   - Modified: `AdminService.java` (updated getAllAdmins signature)
   - Modified: `AdminServiceImpl.java` (implemented dynamic filtering)
   - Modified: `AdminController.java` (updated /list endpoint)

3. **User Module**:
   - Modified: `UserService.java` (updated getUsers signature)
   - Modified: `UserServiceImpl.java` (implemented dynamic filtering)
   - Modified: `UserController.java` (updated endpoint)

4. **News Module**:
   - Modified: `NewsService.java` (updated getAllNews signature)
   - Modified: `NewsServiceImpl.java` (implemented dynamic filtering)
   - Modified: `AdminNewsController.java` (updated /admin/news endpoint)

5. **Role Module** (NEWLY ADDED):
   - Modified: `RoleService.java` (added getAllRoles(AdminFilterRequest) signature)
   - Modified: `RoleServiceImpl.java` (implemented dynamic filtering)
   - Modified: `RoleController.java` (updated /roles endpoint)

---

## 4. Role List - `GET /v1/roles` (NEW)

### Endpoint Details

- **Method**: GET (query parameters in URL)
- **Authentication**: Required (Bearer token)
- **Purpose**: Retrieve all system roles with optional filtering

### Query Parameters

| Parameter | Type    | Description                                        | Example                          |
| --------- | ------- | -------------------------------------------------- | -------------------------------- |
| `page`    | Integer | Page number (1-based, default: 1)                  | `1`                              |
| `size`    | Integer | Items per page (default: 20)                       | `20`                             |
| `filter`  | String  | Flexible filter in format `key:value` (repeatable) | `roleId:ADMIN`, `roleName:Admin` |

### Supported Filter Keys

| Key        | Type   | Matching        | Example                 |
| ---------- | ------ | --------------- | ----------------------- |
| `roleId`   | String | Contains search | `filter=roleId:ADMIN`   |
| `roleName` | String | Contains search | `filter=roleName:Admin` |

### Example Requests

**All roles (no filters):**

```
GET /v1/roles?page=1&size=20
```

**Filter by roleId:**

```
GET /v1/roles?page=1&size=20&filter=roleId:ADMIN
```

**Filter by roleName:**

```
GET /v1/roles?page=1&size=20&filter=roleName:Administrator
```

**Multiple filters:**

```
GET /v1/roles?page=1&size=20&filter=roleId:ADMIN&filter=roleName:Administrator
```

### Response Example

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "page": 1,
    "size": 20,
    "totalElements": 4,
    "totalPages": 1,
    "data": [
      {
        "roleId": "ADMIN",
        "roleName": "Administrator"
      },
      {
        "roleId": "SUPER_ADMIN",
        "roleName": "Super Administrator"
      },
      {
        "roleId": "USER",
        "roleName": "Regular User"
      },
      {
        "roleId": "MODERATOR",
        "roleName": "Content Moderator"
      }
    ]
  }
}
```

---

## Testing the New APIs

Use Postman or curl:

```bash
# Test admin list with multiple filters
curl -X GET "http://localhost:8080/v1/admins/list?page=1&size=10&filter=firstName:John&filter=role:SA" \
  -H "Authorization: Bearer <token>"

# Test user list with broker filter
curl -X GET "http://localhost:8080/v1/users?page=1&size=20&filter=firstName:Jane&filter=isBroker:true" \
  -H "Authorization: Bearer <token>"

# Test news list with multiple filters
curl -X GET "http://localhost:8080/v1/admin/news?page=1&size=20&filter=title:market&filter=status:PUBLISHED" \
  -H "Authorization: Bearer <token>"

# Test roles list with filter
curl -X GET "http://localhost:8080/v1/roles?page=1&size=20&filter=roleId:ADMIN" \
  -H "Authorization: Bearer <token>"
```

---

## Implementation Status

✅ AdminController.getAllAdmins - Updated to support `filter=key:value` format  
✅ UserController.getUsers - Updated to support `filter=key:value` format  
✅ AdminNewsController.getAllNews - Updated to support `filter=key:value` format  
✅ RoleController.getAllRoles - Updated to support `filter=key:value` format (NEW)
✅ All service implementations - Updated with AdminFilterRequest support  
✅ Generic AdminFilterRequest DTO - Created for internal dynamic filtering  
✅ URL filter format - Unified `filter=key:value` across all admin APIs

All APIs now support flexible, unified filtering via the `filter=key:value` query parameter format. The backend internally parses these filters and uses AdminFilterRequest DTO for dynamic, flexible query filtering.
