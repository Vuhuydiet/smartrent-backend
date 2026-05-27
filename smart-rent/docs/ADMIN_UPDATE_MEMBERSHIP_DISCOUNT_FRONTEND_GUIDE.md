# Admin - Update Membership Package Discount: Frontend Integration Guide

## Overview

Admins can update the **discount percentage** of any membership package. The server automatically recomputes the **sale price** from the package's `originalPrice` and the new `discountPercentage`. The admin UI should therefore **only ask the admin for `discountPercentage`** (and optionally other editable fields). It should **never** ask the admin to type in a sale price.

Formula used by the server:

```
salePrice = round(originalPrice * (1 - discountPercentage / 100))
```

- `discountPercentage` is a number from `0` to `100` (decimals allowed, e.g. `12.5`).
- `salePrice` is rounded to 0 decimal places (VND is a no-decimal currency in this project).
- If `discountPercentage = 0`, then `salePrice = originalPrice`.

## Endpoint

```
PUT /v1/admin/memberships/packages/{membershipId}
```

**Auth:** `Authorization: Bearer <admin_jwt_token>`
**Required roles (any of):** `ROLE_SA`, `ROLE_UA`, `ROLE_SPA`

### Path parameters

| Name           | Type   | Required | Description                            |
| -------------- | ------ | -------- | -------------------------------------- |
| `membershipId` | Long   | Yes      | The ID of the membership package to update. |

### Request body

All fields are optional. Only include the fields you actually want to change.

| Field                 | Type        | Notes                                                                                  |
| --------------------- | ----------- | -------------------------------------------------------------------------------------- |
| `packageName`         | string      | Max 100 chars.                                                                         |
| `packageLevel`        | string      | One of `BASIC`, `STANDARD`, `ADVANCED`.                                                |
| `durationMonths`      | integer     | Must be ≥ 1.                                                                           |
| `originalPrice`       | number      | Must be > 0. Triggers recomputation of `salePrice`.                                    |
| `discountPercentage`  | number      | **0 - 100**, decimals allowed. Triggers recomputation of `salePrice`.                  |
| `isActive`            | boolean     | Toggle package visibility.                                                             |
| `description`         | string      | Free-form description.                                                                 |

> **Do NOT send `salePrice`.** It is computed by the server. Any value you send will be ignored.

### Recompute rules

The server recomputes `salePrice` whenever `originalPrice` **or** `discountPercentage` is included in the request:

| Sent fields                        | Server behaviour                                                                  |
| ---------------------------------- | --------------------------------------------------------------------------------- |
| only `discountPercentage`          | Uses existing `originalPrice` from DB to recompute `salePrice`.                   |
| only `originalPrice`               | Uses existing `discountPercentage` from DB to recompute `salePrice`.              |
| both                               | Uses the new values to recompute `salePrice`.                                     |
| neither                            | `salePrice` is left unchanged.                                                    |

## Typical request (admin only changes discount)

```http
PUT /v1/admin/memberships/packages/3
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json
```

```json
{
  "discountPercentage": 25
}
```

**Effect:** if the package had `originalPrice = 1200000`, the server stores
`salePrice = 1200000 * (1 - 25/100) = 900000`.

