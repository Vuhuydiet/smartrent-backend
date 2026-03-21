# Phone Click Analytics & Interest Level — Frontend Integration Guide

> **Backend base URL**: `https://dev.api.smartrent.io.vn` or `http://localhost:8080`

---

## 1. Track Phone Click

When a user clicks the owner's phone number on a listing detail page.

### Endpoint

```
POST /v1/phone-click-details
Authorization: Bearer <token>
```

### Request

```json
{
  "listingId": 123
}
```

### Response (200)

```json
{
  "code": "999999",
  "data": {
    "id": 1,
    "listingId": 123,
    "listingTitle": "Beautiful 2BR Apartment",
    "userId": "user-abc-123",
    "userFirstName": "Nguyen",
    "userLastName": "Van A",
    "userEmail": "a@example.com",
    "userContactPhone": "0912345678",
    "userContactPhoneVerified": true,
    "userAvatarUrl": "https://...",
    "clickedAt": "2026-03-21T10:30:00",
    "ipAddress": "192.168.1.1"
  }
}
```

### Spam Prevention

- Backend ignores duplicate clicks from the same **user** or **IP** within **10 minutes**
- Frontend does NOT need to handle dedup — just call the API on every click
- The response will still return a 200 (same shape), just without creating a duplicate record

### Next.js Example

```tsx
async function trackPhoneClick(listingId: number, token: string) {
  const res = await fetch(`${API_URL}/v1/phone-click-details`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ listingId }),
  });
  return res.json();
}
```

---

## 2. Interest Level (Customer Social Proof)

Show a qualitative badge on listing cards/detail pages. **Public endpoint, no auth required.**

### Endpoint

```
GET /v1/listings/{listingId}/interest-level
```

### Response (200)

```json
{
  "code": "999999",
  "data": {
    "level": "HIGH",
    "label": "Nhiều người đã liên hệ tin đăng này gần đây"
  }
}
```

### Level Values

| Level      | Meaning                        | Suggested UI              |
|------------|--------------------------------|---------------------------|
| `LOW`      | Very few contacts recently     | Gray badge or hide        |
| `MEDIUM`   | Some interest                  | Blue badge 🔵             |
| `HIGH`     | Many contacts recently         | Orange badge 🟠           |
| `TRENDING` | Very hot listing               | Red/fire badge 🔥         |

### Next.js Component Example

```tsx
'use client';

import { useEffect, useState } from 'react';

interface InterestLevel {
  level: 'LOW' | 'MEDIUM' | 'HIGH' | 'TRENDING';
  label: string;
}

const BADGE_STYLES: Record<string, { bg: string; text: string; icon: string }> = {
  LOW: { bg: 'bg-gray-100', text: 'text-gray-600', icon: '' },
  MEDIUM: { bg: 'bg-blue-100', text: 'text-blue-700', icon: '💬' },
  HIGH: { bg: 'bg-orange-100', text: 'text-orange-700', icon: '🔥' },
  TRENDING: { bg: 'bg-red-100', text: 'text-red-700', icon: '🚀' },
};

export function InterestBadge({ listingId }: { listingId: number }) {
  const [interest, setInterest] = useState<InterestLevel | null>(null);

  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/v1/listings/${listingId}/interest-level`)
      .then(res => res.json())
      .then(json => setInterest(json.data))
      .catch(() => {});
  }, [listingId]);

  if (!interest || interest.level === 'LOW') return null;

  const style = BADGE_STYLES[interest.level];

  return (
    <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${style.bg} ${style.text}`}>
      {style.icon} {interest.label}
    </span>
  );
}
```

---

## 3. Owner Analytics Dashboard

**Authenticated endpoints for listing owners only.**

### 3.1 Single Listing Analytics

```
GET /v1/owners/listings/{listingId}/analytics
Authorization: Bearer <token>
```

#### Response

```json
{
  "code": "999999",
  "data": {
    "listingId": 123,
    "listingTitle": "Beautiful 2BR Apartment",
    "totalClicks": 45,
    "totalViews": 320,
    "conversionRate": 0.1406,
    "clicksOverTime": [
      { "date": "2026-03-14", "count": 5 },
      { "date": "2026-03-15", "count": 8 },
      { "date": "2026-03-16", "count": 3 },
      { "date": "2026-03-17", "count": 12 },
      { "date": "2026-03-18", "count": 7 },
      { "date": "2026-03-19", "count": 6 },
      { "date": "2026-03-20", "count": 4 }
    ],
    "clicksByDayOfWeek": {
      "MON": 10,
      "TUE": 8,
      "WED": 6,
      "THU": 5,
      "FRI": 7,
      "SAT": 5,
      "SUN": 4
    }
  }
}
```

### 3.2 All Listings Summary

```
GET /v1/owners/listings/analytics
Authorization: Bearer <token>
```

#### Response

```json
{
  "code": "999999",
  "data": {
    "listings": [
      { "listingId": 123, "listingTitle": "2BR Apartment", "totalClicks": 45 },
      { "listingId": 456, "listingTitle": "Studio Near Park", "totalClicks": 12 }
    ]
  }
}
```

### Dashboard UI Suggestions

#### Summary Cards Row

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Total Clicks │  │ Total Views  │  │  Conv. Rate  │
│     45       │  │    320       │  │   14.06%     │
└──────────────┘  └──────────────┘  └──────────────┘
```

#### Clicks Over Time (Line Chart)

Use `clicksOverTime` array directly with [recharts](https://recharts.org):

```tsx
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

function ClicksChart({ data }: { data: { date: string; count: number }[] }) {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data}>
        <XAxis dataKey="date" />
        <YAxis />
        <Tooltip />
        <Line type="monotone" dataKey="count" stroke="#f97316" strokeWidth={2} />
      </LineChart>
    </ResponsiveContainer>
  );
}
```

#### Clicks by Day of Week (Bar Chart)

Use `clicksByDayOfWeek` map:

```tsx
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

