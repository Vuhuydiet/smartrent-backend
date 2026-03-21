# Saved Listings Trend — Frontend Integration Guide

> **Backend base URL**: `https://dev.api.smartrent.io.vn` or `http://localhost:8080`
>
> **Authentication**: All endpoints require a valid **Owner** Bearer token.

---

## 1. Single Listing — Saves Trend (Line Chart)

Shows how many users save a specific listing over time.

### Endpoint

```
GET /v1/owners/listings/{listingId}/saves-trend
Authorization: Bearer <token>
```

### Response (200)

```json
{
  "code": "999999",
  "data": {
    "listingId": 123,
    "listingTitle": "Beautiful 2BR Apartment",
    "totalSaves": 28,
    "savesOverTime": [
      { "date": "2026-03-14", "count": 3 },
      { "date": "2026-03-15", "count": 5 },
      { "date": "2026-03-16", "count": 2 },
      { "date": "2026-03-17", "count": 8 },
      { "date": "2026-03-18", "count": 4 },
      { "date": "2026-03-19", "count": 3 },
      { "date": "2026-03-20", "count": 3 }
    ]
  }
}
```

### TypeScript Interface

```ts
interface DailySaveCount {
  date: string;   // "2026-03-14"
  count: number;
}

interface SavedListingsTrendResponse {
  listingId: number;
  listingTitle: string;
  totalSaves: number;
  savesOverTime: DailySaveCount[];
}
```

### Fetch Helper

```ts
async function fetchSavesTrend(listingId: number, token: string): Promise<SavedListingsTrendResponse> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/v1/owners/listings/${listingId}/saves-trend`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}
```

### Line Chart Component (Recharts)

```tsx
'use client';

import { useEffect, useState } from 'react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';

export function SavesTrendChart({ listingId, token }: { listingId: number; token: string }) {
  const [data, setData] = useState<SavedListingsTrendResponse | null>(null);

  useEffect(() => {
    fetchSavesTrend(listingId, token).then(setData).catch(console.error);
  }, [listingId, token]);

  if (!data) return <div>Loading...</div>;

  return (
    <div>
      {/* Summary */}
      <div style={{ marginBottom: 16 }}>
        <h3>{data.listingTitle}</h3>
        <div style={{
          display: 'inline-flex', alignItems: 'center', gap: 8,
          padding: '8px 16px', borderRadius: 8, background: '#fef3c7'
        }}>
          <span style={{ fontSize: 24, fontWeight: 700 }}>❤️ {data.totalSaves}</span>
          <span style={{ fontSize: 13, color: '#92400e' }}>lượt lưu tin</span>
        </div>
      </div>

      {/* Area Chart */}
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart data={data.savesOverTime}>
          <defs>
            <linearGradient id="colorSaves" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#f59e0b" stopOpacity={0.8} />
              <stop offset="95%" stopColor="#f59e0b" stopOpacity={0.05} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="date" />
          <YAxis allowDecimals={false} />
          <Tooltip
            formatter={(value: number) => [`${value} lượt lưu`, 'Lượt lưu']}
            labelFormatter={(label) => `Ngày: ${label}`}
          />
          <Area
            type="monotone"
            dataKey="count"
            stroke="#f59e0b"
            fillOpacity={1}
            fill="url(#colorSaves)"
            strokeWidth={2}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
```

---

## 2. All Listings — Saves Summary (Table + Bar)

Shows save counts across all of the owner's listings.

### Endpoint

```
GET /v1/owners/listings/saves-analytics
Authorization: Bearer <token>
```

### Response (200)

```json
{
  "code": "999999",
  "data": {
    "listings": [
      { "listingId": 123, "listingTitle": "2BR Apartment", "totalSaves": 28 },
      { "listingId": 456, "listingTitle": "Studio Near Park", "totalSaves": 9 }
    ],
    "totalSavesAcrossAll": 37
  }
}
```

### TypeScript Interface

```ts
interface ListingSaveSummary {
  listingId: number;
  listingTitle: string;
  totalSaves: number;
}

interface OwnerSavedListingsAnalyticsResponse {
  listings: ListingSaveSummary[];
  totalSavesAcrossAll: number;
}
```

### Fetch Helper

```ts
async function fetchSavesAnalytics(token: string): Promise<OwnerSavedListingsAnalyticsResponse> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/v1/owners/listings/saves-analytics`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}
```

### Horizontal Bar Chart Component (Recharts)

```tsx
'use client';

import { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';

const COLORS = ['#f59e0b', '#6366f1', '#10b981', '#ef4444', '#3b82f6', '#8b5cf6'];

export function SavesSummaryChart({ token }: { token: string }) {
  const [data, setData] = useState<OwnerSavedListingsAnalyticsResponse | null>(null);

  useEffect(() => {
    fetchSavesAnalytics(token).then(setData).catch(console.error);
  }, [token]);

  if (!data) return <div>Loading...</div>;

  const chartData = data.listings.map(l => ({
    name: l.listingTitle.length > 25 ? l.listingTitle.substring(0, 25) + '…' : l.listingTitle,
    saves: l.totalSaves,
    listingId: l.listingId,
  }));

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'baseline', gap: 8 }}>
        <span style={{ fontSize: 24, fontWeight: 700 }}>❤️ {data.totalSavesAcrossAll}</span>
        <span style={{ fontSize: 14, color: '#64748b' }}>tổng lượt lưu trên tất cả tin</span>
      </div>

      <ResponsiveContainer width="100%" height={Math.max(200, data.listings.length * 50)}>
        <BarChart data={chartData} layout="vertical">
          <XAxis type="number" allowDecimals={false} />
          <YAxis type="category" dataKey="name" width={180} />
          <Tooltip formatter={(value: number) => [`${value} lượt lưu`, 'Lượt lưu']} />
          <Bar dataKey="saves" radius={[0, 4, 4, 0]}>
            {chartData.map((_, idx) => (
              <Cell key={idx} fill={COLORS[idx % COLORS.length]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
```

### Table Component

```tsx
function SavesTable({ listings, onViewTrend }: {
  listings: ListingSaveSummary[];
  onViewTrend: (listingId: number) => void;
}) {
  return (
    <table>
      <thead>
        <tr>
          <th>Tin đăng</th>
          <th>Lượt lưu</th>
          <th>Hành động</th>
        </tr>
      </thead>
      <tbody>
        {listings.map(l => (
          <tr key={l.listingId}>
            <td>{l.listingTitle}</td>
            <td>❤️ {l.totalSaves}</td>
            <td>
              <button onClick={() => onViewTrend(l.listingId)}>Xem xu hướng</button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

---

## 3. Error Handling

All error responses follow the standard shape:

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
| Not the listing owner | 500 | Calling saves-trend on someone else's listing |
| Listing not found | 500 | Invalid listing ID |

---

## 4. Quick Reference

| Feature | Endpoint | Method | Auth |
|---------|----------|--------|------|
| Single listing saves trend | `/v1/owners/listings/{id}/saves-trend` | GET | ✅ Owner |
| All listings saves summary | `/v1/owners/listings/saves-analytics` | GET | ✅ Owner |
