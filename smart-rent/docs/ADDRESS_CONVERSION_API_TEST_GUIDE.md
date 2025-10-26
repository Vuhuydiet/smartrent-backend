# Address Conversion API - Test Guide for Frontend Integration

## Overview

This guide provides sample test data and API examples for integrating the Address Conversion APIs. The data is minimal but sufficient for FE development and testing.

---

## 🚀 Quick Setup

### 1. Run the Migration

The sample test data will be automatically loaded when you run the application:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Or manually run migration:
```bash
./gradlew flywayMigrate
```

### 2. Verify Data Loaded

Check the migration created these test records:
- **2 Legacy Provinces** (Hà Nội, HCM)
- **3 Legacy Districts** (Ba Đình, Hoàn Kiếm, Quận 1)
- **4 Legacy Wards** (Phúc Xá, Trúc Bạch, Cửa Nam, Bến Nghé)
- **2 New Provinces** (code: 01, 79)
- **4 New Wards** (code: 00001, 00004, 00007, 26740)
- **Mappings** between legacy ↔ new structures

---

## 📊 Test Data Structure

### Legacy Structure (3-tier: Province → District → Ward)

```
Hà Nội (ID: 1, Code: 01)
├── Ba Đình (ID: 1)
│   ├── Phúc Xá (ID: 1)
│   └── Trúc Bạch (ID: 4)
└── Hoàn Kiếm (ID: 2)
    └── Cửa Nam (ID: 7)

HCM (ID: 79, Code: 79)
└── Quận 1 (ID: 760)
    └── Bến Nghé (ID: 26740)
```

### New Structure (2-tier: Province → Ward)

```
Hà Nội (Code: 01)
├── Phúc Xá (Code: 00001)
├── Trúc Bạch (Code: 00004)
└── Cửa Nam (Code: 00007)

HCM (Code: 79)
└── Bến Nghé (Code: 26740)
```

---

## 🔧 API Endpoints

### Base URL
```
http://localhost:8080
```

### Authentication
❌ **No authentication required** - All address endpoints are public

---

## 📝 Test Cases

### ✅ Test Case 1: Convert Legacy → New (Hà Nội - Phúc Xá)

**Request:**
```http
GET /v1/addresses/convert/legacy-to-new?provinceId=1&districtId=1&wardId=1
```

**Expected Response:**
```json
{
  "data": {
    "legacyAddress": {
      "province": {
        "id": 1,
        "name": "Thành phố Hà Nội",
        "nameEn": "Ha Noi",
        "code": "01"
      },
      "district": {
        "id": 1,
        "name": "Ba Đình",
        "nameEn": "Ba Dinh",
        "prefix": "Quận",
        "provinceId": 1,
        "provinceName": "Thành phố Hà Nội"
      },
      "ward": {
        "id": 1,
        "name": "Phúc Xá",
        "nameEn": "Phuc Xa",
        "prefix": "Phường",
        "provinceId": 1,
        "provinceName": "Thành phố Hà Nội",
        "districtId": 1,
        "districtName": "Ba Đình"
      }
    },
    "newAddress": {
      "province": {
        "code": "01",
        "name": "Hà Nội",
        "nameEn": "Ha Noi",
        "fullName": "Thành phố Hà Nội",
        "fullNameEn": "Ha Noi City",
        "codeName": "ha_noi",
        "administrativeUnitType": "Thành phố Trung ương"
      },
      "ward": {
        "code": "00001",
        "name": "Phúc Xá",
        "nameEn": "Phuc Xa",
        "fullName": "Phường Phúc Xá",
        "fullNameEn": "Phuc Xa Ward",
        "codeName": "phuc_xa",
        "provinceCode": "01",
        "provinceName": "Hà Nội",
        "administrativeUnitType": "Phường"
      }
    },
    "conversionNote": "Converted from legacy structure. Merge type: UNCHANGED"
  },
  "message": "Successfully converted address from legacy to new structure"
}
```

---

### ✅ Test Case 2: Convert Legacy → New (Hà Nội - Trúc Bạch)

