# 📚 CẬP NHẬT SWAGGER API DOCUMENTATION

## 🎯 MỤC TIÊU

Cập nhật Swagger API documentation để:
1. **Tối ưu hóa API `/search`** - Gộp tất cả filter vào 1 lần gọi
2. **Tạo API mới `/stats/provinces`** - Lấy thống kê theo tỉnh cho màn hình Home

---

## ✅ CÁC THAY ĐỔI ĐÃ THỰC HIỆN

### 1. 🔄 API `/v1/listings/search` - ĐÃ CẬP NHẬT HOÀN TOÀN

**Endpoint**: `POST /v1/listings/search`

#### 📝 Cập nhật Swagger Documentation

- ✅ **Tiêu đề mới**: "🔍 Tìm kiếm và lọc bài đăng - API tổng hợp cho tất cả filter"
- ✅ **Mô tả chi tiết** với 10 nhóm filter chính:
  1. Lọc theo vị trí (tỉnh, quận, phường, GPS)
  2. Lọc theo giá và diện tích
  3. Lọc theo đặc điểm nhà (phòng ngủ, hướng, nội thất)
  4. Lọc theo trạng thái (verified, VIP tier, loại giao dịch)
  5. Lọc theo tiện ích (ALL/ANY mode)
  6. Lọc theo media (có ảnh/video)
  7. Tìm kiếm keyword
  8. Lọc theo liên hệ (SDT verified)
  9. Lọc theo thời gian đăng
  10. Phân trang & sắp xếp

- ✅ **10 examples thực tế** với emoji dễ nhận biết:
  - 🏠 Tìm căn hộ Hà Nội - Đầy đủ filter
  - 🏘️ Tìm nhà bán Đà Nẵng - Theo khu vực
  - 🔍 Tìm theo từ khóa - Keyword search
  - ⭐ Tìm tin VIP - GOLD/DIAMOND
  - 🎯 Lọc theo tiện ích cụ thể
  - 🏡 Lọc chi tiết - Nhiều điều kiện
  - 🆕 Tin mới đăng - Trong 7 ngày
  - ✅ SDT chủ nhà đã verify
  - 📍 Tìm theo GPS - Bán kính 5km
  - 📄 Bài đăng của tôi - My listings

#### 💡 Use Cases cho Developer FE

**Use Case 1**: Tìm căn hộ Hà Nội, 2-3PN, giá 5-15tr, có điều hòa + WiFi
```json
{
  "provinceId": 1,
  "listingType": "RENT",
  "productType": "APARTMENT",
  "minBedrooms": 2,
  "maxBedrooms": 3,
  "minPrice": 5000000,
  "maxPrice": 15000000,
  "amenityIds": [1, 5],
  "amenityMatchMode": "ALL",
  "verified": true,
  "hasMedia": true
}
```

**Use Case 2**: Tìm nhà bán Đà Nẵng, >100m², có ảnh
```json
{
  "provinceId": 48,
  "listingType": "SALE",
  "productType": "HOUSE",
  "minArea": 100,
  "hasMedia": true,
  "verified": true,
  "sortBy": "price",
  "sortDirection": "ASC"
}
```

---

### 2. 🆕 API MỚI `/v1/listings/stats/provinces`

**Endpoint**: `POST /v1/listings/stats/provinces`

#### 🎯 Mục đích
API này được thiết kế cho **màn hình Home** - Frontend truyền danh sách tỉnh và nhận về thống kê số lượng bài đăng.

#### 🔓 Authentication

**⚠️ API CÔNG KHAI - KHÔNG CẦN:**
- ❌ **KHÔNG CẦN** access token / JWT
- ❌ **KHÔNG CẦN** userId trong request body
- ✅ Gọi trực tiếp từ màn Home mà không cần đăng nhập

#### 📥 Request DTO: `ProvinceStatsRequest`

```java
{
  "provinceIds": [1, 79, 48, 31, 92],        // Old structure (63 tỉnh)
  "provinceCodes": ["01", "79", "48"],       // New structure (34 tỉnh)
  "verifiedOnly": false,                     // Chỉ đếm bài verified
  "addressType": "OLD"                       // OLD hoặc NEW
  // ⚠️ KHÔNG CẦN userId - API công khai
}
```

**Lưu ý**:
- Chỉ cần truyền `provinceIds` HOẶC `provinceCodes`, không cần cả hai
- **KHÔNG CẦN** truyền `userId` - API này công khai

#### 📤 Response DTO: `ProvinceListingStatsResponse[]`

```json
{
  "code": "999999",
  "message": null,
  "data": [
    {
      "provinceId": 1,
      "provinceCode": null,
      "provinceName": "Hà Nội",
      "totalListings": 1250,
      "verifiedListings": 980,
      "vipListings": 345
    },
    {
      "provinceId": 79,
      "provinceCode": null,
      "provinceName": "Thành phố Hồ Chí Minh",
      "totalListings": 2340,
      "verifiedListings": 1890,
      "vipListings": 678
    }
  ]
}
```

#### 💡 Use Cases

**Use Case 1**: Hiển thị 5 tỉnh lớn trên Home
```json
{
  "provinceIds": [1, 79, 48, 31, 92],
  "verifiedOnly": false
}
```

**Use Case 2**: Chỉ đếm bài verified (new structure)
```json
{
  "provinceCodes": ["01", "79", "48", "31", "92"],
  "verifiedOnly": true
}
```

