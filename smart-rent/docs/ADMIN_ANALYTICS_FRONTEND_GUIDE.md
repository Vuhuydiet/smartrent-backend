# Admin Analytics — Frontend Integration Guide

> **Backend base URL**: `https://dev.api.smartrent.io.vn` or `http://localhost:8080`
>
> **Authentication**: All endpoints require a valid **Admin** Bearer token.
>
> **Breaking change**: The old `/v1/admin/dashboard/*` endpoints have all been renamed and moved under
> `/v1/admin/analytics/*`. `revenue` and `memberships/distribution` keep the same response shape;
> `users`, `reports`, and `listings` now return much richer payloads (see below).

## 0. Endpoint map

| Old path | New path | Response change |
|---|---|---|
| `GET /v1/admin/dashboard/revenue` | `GET /v1/admin/analytics/revenue` | none — same shape |
| `GET /v1/admin/dashboard/users/growth` | `GET /v1/admin/analytics/users` | **enhanced** — cumulative curve + role/verification breakdowns |
| `GET /v1/admin/dashboard/reports/count` | `GET /v1/admin/analytics/reports` | **enhanced** — cumulative curve + category/status breakdowns + resolution metrics |
| `GET /v1/admin/dashboard/listings/creation` | `GET /v1/admin/analytics/listings` | **enhanced** — cumulative curve + type/product/verification breakdowns |
| `GET /v1/admin/dashboard/memberships/distribution` | `GET /v1/admin/analytics/memberships/distribution` | none — same shape |

All four analytics endpoints share the same query contract:

| Param | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| `days` | `int` | No | — (`revenue`: 30 / others: 7 when nothing is passed) | Preset range: `7`, `30`, `360`. Takes priority over `from`/`to`. |
| `from` | `YYYY-MM-DD` | No | 7 (or 30 for revenue) days before `to` | Used only when `days` is absent. |
| `to` | `YYYY-MM-DD` | No | Today | Used only when `days` is absent. |

**Precedence**: `days` wins if provided → else use `from`/`to` if either is set → else default (7 days, 30 for revenue).

**Granularity**: `days` ≤ 30 (or custom `from`/`to`) → **DAY** buckets; `days = 360` → **MONTH** buckets (`label`/`date` becomes `"YYYY-MM"`).

Common response envelope:

```json
{ "code": "999999", "data": { /* ... */ } }
```

```ts
interface ApiResponse<T> {
  code: string;
  message?: string;
  data: T;
}

interface TimeSeriesDataPoint {
  label: string;   // "2026-06-20" (DAY) or "2026-06" (MONTH)
  count: number;
}

interface CategoryBreakdownItem {
  category: string;   // enum-like string, see per-endpoint tables below
  count: number;
  percentage: number; // 0–100, rounded to 2 decimals, relative to the sum of this breakdown's own counts
}
```

`memberships/distribution` takes no query params — it's a live snapshot, not a time series:

```ts
interface MembershipDistributionItem {
  packageLevel: 'BASIC' | 'STANDARD' | 'ADVANCED';
  packageName: string;
  count: number;
  percentage: number;
}

interface MembershipDistributionResponse {
  distribution: MembershipDistributionItem[];
  totalActive: number;
}

async function fetchMembershipDistribution(token: string): Promise<MembershipDistributionResponse> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/v1/admin/analytics/memberships/distribution`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}
```

Shared fetch helper used by every other example below:

```ts
async function fetchAnalytics<T>(
  token: string,
  path: 'revenue' | 'users' | 'reports' | 'listings',
  opts?: { days?: number; from?: string; to?: string }
): Promise<T> {
  const params = new URLSearchParams();
  if (opts?.days != null) {
    params.set('days', String(opts.days));
  } else {
    if (opts?.from) params.set('from', opts.from);
    if (opts?.to) params.set('to', opts.to);
  }
  const qs = params.toString();
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/v1/admin/analytics/${path}${qs ? `?${qs}` : ''}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message ?? 'Request failed');
  return json.data as T;
}
```

---

## 1. Revenue — `GET /v1/admin/analytics/revenue`

Unchanged from the previous `dashboard/revenue` contract.

```ts
interface RevenueDataPoint {
  date: string;
  totalAmount: number;
  transactionCount: number;
}

