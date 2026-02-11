# Address API - Postman Testing Guide

## 📋 Table of Contents
1. [Getting Started](#getting-started)
2. [Environment Setup](#environment-setup)
3. [Legacy Structure Endpoints](#legacy-structure-endpoints)
4. [New Structure Endpoints](#new-structure-endpoints)
5. [Common Response Formats](#common-response-formats)
6. [Troubleshooting](#troubleshooting)

---

## 🚀 Getting Started

### Prerequisites
- Postman installed (Download from https://www.postman.com/downloads/)
- SmartRent backend running locally
- Server URL: `http://localhost:8080`

### Quick Start
1. Open Postman
2. Create a new collection named "SmartRent - Address API"
3. Set base URL variable: `{{baseUrl}}` = `http://localhost:8080`
4. No authentication required! ✅

---

## 🔧 Environment Setup

### Create Postman Environment

1. Click **Environments** in Postman
2. Click **Create Environment**
3. Name: `SmartRent Local`
4. Add variables:

| Variable | Initial Value | Current Value |
|----------|--------------|---------------|
| `baseUrl` | `http://localhost:8080` | `http://localhost:8080` |
| `provinceId` | `1` | `1` |
| `districtId` | `1` | `1` |
| `wardId` | `1` | `1` |
| `provinceCode` | `01` | `01` |
| `wardCode` | `00004` | `00004` |

5. Click **Save**
6. Select the environment in the top-right dropdown

---

## 📁 Legacy Structure Endpoints

### 1. Get All Provinces (63 provinces)

**Request:**
```
GET {{baseUrl}}/v1/addresses/provinces
```

**Headers:**
```
Content-Type: application/json
```

**Expected Response (200 OK):**
```json
{
  "data": [
    {
      "provinceId": 1,
      "name": "Thành phố Hà Nội",
      "code": "01",
      "type": "CITY",
      "displayName": "Hà Nội",
      "isActive": true,
      "isMerged": false,
      "isParentProvince": true
    },
    {
      "provinceId": 79,
      "name": "Thành phố Hồ Chí Minh",
      "code": "79",
      "type": "CITY",
      "displayName": "TP. Hồ Chí Minh",
      "isActive": true,
      "isMerged": false,
      "isParentProvince": true
    }
    // ... more provinces
  ],
  "message": "Successfully retrieved 63 provinces"
}
```

**Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has data array", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data).to.be.an('array');
});

pm.test("Province has required fields", function () {
    var jsonData = pm.response.json();
    var firstProvince = jsonData.data[0];
    pm.expect(firstProvince).to.have.property('provinceId');
    pm.expect(firstProvince).to.have.property('name');
    pm.expect(firstProvince).to.have.property('code');
});
```

---

### 2. Get Province by ID

**Request:**
```
GET {{baseUrl}}/v1/addresses/provinces/{{provinceId}}
```

**Example:**
```
GET {{baseUrl}}/v1/addresses/provinces/1
```

**Expected Response (200 OK):**
```json
{
  "data": {
    "provinceId": 1,
    "name": "Thành phố Hà Nội",
    "code": "01",
    "type": "CITY",
    "displayName": "Hà Nội",
    "isActive": true,
    "isMerged": false,
    "isParentProvince": true
  },
  "message": "Successfully retrieved province"
}
```

---

### 3. Search Provinces

**Request:**
```
GET {{baseUrl}}/v1/addresses/provinces/search?q=Hà Nội
```

**Query Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `q` | string | Yes | Search keyword | `Hà Nội`, `HCM`, `Đà Nẵng` |

**Example Searches:**
```
# Search for Hanoi
GET {{baseUrl}}/v1/addresses/provinces/search?q=Hà Nội

# Search for Ho Chi Minh City
GET {{baseUrl}}/v1/addresses/provinces/search?q=HCM

# Search by code
GET {{baseUrl}}/v1/addresses/provinces/search?q=01
```

**Expected Response (200 OK):**
```json
{
  "data": [
    {
      "provinceId": 1,
      "name": "Thành phố Hà Nội",
      "code": "01",
      "type": "CITY",
      "displayName": "Hà Nội",
      "isActive": true
    }
  ],
  "message": "Found 1 provinces matching 'Hà Nội'"
}
```

---

### 4. Get Districts by Province

**Request:**
```
GET {{baseUrl}}/v1/addresses/provinces/{{provinceId}}/districts
```

**Example:**
```
GET {{baseUrl}}/v1/addresses/provinces/1/districts
```

**Expected Response (200 OK):**
```json
{
  "data": [
    {
      "districtId": 1,
      "name": "Quận Ba Đình",
      "code": "001",
      "type": "DISTRICT",
      "provinceId": 1,
      "provinceName": "Thành phố Hà Nội",
      "isActive": true,
      "fullAddressText": "Quận Ba Đình, Thành phố Hà Nội"
    },
    {
      "districtId": 2,
      "name": "Quận Hoàn Kiếm",
      "code": "002",
      "type": "DISTRICT",
      "provinceId": 1,
      "provinceName": "Thành phố Hà Nội",
      "isActive": true,
      "fullAddressText": "Quận Hoàn Kiếm, Thành phố Hà Nội"
    }
    // ... more districts
  ],
  "message": "Successfully retrieved 30 districts for province 1"
}
```

---

### 5. Get District by ID

**Request:**
```
GET {{baseUrl}}/v1/addresses/districts/{{districtId}}
```

**Example:**
```
GET {{baseUrl}}/v1/addresses/districts/1
```

**Expected Response (200 OK):**
```json
{
  "data": {
    "districtId": 1,
    "name": "Quận Ba Đình",
    "code": "001",
    "type": "DISTRICT",
    "provinceId": 1,
    "provinceName": "Thành phố Hà Nội",
    "isActive": true
  },
  "message": "Successfully retrieved district"
}
```

---

### 6. Search Districts

**Request:**
```
GET {{baseUrl}}/v1/addresses/districts/search?q=Ba Đình&provinceId=1
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `q` | string | Yes | Search keyword |
| `provinceId` | integer | No | Filter by province |

**Examples:**
```
# Search all districts
GET {{baseUrl}}/v1/addresses/districts/search?q=Ba Đình

# Search districts in specific province
GET {{baseUrl}}/v1/addresses/districts/search?q=Ba Đình&provinceId=1
```

---

### 7. Get Wards by District

**Request:**
```
GET {{baseUrl}}/v1/addresses/districts/{{districtId}}/wards
```

**Example:**
```
GET {{baseUrl}}/v1/addresses/districts/1/wards
```

**Expected Response (200 OK):**
```json
{
  "data": [
    {
      "wardId": 1,
      "name": "Phường Phúc Xá",
      "code": "00001",
      "type": "WARD",
      "districtId": 1,
      "districtName": "Quận Ba Đình",
      "provinceId": 1,
      "provinceName": "Thành phố Hà Nội",
      "isActive": true,
      "fullAddressText": "Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội"
    }
    // ... more wards
  ],
  "message": "Successfully retrieved 14 wards for district 1"
}
```

---

### 8. Get Ward by ID

**Request:**
```
GET {{baseUrl}}/v1/addresses/wards/{{wardId}}
```

**Example:**
```
GET {{baseUrl}}/v1/addresses/wards/1
```

**Expected Response (200 OK):**
```json
{
  "data": {
    "wardId": 1,
    "name": "Phường Phúc Xá",
    "code": "00001",
    "type": "WARD",
    "districtId": 1,
    "districtName": "Quận Ba Đình",
    "provinceId": 1,
    "provinceName": "Thành phố Hà Nội",
    "isActive": true
  },
  "message": "Successfully retrieved ward"
}
```

---

### 9. Search Wards

**Request:**
```
GET {{baseUrl}}/v1/addresses/wards/search?q=Phúc Xá&districtId=1
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `q` | string | Yes | Search keyword |
| `districtId` | integer | No | Filter by district |

**Examples:**
```
# Search all wards
GET {{baseUrl}}/v1/addresses/wards/search?q=Phúc Xá

# Search wards in specific district
GET {{baseUrl}}/v1/addresses/wards/search?q=Phúc Xá&districtId=1
```

---

## 🆕 New Structure Endpoints

### 10. Get New Provinces (37 provinces - Paginated)

**Request:**
```
GET {{baseUrl}}/v1/addresses/new-provinces?page=1&limit=20
```

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `keyword` | string | No | - | Search keyword |
| `page` | integer | No | 1 | Page number |
| `limit` | integer | No | 20 | Items per page (max 100) |

**Examples:**
```
# Get first page
GET {{baseUrl}}/v1/addresses/new-provinces?page=1&limit=20

# Search provinces
GET {{baseUrl}}/v1/addresses/new-provinces?keyword=Hà Nội&page=1&limit=10

# Get all (use large limit)
GET {{baseUrl}}/v1/addresses/new-provinces?limit=100
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "code": "01",
      "name": "Hà Nội",
      "name_en": "Hanoi",
      "full_name": "Thành phố Hà Nội",
      "full_name_en": "Hanoi City",
      "code_name": "ha_noi",
      "administrative_unit_type": "Thành phố Trung ương"
    },
    {
      "code": "79",
      "name": "Hồ Chí Minh",
      "name_en": "Ho Chi Minh",
      "full_name": "Thành phố Hồ Chí Minh",
      "full_name_en": "Ho Chi Minh City",
      "code_name": "ho_chi_minh",
      "administrative_unit_type": "Thành phố Trung ương"
    }
  ],
  "metadata": {
    "total": 37,
    "page": 1,
    "limit": 20
  }
}
```

**Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has pagination metadata", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('metadata');
    pm.expect(jsonData.metadata).to.have.property('total');
    pm.expect(jsonData.metadata).to.have.property('page');
    pm.expect(jsonData.metadata).to.have.property('limit');
});

pm.test("Province uses code as identifier", function () {
    var jsonData = pm.response.json();
    var firstProvince = jsonData.data[0];
    pm.expect(firstProvince).to.have.property('code');
    pm.expect(firstProvince.code).to.be.a('string');
});
```

---

### 11. Get Wards by New Province

**Request:**
```
GET {{baseUrl}}/v1/addresses/new-provinces/{{provinceCode}}/wards?page=1&limit=20
```

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `keyword` | string | No | - | Search keyword |
| `page` | integer | No | 1 | Page number |
| `limit` | integer | No | 20 | Items per page |

**Examples:**
```
# Get wards for Hanoi
GET {{baseUrl}}/v1/addresses/new-provinces/01/wards?page=1&limit=50

# Search wards in province
GET {{baseUrl}}/v1/addresses/new-provinces/01/wards?keyword=Ba Đình&page=1&limit=10
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "code": "00004",
      "name": "Ba Đình",
      "name_en": "Ba Dinh",
      "full_name": "Phường Ba Đình",
      "full_name_en": "Ba Dinh Ward",
      "code_name": "ba_dinh",
      "province_code": "01",
      "province_name": "Hà Nội",
      "administrative_unit_type": "Phường"
    },
    {
      "code": "09877",
      "name": "An Khánh",
      "name_en": "An Khanh",
      "full_name": "Xã An Khánh",
      "full_name_en": "An Khanh Commune",
      "code_name": "an_khanh",
      "province_code": "01",
      "province_name": "Hà Nội",
      "administrative_unit_type": "Xã"
    }
  ],
  "metadata": {
    "total": 126,
    "page": 1,
    "limit": 20
  }
}
```

---

### 12. Get Full New Address

**Request:**
```
GET {{baseUrl}}/v1/addresses/new-full-address?provinceCode={{provinceCode}}&wardCode={{wardCode}}
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `provinceCode` | string | Yes | Province code (e.g., "01") |
| `wardCode` | string | No | Ward code (e.g., "00004") |

**Examples:**
```
# Get province and ward
GET {{baseUrl}}/v1/addresses/new-full-address?provinceCode=01&wardCode=00004

# Get province only
GET {{baseUrl}}/v1/addresses/new-full-address?provinceCode=01
```

**Expected Response (200 OK):**
```json
{
  "data": {
    "province": {
      "code": "01",
      "name": "Hà Nội",
      "name_en": "Hanoi",
      "full_name": "Thành phố Hà Nội",
      "full_name_en": "Hanoi City",
      "code_name": "ha_noi",
      "administrative_unit_type": "Thành phố Trung ương"
    },
    "ward": {
      "code": "00004",
      "name": "Ba Đình",
      "name_en": "Ba Dinh",
      "full_name": "Phường Ba Đình",
      "full_name_en": "Ba Dinh Ward",
      "code_name": "ba_dinh",
      "province_code": "01",
      "province_name": "Hà Nội",
      "administrative_unit_type": "Phường"
    }
  },
  "message": "Successfully retrieved full address"
}
```

---

### 13. Search New Address

**Request:**
```
GET {{baseUrl}}/v1/addresses/search-new-address?keyword=Hà Nội&page=1&limit=20
```

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `keyword` | string | Yes | - | Search keyword |
| `page` | integer | No | 1 | Page number |
| `limit` | integer | No | 20 | Items per page |

**Examples:**
```
# Search for "Ba Đình"
GET {{baseUrl}}/v1/addresses/search-new-address?keyword=Ba Đình&page=1&limit=10

# Search for "Hà Nội"
GET {{baseUrl}}/v1/addresses/search-new-address?keyword=Hà Nội&page=1&limit=20
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "code": "01",
      "name": "Hà Nội",
      "type": "Thành phố",
      "province_code": "01",
      "province_name": "Hà Nội",
      "full_address": "Hà Nội"
    }
  ],
  "metadata": {
    "total": 1,
    "page": 1,
    "limit": 20
  }
}
```

---

### 14. Health Check

**Request:**
```
GET {{baseUrl}}/v1/addresses/health
```

**Expected Response (200 OK):**
```json
{
  "data": "OK",
  "message": "Address API is healthy and operational (Unified: Legacy 3-tier + New 2-tier structure)"
}
```

---

## 📊 Common Response Formats

### Success Response (Legacy Endpoints)
```json
{
  "data": { ... },           // or array of objects
  "message": "Success message",
  "success": true           // implicit
}
```

### Success Response (New Endpoints - Paginated)
```json
{
  "success": true,
  "message": "Success",
  "data": [ ... ],
  "metadata": {
    "total": 100,
    "page": 1,
    "limit": 20
  }
}
```

### Error Response (404 Not Found)
```json
{
  "code": "4009",
  "message": "Resource not found: Province with code: 99",
  "data": null,
  "success": false
}
```

### Error Response (400 Bad Request)
```json
{
  "code": "2001",
  "message": "This field should not be null or empty",
  "data": null,
  "success": false
}
```

---

## 🔍 Troubleshooting

### Issue 1: 401 Unauthorized
**Symptom:**
```json
{
  "code": "5001",
  "message": "Unauthenticated"
}
```

**Solution:**
- The endpoint is public, no auth needed
- Check URL: Must be `/v1/addresses/...` (not `/api/v1/addresses/...`)
- Restart the application
- Clear Postman cache (Settings → Clear cache)

---

### Issue 2: 404 Not Found (Endpoint)
**Symptom:**
```json
{
  "timestamp": "2025-01-26T10:00:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "path": "/v1/address/provinces"
}
```

**Solution:**
- Check URL: `/v1/addresses/` (with 's')
- Not: `/v1/address/` (without 's')

---

### Issue 3: 404 Not Found (Resource)
**Symptom:**
```json
{
  "code": "4009",
  "message": "Resource not found: Province not found with id: 999"
}
```

**Solution:**
- The resource doesn't exist in the database
- Use valid IDs/codes
- Check available data first

---

### Issue 4: Connection Refused
**Symptom:**
```
Error: connect ECONNREFUSED 127.0.0.1:8080
```

**Solution:**
- Make sure the backend is running
- Start with: `./gradlew bootRun --args='--spring.profiles.active=local'`
- Check port 8080 is not occupied

---

### Issue 5: Empty Data Array
**Symptom:**
```json
{
  "data": [],
  "message": "Successfully retrieved 0 provinces"
}
```

**Solution:**
- Database is empty
- Run migrations: `./gradlew flywayMigrate`
- Check database connection

---

## 📦 Postman Collection JSON

You can import this collection directly into Postman:

```json
{
  "info": {
    "name": "SmartRent - Address API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Legacy Structure",
      "item": [
        {
          "name": "Get All Provinces",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{baseUrl}}/v1/addresses/provinces",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "addresses", "provinces"]
            }
          }
        },
        {
          "name": "Search Provinces",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{baseUrl}}/v1/addresses/provinces/search?q=Hà Nội",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "addresses", "provinces", "search"],
              "query": [
                {
                  "key": "q",
                  "value": "Hà Nội"
                }
              ]
            }
          }
        }
      ]
    },
    {
      "name": "New Structure",
      "item": [
        {
          "name": "Get New Provinces",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{baseUrl}}/v1/addresses/new-provinces?page=1&limit=20",
              "host": ["{{baseUrl}}"],
              "path": ["v1", "addresses", "new-provinces"],
              "query": [
                {
                  "key": "page",
                  "value": "1"
                },
                {
                  "key": "limit",
                  "value": "20"
                }
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{baseUrl}}/v1/addresses/health",
          "host": ["{{baseUrl}}"],
          "path": ["v1", "addresses", "health"]
        }
      }
    }
  ]
}
```

---

## 🎯 Testing Workflow

### Complete Test Flow (Legacy Structure)
```
1. GET /provinces → Get all provinces
   ↓ (pick a province ID, e.g., 1)
2. GET /provinces/1/districts → Get districts
   ↓ (pick a district ID, e.g., 1)
3. GET /districts/1/wards → Get wards
   ↓ (pick a ward ID, e.g., 1)
4. GET /wards/1 → Get ward details
```

### Complete Test Flow (New Structure)
```
1. GET /new-provinces?page=1&limit=20 → Get all provinces
   ↓ (pick a province code, e.g., "01")
2. GET /new-provinces/01/wards?page=1&limit=50 → Get wards
   ↓ (pick a ward code, e.g., "00004")
3. GET /new-full-address?provinceCode=01&wardCode=00004 → Get full address
```

---

## 📝 Tips

1. **Use Variables**: Set up Postman environment variables for reusable values
2. **Save Responses**: Use Postman's "Save Response" feature for reference
3. **Test Scripts**: Add test scripts to validate responses automatically
4. **Collections**: Organize requests into folders by feature
5. **Export**: Export your collection to share with team members

---

**Happy Testing! 🚀**

For more information, see:
- API Documentation: http://localhost:8080/swagger-ui.html
- Security Guide: `docs/SECURITY_CHECK_REPORT.md`
- DTO Reference: `docs/DTO_REFERENCE.md`