# PHÂN TÍCH YÊU CẦU FILTER LISTING

## 🎯 YÊU CẦU CỦA BẠN
- **1 API duy nhất** cho tất cả các trường hợp
- Tất cả field đều **optional**
- Field nào có giá trị thì filter theo field đó
- Hỗ trợ:
  - Lọc theo vị trí user (user location)
  - Lọc theo category
  - Lọc theo user (my listings)
  - Lọc theo isDraft

---

## ✅ ĐÃ CÓ (Filters hiện tại)

### Location Filters
- ✅ `provinceId` - Tỉnh/Thành (cấu trúc cũ - 63 tỉnh)
- ✅ `provinceCode` - Tỉnh/Thành (cấu trúc mới - 34 tỉnh)
- ❌ `districtId` - **THIẾU** - Quận/Huyện (cấu trúc cũ)
- ❌ `wardId` - **THIẾU** - Phường/Xã (cấu trúc cũ)
- ❌ `newWardCode` - **THIẾU** - Phường/Xã (cấu trúc mới)
- ❌ `streetId` - **THIẾU** - Đường/Phố

### User Location-based Search (GEO)
- ❌ `userLatitude` - **THIẾU** - Vĩ độ vị trí user
- ❌ `userLongitude` - **THIẾU** - Kinh độ vị trí user
- ❌ `radiusKm` - **THIẾU** - Bán kính tìm kiếm (km)
- ❌ `distanceSort` - **THIẾU** - Sắp xếp theo khoảng cách

### Category & Type Filters
- ✅ `categoryId` - Loại BĐS (cho thuê, bán, ở ghép)
- ✅ `listingType` - RENT/SALE/SHARE
- ✅ `vipType` - NORMAL/SILVER/GOLD/DIAMOND
- ✅ `productType` - ROOM/APARTMENT/HOUSE/OFFICE/STUDIO

### Property Specs Filters
- ✅ `minPrice`, `maxPrice` - Khoảng giá
- ✅ `minArea`, `maxArea` - Diện tích
- ✅ `bedrooms` - Số phòng ngủ
- ✅ `bathrooms` - Số phòng tắm
- ❌ `minBedrooms`, `maxBedrooms` - **THIẾU** - Khoảng số phòng ngủ
- ❌ `minBathrooms`, `maxBathrooms` - **THIẾU** - Khoảng số phòng tắm
- ❌ `minRoomCapacity`, `maxRoomCapacity` - **THIẾU** - Sức chứa
- ❌ `furnishing` - **THIẾU** - Nội thất (FULLY/SEMI/UNFURNISHED)
- ❌ `direction` - **THIẾU** - Hướng nhà
- ❌ `propertyType` - **THIẾU** - Loại hình (APARTMENT/HOUSE/ROOM...)
- ❌ `minPricePerSqm`, `maxPricePerSqm` - **THIẾU** - Giá/m²

### Status & Verification Filters
- ✅ `verified` - Đã xác minh
- ✅ `excludeExpired` - Loại bỏ hết hạn
- ❌ `expired` - **THIẾU** - Lọc listing hết hạn (cho my-listings)
- ❌ `isVerify` - **THIẾU** - Đang chờ xác minh
- ❌ `isDraft` - **THIẾU** - Nháp (chưa hoàn thành)
- ❌ `ownerPhoneVerified` - **THIẾU** - Chủ đã xác thực SĐT

### User & Ownership Filters
- ✅ `userId` (trong my-listings) - Listing của user
- ❌ `userId` trong search API - **THIẾU** - Để unify 2 API
- ❌ `excludeMyListings` - **THIẾU** - Loại bỏ listing của chính mình

### Content & Media Filters
- ❌ `keyword` - **THIẾU** - Tìm kiếm theo tiêu đề/mô tả
- ❌ `hasMedia` - **THIẾU** - Chỉ hiện listing có ảnh/video
- ❌ `minMediaCount` - **THIẾU** - Tối thiểu số ảnh
- ❌ `amenityIds` - **THIẾU** - Lọc theo tiện ích (điều hòa, máy giặt...)