**Request:**
```http
GET /v1/addresses/convert/legacy-to-new?provinceId=1&districtId=1&wardId=4
```

**Expected Response:**
```json
{
  "data": {
    "legacyAddress": { ... },
    "newAddress": {
      "province": { "code": "01", "name": "Hà Nội", ... },
      "ward": { "code": "00004", "name": "Trúc Bạch", ... }
    },
    "conversionNote": "Converted from legacy structure. Merge type: UNCHANGED"
  },
  "message": "Successfully converted address from legacy to new structure"
}
```

---

### ✅ Test Case 3: Convert Legacy → New (HCM - Bến Nghé)

**Request:**
```http
GET /v1/addresses/convert/legacy-to-new?provinceId=79&districtId=760&wardId=26740
```

**Expected Response:**
```json
{
  "data": {
    "legacyAddress": { ... },
    "newAddress": {
      "province": { "code": "79", "name": "Hồ Chí Minh", ... },
      "ward": { "code": "26740", "name": "Bến Nghé", ... }
    },
    "conversionNote": "Converted from legacy structure. Merge type: UNCHANGED"
  },
  "message": "Successfully converted address from legacy to new structure"
}
```

---

### ✅ Test Case 4: Convert New → Legacy (Hà Nội - Phúc Xá)

**Request:**
```http
GET /v1/addresses/convert/new-to-legacy?provinceCode=01&wardCode=00001
```

**Expected Response:**
```json
{
  "data": {
    "legacyAddress": {
      "province": { "id": 1, "name": "Thành phố Hà Nội", ... },
      "district": { "id": 1, "name": "Ba Đình", ... },
      "ward": { "id": 1, "name": "Phúc Xá", ... }
    },
    "newAddress": {
      "province": { "code": "01", "name": "Hà Nội", ... },
      "ward": { "code": "00001", "name": "Phúc Xá", ... }
    },
    "conversionNote": "Converted to legacy structure. Merge type: UNCHANGED"
  },
  "message": "Successfully converted address from new to legacy structure"
}
```

---

### ✅ Test Case 5: Convert New → Legacy (Hà Nội - Trúc Bạch)

**Request:**
```http
GET /v1/addresses/convert/new-to-legacy?provinceCode=01&wardCode=00004
```

**Expected Response:**
```json
{
  "data": {
    "legacyAddress": {
      "province": { "id": 1, ... },
      "district": { "id": 1, "name": "Ba Đình", ... },
      "ward": { "id": 4, "name": "Trúc Bạch", ... }
    },
    "newAddress": {
      "province": { "code": "01", ... },
      "ward": { "code": "00004", "name": "Trúc Bạch", ... }
    },
    "conversionNote": "Converted to legacy structure. Merge type: UNCHANGED"
  },
  "message": "Successfully converted address from new to legacy structure"
}
```

---

### ✅ Test Case 6: Convert New → Legacy (HCM - Bến Nghé)

**Request:**
```http
GET /v1/addresses/convert/new-to-legacy?provinceCode=79&wardCode=26740
```

**Expected Response:**
```json
{
  "data": {
    "legacyAddress": {
      "province": { "id": 79, "name": "Thành phố Hồ Chí Minh", ... },
      "district": { "id": 760, "name": "Quận 1", ... },
      "ward": { "id": 26740, "name": "Bến Nghé", ... }
    },
    "newAddress": {
      "province": { "code": "79", "name": "Hồ Chí Minh", ... },
      "ward": { "code": "26740", "name": "Bến Nghé", ... }
    },
    "conversionNote": "Converted to legacy structure. Merge type: UNCHANGED"
  },
  "message": "Successfully converted address from new to legacy structure"
}
```

---

## ❌ Error Cases

### Test Case 7: Invalid Legacy IDs

**Request:**
```http
GET /v1/addresses/convert/legacy-to-new?provinceId=999&districtId=999&wardId=999
```

**Expected Response:**
```json
{
  "code": "4001",
  "message": "Resource not found: Province not found with id: 999"
}
```
**Status:** 404

