# Admin Dashboard Charts — Frontend Integration Guide

> **Backend base URL**: `https://dev.api.smartrent.io.vn` or `http://localhost:8080`
>
> **Authentication**: All endpoints require a valid **Admin** Bearer token.

---

## 1. Revenue Over Time (Area Chart)

### Endpoint

```
GET /v1/admin/dashboard/revenue?from=2026-03-01&to=2026-03-21
Authorization: Bearer <admin-token>
```

| Param | Type       | Required | Default          |
|-------|------------|----------|------------------|
| `from`| `YYYY-MM-DD` | No     | 30 days ago      |
| `to`  | `YYYY-MM-DD` | No     | Today             |

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
    ]
  }
}
```

### TypeScript Interface

```ts
interface RevenueDataPoint {
  date: string;         // "2026-03-01"
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
async function fetchRevenue(token: string, from?: string, to?: string): Promise<RevenueOverTimeResponse> {
  const params = new URLSearchParams();
  if (from) params.set('from', from);
  if (to) params.set('to', to);

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

export function RevenueChart({ token, from, to }: { token: string; from?: string; to?: string }) {
  const [data, setData] = useState<RevenueOverTimeResponse | null>(null);

  useEffect(() => {
    fetchRevenue(token, from, to).then(setData).catch(console.error);
  }, [token, from, to]);

  if (!data) return <div>Loading...</div>;

  return (
    <div>
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
          <XAxis dataKey="date" />
          <YAxis tickFormatter={(v) => `${(v / 1_000_000).toFixed(0)}M`} />
          <Tooltip
            formatter={(value: number) => [formatVND(value), 'Doanh thu']}
            labelFormatter={(label) => `Ngày: ${label}`}
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
| Not admin | 403 | Token belongs to a regular user, not an admin |
| Invalid date format | 400 | `from` or `to` is not `YYYY-MM-DD` |

---

## 4. Quick Reference

| Feature | Endpoint | Method | Auth |
|---------|----------|--------|------|
| Revenue over time | `/v1/admin/dashboard/revenue` | GET | ✅ Admin |
| Membership distribution | `/v1/admin/dashboard/memberships/distribution` | GET | ✅ Admin |
