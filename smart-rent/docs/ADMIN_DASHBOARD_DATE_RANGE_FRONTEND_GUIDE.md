# Admin Dashboard — Custom Date Range Filter (Frontend Integration Guide)

> **Backend base URL**: `https://dev.api.smartrent.io.vn` or `http://localhost:8080`
>
> **Authentication**: All endpoints require a valid **Admin** Bearer token.
>
> **Companion guide**: see [ADMIN_DASHBOARD_CHARTS_FRONTEND_GUIDE.md](ADMIN_DASHBOARD_CHARTS_FRONTEND_GUIDE.md) for the full chart-rendering walkthrough.

---

## 1. What changed

The three time-series dashboard endpoints now accept **custom date ranges** via `from`/`to` query parameters, in addition to the existing `days` preset.

| Endpoint | Previously | Now |
|---|---|---|
| `GET /v1/admin/dashboard/users/growth` | `days` only | `days` **or** `from`+`to` |
| `GET /v1/admin/dashboard/reports/count` | `days` only | `days` **or** `from`+`to` |
| `GET /v1/admin/dashboard/listings/creation` | `days` only | `days` **or** `from`+`to` |

The revenue endpoint (`/v1/admin/dashboard/revenue`) already supported `from`/`to` — that contract is unchanged.

---

## 2. Parameter reference

| Param  | Type           | Required | Default                        | Notes |
|--------|----------------|----------|--------------------------------|-------|
| `days` | `int`          | No       | `7` (when no params at all)    | Preset range: `7`, `30`, `360`. Takes priority over `from`/`to`. |
| `from` | `YYYY-MM-DD`   | No       | 7 days before `to`             | Used only when `days` is absent. |
| `to`   | `YYYY-MM-DD`   | No       | Today                          | Used only when `days` is absent. |

### Precedence rules

1. If `days` is provided → use the preset range, ignore `from`/`to`.
2. If `days` is absent **and** either `from` or `to` is provided → use the custom range.
3. If nothing is provided → defaults to last 7 days (`days=7`).

### Granularity

| Mode | Granularity |
|------|-------------|
| `days=7` or `days=30` | **Day** |
| `days=360` | **Month** |
| Custom `from`/`to` | **Day** (always) |

Date boundaries are **inclusive on both ends**. `from` is interpreted as start-of-day; `to` is interpreted as end-of-day, in server time.

---

## 3. Request examples

```http
# Preset (unchanged)
GET /v1/admin/dashboard/users/growth?days=30

# Custom range — both ends specified
GET /v1/admin/dashboard/users/growth?from=2026-05-01&to=2026-05-24

# Custom range — only "from" (to defaults to today)
GET /v1/admin/dashboard/reports/count?from=2026-05-01

# Custom range — only "to" (from defaults to 7 days before to)
GET /v1/admin/dashboard/listings/creation?to=2026-04-30
```

All variants return the same `TimeSeriesResponse` shape — no response change.

---

## 4. Response shape (unchanged)

```json
{
  "code": "999999",
  "data": {
    "dataPoints": [
      { "label": "2026-05-01", "count": 12 },
      { "label": "2026-05-02", "count": 8 },
      { "label": "2026-05-03", "count": 15 }
    ],
    "total": 35,
    "granularity": "DAY"
  }
}
```

```ts
interface TimeSeriesDataPoint {
  label: string;   // "2026-05-01" for DAY, "2026-05" for MONTH
  count: number;
}

interface TimeSeriesResponse {
  dataPoints: TimeSeriesDataPoint[];
  total: number;
  granularity: 'DAY' | 'MONTH';
}
```

---

## 5. Updated fetch helpers

A single options shape works for all three endpoints. `days` wins if both are sent, so prefer to send **one or the other**.

```ts
interface DashboardTimeQuery {
  days?: number;          // 7 | 30 | 360
  from?: string;          // "YYYY-MM-DD"
  to?: string;            // "YYYY-MM-DD"
}

function buildQuery(opts?: DashboardTimeQuery): string {
  const params = new URLSearchParams();
  if (opts?.days != null) {
    params.set('days', String(opts.days));
  } else {
    if (opts?.from) params.set('from', opts.from);
    if (opts?.to)   params.set('to',   opts.to);
  }
  const qs = params.toString();
  return qs ? `?${qs}` : '';
}

async function fetchTimeSeries(
  token: string,
  path: '/users/growth' | '/reports/count' | '/listings/creation',
  opts?: DashboardTimeQuery
): Promise<TimeSeriesResponse> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/v1/admin/dashboard${path}${buildQuery(opts)}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  if (json.code !== '999999') throw new Error(json.message);
  return json.data;
}

// Convenience wrappers
export const fetchUserGrowth      = (t: string, o?: DashboardTimeQuery) => fetchTimeSeries(t, '/users/growth', o);
export const fetchReportCount     = (t: string, o?: DashboardTimeQuery) => fetchTimeSeries(t, '/reports/count', o);
export const fetchListingCreation = (t: string, o?: DashboardTimeQuery) => fetchTimeSeries(t, '/listings/creation', o);
```

