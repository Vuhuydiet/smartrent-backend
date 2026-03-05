# Realtime Notification System — Frontend Integration Guide

This guide covers how to integrate with the SmartRent **realtime notification system** from a frontend application. Notifications are delivered via **WebSocket (STOMP/SockJS)** for realtime updates and a **REST API** for notification history.

---

## Architecture Overview

```
┌──────────────────────┐         ┌──────────────────────┐
│    Frontend App      │         │    Backend Server     │
│                      │         │                      │
│  ┌────────────────┐  │  WS     │  ┌────────────────┐  │
│  │ STOMP Client   │◄─┼────────┼──│ WebSocket /ws   │  │
│  └────────────────┘  │         │  └────────────────┘  │
│                      │         │                      │
│  ┌────────────────┐  │  REST   │  ┌────────────────┐  │
│  │ REST Client    │◄─┼────────┼──│ /v1/notifications│  │
│  └────────────────┘  │         │  └────────────────┘  │
└──────────────────────┘         └──────────────────────┘
```

- **WebSocket** → realtime push (new notifications appear instantly)
- **REST API** → notification history, unread count, mark-as-read

---

## 1. WebSocket Connection (Realtime)

### Install Dependencies

```bash
npm install sockjs-client @stomp/stompjs
```

### Connection Setup (React Example)

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const BASE_URL = 'https://dev.api.smartrent.io.vn'; // or your backend URL

function connectNotifications(userId, onNotification) {
  const client = new Client({
    // SockJS factory for fallback support
    webSocketFactory: () => new SockJS(`${BASE_URL}/ws`),

    // Reconnect every 5 seconds if disconnected
    reconnectDelay: 5000,

    onConnect: () => {
      console.log('WebSocket connected');

      // Subscribe to user-specific notification channel
      client.subscribe(`/topic/notifications/${userId}`, (message) => {
        const notification = JSON.parse(message.body);
        onNotification(notification);
      });
    },

    onStompError: (frame) => {
      console.error('STOMP error:', frame.headers['message']);
    },

    onDisconnect: () => {
      console.log('WebSocket disconnected');
    },
  });

  client.activate();
  return client; // Return for cleanup
}

// Usage in React component:
// const clientRef = useRef(null);
// useEffect(() => {
//   clientRef.current = connectNotifications(currentUser.id, (notif) => {
//     setNotifications(prev => [notif, ...prev]);
//     setUnreadCount(prev => prev + 1);
//   });
//   return () => clientRef.current?.deactivate();
// }, [currentUser.id]);
```

### Channel Format

| User Type | Channel | Example |
|-----------|---------|---------|
| Regular User | `/topic/notifications/{userId}` | `/topic/notifications/abc-123-def` |
| Admin | `/topic/notifications/{adminId}` | `/topic/notifications/xyz-789-ghi` |

> **Note:** The `userId` / `adminId` is the UUID from the JWT token. Use the same ID that you extract from authentication.

---

## 2. REST API Reference

All endpoints require JWT `Authorization: Bearer <token>` header.

### Get Notifications (paginated)

```
GET /v1/notifications?page=0&size=20
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "type": "LISTING_APPROVED",
      "title": "Listing moderation update",
      "message": "Your listing \"Studio apartment\" has been approved and is now visible.",
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

### Get Unread Count

```
GET /v1/notifications/unread-count
```

**Response:**
```json
{
  "unreadCount": 3
}
```

### Mark Single Notification as Read

```
PATCH /v1/notifications/{id}/read
```

**Response:** `200 OK` (empty body)

### Mark All Notifications as Read

```
PATCH /v1/notifications/read-all
```

**Response:** `200 OK` (empty body)

---

## 3. Notification Types

### Report Flow Notifications

| Type | Who Receives | When |
|------|-------------|------|
| `NEW_REPORT` | All Admins + Listing Owner | User submits a report on a listing |
| `REPORT_RESOLVED` | Reporter + Listing Owner | Admin resolves a report (accepted) |
| `REPORT_REJECTED` | Reporter + Listing Owner | Admin rejects a report |
| `REPORT_ACTION_REQUIRED` | Listing Owner | Admin resolves report and requires owner action |

### Moderation Flow Notifications

| Type | Who Receives | When |
|------|-------------|------|
| `LISTING_APPROVED` | Listing Owner | Admin approves a listing |
| `LISTING_REJECTED` | Listing Owner | Admin rejects a listing |
| `LISTING_REVISION_REQUIRED` | Listing Owner | Admin requests revision on a listing |
| `LISTING_SUSPENDED` | Listing Owner | Admin suspends a listing |
| `LISTING_RESUBMITTED` | All Admins | Owner resubmits a listing for review |

---

## 4. WebSocket Notification Payload

All notifications pushed via WebSocket have this structure:

```typescript
interface Notification {
  id: number;                // Unique notification ID
  type: string;              // NotificationType enum value (see table above)
  title: string;             // Short notification title
  message: string;           // Detailed notification message
  referenceId: number | null; // Related listing ID or report ID
  referenceType: string | null; // "LISTING" or "REPORT"
  isRead: boolean;           // Always false for new realtime notifications
  createdAt: string;         // ISO 8601 timestamp
}
```

---

## 5. Frontend Navigation by referenceType

Use `referenceType` and `referenceId` to navigate the user when they click a notification:

```javascript
function handleNotificationClick(notification) {
  markAsRead(notification.id);

  switch (notification.referenceType) {
    case 'LISTING':
      navigate(`/listings/${notification.referenceId}`);
      break;
    case 'REPORT':
      // For admins: go to report detail
      navigate(`/admin/reports/${notification.referenceId}`);
      break;
    default:
      navigate('/notifications');
  }
}
```

---

## 6. Complete React Hook Example

```javascript
import { useState, useEffect, useRef, useCallback } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const API_BASE = 'https://dev.api.smartrent.io.vn';

export function useNotifications(userId, token) {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const clientRef = useRef(null);

  // Fetch initial notifications
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

  // WebSocket connection
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
    clientRef.current = client;
    return () => client.deactivate();
  }, [userId]);

  const markAsRead = useCallback(
    (id) => {
      fetch(`${API_BASE}/v1/notifications/${id}/read`, {
        method: 'PATCH',
        headers: { Authorization: `Bearer ${token}` },
      }).then(() => {
        setNotifications((prev) =>
          prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      });
    },
    [token]
  );

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

## 7. Admin-Specific Notes

Admin accounts receive notifications for:
- **New reports** (`NEW_REPORT`) — when any user submits a listing report
- **Listing resubmissions** (`LISTING_RESUBMITTED`) — when an owner updates and resubmits

Admin frontend should subscribe using the `adminId` from the JWT:

```javascript
const adminId = jwtDecode(token).admin_id;
client.subscribe(`/topic/notifications/${adminId}`, callback);
```

---

## 8. Quick Reference — Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| WS | `/ws` | No (public) | WebSocket STOMP + SockJS endpoint |
| GET | `/v1/notifications` | Yes | Paginated notification history |
| GET | `/v1/notifications/unread-count` | Yes | Unread notification count |
| PATCH | `/v1/notifications/{id}/read` | Yes | Mark single as read |
| PATCH | `/v1/notifications/read-all` | Yes | Mark all as read |
