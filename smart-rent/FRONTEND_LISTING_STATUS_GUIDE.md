# Frontend Integration Guide - Listing Status Filter

## Tổng quan

API `POST /v1/listings/search` hỗ trợ filter `listingStatus` để owner quản lý listings theo 7 trạng thái khác nhau.

## 📋 Các Trạng Thái (ListingStatus)

| Status | Code | Mô tả | Điều kiện |
|--------|------|-------|-----------|
| `EXPIRED` | 1 | Hết hạn | `expired = true` HOẶC `expiryDate < now` |
| `EXPIRING_SOON` | 2 | Sắp hết hạn | `verified = true` VÀ còn ≤ 7 ngày |
| `DISPLAYING` | 3 | Đang hiển thị | `verified = true` VÀ chưa hết hạn |
| `IN_REVIEW` | 4 | Đang chờ duyệt | `isVerify = true` VÀ `verified = false` |
| `PENDING_PAYMENT` | 5 | Chờ thanh toán | Có `transactionId` nhưng chưa verify |
| `REJECTED` | 6 | Bị từ chối | `verified = false`, `isVerify = false`, đã post |
| `VERIFIED` | 7 | Đã xác thực | `verified = true` |

---

## 🎯 Use Cases cho Owner

### 1. Tất cả listings của owner (Không filter status)

**Endpoint:** `POST /v1/listings/search`

**Request:**
```json
{
  "userId": "user-abc-123",
  "page": 0,
  "size": 20,
  "sortBy": "updatedAt",
  "sortDirection": "DESC"
}
```

**Response:**
```json
{
  "code": "999999",
  "data": {
    "listings": [
      {
        "listingId": 1,
        "title": "Căn hộ 2PN cao cấp",
        "verified": true,
        "expired": false,
        "listingStatus": "DISPLAYING",
        "expiryDate": "2025-12-30T00:00:00",
        "postDate": "2025-11-01T10:00:00",
        ...
      },
      {
        "listingId": 2,
        "title": "Phòng trọ sinh viên",
        "verified": false,
        "isVerify": true,
        "listingStatus": "IN_REVIEW",
        ...
      }
    ],
    "totalCount": 25,
    "currentPage": 0,
    "pageSize": 20,
    "totalPages": 2
  }
}
```

---

### 2. Listings đang hiển thị (DISPLAYING)

**Mục đích:** Xem các bài đang active và hiển thị cho người dùng

**Request:**
```json
{
  "userId": "user-abc-123",
  "listingStatus": "DISPLAYING",
  "page": 0,
  "size": 20,
  "sortBy": "postDate",
  "sortDirection": "DESC"
}
```

**Frontend Display:**
```typescript
// Hiển thị tab "Đang hiển thị" với badge màu xanh
<Tab label="Đang hiển thị" count={15} color="success" />
```

---

### 3. Listings sắp hết hạn (EXPIRING_SOON)

**Mục đích:** Cảnh báo owner về các bài sắp hết hạn (còn ≤ 7 ngày)

**Request:**
```json
{
  "userId": "user-abc-123",
  "listingStatus": "EXPIRING_SOON",
  "sortBy": "expiryDate",
  "sortDirection": "ASC"
}
```

**Frontend Display:**
```typescript
// Hiển thị với warning badge và countdown
<Alert severity="warning">
  Bạn có {count} bài đăng sắp hết hạn!
</Alert>

// Trong danh sách
<ListItem>
  <Typography>{listing.title}</Typography>
  <Chip
    label={`Còn ${daysLeft} ngày`}
    color="warning"
  />
</ListItem>
```

**Tính số ngày còn lại:**
```typescript
const daysLeft = Math.ceil(
  (new Date(listing.expiryDate).getTime() - Date.now())
  / (1000 * 60 * 60 * 24)
);
```

---

### 4. Listings đã hết hạn (EXPIRED)

**Mục đích:** Xem và gia hạn các bài đã hết hạn

**Request:**
```json
{
  "userId": "user-abc-123",
  "listingStatus": "EXPIRED",
  "sortBy": "expiryDate",
  "sortDirection": "DESC"
}
```

**Frontend Display:**
```typescript
<Tab label="Đã hết hạn" count={5} color="error" />

// Trong danh sách
<ListItem>
  <Typography color="text.secondary">{listing.title}</Typography>
  <Chip label="Hết hạn" color="error" size="small" />
  <Button variant="outlined" onClick={handleRenew}>
    Gia hạn
  </Button>
</ListItem>
```

---

### 5. Listings đang chờ duyệt (IN_REVIEW)

**Mục đích:** Theo dõi tiến trình duyệt bài

**Request:**
```json
{
  "userId": "user-abc-123",
  "listingStatus": "IN_REVIEW",
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}
```

**Frontend Display:**
```typescript
<Tab label="Đang chờ duyệt" count={3} color="info" />

// Trong danh sách
<ListItem>
  <Typography>{listing.title}</Typography>
  <Chip
    icon={<PendingIcon />}
    label="Đang duyệt"
    color="info"
  />
  <Typography variant="caption" color="text.secondary">
    Gửi lúc: {formatDate(listing.createdAt)}
  </Typography>
</ListItem>
```

