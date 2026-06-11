# SePay Membership Upgrade 404 — Frontend Fix

> **Symptom:** Purchasing a membership with SePay works, but **upgrading** a membership with SePay
> sends the browser to `https://pay-sandbox.sepay.vn/v1/checkout/init` as a **`GET`** and gets
> **`404 Not Found`**.
>
> **Cause:** SePay's hosted checkout is **POST-only** (the browser must submit a signed hidden form —
> see [`SEPAY_INTEGRATION_GUIDE.md`](./SEPAY_INTEGRATION_GUIDE.md)). The upgrade flow was redirecting
> with `window.location.href = paymentUrl`, i.e. a `GET` to the checkout URL → 404.
>
> **Backend is already fixed.** `POST /v1/memberships/initiate-upgrade` now returns `providerData`
> (the same `{ method, checkoutUrl, fields }` block the purchase flow returns). **The frontend must
> stop GET-redirecting to `paymentUrl` and instead POST the `providerData.fields` form** — exactly
> like the purchase flow already does.

---

## TL;DR — what to change

In the upgrade confirm handler, **do not** do `window.location.href = result.paymentUrl` for SePay.
Reuse the **same** hidden-form-POST helper the purchase flow uses, fed from `result.providerData`.

```diff
  const result = await initiateUpgrade(targetPackageId);

- if (result.paymentUrl) {
-   window.location.href = result.paymentUrl; // ❌ GET → SePay 404
- } else {
-   toast.success(result.message); // free upgrade, no payment
-   refreshMembership();
- }
+ if (result.status === 'COMPLETED' || result.finalAmount === 0) {
+   // Free upgrade (discount covered the price) — no payment needed
+   toast.success(result.message);
+   refreshMembership();
+ } else if (result.providerData?.checkoutUrl) {
+   // SePay (and any POST-based hosted checkout): submit the signed form
+   localStorage.setItem('txnRef', result.transactionRef); // for polling on the result page
+   redirectToSePay(result);            // same helper as the purchase flow
+ } else if (result.paymentUrl) {
+   // Legacy GET-redirect gateways (e.g. VNPay)
+   window.location.href = result.paymentUrl;
+ }
```

That's the whole fix. The rest of this doc is detail.

---

## Why purchase worked but upgrade didn't

| Flow | Endpoint | Response carried `providerData`? | Result |
|---|---|---|---|
| Purchase | `POST /v1/memberships/initiate-purchase` | ✅ yes (full `PaymentResponse`) | FE POSTed the form → checkout opened ✔ |
| Upgrade (before fix) | `POST /v1/memberships/initiate-upgrade` | ❌ no — only `paymentUrl` | FE could only GET `paymentUrl` → **404** |
| Upgrade (after fix) | `POST /v1/memberships/initiate-upgrade` | ✅ yes — now mirrors purchase | FE can POST the form → checkout opens ✔ |

The 404 had nothing to do with "already paid once" — it was simply that the upgrade response didn't
hand the FE the signed form fields needed for the POST.

---

## New `initiate-upgrade` response shape

`POST /v1/memberships/initiate-upgrade` (payment required) now returns:

```json
{
  "code": "999999",
  "data": {
    "transactionRef": "8e4e999b-e590-45c6-a317-3a1fd7dbcf48",
    "paymentUrl": "https://pay-sandbox.sepay.vn/v1/checkout/init",
    "providerData": {
      "method": "POST",
      "checkoutUrl": "https://pay-sandbox.sepay.vn/v1/checkout/init",
      "fields": {
        "operation": "PURCHASE",
        "payment_method": "BANK_TRANSFER",
        "order_amount": "700000",
        "currency": "VND",
        "order_invoice_number": "8e4e999b-e590-45c6-a317-3a1fd7dbcf48",
        "order_description": "SmartRent Membership Upgrade to Gói Tiêu Chuẩn",
        "success_url": "https://www.smartrent.io.vn/payment/result?status=success",
        "error_url": "https://www.smartrent.io.vn/payment/result?status=error",
        "cancel_url": "https://www.smartrent.io.vn/payment/result?status=cancel",
        "merchant": "SP-TEST-...",
        "signature": "4eQSJiJExejDwyXTFvkTilLR/PAYmzwcs00N8asUg3U="
      }
    },
    "paymentProvider": "SEPAY",
    "previousMembershipId": 1,
    "newMembershipPackageId": 2,
    "newPackageName": "Gói Tiêu Chuẩn",
    "newPackageLevel": "STANDARD",
    "originalPrice": 700000,
    "discountAmount": 0,
    "finalAmount": 700000,
    "status": "PENDING_PAYMENT",
    "message": "Please complete payment to finalize upgrade."
  }
}
```

