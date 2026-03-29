# Admin Dashboard Charts — Frontend Integration Guide

> **Backend base URL**: `https://dev.api.smartrent.io.vn` or `http://localhost:8080`
>
> **Authentication**: All endpoints require a valid **Admin** Bearer token.

---

## 1. Revenue Over Time (Area Chart)

### Endpoint

```
GET /v1/admin/dashboard/revenue
Authorization: Bearer <admin-token>
```

| Param | Type       | Required | Default          | Notes |
|-------|------------|----------|------------------|-------|
| `days`| `int`      | No       | —                | Preset range: `7`, `30`, `360`. Takes priority over `from`/`to` |
| `from`| `YYYY-MM-DD` | No     | 30 days ago      | Used when `days` is absent |
| `to`  | `YYYY-MM-DD` | No     | Today             | Used when `days` is absent |

**Granularity**: `days ≤ 30` → grouped by **day**, `days > 30` → grouped by **month**. Custom `from`/`to` always uses day-level.

### Response (200)

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "date": "2026-03-01", "totalAmount": 2800000, "transactionCount": 2 },
      { "date": "2026-03-02", "totalAmount": 1400000, "transactionCount": 1 },
      { "date": "2026-03-05", "totalAmount": 4200000, "transactionCount": 3 }
    ],
    "grandTotal": 8400000,
    "totalTransactions": 6,
    "revenueByType": [
      { "transactionType": "MEMBERSHIP_PURCHASE", "totalAmount": 5600000, "transactionCount": 3 },
      { "transactionType": "POST_FEE", "totalAmount": 1400000, "transactionCount": 2 },
      { "transactionType": "PUSH_FEE", "totalAmount": 1400000, "transactionCount": 1 }
    ],
    "granularity": "DAY"
  }
}
```

Monthly response (when `days=360`):

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "date": "2025-04", "totalAmount": 15000000, "transactionCount": 12 },
      { "date": "2025-05", "totalAmount": 22000000, "transactionCount": 18 }
    ],
    "grandTotal": 37000000,
    "totalTransactions": 30,
    "revenueByType": [...],
    "granularity": "MONTH"
  }
}
```

### TypeScript Interface

```ts
interface RevenueDataPoint {
  date: string;         // "2026-03-01" or "2025-04"
  totalAmount: number;  // VND
  transactionCount: number;
}

interface RevenueByTypeItem {
  transactionType: 'MEMBERSHIP_PURCHASE' | 'MEMBERSHIP_UPGRADE' | 'POST_FEE' | 'PUSH_FEE' | 'WALLET_TOPUP' | 'REFUND';
  totalAmount: number;
  transactionCount: number;
}

interface RevenueOverTimeResponse {
  dataPoints: RevenueDataPoint[];
  grandTotal: number;
  totalTransactions: number;
  revenueByType: RevenueByTypeItem[];
  granularity: 'DAY' | 'MONTH';
}
```

### Transaction Type Labels (Vietnamese)

| Type | Label |
|------|-------|
| `MEMBERSHIP_PURCHASE` | Mua gói thành viên |
| `MEMBERSHIP_UPGRADE`  | Nâng cấp gói thành viên |
| `POST_FEE`            | Phí đăng tin |
| `PUSH_FEE`            | Phí đẩy tin |
| `WALLET_TOPUP`        | Nạp ví |
| `REFUND`              | Hoàn tiền |

### Fetch Helper

```ts
async function fetchRevenue(
  token: string,
  options?: { days?: number; from?: string; to?: string }
): Promise<RevenueOverTimeResponse> {
  const params = new URLSearchParams();
  if (options?.days) {
    params.set('days', options.days.toString());
  } else {
    if (options?.from) params.set('from', options.from);
    if (options?.to) params.set('to', options.to);
  }

  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/v1/admin/dashboard/revenue?${params}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}
```

### Area Chart Component (Recharts)