interface RevenueByTypeItem {
  transactionType: string; // MEMBERSHIP_PURCHASE | MEMBERSHIP_UPGRADE | MEMBERSHIP_RENEWAL | POST_FEE | PUSH_FEE | REPOST_FEE | WALLET_TOPUP | ROOM_RENT | DEPOSIT | MONTHLY_INVOICE | UTILITY_BILL | SERVICE_FEE | REFUND
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

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "date": "2026-06-20", "totalAmount": 2800000, "transactionCount": 2 }
    ],
    "grandTotal": 4200000,
    "totalTransactions": 3,
    "revenueByType": [
      { "transactionType": "MEMBERSHIP_PURCHASE", "totalAmount": 2800000, "transactionCount": 2 }
    ],
    "granularity": "DAY"
  }
}
```

Chart suggestion: area chart on `dataPoints` (see the generic components in §5), horizontal bar/pie on `revenueByType`. This part of the dashboard is already implemented — no change needed on the FE beyond updating the URL.

---

## 2. Users — `GET /v1/admin/analytics/users`

```ts
interface AdminUserAnalyticsResponse {
  dataPoints: TimeSeriesDataPoint[];            // new registrations per bucket
  total: number;                                 // new registrations in range
  granularity: 'DAY' | 'MONTH';

  cumulativeDataPoints: TimeSeriesDataPoint[];   // running total of ALL users, same buckets as dataPoints
  totalUsersAsOfRangeEnd: number;                // snapshot: total users that exist as of `to`

  roleBreakdown: CategoryBreakdownItem[];        // category: 'REGULAR' | 'BROKER' — among users registered in range
  brokerVerificationBreakdown: CategoryBreakdownItem[]; // category: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED' — among brokers registered in range
}
```

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "label": "2026-06-29", "count": 5 },
      { "label": "2026-06-30", "count": 12 },
      { "label": "2026-07-01", "count": 8 }
    ],
    "total": 25,
    "granularity": "DAY",
    "cumulativeDataPoints": [
      { "label": "2026-06-29", "count": 1005 },
      { "label": "2026-06-30", "count": 1017 },
      { "label": "2026-07-01", "count": 1025 }
    ],
    "totalUsersAsOfRangeEnd": 1025,
    "roleBreakdown": [
      { "category": "REGULAR", "count": 20, "percentage": 80.0 },
      { "category": "BROKER", "count": 5, "percentage": 20.0 }
    ],
    "brokerVerificationBreakdown": [
      { "category": "PENDING", "count": 3, "percentage": 60.0 },
      { "category": "APPROVED", "count": 2, "percentage": 40.0 }
    ]
  }
}
```

Notes:
- `brokerVerificationBreakdown` can be an empty array when no brokers registered in the window — render a "no broker signups" empty state rather than an empty pie.
- `cumulativeDataPoints` lets you draw a combo chart: bars for new signups + a line for the running total (classic growth-curve pattern).

### Suggested charts
1. **Combo bar + line** — `dataPoints.count` as bars, `cumulativeDataPoints.count` as a secondary-axis line.
2. **Donut** — `roleBreakdown` (Regular vs Broker).
3. **Donut/stacked bar** — `brokerVerificationBreakdown` (broker approval funnel).

```tsx
'use client';

import {
  ComposedChart, Bar, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';

function formatXLabel(label: string, granularity: string) {
  if (granularity === 'MONTH') {
    const [year, month] = label.split('-');
    return `T${month}/${year}`;
  }
  return label.slice(5);
}

export function UserGrowthChart({ data }: { data: AdminUserAnalyticsResponse }) {
  const merged = data.dataPoints.map((dp, i) => ({
    label: dp.label,
    newUsers: dp.count,
    totalUsers: data.cumulativeDataPoints[i]?.count ?? null,
  }));

  return (
    <ResponsiveContainer width="100%" height={320}>
      <ComposedChart data={merged}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="label" tickFormatter={(v) => formatXLabel(v, data.granularity)} />
        <YAxis yAxisId="left" allowDecimals={false} />
        <YAxis yAxisId="right" orientation="right" allowDecimals={false} />
        <Tooltip />
        <Legend />
        <Bar yAxisId="left" dataKey="newUsers" name="Người dùng mới" fill="#6366f1" radius={[4, 4, 0, 0]} />
        <Line yAxisId="right" dataKey="totalUsers" name="Tổng người dùng" stroke="#f59e0b" strokeWidth={2} dot={false} />
      </ComposedChart>
    </ResponsiveContainer>
  );
}
```