function DayOfWeekChart({ data }: { data: Record<string, number> }) {
  const chartData = Object.entries(data).map(([day, count]) => ({ day, count }));

  return (
    <ResponsiveContainer width="100%" height={250}>
      <BarChart data={chartData}>
        <XAxis dataKey="day" />
        <YAxis />
        <Tooltip />
        <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
```

#### Multi-Listing Table

Use the `/v1/owners/listings/analytics` endpoint:

```tsx
function ListingsTable({ listings }: { listings: { listingId: number; listingTitle: string; totalClicks: number }[] }) {
  return (
    <table>
      <thead>
        <tr>
          <th>Listing</th>
          <th>Total Clicks</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        {listings.map(l => (
          <tr key={l.listingId}>
            <td>{l.listingTitle}</td>
            <td>{l.totalClicks}</td>
            <td>
              <a href={`/owner/listings/${l.listingId}/analytics`}>View Details</a>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

---

## 4. Existing Phone Click APIs (for reference)

These were already available before this feature:

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/v1/phone-click-details/listing/{id}` | ✅ | Get users who clicked on a listing |
| GET | `/v1/phone-click-details/listing/{id}/users` | ✅ | Get user details with click history |
| GET | `/v1/phone-click-details/listing/{id}/stats` | ✅ | Get click stats (total + unique) |
| GET | `/v1/phone-click-details/my-clicks` | ✅ | User's own click history |
| GET | `/v1/phone-click-details/my-listings` | ✅ | All clicks on owner's listings |
| GET | `/v1/phone-click-details/my-listings/users` | ✅ | Users who clicked on owner's listings |
| GET | `/v1/phone-click-details/my-listings/search` | ✅ | Search by listing title |

---

## 5. Error Handling

All error responses follow the same shape:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "data": null
}
```

| Scenario | HTTP Status | When |
|----------|-------------|------|
| Not authenticated | 401 | Missing or invalid Bearer token |
| Not the listing owner | 500 | Calling analytics on someone else's listing |
| Listing not found | 500 | Invalid listing ID |

### Suggested Error Handling

```tsx
async function fetchAnalytics(listingId: number, token: string) {
  const res = await fetch(
    `${API_URL}/v1/owners/listings/${listingId}/analytics`,
    { headers: { Authorization: `Bearer ${token}` } }
  );

  const json = await res.json();

  if (json.code !== '999999') {
    throw new Error(json.message || 'Failed to load analytics');
  }

  return json.data;
}
```

---

## 6. Quick Reference

| Feature | Endpoint | Auth | Method |
|---------|----------|------|--------|
| Track phone click | `/v1/phone-click-details` | ✅ | POST |
| Interest level (public) | `/v1/listings/{id}/interest-level` | ❌ | GET |
| Single listing analytics | `/v1/owners/listings/{id}/analytics` | ✅ | GET |
| All listings analytics | `/v1/owners/listings/analytics` | ✅ | GET |