### Time-based Filters
- ❌ `postedWithinDays` - **THIẾU** - Mới đăng trong X ngày
- ❌ `updatedWithinDays` - **THIẾU** - Cập nhật trong X ngày
- ❌ `fromDate`, `toDate` - **THIẾU** - Khoảng thời gian đăng

### Pagination & Sorting
- ✅ `page`, `size` - Phân trang
- ✅ `sortBy` - Sắp xếp theo (postDate, price, area, createdAt)
- ✅ `sortDirection` - ASC/DESC
- ❌ `sortBy: distance` - **THIẾU** - Sắp xếp theo khoảng cách (cần có userLocation)
- ❌ `sortBy: pricePerSqm` - **THIẾU** - Sắp xếp theo giá/m²
- ❌ `sortBy: relevance` - **THIẾU** - Sắp xếp theo độ liên quan (search)

---

## 🚨 CÁC YÊU CẦU QUAN TRỌNG THIẾU

### 1. **LOCATION-BASED SEARCH (Ưu tiên cao)**
```json
{
  "userLatitude": 21.0285,
  "userLongitude": 105.8542,
  "radiusKm": 5.0,
  "sortBy": "distance"
}
```
- Tìm listing trong bán kính X km từ vị trí user
- Sắp xếp theo khoảng cách gần nhất
- **Use case**: User ở Hà Nội muốn tìm nhà trọ trong vòng 3km

### 2. **DISTRICT/WARD FILTERING (Ưu tiên cao)**
```json
{
  "provinceId": 1,
  "districtId": 5,
  "wardId": 123
}
```
- Lọc chi tiết đến cấp quận/huyện, phường/xã
- **Use case**: User muốn tìm nhà ở Quận Ba Đình, Phường Phúc Xá

### 3. **UNIFIED API với userId optional (Ưu tiên cao)**
```json
// Public search
{
  "categoryId": 1,
  "provinceId": 1
}

// My listings (userId từ JWT hoặc từ request)
{
  "userId": "user-123",
  "isDraft": true
}

// Search listings của 1 user khác
{
  "userId": "other-user-456",
  "verified": true
}
```

### 4. **KEYWORD SEARCH (Ưu tiên trung bình)**
```json
{
  "keyword": "căn hộ cao cấp view đẹp",
  "sortBy": "relevance"
}
```
- Full-text search trên title + description
- Sắp xếp theo độ liên quan

### 5. **AMENITIES FILTER (Ưu tiên trung bình)**
```json
{
  "amenityIds": [1, 3, 5],  // Điều hòa, Máy giặt, WiFi
  "amenityMatchMode": "ALL"  // ALL hoặc ANY
}
```

### 6. **PROPERTY SPECS RANGE (Ưu tiên thấp)**
```json
{
  "minBedrooms": 2,
  "maxBedrooms": 3,
  "furnishing": "FULLY_FURNISHED",
  "direction": "SOUTH",
  "minPricePerSqm": 100000,
  "maxPricePerSqm": 200000
}
```

---

## 💡 ĐỀ XUẤT GIẢI PHÁP

### Option 1: UNIFIED SINGLE API (Khuyến nghị) ⭐
**Endpoint**: `POST /v1/listings/search`

**Ưu điểm**:
- Đơn giản cho Frontend - chỉ cần 1 API
- Tất cả filter đều optional
- Linh hoạt cho mọi use case