```tsx
'use client';

import { useEffect, useState } from 'react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';

function formatVND(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
}

function formatXLabel(date: string, granularity: string) {
  if (granularity === 'MONTH') {
    const [year, month] = date.split('-');
    return `T${month}/${year}`;
  }
  return date.slice(5); // "03-01"
}

export function RevenueChart({ token }: { token: string }) {
  const [data, setData] = useState<RevenueOverTimeResponse | null>(null);
  const [days, setDays] = useState(7);

  useEffect(() => {
    fetchRevenue(token, { days }).then(setData).catch(console.error);
  }, [token, days]);

  if (!data) return <div>Loading...</div>;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h3 style={{ margin: 0 }}>Doanh thu</h3>
        <TimeRangeSelector value={days} onChange={setDays} />
      </div>

      {/* Summary Cards */}
      <div style={{ display: 'flex', gap: 16, marginBottom: 24 }}>
        <SummaryCard label="Tổng doanh thu" value={formatVND(data.grandTotal)} />
        <SummaryCard label="Số giao dịch" value={data.totalTransactions.toString()} />
      </div>

      {/* Area Chart */}
      <ResponsiveContainer width="100%" height={350}>
        <AreaChart data={data.dataPoints}>
          <defs>
            <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#6366f1" stopOpacity={0.8} />
              <stop offset="95%" stopColor="#6366f1" stopOpacity={0.05} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="date"
            tickFormatter={(v) => formatXLabel(v, data.granularity)}
          />
          <YAxis tickFormatter={(v) => `${(v / 1_000_000).toFixed(0)}M`} />
          <Tooltip
            formatter={(value: number) => [formatVND(value), 'Doanh thu']}
            labelFormatter={(label) => data.granularity === 'MONTH' ? `Tháng: ${label}` : `Ngày: ${label}`}
          />
          <Area
            type="monotone"
            dataKey="totalAmount"
            stroke="#6366f1"
            fillOpacity={1}
            fill="url(#colorRevenue)"
            strokeWidth={2}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div style={{
      padding: 16, borderRadius: 12, background: '#f8fafc',
      border: '1px solid #e2e8f0', minWidth: 180
    }}>
      <div style={{ fontSize: 13, color: '#64748b' }}>{label}</div>
      <div style={{ fontSize: 24, fontWeight: 700, color: '#0f172a' }}>{value}</div>
    </div>
  );
}
```

### Revenue by Type — Stacked Bar (Optional)

Use the `revenueByType` field for a breakdown bar chart:

```tsx
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';

const TYPE_COLORS: Record<string, string> = {
  MEMBERSHIP_PURCHASE: '#6366f1',
  MEMBERSHIP_UPGRADE: '#8b5cf6',
  POST_FEE: '#f59e0b',
  PUSH_FEE: '#10b981',
  WALLET_TOPUP: '#3b82f6',
  REFUND: '#ef4444',
};

const TYPE_LABELS: Record<string, string> = {
  MEMBERSHIP_PURCHASE: 'Mua gói',
  MEMBERSHIP_UPGRADE: 'Nâng cấp gói',
  POST_FEE: 'Phí đăng tin',
  PUSH_FEE: 'Phí đẩy tin',
  WALLET_TOPUP: 'Nạp ví',
  REFUND: 'Hoàn tiền',
};

export function RevenueByTypeChart({ data }: { data: RevenueByTypeItem[] }) {
  const chartData = data.map(d => ({
    name: TYPE_LABELS[d.transactionType] || d.transactionType,
    value: d.totalAmount,
    type: d.transactionType,
  }));

  return (
    <ResponsiveContainer width="100%" height={250}>
      <BarChart data={chartData} layout="vertical">
        <XAxis type="number" tickFormatter={(v) => `${(v / 1_000_000).toFixed(0)}M`} />
        <YAxis type="category" dataKey="name" width={120} />
        <Tooltip formatter={(value: number) => formatVND(value)} />
        <Bar dataKey="value" radius={[0, 4, 4, 0]}>
          {chartData.map((entry) => (
            <Cell key={entry.type} fill={TYPE_COLORS[entry.type] || '#94a3b8'} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
```

---

## 2. Active Memberships by Package (Donut Chart)

### Endpoint

```
GET /v1/admin/dashboard/memberships/distribution
Authorization: Bearer <admin-token>
```

No query parameters.

### Response (200)

```json
{
  "code": "999999",
  "data": {
    "distribution": [
      { "packageLevel": "BASIC", "packageName": "Gói Cơ Bản 1 Tháng", "count": 45, "percentage": 56.25 },
      { "packageLevel": "STANDARD", "packageName": "Gói Tiêu Chuẩn 1 Tháng", "count": 25, "percentage": 31.25 },
      { "packageLevel": "ADVANCED", "packageName": "Gói Nâng Cao 1 Tháng", "count": 10, "percentage": 12.50 }
    ],
    "totalActive": 80
  }
}
```

### TypeScript Interface

