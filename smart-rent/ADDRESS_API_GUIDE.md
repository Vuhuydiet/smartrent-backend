# Address API Guide - Vietnamese Administrative Structure

## Overview

SmartRent Address API cung cấp truy cập đến cấu trúc hành chính 3 tầng truyền thống của Việt Nam (63 tỉnh/thành).

## Cấu Trúc Hành Chính

### 📍 Province Level (Tỉnh/Thành phố) - 63 đơn vị
- **5 Thành phố trực thuộc Trung ương:**
  - Hà Nội (code: 01)
  - Thành phố Hồ Chí Minh (code: 79)
  - Đà Nẵng (code: 48)
  - Hải Phòng (code: 31)
  - Cần Thơ (code: 92)
- **58 Tỉnh**

### 🏘️ District Level (Quận/Huyện/Thị xã) - ~700 đơn vị
- **Quận** (Urban district) - Khu vực thành thị
- **Huyện** (Rural district) - Khu vực nông thôn
- **Thị xã** (Town) - Khu vực đô thị hóa trung bình

### 🏠 Ward Level (Phường/Xã/Thị trấn) - ~11,000 đơn vị
- **Phường** (Ward) - Đơn vị hành chính đô thị
- **Xã** (Commune) - Đơn vị hành chính nông thôn
- **Thị trấn** (Township) - Đơn vị hành chính thị trấn nhỏ

## API Endpoints

### Province Operations

#### Get All Provinces
```http
GET /v1/addresses/provinces
```
**Response:** Danh sách 63 tỉnh/thành phố Việt Nam

**Example Response:**
```json
{
  "data": [
    {
      "id": 1,
      "name": "Thành phố Hà Nội",
      "code": "01",
      "type": "CITY",
      "level": "PROVINCE",
      "isActive": true,
      "fullAddressText": "Thành phố Hà Nội"
    }
  ],
  "message": "Successfully retrieved 63 provinces"
}
```

#### Get Province by ID
```http
GET /v1/addresses/provinces/{provinceId}
```

#### Search Provinces
```http
GET /v1/addresses/provinces/search?q={searchTerm}
```

### District Operations

#### Get Districts by Province
```http
GET /v1/addresses/provinces/{provinceId}/districts
```
**Example:** Hà Nội (ID: 1) có 30 quận/huyện

#### Get District by ID
```http
GET /v1/addresses/districts/{districtId}
```

#### Search Districts
```http
GET /v1/addresses/districts/search?q={searchTerm}&provinceId={provinceId}
```

### Ward Operations

#### Get Wards by District
```http
GET /v1/addresses/districts/{districtId}/wards
```
**Example:** Quận Ba Đình (ID: 1) có 14 phường

#### Get Ward by ID
```http
GET /v1/addresses/wards/{wardId}
```

#### Search Wards
```http
GET /v1/addresses/wards/search?q={searchTerm}&districtId={districtId}
```

## AddressUnitDTO Response Format

Tất cả endpoints trả về format thống nhất `AddressUnitDTO`:

```json
{
  "id": 1,
  "name": "Phường Phúc Xá",
  "code": "00001",
  "type": "WARD",
  "level": "WARD",
  "isActive": true,
  "provinceId": 1,
  "provinceName": "Thành phố Hà Nội",
  "districtId": 1,
  "districtName": "Quận Ba Đình",
  "fullAddressText": "Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội",
  "isMerged": null,
  "originalName": null
}
```

### Field Descriptions

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `id` | Long | Unique identifier | `1` |
| `name` | String | Administrative unit name | `"Phường Phúc Xá"` |
| `code` | String | Administrative code | `"00001"` |
| `type` | String | Unit type | `"WARD"`, `"DISTRICT"`, `"CITY"`, `"PROVINCE"` |
| `level` | String | Hierarchy level | `"PROVINCE"`, `"DISTRICT"`, `"WARD"` |
| `isActive` | Boolean | Whether unit is active | `true` |
| `provinceId` | Long | Parent province ID (for districts/wards) | `1` |
| `provinceName` | String | Parent province name | `"Thành phố Hà Nội"` |
| `districtId` | Long | Parent district ID (for wards only) | `1` |
| `districtName` | String | Parent district name | `"Quận Ba Đình"` |
| `fullAddressText` | String | Complete hierarchical address | `"Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội"` |
| `isMerged` | Boolean | Whether province is merged (province level only) | `null` |
| `originalName` | String | Original name before administrative changes | `null` |