```tsx
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const ROLE_LABELS: Record<string, string> = { REGULAR: 'Người dùng thường', BROKER: 'Môi giới' };
const ROLE_COLORS: Record<string, string> = { REGULAR: '#6366f1', BROKER: '#10b981' };

const BROKER_STATUS_LABELS: Record<string, string> = {
  NONE: 'Chưa đăng ký', PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Bị từ chối',
};
const BROKER_STATUS_COLORS: Record<string, string> = {
  NONE: '#94a3b8', PENDING: '#f59e0b', APPROVED: '#10b981', REJECTED: '#ef4444',
};

export function BreakdownDonut({
  data, labels, colors, emptyMessage,
}: {
  data: CategoryBreakdownItem[];
  labels: Record<string, string>;
  colors: Record<string, string>;
  emptyMessage: string;
}) {
  if (data.length === 0) return <div style={{ color: '#64748b', fontSize: 14 }}>{emptyMessage}</div>;

  return (
    <ResponsiveContainer width="100%" height={260}>
      <PieChart>
        <Pie
          data={data}
          dataKey="count"
          nameKey="category"
          cx="50%" cy="50%"
          innerRadius={60} outerRadius={95}
          paddingAngle={3}
          label={({ category, percentage }) => `${labels[category] ?? category} (${percentage.toFixed(1)}%)`}
        >
          {data.map((d) => <Cell key={d.category} fill={colors[d.category] ?? '#e2e8f0'} />)}
        </Pie>
        <Tooltip formatter={(value: number, _n, entry) => [`${value}`, labels[entry.payload.category] ?? entry.payload.category]} />
        <Legend formatter={(value) => labels[value] ?? value} />
      </PieChart>
    </ResponsiveContainer>
  );
}

// Usage:
// <BreakdownDonut data={data.roleBreakdown} labels={ROLE_LABELS} colors={ROLE_COLORS} emptyMessage="Chưa có người dùng mới" />
// <BreakdownDonut data={data.brokerVerificationBreakdown} labels={BROKER_STATUS_LABELS} colors={BROKER_STATUS_COLORS} emptyMessage="Chưa có môi giới đăng ký mới" />
```

---

## 3. Listings — `GET /v1/admin/analytics/listings`

```ts
interface AdminListingAnalyticsResponse {
  dataPoints: TimeSeriesDataPoint[];             // new listings per bucket (excludes drafts & shadow listings)
  total: number;
  granularity: 'DAY' | 'MONTH';

  cumulativeDataPoints: TimeSeriesDataPoint[];
  totalListingsAsOfRangeEnd: number;

  listingTypeBreakdown: CategoryBreakdownItem[];   // category: 'RENT' | 'SALE' | 'SHARE'
  productTypeBreakdown: CategoryBreakdownItem[];   // category: 'ROOM' | 'APARTMENT' | 'HOUSE' | 'OFFICE' | 'STUDIO' | 'STORE'
  verificationBreakdown: CategoryBreakdownItem[];  // category: 'VERIFIED' | 'UNVERIFIED' — current status of listings created in range
}
```

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "label": "2026-06-29", "count": 10 },
      { "label": "2026-06-30", "count": 15 },
      { "label": "2026-07-01", "count": 7 }
    ],
    "total": 32,
    "granularity": "DAY",
    "cumulativeDataPoints": [
      { "label": "2026-06-29", "count": 4210 },
      { "label": "2026-06-30", "count": 4225 },
      { "label": "2026-07-01", "count": 4232 }
    ],
    "totalListingsAsOfRangeEnd": 4232,
    "listingTypeBreakdown": [
      { "category": "RENT", "count": 24, "percentage": 75.0 },
      { "category": "SALE", "count": 6, "percentage": 18.75 },
      { "category": "SHARE", "count": 2, "percentage": 6.25 }
    ],
    "productTypeBreakdown": [
      { "category": "ROOM", "count": 18, "percentage": 56.25 },
      { "category": "APARTMENT", "count": 10, "percentage": 31.25 },
      { "category": "HOUSE", "count": 4, "percentage": 12.5 }
    ],
    "verificationBreakdown": [
      { "category": "VERIFIED", "count": 20, "percentage": 62.5 },
      { "category": "UNVERIFIED", "count": 12, "percentage": 37.5 }
    ]
  }
}
```

Note: `verificationBreakdown` reflects the **current** `verified` flag of listings created in the window (a snapshot at query time), not their status at creation time — a listing created 3 days ago and approved yesterday counts as `VERIFIED`.

### Suggested charts
1. **Bar/area** on `dataPoints` + `cumulativeDataPoints` (same combo pattern as Users).
2. **Pie** on `listingTypeBreakdown`.
3. **Horizontal bar** on `productTypeBreakdown` (6 categories reads better as a bar than a donut).
4. **Simple 2-segment donut or progress bar** on `verificationBreakdown`.

```tsx
const LISTING_TYPE_LABELS: Record<string, string> = { RENT: 'Cho thuê', SALE: 'Bán', SHARE: 'Ở ghép' };
const LISTING_TYPE_COLORS: Record<string, string> = { RENT: '#6366f1', SALE: '#f59e0b', SHARE: '#10b981' };

