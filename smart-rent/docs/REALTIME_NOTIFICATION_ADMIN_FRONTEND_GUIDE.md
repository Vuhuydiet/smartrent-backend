# Realtime Notification — Admin Frontend Integration Guide

Hướng dẫn tích hợp **thông báo realtime** cho Admin Dashboard sử dụng **WebSocket (STOMP/SockJS)** + **REST API**.

---

## 1. Cài Đặt Dependencies

```bash
npm install sockjs-client @stomp/stompjs
```

---

## 2. Kết Nối WebSocket (Realtime)

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const BASE_URL = 'https://dev.api.smartrent.io.vn';

function connectAdminNotifications(adminId, onNotification) {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${BASE_URL}/ws`),
    reconnectDelay: 5000,

    onConnect: () => {
      console.log('Admin WebSocket connected');
      // Subscribe kênh thông báo của admin
      client.subscribe(`/topic/notifications/${adminId}`, (message) => {
        const notification = JSON.parse(message.body);
        onNotification(notification);
      });
    },

    onStompError: (frame) => {
      console.error('STOMP error:', frame.headers['message']);
    },
  });

  client.activate();
  return client;
}
```

> **Lưu ý:** `adminId` là UUID từ JWT token (`admin_id` claim). Ví dụ:
> ```javascript
> import jwtDecode from 'jwt-decode';
> const adminId = jwtDecode(token).admin_id;
> ```

---

## 3. Các Loại Thông Báo Admin Nhận Được

| Type | Khi nào | Hành động gợi ý trong UI |
|------|---------|--------------------------|
| `NEW_REPORT` | Có báo cáo mới từ người dùng | Hiện badge, link đến trang quản lý báo cáo |
| `LISTING_RESUBMITTED` | Chủ nhà gửi lại tin đăng sau khi sửa | Hiện badge, link đến hàng đợi duyệt tin |

---

## 4. Notification Payload (WebSocket & REST)

```typescript
interface Notification {
  id: number;
  type: string;              // "NEW_REPORT" | "LISTING_RESUBMITTED"
  title: string;             // Tiêu đề ngắn
  message: string;           // Nội dung chi tiết
  referenceId: number | null; // report_id hoặc listing_id
  referenceType: string | null; // "REPORT" hoặc "LISTING"
  isRead: boolean;
  createdAt: string;         // ISO 8601
}
```

---

## 5. REST API

Tất cả endpoint yêu cầu header `Authorization: Bearer <admin_token>`.

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/v1/notifications?page=0&size=20` | Lấy danh sách thông báo (phân trang) |
| `GET` | `/v1/notifications/unread-count` | Đếm thông báo chưa đọc |
| `PATCH` | `/v1/notifications/{id}/read` | Đánh dấu đã đọc 1 thông báo |
| `PATCH` | `/v1/notifications/read-all` | Đánh dấu tất cả đã đọc |

**Response mẫu `GET /v1/notifications`:**
```json
{
  "content": [
    {
      "id": 5,
      "type": "NEW_REPORT",
      "title": "New listing report",
      "message": "A new report has been submitted for listing: Studio apartment",
      "referenceId": 12,
      "referenceType": "REPORT",
      "isRead": false,
      "createdAt": "2026-03-05T23:30:00"
    },
    {
      "id": 8,
      "type": "LISTING_RESUBMITTED",
      "title": "Listing resubmitted for review",
      "message": "Listing \"2BR Apartment\" has been updated and resubmitted for review.",
      "referenceId": 42,
      "referenceType": "LISTING",
      "isRead": false,
      "createdAt": "2026-03-05T23:35:00"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

**Response mẫu `GET /v1/notifications/unread-count`:**
```json
{ "unreadCount": 5 }
```

---

## 6. Điều Hướng Khi Click Notification

```javascript
function handleNotificationClick(notification) {
  markAsRead(notification.id);

  switch (notification.type) {
    case 'NEW_REPORT':
      // Đi đến chi tiết report
      navigate(`/admin/reports/${notification.referenceId}`);
      break;
    case 'LISTING_RESUBMITTED':
      // Đi đến trang duyệt tin đăng
      navigate(`/admin/listings/${notification.referenceId}/review`);
      break;
    default:
      navigate('/admin/notifications');
  }
}
```

---

## 7. React Hook Hoàn Chỉnh

```javascript
import { useState, useEffect, useCallback } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const API_BASE = 'https://dev.api.smartrent.io.vn';

export function useAdminNotifications(adminId, token) {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  // Fetch initial data
  useEffect(() => {
    fetch(`${API_BASE}/v1/notifications?page=0&size=20`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => setNotifications(data.content || []));

    fetch(`${API_BASE}/v1/notifications/unread-count`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => setUnreadCount(data.unreadCount || 0));
  }, [token]);

  // WebSocket
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/notifications/${adminId}`, (msg) => {
          const notif = JSON.parse(msg.body);
          setNotifications((prev) => [notif, ...prev]);
          setUnreadCount((prev) => prev + 1);
        });
      },
    });
    client.activate();
    return () => client.deactivate();
  }, [adminId]);

  const markAsRead = useCallback((id) => {
    fetch(`${API_BASE}/v1/notifications/${id}/read`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${token}` },
    }).then(() => {
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    });
  }, [token]);

  const markAllAsRead = useCallback(() => {
    fetch(`${API_BASE}/v1/notifications/read-all`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${token}` },
    }).then(() => {
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    });
  }, [token]);

  return { notifications, unreadCount, markAsRead, markAllAsRead };
}
```

---

## 8. Gợi Ý UI cho Admin Dashboard

- **Header bell icon** với badge hiển thị `unreadCount`
- **Dropdown panel** hiển thị danh sách thông báo mới nhất
- **Notification badge trên tab "Báo cáo"** khi có `NEW_REPORT`
- **Notification badge trên tab "Chờ duyệt"** khi có `LISTING_RESUBMITTED`
- **Sound/toast** khi nhận thông báo realtime qua WebSocket
