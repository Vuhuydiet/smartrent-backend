# 📖 HƯỚNG DẪN SỬ DỤNG API MỚI - CHO FRONTEND DEVELOPER

## 🎯 TÓM TẮT

Đã cập nhật Swagger documentation để:
1. **API `/search`** - Gộp TẤT CẢ filter vào 1 lần gọi duy nhất
2. **API mới `/stats/provinces`** - Lấy thống kê theo tỉnh cho màn Home

---

## 📱 1. API TÌM KIẾM - `/v1/listings/search`

### 🔥 Điểm mới

- ✅ **1 API cho TẤT CẢ filter** - không cần gọi nhiều endpoint
- ✅ **10 examples thực tế** với emoji trong Swagger UI
- ✅ **Documentation tiếng Việt** chi tiết, dễ hiểu
- ✅ **Hỗ trợ đầy đủ**: giá, diện tích, vị trí, tiện ích, hướng nhà, verified, v.v.

### 💡 Cách sử dụng

**1. Mở Swagger UI**: `http://localhost:8080/swagger-ui.html`

**2. Tìm endpoint**: `POST /v1/listings/search`

**3. Xem 10 examples** - Click vào từng example để xem:
- 🏠 Tìm căn hộ Hà Nội - Đầy đủ filter
- 🏘️ Tìm nhà bán Đà Nẵng - Theo khu vực
- 🔍 Tìm theo từ khóa
- ⭐ Tìm tin VIP
- 🎯 Lọc theo tiện ích
- 🏡 Lọc chi tiết
- 🆕 Tin mới đăng
- ✅ SDT chủ nhà đã verify
- 📍 Tìm theo GPS
- 📄 Bài đăng của tôi

### 📝 Ví dụ nhanh

**Tìm căn hộ cho thuê Hà Nội, 2-3 phòng, giá 5-15 triệu:**

```javascript
// Request
POST /v1/listings/search
{
  "provinceId": 1,              // Hà Nội
  "listingType": "RENT",        // Cho thuê
  "productType": "APARTMENT",   // Căn hộ
  "minBedrooms": 2,
  "maxBedrooms": 3,
  "minPrice": 5000000,
  "maxPrice": 15000000,
  "verified": true,             // Chỉ tin đã verify
  "hasMedia": true,             // Phải có ảnh
  "page": 0,
  "size": 20
}

// Response
{
  "code": "999999",
  "data": {
    "listings": [...],          // Danh sách bài đăng
    "totalCount": 150,          // Tổng số tìm được
    "currentPage": 0,
    "pageSize": 20,
    "totalPages": 8,
    "recommendations": [...]     // Bài gợi ý
  }
}
```

### 📋 Các filter có thể dùng

| Nhóm | Field | Giá trị | Ví dụ |
|------|-------|---------|-------|
| **Vị trí** | `provinceId` | 1-63 | `1` (Hà Nội), `48` (Đà Nẵng) |
| | `districtId` | Integer | `5` (Ba Đình) |
| **Giá** | `minPrice`, `maxPrice` | VNĐ | `5000000`, `15000000` |
| **Diện tích** | `minArea`, `maxArea` | m² | `60.0`, `100.0` |
| **Phòng** | `minBedrooms`, `maxBedrooms` | Integer | `2`, `3` |
| **Hướng** | `direction` | NORTH/SOUTH/... | `"SOUTH"` |
| **Verified** | `verified` | Boolean | `true` |
| **Loại** | `listingType` | RENT/SALE/SHARE | `"RENT"` |
| **VIP** | `vipType` | NORMAL/SILVER/GOLD/DIAMOND | `"GOLD"` |
| **Tiện ích** | `amenityIds` | Array<Long> | `[1, 3, 5]` |
| | `amenityMatchMode` | ALL/ANY | `"ALL"` |
| **Keyword** | `keyword` | String | `"view biển"` |
| **Phân trang** | `page`, `size` | Integer | `0`, `20` |
| **Sắp xếp** | `sortBy` | postDate/price/area | `"price"` |
| | `sortDirection` | ASC/DESC | `"ASC"` |