The **free-upgrade** response is unchanged (`paymentUrl: null`, `status: "COMPLETED"`,
`finalAmount: 0`, no `providerData`) — keep handling it as before.

### Key fields
- **`providerData.checkoutUrl`** — POST the fields here (mirrored in `paymentUrl`).
- **`providerData.fields`** — the **signed** fields. POST them **exactly as given, in the same
  order** — the `signature` is computed over them; any change is rejected by the gateway.
- **`transactionRef`** — save it; you poll `GET /v1/payments/transactions/{txnRef}` on the result
  page (it stays `COMPLETED` on success — see status note below). Equals `fields.order_invoice_number`.

---

## The form-POST helper (shared with purchase)

This is the existing helper from [`SEPAY_INTEGRATION_GUIDE.md`](./SEPAY_INTEGRATION_GUIDE.md) §3 —
the upgrade flow should call the **same** function. Do not write a second one.

```jsx
function redirectToSePay(initData) {
  // initData = response.data from initiate-purchase OR initiate-upgrade
  const { checkoutUrl, fields } = initData.providerData;

  const form = document.createElement('form');
  form.method = 'POST';
  form.action = checkoutUrl;

  // Preserve field order exactly as returned (the signature depends on it).
  Object.entries(fields).forEach(([name, value]) => {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
  });

  document.body.appendChild(form);
  form.submit(); // browser navigates (POST) to SePay's hosted checkout
}
```

After payment, SePay redirects the browser to `success_url`/`error_url`/`cancel_url`, then your
result page polls `GET /v1/payments/transactions/{txnRef}` until the status is final — identical to
the purchase flow.

---

## Updated TypeScript type for the upgrade response

```ts
interface SePayProviderData {
  method: 'POST';
  checkoutUrl: string;
  fields: Record<string, string>; // POST verbatim, preserve order, includes `signature`
}

interface UpgradeResponse {
  transactionRef: string;
  paymentUrl: string | null;
  providerData?: SePayProviderData; // ← NEW: present for SePay when payment is required
  paymentProvider: string;
  previousMembershipId: number;
  newMembershipPackageId: number;
  newPackageName: string;
  newPackageLevel: string;
  originalPrice: number;
  discountAmount: number;
  finalAmount: number;
  status: 'PENDING_PAYMENT' | 'COMPLETED';
  message: string;
}
```

---

## Note on transaction-history status — **no frontend change needed**

There was a second report: a paid transaction showed **"Không xác định"** in transaction history even
though it was `COMPLETED` in the DB. This was a **backend-only** fix and needs **no frontend change**:

- `GET /v1/payments/history` now returns `status: "SUCCESS"` for completed transactions (previously it
  returned the raw `"COMPLETED"`, which the FE status badge didn't recognise → "Không xác định").
  It now matches the vocabulary used by `GET /v1/me/transactions`
  (`PENDING | SUCCESS | FAILED | CANCELLED | REFUNDED`).
- **Unchanged:** the SePay **polling** endpoint `GET /v1/payments/transactions/{txnRef}` still returns
  `status: "COMPLETED"` on success. Keep your polling check as `status === 'COMPLETED'` (and stop on
  any non-`PENDING` value), exactly as in `SEPAY_INTEGRATION_GUIDE.md` §4.

> So: history list → `SUCCESS`; checkout polling → `COMPLETED`. They intentionally differ. If you ever
> consolidate, prefer `GET /v1/me/transactions` for the history screen.

---

## Test checklist

1. Upgrade a membership with `"paymentProvider": "SEPAY"` where `finalAmount > 0`.
2. Confirm the FE **POSTs** `providerData.fields` to `providerData.checkoutUrl` (Network tab shows a
   `POST`, not a `GET`) and the SePay sandbox checkout page opens — **no 404**.
3. Complete the sandbox payment → result page polls `/v1/payments/transactions/{txnRef}` →
   `COMPLETED` → upgrade activates.
4. Upgrade where discount covers the full price (`finalAmount: 0`) → completes immediately, no
   checkout, `status: "COMPLETED"`.
5. Open transaction history → the paid upgrade shows a proper **Success** badge (no "Không xác định").

---

## Related docs
- [`SEPAY_INTEGRATION_GUIDE.md`](./SEPAY_INTEGRATION_GUIDE.md) — full SePay hosted-checkout flow (the form-POST helper lives here).
- [`MEMBERSHIP_UPGRADE_FRONTEND_GUIDE.md`](./MEMBERSHIP_UPGRADE_FRONTEND_GUIDE.md) — upgrade flow (its
  `handleConfirmUpgrade` example still shows the old `window.location.href = paymentUrl` redirect;
  apply the diff at the top of this doc).