---

### 6. Listings bị từ chối (REJECTED)

**Mục đích:** Xem lý do từ chối và chỉnh sửa lại

**Request:**
```json
{
  "userId": "user-abc-123",
  "listingStatus": "REJECTED",
  "sortBy": "updatedAt",
  "sortDirection": "DESC"
}
```

**Frontend Display:**
```typescript
<Tab label="Bị từ chối" count={2} color="error" />

// Trong danh sách
<ListItem>
  <Typography>{listing.title}</Typography>
  <Chip label="Bị từ chối" color="error" />
  <Alert severity="error">
    Lý do: {listing.rejectionReason || "Không rõ"}
  </Alert>
  <Button variant="contained" onClick={handleEdit}>
    Chỉnh sửa lại
  </Button>
</ListItem>
```

---

### 7. Listings chờ thanh toán (PENDING_PAYMENT)

**Mục đích:** Hoàn tất thanh toán cho bài đăng

**Request:**
```json
{
  "userId": "user-abc-123",
  "listingStatus": "PENDING_PAYMENT",
  "sortBy": "createdAt",
  "sortDirection": "ASC"
}
```

**Frontend Display:**
```typescript
<Tab label="Chờ thanh toán" count={1} color="warning" />

// Trong danh sách
<ListItem>
  <Typography>{listing.title}</Typography>
  <Chip label="Chờ thanh toán" color="warning" />
  <Button
    variant="contained"
    color="primary"
    onClick={() => handlePayment(listing.transactionId)}
  >
    Thanh toán ngay
  </Button>
</ListItem>
```

---

### 8. Listings đã xác thực (VERIFIED)

**Mục đích:** Xem tất cả bài đã được verify (bao gồm cả đã hết hạn)

**Request:**
```json
{
  "userId": "user-abc-123",
  "listingStatus": "VERIFIED",
  "page": 0,
  "size": 20
}
```

**Note:** `VERIFIED` khác với `DISPLAYING`:
- `VERIFIED`: Đã verify (có thể đã hết hạn hoặc chưa)
- `DISPLAYING`: Đang hiển thị (verified + chưa hết hạn)

---

## 🎨 Frontend Implementation Examples

### React/TypeScript Example

```typescript
import { useState } from 'react';

type ListingStatus =
  | 'EXPIRED'
  | 'EXPIRING_SOON'
  | 'DISPLAYING'
  | 'IN_REVIEW'
  | 'PENDING_PAYMENT'
  | 'REJECTED'
  | 'VERIFIED';

interface ListingFilterRequest {
  userId: string;
  listingStatus?: ListingStatus;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

const MyListingsPage = () => {
  const [selectedStatus, setSelectedStatus] = useState<ListingStatus | null>(null);
  const userId = "user-abc-123"; // From auth context

  const fetchListings = async (status: ListingStatus | null) => {
    const filter: ListingFilterRequest = {
      userId,
      page: 0,
      size: 20,
    };

    if (status) {
      filter.listingStatus = status;
    }

    const response = await fetch('/v1/listings/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(filter),
    });

    return await response.json();
  };

  return (
    <div>
      <Tabs value={selectedStatus} onChange={(_, v) => setSelectedStatus(v)}>
        <Tab label="Tất cả" value={null} />
        <Tab label="Đang hiển thị" value="DISPLAYING" />
        <Tab label="Sắp hết hạn" value="EXPIRING_SOON" />
        <Tab label="Đã hết hạn" value="EXPIRED" />
        <Tab label="Đang duyệt" value="IN_REVIEW" />
        <Tab label="Bị từ chối" value="REJECTED" />
        <Tab label="Chờ thanh toán" value="PENDING_PAYMENT" />
      </Tabs>

      <ListingsGrid status={selectedStatus} />
    </div>
  );
};
```

### Vue.js Example