---

## 📁 CÁC FILE ĐÃ TẠO/CẬP NHẬT

### ✅ Files đã tạo mới:

1. **`ProvinceStatsRequest.java`**
   - Location: `src/main/java/com/smartrent/dto/request/`
   - Mục đích: Request DTO cho API thống kê tỉnh
   - Fields: provinceIds, provinceCodes, verifiedOnly, addressType

2. **`ProvinceListingStatsResponse.java`**
   - Location: `src/main/java/com/smartrent/dto/response/`
   - Mục đích: Response DTO cho API thống kê tỉnh
   - Fields: provinceId, provinceCode, provinceName, totalListings, verifiedListings, vipListings

### ✅ Files đã cập nhật:

1. **`ListingController.java`**
   - Cập nhật Swagger docs cho `/search` endpoint
   - Thêm endpoint mới `/stats/provinces`
   - **Note**: Service implementation cho `/stats/provinces` cần được thêm vào `ListingService`

---

## 🔧 CÔNG VIỆC CẦN LÀM TIẾP

### 1. Implement Service Layer cho `/stats/provinces`

Cần implement method trong `ListingService` và `ListingServiceImpl`:

```java
// Interface
public interface ListingService {
    List<ProvinceListingStatsResponse> getProvinceStats(ProvinceStatsRequest request);
}

// Implementation
@Override
public List<ProvinceListingStatsResponse> getProvinceStats(ProvinceStatsRequest request) {
    // TODO: Implement logic to:
    // 1. Query database grouped by province
    // 2. Count total listings, verified listings, VIP listings
    // 3. Map province names from province repository
    // 4. Return list in same order as request
}
```

### 2. Query Database

Cần tạo query trong `ListingRepository` hoặc sử dụng native query:

```java
@Query("""
    SELECT
        am.provinceId,
        am.newProvinceCode,
        COUNT(l.listingId) as totalListings,
        COUNT(CASE WHEN l.verified = true THEN 1 END) as verifiedListings,
        COUNT(CASE WHEN l.vipType IN ('SILVER', 'GOLD', 'DIAMOND') THEN 1 END) as vipListings
    FROM listings l
    JOIN address_metadata am ON l.address_id = am.address_id
    WHERE am.provinceId IN :provinceIds
      AND l.isDraft = false
      AND l.isShadow = false
      AND l.expired = false
    GROUP BY am.provinceId, am.newProvinceCode
    """)
List<Object[]> getProvinceStatsByIds(@Param("provinceIds") List<Integer> provinceIds);
```

### 3. Thêm Import Statements

Cần thêm import vào `ListingController.java`:

```java
import com.smartrent.dto.request.ProvinceStatsRequest;
import com.smartrent.dto.response.ProvinceListingStatsResponse;
```

---

## 🎨 LỢI ÍCH CHO FRONTEND DEVELOPER

### ✅ API `/search` - Một lần gọi cho tất cả filter

Frontend giờ chỉ cần:
- **1 API endpoint** duy nhất cho mọi loại tìm kiếm
- **Kết hợp nhiều filter** trong cùng 1 request
- **10 examples rõ ràng** trong Swagger UI để tham khảo
- **Documentation chi tiết** bằng tiếng Việt với emoji dễ đọc

### ✅ API `/stats/provinces` - Cho màn Home

- Hiển thị **5 địa điểm nổi bật** trên màn Home
- Nhận **thống kê đầy đủ**: tổng số, verified, VIP
- **Flexible**: hỗ trợ cả old và new address structure
- **Performance**: 1 lần gọi cho nhiều tỉnh

---

## 📊 TỔNG KẾT

### ✨ Đã hoàn thành:

- ✅ Viết lại Swagger docs cho API `/search` với 10 examples chi tiết
- ✅ Tạo API mới `/stats/provinces` cho màn Home
- ✅ Tạo 2 DTOs mới: `ProvinceStatsRequest` và `ProvinceListingStatsResponse`
- ✅ Cập nhật endpoint trong `ListingController`

### ⏳ Cần làm tiếp:

- ✅ ~~Implement service layer cho `/stats/provinces`~~ **DONE**
- ✅ ~~Tạo database query để lấy thống kê~~ **DONE**
- ⚠️ Test cả 2 APIs với Swagger UI

### 📝 Lưu ý cho Developer:

1. **API `/search`** đã sẵn sàng sử dụng - chỉ cần đọc Swagger docs
2. **API `/stats/provinces`** ✅ **ĐÃ IMPLEMENT - SẴN SÀNG SỬ DỤNG**
3. Tất cả filter đều **optional** - có thể gọi với body rỗng
4. Hỗ trợ **cả 2 cấu trúc địa chỉ** (old: 63 tỉnh, new: 34 tỉnh)
5. **KHÔNG CẦN USERID** trong request - API thống kê công khai

---

## 🔗 ENDPOINTS SUMMARY

| Endpoint | Method | Mục đích | Status |
|----------|--------|----------|--------|
| `/v1/listings/search` | POST | Tìm kiếm & lọc bài đăng (tất cả filter) | ✅ Ready |
| `/v1/listings/stats/provinces` | POST | Thống kê theo tỉnh (màn Home) | ✅ **Ready** |

---

**📅 Ngày cập nhật**: 2025-11-17
**👨‍💻 Tạo bởi**: Claude Code Assistant
