# Realtime Notification — User Frontend Integration Guide

Hướng dẫn tích hợp **thông báo realtime** cho ứng dụng người dùng (User App) sử dụng **WebSocket (STOMP/SockJS)** + **REST API**.

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

function connectNotifications(userId, onNotification) {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${BASE_URL}/ws`),
    reconnectDelay: 5000,

    onConnect: () => {
      console.log('WebSocket connected');
      // Subscribe kênh thông báo của user
      client.subscribe(`/topic/notifications/${userId}`, (message) => {
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

> **Lưu ý:** `userId` là UUID từ JWT token (`user_id` claim).

---

## 3. Các Loại Thông Báo User Nhận Được

| Type | Khi nào | Ý nghĩa |
|------|---------|---------|
| `NEW_REPORT` | Có người báo cáo tin đăng của bạn | Tin đăng bị report |
| `REPORT_RESOLVED` | Admin xử lý xong báo cáo bạn gửi | Report được chấp nhận |
| `REPORT_REJECTED` | Admin từ chối báo cáo bạn gửi | Report bị từ chối |
| `REPORT_ACTION_REQUIRED` | Admin yêu cầu bạn sửa tin đăng | Cần chỉnh sửa & gửi lại |
| `LISTING_APPROVED` | Tin đăng được duyệt | Tin đã hiển thị |
| `LISTING_REJECTED` | Tin đăng bị từ chối | Cần sửa & gửi lại |
| `LISTING_REVISION_REQUIRED` | Admin yêu cầu chỉnh sửa | Cần sửa & gửi lại |
| `LISTING_SUSPENDED` | Tin đăng bị tạm ngưng | Liên hệ hỗ trợ |

---

## 4. Notification Payload (WebSocket & REST)

```typescript
interface Notification {
  id: number;
  type: string;              // NotificationType (bảng trên)
  title: string;             // Tiêu đề ngắn
  message: string;           // Nội dung chi tiết
  referenceId: number | null; // listing_id hoặc report_id
  referenceType: string | null; // "LISTING" hoặc "REPORT"
  isRead: boolean;
  createdAt: string;         // ISO 8601
}
```

---

## 5. REST API

Tất cả endpoint yêu cầu header `Authorization: Bearer <token>`.

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
      "id": 1,
      "type": "LISTING_APPROVED",
      "title": "Listing moderation update",
      "message": "Your listing \"Studio apartment\" has been approved.",
      "referenceId": 42,
      "referenceType": "LISTING",
      "isRead": false,
      "createdAt": "2026-03-05T23:30:00"
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

**Response mẫu `GET /v1/notifications/unread-count`:**
```json
{ "unreadCount": 3 }
```

---

## 6. Điều Hướng Khi Click Notification

```javascript
function handleNotificationClick(notification) {
  markAsRead(notification.id);

  switch (notification.referenceType) {
    case 'LISTING':
      navigate(`/listings/${notification.referenceId}`);
      break;
    case 'REPORT':
      navigate(`/reports/${notification.referenceId}`);
      break;
    default:
      navigate('/notifications');
  }
}
```

---

## 7. React Hook Hoàn Chỉnh

```javascript
import { useState, useEffect, useRef, useCallback } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const API_BASE = 'https://dev.api.smartrent.io.vn';

export function useNotifications(userId, token) {
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
        client.subscribe(`/topic/notifications/${userId}`, (msg) => {
          const notif = JSON.parse(msg.body);
          setNotifications((prev) => [notif, ...prev]);
          setUnreadCount((prev) => prev + 1);
        });
      },
    });
    client.activate();
    return () => client.deactivate();
  }, [userId]);

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