## Successful response (200)

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "membershipId": 3,
    "packageCode": "ADV_12M",
    "packageName": "Premium 12 Months",
    "packageLevel": "ADVANCED",
    "durationMonths": 12,
    "originalPrice": 1200000,
    "salePrice": 900000,
    "discountPercentage": 25.00,
    "isActive": true,
    "description": "Premium membership with all features for 12 months",
    "benefits": [ /* ... */ ],
    "createdAt": "2026-01-10T08:30:00",
    "updatedAt": "2026-05-27T14:22:11"
  }
}
```

The response always echoes back the newly computed `salePrice` and the stored `discountPercentage`. The frontend should use these values to refresh the package row in the admin table without re-fetching.

## Validation errors (400)

The server validates `discountPercentage` and `originalPrice` server-side. The frontend should mirror these to give instant feedback.

| Case                                              | Server message                                            |
| ------------------------------------------------- | --------------------------------------------------------- |
| `discountPercentage` < 0                          | `Discount percentage must be at least 0`                  |
| `discountPercentage` > 100                        | `Discount percentage must not exceed 100`                 |
| `originalPrice` ≤ 0                               | `Original price must be greater than 0`                   |
| `packageName` longer than 100 chars               | `Package name must not exceed 100 characters`             |
| `durationMonths` < 1                              | `Duration must be at least 1 month`                       |
| Invalid `packageLevel`                            | `Invalid package level: <value>. Must be one of: BASIC, STANDARD, ADVANCED` |

## Other errors

| Status | Code     | Meaning                                  |
| ------ | -------- | ---------------------------------------- |
| 401    | -        | Missing/invalid bearer token.            |
| 403    | -        | Admin token, but missing required role.  |
| 404    | `4015`   | Membership package not found.            |

```json
{
  "code": "4015",
  "message": "Membership package not found"
}
```

## Suggested admin UI

A minimal "Edit discount" modal that uses this endpoint:

```
┌──────────────────────────────────────────────┐
│  Edit discount — Premium 12 Months           │
├──────────────────────────────────────────────┤
│  Original price (VND)   : 1,200,000  (readonly) │
│                                              │
│  Discount %             : [   25   ] %       │
│                                              │
│  Sale price (preview)   : 900,000  ←─ client-side preview │
│  You save               : 300,000            │
│                                              │
│              [ Cancel ]   [ Save ]           │
└──────────────────────────────────────────────┘
```

- Show `originalPrice` read-only (or as a separate edit).
- Show `discountPercentage` as a number input, `min=0`, `max=100`, `step=0.01`.
- Render a **client-side preview** of `salePrice` using the same formula so the admin can see the result before saving.
- On save, send only the fields the admin actually changed.

### Client-side preview helper (TypeScript)

```ts
export function previewSalePrice(
  originalPrice: number,
  discountPercentage: number
): number {
  const clamped = Math.min(100, Math.max(0, discountPercentage || 0));
  return Math.round(originalPrice * (1 - clamped / 100));
}
```

> The server is the source of truth — always update your local state from the response, never from the preview.

## Example: React + fetch

```tsx
import { useState } from "react";

type MembershipPackage = {
  membershipId: number;
  packageName: string;
  originalPrice: number;
  salePrice: number;
  discountPercentage: number;
  // ...other fields
};

async function updateDiscount(
  membershipId: number,
  discountPercentage: number,
  token: string
): Promise<MembershipPackage> {
  const res = await fetch(
    `/v1/admin/memberships/packages/${membershipId}`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ discountPercentage }),
    }
  );

  const body = await res.json();
  if (!res.ok) {
    throw new Error(body?.message ?? "Failed to update discount");
  }
  return body.data as MembershipPackage;
}

export function EditDiscountModal({
  pkg,
  onSaved,
  token,
}: {
  pkg: MembershipPackage;
  onSaved: (updated: MembershipPackage) => void;
  token: string;
}) {
  const [discount, setDiscount] = useState<number>(pkg.discountPercentage);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const preview = Math.round(pkg.originalPrice * (1 - (discount || 0) / 100));

  const onSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const updated = await updateDiscount(pkg.membershipId, discount, token);
      onSaved(updated);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <h3>Edit discount — {pkg.packageName}</h3>

      <div>Original price: {pkg.originalPrice.toLocaleString()} VND</div>

      <label>
        Discount %
        <input
          type="number"
          min={0}
          max={100}
          step={0.01}
          value={discount}
          onChange={(e) => setDiscount(Number(e.target.value))}
        />
      </label>

      <div>Sale price (preview): {preview.toLocaleString()} VND</div>
      <div>You save: {(pkg.originalPrice - preview).toLocaleString()} VND</div>

      {error && <div style={{ color: "red" }}>{error}</div>}

      <button disabled={saving} onClick={onSave}>
        {saving ? "Saving…" : "Save"}
      </button>
    </div>
  );
}
```

## Example: cURL

```bash
curl -X PUT \
  "https://api.smartrent.example.com/v1/admin/memberships/packages/3" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "discountPercentage": 25 }'
```

## FAQ

**Q: What if the admin wants the package to have NO discount?**
Send `discountPercentage: 0`. The server will set `salePrice = originalPrice`.

**Q: What if the admin sends `salePrice` anyway?**
The field has been removed from the update DTO; it will simply be ignored by the server.

**Q: Will updating discount affect users who already purchased the package?**
No. Existing `UserMembership` records store the price they actually paid (`totalPaid`). Changing a package's discount only affects **future** purchases / upgrades.

**Q: How is the discount applied to the user-facing membership upgrade flow?**
This endpoint only updates the package definition. Upgrade-time pro-rating is a separate calculation and is documented in `MEMBERSHIP_UPGRADE_FRONTEND_GUIDE.md`.