**Request structure**:
```json
{
  // Location filters
  "provinceId": 1,
  "districtId": 5,
  "wardId": 123,
  "userLatitude": 21.0285,
  "userLongitude": 105.8542,
  "radiusKm": 5.0,

  // User/Ownership filters
  "userId": "user-123",  // Optional - nếu có thì search listing của user này
  "isDraft": true,       // Optional - search draft listings
  "excludeMyListings": true,  // Loại bỏ listing của chính mình

  // Category filters
  "categoryId": 1,
  "listingType": "RENT",
  "vipType": "SILVER",
  "productType": "APARTMENT",

  // Property specs
  "minPrice": 5000000,
  "maxPrice": 15000000,
  "minArea": 50.0,
  "maxArea": 100.0,
  "minBedrooms": 2,
  "maxBedrooms": 3,
  "minBathrooms": 1,
  "furnishing": "FULLY_FURNISHED",
  "direction": "SOUTH",
  "propertyType": "APARTMENT",

  // Content filters
  "keyword": "căn hộ cao cấp",
  "hasMedia": true,
  "amenityIds": [1, 3, 5],

  // Status filters
  "verified": true,
  "excludeExpired": true,
  "ownerPhoneVerified": true,

  // Time filters
  "postedWithinDays": 7,

  // Pagination & sorting
  "page": 0,
  "size": 20,
  "sortBy": "distance",  // distance, postDate, price, area, pricePerSqm
  "sortDirection": "ASC"
}
```

### Option 2: Giữ 2 API riêng
**Ưu điểm**: Separation of concerns
**Nhược điểm**: Frontend phải gọi 2 API khác nhau

---

## 🎯 CÁC USE CASE THỰC TẾ

### 1. **User tìm nhà gần vị trí hiện tại**
```json
POST /v1/listings/search
{
  "userLatitude": 21.0285,
  "userLongitude": 105.8542,
  "radiusKm": 3.0,
  "listingType": "RENT",
  "minPrice": 3000000,
  "maxPrice": 8000000,
  "verified": true,
  "hasMedia": true,
  "sortBy": "distance"
}
```

### 2. **User xem draft listings của mình**
```json
POST /v1/listings/search
{
  "userId": "current-user-123",  // Từ JWT
  "isDraft": true,
  "sortBy": "updatedAt",
  "sortDirection": "DESC"
}
```

### 3. **User tìm căn hộ cao cấp ở Hà Nội**
```json
POST /v1/listings/search
{
  "keyword": "căn hộ cao cấp",
  "provinceId": 1,
  "productType": "APARTMENT",
  "vipType": "GOLD",
  "minArea": 80.0,
  "amenityIds": [1, 3, 5, 7],  // Điều hòa, WiFi, Máy giặt, Bảo vệ
  "verified": true
}
```

### 4. **User xem listings đang chờ verify**
```json
POST /v1/listings/search
{
  "userId": "current-user-123",
  "isVerify": true,
  "verified": false,
  "sortBy": "createdAt"
}
```

---

## 📋 CHECKLIST IMPLEMENTATION

### Phase 1: Core Features (Cao)
- [ ] Unified API với userId optional
- [ ] District filter (`districtId`, `newDistrictCode`)
- [ ] Ward filter (`wardId`, `newWardCode`)
- [ ] Street filter (`streetId`)
- [ ] User location search (latitude, longitude, radius)
- [ ] Distance calculation & sorting
- [ ] Keyword search (title + description)
- [ ] isDraft filter trong unified API

### Phase 2: Enhanced Filters (Trung bình)
- [ ] Furnishing filter
- [ ] Direction filter
- [ ] PropertyType filter
- [ ] Room capacity range
- [ ] Price per sqm range
- [ ] Amenities filter với match mode (ALL/ANY)
- [ ] Has media filter
- [ ] Owner phone verified filter

### Phase 3: Advanced Features (Thấp)
- [ ] Posted within days filter
- [ ] Updated within days filter
- [ ] Date range filter
- [ ] Exclude my listings
- [ ] Min/max bedrooms/bathrooms range
- [ ] Relevance scoring cho keyword search

---

## 🚀 RECOMMENDATION

**ĐỀ XUẤT CỦA TÔI:**

1. ✅ **Implement Unified API** - Merge search và my-listings
2. ✅ **Priority 1**: Location filters (district, ward) + User location search
3. ✅ **Priority 2**: Keyword search + Amenities filter
4. ✅ **Priority 3**: Property specs filters (furnishing, direction, etc.)

Bạn muốn tôi implement theo Option 1 (Unified API) không?