## Use Cases

### 1. Cascading Address Selection (Form Dropdown)

```javascript
// Step 1: Load provinces
GET /v1/addresses/provinces
// User selects: Hà Nội (ID: 1)

// Step 2: Load districts for selected province
GET /v1/addresses/provinces/1/districts
// User selects: Quận Ba Đình (ID: 1)

// Step 3: Load wards for selected district
GET /v1/addresses/districts/1/wards
// User selects: Phường Phúc Xá (ID: 1)
```

### 2. Search Functionality

```javascript
// Search provinces
GET /v1/addresses/provinces/search?q=Hà Nội
// Returns: Thành phố Hà Nội

// Search districts in a province
GET /v1/addresses/districts/search?q=Ba&provinceId=1
// Returns: Quận Ba Đình

// Search wards in a district
GET /v1/addresses/wards/search?q=Phúc&districtId=1
// Returns: Phường Phúc Xá
```

### 3. Creating Listing with Address

Khi tạo listing, sử dụng `AddressCreationRequest`:

```json
{
  "title": "Cho thuê căn hộ cao cấp",
  "price": 10000000,
  "address": {
    "streetNumber": "123",
    "streetId": 1,
    "wardId": 1,
    "districtId": 1,
    "provinceId": 1,
    "latitude": 21.0285,
    "longitude": 105.8542
  }
}
```

**Benefits:**
- ✅ Address tự động được tạo trong cùng transaction với listing
- ✅ Không có orphaned data
- ✅ Full rollback nếu có lỗi
- ✅ Auto-generate `fullAddress` nếu không cung cấp

## Testing

### Using Postman
Import file `AddressAPI_Legacy.postman_collection.json` vào Postman để test.

**Environment Variables:**
```json
{
  "base_url": "http://localhost:8080"
}
```

### Using cURL

**Get all provinces:**
```bash
curl -X GET http://localhost:8080/v1/addresses/provinces
```

**Get districts for Hà Nội:**
```bash
curl -X GET http://localhost:8080/v1/addresses/provinces/1/districts
```

**Search wards:**
```bash
curl -X GET "http://localhost:8080/v1/addresses/wards/search?q=Phúc Xá&districtId=1"
```

## Swagger UI

Truy cập Swagger UI để xem full documentation:
```
http://localhost:8080/swagger-ui/index.html
```

Chọn group: **"Address API (Legacy Structure)"**

## Error Handling

### Not Found (404)
```json
{
  "code": "4001",
  "message": "Province not found with id: 999"
}
```

### Validation Error (400)
```json
{
  "code": "2001",
  "message": "Invalid search query"
}
```

## Performance Considerations

- ✅ **Caching**: Province, District, Ward data được cache
- ✅ **Indexing**: Tất cả foreign keys được index
- ✅ **Lazy Loading**: Address relationships loaded khi cần
- ✅ **Pagination**: Không cần pagination vì số lượng provinces/districts nhỏ

## Migration from Old Structure

Nếu bạn đang sử dụng `addressId` thay vì `address` entity:

**Old (Deprecated):**
```java
listing.setAddressId(123L);
```

**New (Recommended):**
```java
Address address = addressService.createAddress(addressRequest);
listing.setAddress(address);
```

## Support

- **Swagger Documentation**: `/swagger-ui/index.html`
- **Postman Collection**: `AddressAPI_Legacy.postman_collection.json`
- **Health Check**: `GET /v1/addresses/health`

## Notes

- API này sử dụng cấu trúc hành chính **CŨ** (63 tỉnh)
- Không hỗ trợ cấu trúc mới 2025 (34 tỉnh)
- Tất cả responses sử dụng `AddressUnitDTO` thống nhất
- ID-based operations (không dùng code-based)