---

### Test Case 8: Invalid New Codes

**Request:**
```http
GET /v1/addresses/convert/new-to-legacy?provinceCode=99&wardCode=99999
```

**Expected Response:**
```json
{
  "code": "4001",
  "message": "Resource not found: Province not found with code: 99"
}
```
**Status:** 404

---

### Test Case 9: No Mapping Found

**Request:**
```http
GET /v1/addresses/convert/new-to-legacy?provinceCode=01&wardCode=99999
```

**Expected Response:**
```json
{
  "code": "4001",
  "message": "Resource not found: Ward not found with code: 99999"
}
```
**Status:** 404

---

## 🧪 Other Address APIs (For FE Development)

### Get All Legacy Provinces
```http
GET /v1/addresses/provinces
```
Returns: List of 63 provinces (in test: only 2)

### Get Districts by Province
```http
GET /v1/addresses/provinces/1/districts
```
Returns: List of districts in Hà Nội (in test: 2 districts)

### Get Wards by District
```http
GET /v1/addresses/districts/1/wards
```
Returns: List of wards in Ba Đình (in test: 2 wards)

### Get All New Provinces
```http
GET /v1/addresses/new-provinces
```
Returns: List of 34 provinces (in test: only 2)

### Get New Wards by Province
```http
GET /v1/addresses/new-provinces/01/wards
```
Returns: List of wards in Hà Nội (in test: 3 wards)

### Search New Address
```http
GET /v1/addresses/search-new-address?keyword=Phúc Xá
```
Returns: Search results matching "Phúc Xá"

---

## 📋 Quick Reference Table

| Test Scenario | Method | Endpoint | Query Params |
|--------------|--------|----------|--------------|
| Legacy → New (Phúc Xá) | GET | `/convert/legacy-to-new` | `provinceId=1&districtId=1&wardId=1` |
| Legacy → New (Trúc Bạch) | GET | `/convert/legacy-to-new` | `provinceId=1&districtId=1&wardId=4` |
| Legacy → New (Bến Nghé) | GET | `/convert/legacy-to-new` | `provinceId=79&districtId=760&wardId=26740` |
| New → Legacy (Phúc Xá) | GET | `/convert/new-to-legacy` | `provinceCode=01&wardCode=00001` |
| New → Legacy (Trúc Bạch) | GET | `/convert/new-to-legacy` | `provinceCode=01&wardCode=00004` |
| New → Legacy (Bến Nghé) | GET | `/convert/new-to-legacy` | `provinceCode=79&wardCode=26740` |

---

## 🔍 Troubleshooting

### Issue 1: 401 Unauthorized
**Solution:** Conversion endpoints should be public. Check if you restarted the app after config changes.

### Issue 2: 404 Resource Not Found
**Solution:** Run the migration script `V25__Sample_test_data_for_conversion.sql` to populate test data.

### Issue 3: No mapping found
**Solution:** The mapping data is limited to test cases above. Use only the provided IDs/codes.

### Issue 4: Database connection error
**Solution:** Ensure MySQL is running and `application-local.yaml` has correct DB credentials.

---

## 📞 Contact

For questions or issues, contact the backend team or check:
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

---

## ⚠️ Important Notes

1. **Test Data Only**: This is minimal sample data for FE integration testing
2. **Delete After Migration**: Remove `V25__Sample_test_data_for_conversion.sql` after real data migration
3. **Limited Coverage**: Only 4 wards are available for testing
4. **No Authentication**: All address endpoints are publicly accessible
5. **Merge Types**: All test data uses `UNCHANGED` merge type

---

## 🎯 Integration Checklist for FE

- [ ] Can call conversion API without authentication
- [ ] Legacy → New conversion displays both structures
- [ ] New → Legacy conversion displays both structures
- [ ] Error handling works for invalid IDs/codes
- [ ] Conversion notes are displayed to users
- [ ] UI shows merge type information
- [ ] All 6 test cases work as expected
- [ ] Can integrate with existing address selection flows