const PRODUCT_TYPE_LABELS: Record<string, string> = {
  ROOM: 'Phòng trọ', APARTMENT: 'Căn hộ', HOUSE: 'Nhà nguyên căn',
  OFFICE: 'Văn phòng', STUDIO: 'Studio', STORE: 'Mặt bằng kinh doanh',
};
const PRODUCT_TYPE_COLORS: Record<string, string> = {
  ROOM: '#6366f1', APARTMENT: '#8b5cf6', HOUSE: '#f59e0b',
  OFFICE: '#3b82f6', STUDIO: '#10b981', STORE: '#ef4444',
};

const VERIFICATION_LABELS: Record<string, string> = { VERIFIED: 'Đã xác minh', UNVERIFIED: 'Chưa xác minh' };
const VERIFICATION_COLORS: Record<string, string> = { VERIFIED: '#10b981', UNVERIFIED: '#94a3b8' };

export function ProductTypeBarChart({ data }: { data: CategoryBreakdownItem[] }) {
  const chartData = data.map((d) => ({ ...d, name: PRODUCT_TYPE_LABELS[d.category] ?? d.category }));
  return (
    <ResponsiveContainer width="100%" height={240}>
      <BarChart data={chartData} layout="vertical">
        <XAxis type="number" allowDecimals={false} />
        <YAxis type="category" dataKey="name" width={130} />
        <Tooltip formatter={(v: number) => [`${v} tin đăng`, 'Số lượng']} />
        <Bar dataKey="count" radius={[0, 4, 4, 0]}>
          {chartData.map((d) => <Cell key={d.category} fill={PRODUCT_TYPE_COLORS[d.category] ?? '#94a3b8'} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
```

(`BarChart`, `XAxis`, `YAxis`, `Tooltip`, `Cell` imported from `recharts` as in earlier snippets. Reuse `BreakdownDonut` from §2 for `listingTypeBreakdown` and `verificationBreakdown` with the label/color maps above.)

---

## 4. Reports — `GET /v1/admin/analytics/reports`

```ts
interface AdminReportAnalyticsResponse {
  dataPoints: TimeSeriesDataPoint[];      // reports filed per bucket
  total: number;
  granularity: 'DAY' | 'MONTH';

  cumulativeDataPoints: TimeSeriesDataPoint[];

  categoryBreakdown: CategoryBreakdownItem[]; // category: 'LISTING' | 'MAP'
  statusBreakdown: CategoryBreakdownItem[];   // category: 'PENDING' | 'RESOLVED' | 'REJECTED'

  resolutionRatePercent: number;  // (RESOLVED + REJECTED) / total * 100, for reports filed in range
  avgResolutionHours: number | null; // avg hours between creation and resolution, for reports filed in range that have been resolved; null if none resolved yet
}
```

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "label": "2026-06-29", "count": 2 },
      { "label": "2026-06-30", "count": 0 },
      { "label": "2026-07-01", "count": 5 }
    ],
    "total": 7,
    "granularity": "DAY",
    "cumulativeDataPoints": [
      { "label": "2026-06-29", "count": 102 },
      { "label": "2026-06-30", "count": 102 },
      { "label": "2026-07-01", "count": 107 }
    ],
    "categoryBreakdown": [
      { "category": "LISTING", "count": 5, "percentage": 71.43 },
      { "category": "MAP", "count": 2, "percentage": 28.57 }
    ],
    "statusBreakdown": [
      { "category": "PENDING", "count": 4, "percentage": 57.14 },
      { "category": "RESOLVED", "count": 2, "percentage": 28.57 },
      { "category": "REJECTED", "count": 1, "percentage": 14.29 }
    ],
    "resolutionRatePercent": 42.86,
    "avgResolutionHours": 5.75
  }
}
```

Notes:
- `resolutionRatePercent` and `avgResolutionHours` are both scoped to reports **filed** within `[from, to]` — a report filed 2 days ago and resolved today counts toward this window's rate, not the resolution day's window.
- `avgResolutionHours` is `null` (omitted from JSON, since DTOs use `@JsonInclude(NON_NULL)`) when nothing in the window has been resolved yet — render a "—" / "N/A", not `0h`.

### Suggested charts
1. **Line/bar** on `dataPoints` + `cumulativeDataPoints`.
2. **Donut** on `categoryBreakdown` (Listing vs Map issues).
3. **Stacked bar or donut** on `statusBreakdown`.
4. **Two KPI tiles**: `resolutionRatePercent` (with a small radial/progress indicator) and `avgResolutionHours` (formatted, e.g. "5.8h" or "2.3 ngày" if > 24h).

```tsx
const REPORT_CATEGORY_LABELS: Record<string, string> = { LISTING: 'Tin đăng', MAP: 'Bản đồ' };
const REPORT_CATEGORY_COLORS: Record<string, string> = { LISTING: '#6366f1', MAP: '#f59e0b' };

const REPORT_STATUS_LABELS: Record<string, string> = { PENDING: 'Chờ xử lý', RESOLVED: 'Đã xử lý', REJECTED: 'Đã từ chối' };
const REPORT_STATUS_COLORS: Record<string, string> = { PENDING: '#f59e0b', RESOLVED: '#10b981', REJECTED: '#94a3b8' };

function formatResolutionTime(hours: number | null): string {
  if (hours == null) return '—';
  if (hours < 24) return `${hours.toFixed(1)}h`;
  return `${(hours / 24).toFixed(1)} ngày`;
}

export function ReportKpiTiles({ data }: { data: AdminReportAnalyticsResponse }) {
  return (
    <div style={{ display: 'flex', gap: 16 }}>
      <div style={{ padding: 16, borderRadius: 12, background: '#f8fafc', border: '1px solid #e2e8f0', minWidth: 180 }}>
        <div style={{ fontSize: 13, color: '#64748b' }}>Tỉ lệ xử lý</div>
        <div style={{ fontSize: 24, fontWeight: 700, color: '#0f172a' }}>{data.resolutionRatePercent.toFixed(1)}%</div>
      </div>
      <div style={{ padding: 16, borderRadius: 12, background: '#f8fafc', border: '1px solid #e2e8f0', minWidth: 180 }}>
        <div style={{ fontSize: 13, color: '#64748b' }}>Thời gian xử lý TB</div>
        <div style={{ fontSize: 24, fontWeight: 700, color: '#0f172a' }}>{formatResolutionTime(data.avgResolutionHours)}</div>
      </div>
    </div>
  );
}
```

(Reuse `BreakdownDonut` from §2 for `categoryBreakdown` / `statusBreakdown`, and the combo bar+line pattern from §2 for `dataPoints` + `cumulativeDataPoints`.)

---

## 5. Shared UI pieces

### Preset + custom range selector

Works for all four endpoints — pass the resulting query straight into `fetchAnalytics`.

```tsx
'use client';

import { useState } from 'react';

type Mode =
  | { kind: 'preset'; days: 7 | 30 | 360 }
  | { kind: 'custom'; from: string; to: string };

function toQuery(mode: Mode) {
  return mode.kind === 'preset' ? { days: mode.days } : { from: mode.from, to: mode.to };
}

export function AnalyticsRangeSelector({ value, onChange }: { value: Mode; onChange: (m: Mode) => void }) {
  const today = new Date().toISOString().slice(0, 10);

  return (
    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
      {[7, 30, 360].map((d) => (
        <button
          key={d}
          onClick={() => onChange({ kind: 'preset', days: d as 7 | 30 | 360 })}
          style={btnStyle(value.kind === 'preset' && value.days === d)}
        >
          {d === 360 ? '12 tháng' : `${d} ngày`}
        </button>
      ))}
      <button onClick={() => onChange({ kind: 'custom', from: today, to: today })} style={btnStyle(value.kind === 'custom')}>
        Tuỳ chỉnh
      </button>
      {value.kind === 'custom' && (
        <div style={{ display: 'flex', gap: 6, marginLeft: 12 }}>
          <input type="date" value={value.from} max={value.to} onChange={(e) => onChange({ ...value, from: e.target.value })} />
          <span>—</span>
          <input type="date" value={value.to} min={value.from} max={today} onChange={(e) => onChange({ ...value, to: e.target.value })} />
        </div>
      )}
    </div>
  );
}

function btnStyle(active: boolean): React.CSSProperties {
  return {
    padding: '6px 14px', borderRadius: 8,
    border: active ? '2px solid #6366f1' : '1px solid #e2e8f0',
    background: active ? '#eef2ff' : '#fff',
    color: active ? '#6366f1' : '#64748b',
    fontWeight: active ? 600 : 400, cursor: 'pointer', fontSize: 13,
  };
}
```

Wiring example for the Users tab:

```tsx
export function UsersAnalyticsPanel({ token }: { token: string }) {
  const [mode, setMode] = useState<Mode>({ kind: 'preset', days: 7 });
  const [data, setData] = useState<AdminUserAnalyticsResponse | null>(null);

  useEffect(() => {
    fetchAnalytics<AdminUserAnalyticsResponse>(token, 'users', toQuery(mode)).then(setData).catch(console.error);
  }, [token, mode]);

  if (!data) return <div>Loading...</div>;

  return (
    <div>
      <AnalyticsRangeSelector value={mode} onChange={setMode} />
      <UserGrowthChart data={data} />
      <div style={{ display: 'flex', gap: 24 }}>
        <BreakdownDonut data={data.roleBreakdown} labels={ROLE_LABELS} colors={ROLE_COLORS} emptyMessage="Chưa có người dùng mới" />
        <BreakdownDonut data={data.brokerVerificationBreakdown} labels={BROKER_STATUS_LABELS} colors={BROKER_STATUS_COLORS} emptyMessage="Chưa có môi giới đăng ký mới" />
      </div>
    </div>
  );
}
```

### Gap-filling for continuous time series

Empty buckets are omitted from `dataPoints`/`cumulativeDataPoints`, not back-filled with zeros. Fill gaps client-side for smooth line/area charts:

```ts
function fillGaps(points: TimeSeriesDataPoint[], from: string, to: string): TimeSeriesDataPoint[] {
  const map = new Map(points.map((p) => [p.label, p.count]));
  const out: TimeSeriesDataPoint[] = [];
  const start = new Date(from);
  const end = new Date(to);
  for (let d = start; d <= end; d.setDate(d.getDate() + 1)) {
    const label = d.toISOString().slice(0, 10);
    out.push({ label, count: map.get(label) ?? 0 });
  }
  return out;
}
```

Do **not** zero-fill `cumulativeDataPoints` the same way — a missing bucket there should carry forward the previous cumulative value, not drop to 0. Use a forward-fill instead:

```ts
function forwardFillCumulative(points: TimeSeriesDataPoint[], from: string, to: string, baseline: number): TimeSeriesDataPoint[] {
  const map = new Map(points.map((p) => [p.label, p.count]));
  const out: TimeSeriesDataPoint[] = [];
  let last = baseline;
  const start = new Date(from);
  const end = new Date(to);
  for (let d = start; d <= end; d.setDate(d.getDate() + 1)) {
    const label = d.toISOString().slice(0, 10);
    last = map.get(label) ?? last;
    out.push({ label, count: last });
  }
  return out;
}
```

---

## 6. Error handling

```json
{ "code": "ERROR_CODE", "message": "Human-readable error message", "data": null }
```

| HTTP | When |
|------|------|
| 400 | `from`/`to` not `YYYY-MM-DD`, or `from > to` |
| 401 | Missing/invalid Bearer token |
| 403 | Token is not an admin |

---

## 7. Quick reference

| Endpoint | Method | Params | Granularity |
|----------|--------|--------|-------------|
| `/v1/admin/analytics/revenue` | GET | `days` (7/30/360) **or** `from`+`to` | day / month |
| `/v1/admin/analytics/users` | GET | `days` (7/30/360) **or** `from`+`to` | day / month |
| `/v1/admin/analytics/reports` | GET | `days` (7/30/360) **or** `from`+`to` | day / month |
| `/v1/admin/analytics/listings` | GET | `days` (7/30/360) **or** `from`+`to` | day / month |
| `/v1/admin/analytics/memberships/distribution` | GET | none | n/a (snapshot) |