Usage:

```ts
// Preset
const last30 = await fetchUserGrowth(token, { days: 30 });

// Custom range
const may = await fetchUserGrowth(token, { from: '2026-05-01', to: '2026-05-24' });
```

---

## 6. UI pattern — preset + custom range selector

A common layout is a segmented control for presets plus a "Custom" mode that swaps in two date inputs.

```tsx
'use client';

import { useEffect, useState } from 'react';

type Mode =
  | { kind: 'preset'; days: 7 | 30 | 360 }
  | { kind: 'custom'; from: string; to: string };

function toQuery(mode: Mode): DashboardTimeQuery {
  return mode.kind === 'preset'
    ? { days: mode.days }
    : { from: mode.from, to: mode.to };
}

export function DashboardRangeSelector({
  value,
  onChange,
}: {
  value: Mode;
  onChange: (m: Mode) => void;
}) {
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
      <button
        onClick={() =>
          onChange({
            kind: 'custom',
            from: today,
            to: today,
          })
        }
        style={btnStyle(value.kind === 'custom')}
      >
        Tuỳ chỉnh
      </button>

      {value.kind === 'custom' && (
        <div style={{ display: 'flex', gap: 6, marginLeft: 12 }}>
          <input
            type="date"
            value={value.from}
            max={value.to}
            onChange={(e) => onChange({ ...value, from: e.target.value })}
          />
          <span>—</span>
          <input
            type="date"
            value={value.to}
            min={value.from}
            max={today}
            onChange={(e) => onChange({ ...value, to: e.target.value })}
          />
        </div>
      )}
    </div>
  );
}

function btnStyle(active: boolean): React.CSSProperties {
  return {
    padding: '6px 14px',
    borderRadius: 8,
    border: active ? '2px solid #6366f1' : '1px solid #e2e8f0',
    background: active ? '#eef2ff' : '#fff',
    color: active ? '#6366f1' : '#64748b',
    fontWeight: active ? 600 : 400,
    cursor: 'pointer',
    fontSize: 13,
  };
}
```

Wiring it into one of the existing charts:

```tsx
export function UserGrowthChart({ token }: { token: string }) {
  const [mode, setMode] = useState<Mode>({ kind: 'preset', days: 7 });
  const [data, setData] = useState<TimeSeriesResponse | null>(null);

  useEffect(() => {
    fetchUserGrowth(token, toQuery(mode)).then(setData).catch(console.error);
  }, [token, mode]);

  // ...render header + chart (see ADMIN_DASHBOARD_CHARTS_FRONTEND_GUIDE.md §3)
}
```

---

## 7. Validation & error handling

Server-side validation already enforces these. The frontend should mirror them so users get instant feedback:

| Rule | Reason |
|------|--------|
| `from <= to` | Otherwise the response is an empty array. |
| `to` not in the future | Future dates produce empty buckets. |
| Both `from` and `to` are valid `YYYY-MM-DD` | Malformed dates return `400`. |

Error response shape (unchanged):

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "data": null
}
```

| HTTP | When |
|------|------|
| 400  | `from` or `to` is not `YYYY-MM-DD`. |
| 401  | Missing/invalid Bearer token. |
| 403  | Token is not an admin. |

---

## 8. Migration notes

- **No breaking change.** Existing `?days=7` callers continue to work identically — the previous default (`days=7`) is preserved.
- **Empty buckets are not back-filled.** If a date in the requested range has zero events, the `dataPoints` array simply omits that date. The frontend should fill gaps when rendering continuous time-series charts:

  ```ts
  function fillGaps(
    points: TimeSeriesDataPoint[],
    from: string,
    to: string,
  ): TimeSeriesDataPoint[] {
    const map = new Map(points.map((p) => [p.label, p.count]));
    const out: TimeSeriesDataPoint[] = [];
    const start = new Date(from);
    const end   = new Date(to);
    for (let d = start; d <= end; d.setDate(d.getDate() + 1)) {
      const label = d.toISOString().slice(0, 10);
      out.push({ label, count: map.get(label) ?? 0 });
    }
    return out;
  }
  ```

---

## 9. Quick reference

| Endpoint | Method | Params | Granularity |
|----------|--------|--------|-------------|
| `/v1/admin/dashboard/users/growth`      | GET | `days` (7/30/360) **or** `from`+`to` | day / month / day |
| `/v1/admin/dashboard/reports/count`     | GET | `days` (7/30/360) **or** `from`+`to` | day / month / day |
| `/v1/admin/dashboard/listings/creation` | GET | `days` (7/30/360) **or** `from`+`to` | day / month / day |
| `/v1/admin/dashboard/revenue`           | GET | `days` (7/30/360) **or** `from`+`to` | day / month / day |