```ts
interface MembershipDistributionItem {
  packageLevel: 'BASIC' | 'STANDARD' | 'ADVANCED';
  packageName: string;
  count: number;
  percentage: number;  // e.g. 56.25
}

interface MembershipDistributionResponse {
  distribution: MembershipDistributionItem[];
  totalActive: number;
}
```

### Fetch Helper

```ts
async function fetchMembershipDistribution(token: string): Promise<MembershipDistributionResponse> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/v1/admin/dashboard/memberships/distribution`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}
```

### Donut Chart Component (Recharts)

```tsx
'use client';

import { useEffect, useState } from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';

const LEVEL_COLORS: Record<string, string> = {
  BASIC: '#94a3b8',
  STANDARD: '#6366f1',
  ADVANCED: '#f59e0b',
};

const LEVEL_LABELS: Record<string, string> = {
  BASIC: 'Cơ Bản',
  STANDARD: 'Tiêu Chuẩn',
  ADVANCED: 'Nâng Cao',
};

export function MembershipDonutChart({ token }: { token: string }) {
  const [data, setData] = useState<MembershipDistributionResponse | null>(null);

  useEffect(() => {
    fetchMembershipDistribution(token).then(setData).catch(console.error);
  }, [token]);

  if (!data) return <div>Loading...</div>;

  const chartData = data.distribution.map(d => ({
    name: LEVEL_LABELS[d.packageLevel] || d.packageName,
    value: d.count,
    level: d.packageLevel,
    percentage: d.percentage,
  }));

  return (
    <div>
      <div style={{ textAlign: 'center', marginBottom: 8 }}>
        <span style={{ fontSize: 28, fontWeight: 700 }}>{data.totalActive}</span>
        <span style={{ fontSize: 14, color: '#64748b', marginLeft: 8 }}>thành viên đang hoạt động</span>
      </div>

      <ResponsiveContainer width="100%" height={320}>
        <PieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            innerRadius={70}
            outerRadius={110}
            paddingAngle={3}
            dataKey="value"
            label={({ name, percentage }) => `${name} (${percentage.toFixed(1)}%)`}
          >
            {chartData.map((entry) => (
              <Cell key={entry.level} fill={LEVEL_COLORS[entry.level] || '#e2e8f0'} />
            ))}
          </Pie>
          <Tooltip
            formatter={(value: number, name: string) => [`${value} thành viên`, name]}
          />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
```

---

## 3. User Growth Chart (Line / Bar Chart)

### Endpoint

```
GET /v1/admin/dashboard/users/growth?days=7
Authorization: Bearer <admin-token>
```

| Param | Type  | Required | Default | Options       |
|-------|-------|----------|---------|---------------|
| `days`| `int` | No       | `7`     | `7`, `30`, `360` |

**Granularity**: `7` or `30` → grouped by **day**,  `360` → grouped by **month**

### Response (200)

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "label": "2026-03-23", "count": 5 },
      { "label": "2026-03-24", "count": 12 },
      { "label": "2026-03-25", "count": 8 },
      { "label": "2026-03-26", "count": 3 },
      { "label": "2026-03-29", "count": 10 }
    ],
    "total": 38,
    "granularity": "DAY"
  }
}
```

Monthly response (when `days=360`):

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "label": "2025-04", "count": 45 },
      { "label": "2025-05", "count": 62 },
      { "label": "2026-03", "count": 38 }
    ],
    "total": 520,
    "granularity": "MONTH"
  }
}
```

### TypeScript Interface

```ts
interface TimeSeriesDataPoint {
  label: string;  // "2026-03-29" or "2025-04"
  count: number;
}

interface TimeSeriesResponse {
  dataPoints: TimeSeriesDataPoint[];
  total: number;
  granularity: 'DAY' | 'MONTH';
}
```

### Fetch Helper

```ts
async function fetchUserGrowth(token: string, days: number = 7): Promise<TimeSeriesResponse> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/v1/admin/dashboard/users/growth?days=${days}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}
```

### Chart Component

```tsx
'use client';

import { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';

function formatXLabel(label: string, granularity: string) {
  if (granularity === 'MONTH') {
    const [year, month] = label.split('-');
    return `T${month}/${year}`;
  }
  return label.slice(5); // "03-29"
}

export function UserGrowthChart({ token }: { token: string }) {
  const [data, setData] = useState<TimeSeriesResponse | null>(null);
  const [days, setDays] = useState(7);

  useEffect(() => {
    fetchUserGrowth(token, days).then(setData).catch(console.error);
  }, [token, days]);

  if (!data) return <div>Loading...</div>;
  if (data.dataPoints.length === 0) return <EmptyState message="Chưa có dữ liệu người dùng mới" />;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h3 style={{ margin: 0 }}>Người dùng mới</h3>
          <span style={{ fontSize: 14, color: '#64748b' }}>Tổng: {data.total}</span>
        </div>
        <TimeRangeSelector value={days} onChange={setDays} />
      </div>

      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data.dataPoints}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="label"
            tickFormatter={(v) => formatXLabel(v, data.granularity)}
          />
          <YAxis allowDecimals={false} />
          <Tooltip
            labelFormatter={(label) =>
              data.granularity === 'MONTH' ? `Tháng: ${label}` : `Ngày: ${label}`
            }
            formatter={(value: number) => [`${value} người dùng`, 'Đăng ký']}
          />
          <Bar
            dataKey="count"
            fill="#6366f1"
            radius={[4, 4, 0, 0]}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
```

---

## 4. Report Count Chart (Line Chart)

### Endpoint

```
GET /v1/admin/dashboard/reports/count?days=7
Authorization: Bearer <admin-token>
```

| Param | Type  | Required | Default | Options       |
|-------|-------|----------|---------|---------------|
| `days`| `int` | No       | `7`     | `7`, `30`, `360` |

### Response (200)

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "label": "2026-03-23", "count": 2 },
      { "label": "2026-03-24", "count": 5 },
      { "label": "2026-03-25", "count": 1 }
    ],
    "total": 8,
    "granularity": "DAY"
  }
}
```

### Fetch Helper

```ts
async function fetchReportCount(token: string, days: number = 7): Promise<TimeSeriesResponse> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/v1/admin/dashboard/reports/count?days=${days}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}
```

### Chart Component

```tsx
'use client';

import { useEffect, useState } from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';

export function ReportCountChart({ token }: { token: string }) {
  const [data, setData] = useState<TimeSeriesResponse | null>(null);
  const [days, setDays] = useState(7);

  useEffect(() => {
    fetchReportCount(token, days).then(setData).catch(console.error);
  }, [token, days]);

  if (!data) return <div>Loading...</div>;
  if (data.dataPoints.length === 0) return <EmptyState message="Chưa có báo cáo nào" />;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h3 style={{ margin: 0 }}>Báo cáo vi phạm</h3>
          <span style={{ fontSize: 14, color: '#64748b' }}>Tổng: {data.total}</span>
        </div>
        <TimeRangeSelector value={days} onChange={setDays} />
      </div>

      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={data.dataPoints}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="label"
            tickFormatter={(v) => formatXLabel(v, data.granularity)}
          />
          <YAxis allowDecimals={false} />
          <Tooltip
            labelFormatter={(label) =>
              data.granularity === 'MONTH' ? `Tháng: ${label}` : `Ngày: ${label}`
            }
            formatter={(value: number) => [`${value} báo cáo`, 'Báo cáo']}
          />
          <Line
            type="monotone"
            dataKey="count"
            stroke="#ef4444"
            strokeWidth={2}
            dot={{ r: 4 }}
            activeDot={{ r: 6 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
```

---

## 5. Listing Creation Chart (Bar Chart)

### Endpoint

```
GET /v1/admin/dashboard/listings/creation?days=7
Authorization: Bearer <admin-token>
```

| Param | Type  | Required | Default | Options       |
|-------|-------|----------|---------|---------------|
| `days`| `int` | No       | `7`     | `7`, `30`, `360` |

### Response (200)

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "label": "2026-03-23", "count": 10 },
      { "label": "2026-03-24", "count": 15 },
      { "label": "2026-03-25", "count": 7 }
    ],
    "total": 32,
    "granularity": "DAY"
  }
}
```

### Fetch Helper

```ts
async function fetchListingCreation(token: string, days: number = 7): Promise<TimeSeriesResponse> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/v1/admin/dashboard/listings/creation?days=${days}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}
```

### Chart Component

```tsx
'use client';

import { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';

export function ListingCreationChart({ token }: { token: string }) {
  const [data, setData] = useState<TimeSeriesResponse | null>(null);
  const [days, setDays] = useState(7);

  useEffect(() => {
    fetchListingCreation(token, days).then(setData).catch(console.error);
  }, [token, days]);

  if (!data) return <div>Loading...</div>;
  if (data.dataPoints.length === 0) return <EmptyState message="Chưa có tin đăng mới" />;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h3 style={{ margin: 0 }}>Tin đăng mới</h3>
          <span style={{ fontSize: 14, color: '#64748b' }}>Tổng: {data.total}</span>
        </div>
        <TimeRangeSelector value={days} onChange={setDays} />
      </div>

      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data.dataPoints}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="label"
            tickFormatter={(v) => formatXLabel(v, data.granularity)}
          />
          <YAxis allowDecimals={false} />
          <Tooltip
            labelFormatter={(label) =>
              data.granularity === 'MONTH' ? `Tháng: ${label}` : `Ngày: ${label}`
            }
            formatter={(value: number) => [`${value} tin đăng`, 'Tin đăng']}
          />
          <Bar
            dataKey="count"
            fill="#10b981"
            radius={[4, 4, 0, 0]}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
```

---

## 6. Shared Components

### Time Range Selector

```tsx
const TIME_RANGES = [
  { label: '7 ngày', value: 7 },
  { label: '30 ngày', value: 30 },
  { label: '360 ngày', value: 360 },
];

function TimeRangeSelector({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  return (
    <div style={{ display: 'flex', gap: 4 }}>
      {TIME_RANGES.map((range) => (
        <button
          key={range.value}
          onClick={() => onChange(range.value)}
          style={{
            padding: '6px 14px',
            borderRadius: 8,
            border: value === range.value ? '2px solid #6366f1' : '1px solid #e2e8f0',
            background: value === range.value ? '#eef2ff' : '#fff',
            color: value === range.value ? '#6366f1' : '#64748b',
            fontWeight: value === range.value ? 600 : 400,
            cursor: 'pointer',
            fontSize: 13,
            transition: 'all 0.15s ease',
          }}
        >
          {range.label}
        </button>
      ))}
    </div>
  );
}
```

### Empty State

```tsx
function EmptyState({ message }: { message: string }) {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      height: 200,
      color: '#94a3b8',
      fontSize: 14,
    }}>
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <path d="M3 3v18h18" />
        <path d="M7 16l4-4 4 4 4-8" />
      </svg>
      <p style={{ marginTop: 12 }}>{message}</p>
    </div>
  );
}
```

### Reusable `formatXLabel`

```ts
function formatXLabel(label: string, granularity: string) {
  if (granularity === 'MONTH') {
    const [year, month] = label.split('-');
    return `T${month}/${year}`;
  }
  return label.slice(5); // "03-29"
}
```

---

## 7. Error Handling

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
| Not admin | 403 | Token belongs to a regular user, not an admin |
| Invalid date format | 400 | `from` or `to` is not `YYYY-MM-DD` |

---

## 8. Quick Reference

| Feature | Endpoint | Method | Params | Auth |
|---------|----------|--------|--------|------|
| Revenue over time | `/v1/admin/dashboard/revenue` | GET | `days` or `from`+`to` | ✅ Admin |
| Membership distribution | `/v1/admin/dashboard/memberships/distribution` | GET | — | ✅ Admin |
| User growth | `/v1/admin/dashboard/users/growth` | GET | `days` (7/30/360) | ✅ Admin |
| Report count | `/v1/admin/dashboard/reports/count` | GET | `days` (7/30/360) | ✅ Admin |
| Listing creation | `/v1/admin/dashboard/listings/creation` | GET | `days` (7/30/360) | ✅ Admin |

---

## 9. Dashboard Layout Example

```tsx
'use client';

export function AdminDashboard({ token }: { token: string }) {
  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: '24px 16px' }}>
      <h1 style={{ fontSize: 24, fontWeight: 700, marginBottom: 32 }}>Bảng điều khiển</h1>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 24, marginBottom: 24 }}>
        <ChartCard>
          <RevenueChart token={token} />
        </ChartCard>
        <ChartCard>
          <MembershipDonutChart token={token} />
        </ChartCard>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 24 }}>
        <ChartCard>
          <UserGrowthChart token={token} />
        </ChartCard>
        <ChartCard>
          <ReportCountChart token={token} />
        </ChartCard>
        <ChartCard>
          <ListingCreationChart token={token} />
        </ChartCard>
      </div>
    </div>
  );
}

function ChartCard({ children }: { children: React.ReactNode }) {
  return (
    <div style={{
      background: '#fff',
      borderRadius: 16,
      border: '1px solid #e2e8f0',
      padding: 24,
      boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
    }}>
      {children}
    </div>
  );
}
```