```vue
<template>
  <div>
    <v-tabs v-model="selectedStatus">
      <v-tab value="">Tất cả</v-tab>
      <v-tab value="DISPLAYING">Đang hiển thị</v-tab>
      <v-tab value="EXPIRING_SOON">Sắp hết hạn</v-tab>
      <v-tab value="EXPIRED">Đã hết hạn</v-tab>
      <v-tab value="IN_REVIEW">Đang duyệt</v-tab>
      <v-tab value="REJECTED">Bị từ chối</v-tab>
      <v-tab value="PENDING_PAYMENT">Chờ thanh toán</v-tab>
    </v-tabs>

    <v-list>
      <v-list-item v-for="listing in listings" :key="listing.listingId">
        <v-list-item-title>{{ listing.title }}</v-list-item-title>
        <v-chip :color="getStatusColor(listing.listingStatus)">
          {{ getStatusLabel(listing.listingStatus) }}
        </v-chip>
      </v-list-item>
    </v-list>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const selectedStatus = ref('');
const listings = ref([]);

const fetchListings = async () => {
  const filter: any = {
    userId: 'user-abc-123',
    page: 0,
    size: 20,
  };

  if (selectedStatus.value) {
    filter.listingStatus = selectedStatus.value;
  }

  const response = await fetch('/v1/listings/search', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(filter),
  });

  const data = await response.json();
  listings.value = data.data.listings;
};

watch(selectedStatus, fetchListings);

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    DISPLAYING: 'success',
    EXPIRING_SOON: 'warning',
    EXPIRED: 'error',
    IN_REVIEW: 'info',
    REJECTED: 'error',
    PENDING_PAYMENT: 'warning',
    VERIFIED: 'success',
  };
  return colors[status] || 'default';
};

const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    DISPLAYING: 'Đang hiển thị',
    EXPIRING_SOON: 'Sắp hết hạn',
    EXPIRED: 'Đã hết hạn',
    IN_REVIEW: 'Đang duyệt',
    REJECTED: 'Bị từ chối',
    PENDING_PAYMENT: 'Chờ thanh toán',
    VERIFIED: 'Đã xác thực',
  };
  return labels[status] || status;
};
</script>
```

---

## 📊 Dashboard Statistics

Frontend có thể gọi API nhiều lần để lấy số lượng cho mỗi status:

```typescript
const fetchStatusCounts = async (userId: string) => {
  const statuses: ListingStatus[] = [
    'DISPLAYING',
    'EXPIRING_SOON',
    'EXPIRED',
    'IN_REVIEW',
    'PENDING_PAYMENT',
    'REJECTED',
  ];

  const counts = await Promise.all(
    statuses.map(async (status) => {
      const response = await fetch('/v1/listings/search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId,
          listingStatus: status,
          page: 0,
          size: 1, // Chỉ cần count, không cần data
        }),
      });
      const data = await response.json();
      return {
        status,
        count: data.data.totalCount,
      };
    })
  );

  return counts;
};

// Usage
const statusCounts = await fetchStatusCounts('user-abc-123');
// [
//   { status: 'DISPLAYING', count: 15 },
//   { status: 'EXPIRING_SOON', count: 3 },
//   { status: 'EXPIRED', count: 5 },
//   ...
// ]
```

---

## 🎯 Best Practices

### 1. Cache Status Counts
```typescript
// Cache counts for 5 minutes
const CACHE_TTL = 5 * 60 * 1000;
let cachedCounts: any = null;
let cacheTimestamp = 0;

const getStatusCounts = async (userId: string) => {
  const now = Date.now();
  if (cachedCounts && (now - cacheTimestamp) < CACHE_TTL) {
    return cachedCounts;
  }

  cachedCounts = await fetchStatusCounts(userId);
  cacheTimestamp = now;
  return cachedCounts;
};
```

### 2. Real-time Updates
```typescript
// Refresh sau khi thực hiện action
const handleRenewListing = async (listingId: number) => {
  await renewListing(listingId);

  // Refresh danh sách
  await fetchListings(selectedStatus);

  // Refresh counts
  cachedCounts = null; // Invalidate cache
  await getStatusCounts(userId);
};
```

### 3. Error Handling
```typescript
try {
  const response = await fetch('/v1/listings/search', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(filter),
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  const data = await response.json();

  if (data.code !== '999999') {
    throw new Error(data.message || 'Unknown error');
  }

  return data.data;
} catch (error) {
  console.error('Failed to fetch listings:', error);
  // Show error toast/notification
}
```

---

## 🔔 Notifications

### Cảnh báo sắp hết hạn
```typescript
const checkExpiringListings = async (userId: string) => {
  const response = await fetch('/v1/listings/search', {
    method: 'POST',
    body: JSON.stringify({
      userId,
      listingStatus: 'EXPIRING_SOON',
    }),
  });

  const data = await response.json();

  if (data.data.totalCount > 0) {
    // Show notification
    showNotification({
      title: 'Cảnh báo hết hạn',
      message: `Bạn có ${data.data.totalCount} bài đăng sắp hết hạn!`,
      type: 'warning',
    });
  }
};

// Run on page load
useEffect(() => {
  checkExpiringListings(userId);
}, [userId]);
```

---

## ✅ Checklist Implementation

- [ ] Hiển thị tabs cho các status khác nhau
- [ ] Fetch và display listings theo status
- [ ] Hiển thị badge/chip với màu phù hợp
- [ ] Show counts cho mỗi status
- [ ] Implement sorting (expiryDate, createdAt, etc.)
- [ ] Cache status counts
- [ ] Error handling
- [ ] Loading states
- [ ] Empty states cho từng status
- [ ] Notifications cho EXPIRING_SOON
- [ ] Action buttons (Gia hạn, Chỉnh sửa, Thanh toán)
- [ ] Refresh after actions
- [ ] Responsive design

---

## 📞 Support

Nếu gặp vấn đề:
1. Check console logs
2. Verify `userId` đúng
3. Check `listingStatus` value hợp lệ
4. Verify API response structure
5. Check network tab trong DevTools

Happy coding! 🚀