**Lưu ý**: TẤT CẢ filter đều **optional** - có thể gọi với body rỗng để lấy tất cả.

---

## 🏠 2. API THỐNG KÊ THEO TỈNH - `/v1/listings/stats/provinces`

### 🎯 Mục đích

Dùng cho **màn hình Home** - Hiển thị 5 địa điểm với số lượng bài đăng.

### 🔓 Authentication

**⚠️ API CÔNG KHAI - KHÔNG CẦN:**
- ❌ **KHÔNG CẦN** access token / JWT
- ❌ **KHÔNG CẦN** userId trong request
- ✅ Gọi trực tiếp từ màn Home mà không cần đăng nhập

### 📥 Request

```javascript
POST /v1/listings/stats/provinces
{
  "provinceIds": [1, 79, 48, 31, 92],  // Top 5 tỉnh lớn
  "verifiedOnly": false                 // true = chỉ đếm bài verified
  // ⚠️ KHÔNG CẦN userId - API công khai
}
```

### 📤 Response

```javascript
{
  "code": "999999",
  "data": [
    {
      "provinceId": 1,
      "provinceName": "Hà Nội",
      "totalListings": 1250,      // Tổng số bài đăng
      "verifiedListings": 980,    // Số bài verified
      "vipListings": 345          // Số bài VIP (SILVER/GOLD/DIAMOND)
    },
    {
      "provinceId": 79,
      "provinceName": "Thành phố Hồ Chí Minh",
      "totalListings": 2340,
      "verifiedListings": 1890,
      "vipListings": 678
    },
    ...
  ]
}
```

### 💡 Cách sử dụng

**Use Case 1**: Màn Home - Hiển thị 5 tỉnh lớn

```javascript
// ✅ KHÔNG CẦN authentication token
// ✅ KHÔNG CẦN userId
// Gọi API trực tiếp
const response = await fetch('/v1/listings/stats/provinces', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
    // ⚠️ KHÔNG CẦN 'Authorization' header
  },
  body: JSON.stringify({
    provinceIds: [1, 79, 48, 31, 92],  // Hà Nội, TP.HCM, Đà Nẵng, HP, CT
    verifiedOnly: false
    // ⚠️ KHÔNG CẦN userId trong body
  })
});

const data = await response.json();

// Hiển thị
data.data.forEach(province => {
  console.log(`${province.provinceName}: ${province.totalListings} bài đăng`);
});
```

**Use Case 2**: Chỉ đếm bài verified

```javascript
{
  "provinceIds": [1, 79, 48],
  "verifiedOnly": true          // Chỉ đếm bài đã verify
}
```

### ✅ Trạng thái

- **API ĐÃ SẴN SÀNG** - Đã implement đầy đủ service layer
- Response trả về thống kê thực từ database
- **KHÔNG CẦN userId** trong request - API thống kê công khai

---

## 🚀 QUICK START

### Bước 1: Mở Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### Bước 2: Tìm section "Property Listings"

### Bước 3: Thử nghiệm API

**Tất cả API đã sẵn sàng:**
- ✅ `POST /v1/listings/search` - Tìm kiếm và lọc
- ✅ `POST /v1/listings/stats/provinces` - Thống kê theo tỉnh

---

## 📞 HỖ TRỢ

### Cần giúp đỡ?

1. **Xem Swagger UI** - Có 10 examples chi tiết
2. **Đọc file** `API_SWAGGER_UPDATE_SUMMARY.md` - Tài liệu đầy đủ
3. **Test API** - Tất cả API đã sẵn sàng để test

### Các ID tỉnh thường dùng

| Tỉnh | Old ID | New Code |
|------|--------|----------|
| Hà Nội | 1 | 01 |
| TP. Hồ Chí Minh | 79 | 79 |
| Đà Nẵng | 48 | 48 |
| Hải Phòng | 31 | 31 |
| Cần Thơ | 92 | 92 |

---

**📅 Cập nhật**: 2025-11-17
**✍️ Tạo bởi**: Claude Code Assistant
**📋 Xem thêm**: `API_SWAGGER_UPDATE_SUMMARY.md` (chi tiết kỹ thuật)
