# SePay Payment Gateway Integration Guide (Frontend)

> SmartRent uses the **SePay Payment Gateway (Cổng thanh toán)** — https://developer.sepay.vn/vi/cong-thanh-toan.
> This is a **hosted-checkout** gateway: the user is sent to a SePay-hosted payment page (bank
> transfer / NAPAS QR / card), pays there, and is redirected back. SePay confirms the result to our
> backend via a server-to-server **IPN**. Your job on the frontend: **send the user to the checkout,
> then poll for the final status.**

> ⚠️ This replaces the previous SePay *bank-transfer/VietQR* model (render-QR-and-poll). There is now
> a **redirect** to a hosted checkout, similar to the old VNPay/PayOS flow.

---

## 1. The 30-second mental model

```
Frontend                         SePay (hosted checkout)            Backend
   |                                     |                             |
   | 1. POST /initiate-purchase ------------------------------------->|
   |    { ..., paymentProvider: "SEPAY" }                            |
   |<-- { transactionRef, paymentUrl, providerData{ method, checkoutUrl, fields } }
   |                                     |                             |
   | 2. auto-submit a hidden POST form (fields) to checkoutUrl ------>|
   |    ===> browser lands on SePay's hosted checkout page            |
   |                                     |                             |
   | 3. user pays on SePay ------------->|                             |
   |                                     |--- 4. IPN (POST) --------->|  marks COMPLETED,
   |                                     |     X-Secret-Key header     |  activates purchase
   |                                     |                             |
   | 5. SePay redirects browser to success_url / error_url / cancel_url
   |                                     |                             |
   | 6. poll GET /transactions/{txnRef} every ~3s ----------------->|
   |<------------- { status: "COMPLETED" } ----------------------|
   | 7. show success                     |                             |
```

| Mechanism | Who calls it | Purpose | Do **you** (frontend) handle it? |
|---|---|---|---|
| **Checkout form** (POST to `checkoutUrl`) | Your app → SePay | Sends the user to the hosted payment page with the signed fields. | ✅ Yes — auto-submit the form |
| **IPN** (`POST /v1/payments/webhook/sepay`) | SePay → backend | Source of truth. Verifies `X-Secret-Key`, matches the order, activates the purchase. | ❌ No — backend only |
| **Redirect** (`success_url`/`error_url`/`cancel_url`) | SePay → browser | Brings the user back to your result page. | ✅ Yes — render a result page |
| **Polling** (`GET /v1/payments/transactions/{txnRef}`) | Your app → backend | Confirm the final status (don't trust the redirect alone). | ✅ Yes |

> The redirect tells you the user *came back*; the **IPN** is what actually settles the payment.
> Always confirm via polling, not just the redirect query params.

---

## 2. Step 1 — Initiate a payment

Same "buy" endpoints as before — just send `"paymentProvider": "SEPAY"`.

### Membership purchase
**`POST /v1/memberships/initiate-purchase`** — `Authorization: Bearer <token>`
```json
{ "membershipId": 2, "paymentProvider": "SEPAY" }
```

### Pay-per-push
**`POST /v1/pushes/push`** — Bearer token
```json
{ "listingId": 123, "useMembershipQuota": false, "paymentProvider": "SEPAY" }
```

(Membership upgrade, pay-per-post, VIP listing and repost work the same way — pass
`"paymentProvider": "SEPAY"` when not using membership quota.)

### Response
```json
{
  "code": "999999",
  "data": {
    "transactionRef": "SEPAY_8f1c...-uuid",
    "provider": "SEPAY",
    "amount": 1400000,
    "currency": "VND",
    "paymentUrl": "https://pay-sandbox.sepay.vn/v1/checkout/init",
    "providerData": {
      "method": "POST",
      "checkoutUrl": "https://pay-sandbox.sepay.vn/v1/checkout/init",
      "fields": {
        "operation": "PURCHASE",
        "payment_method": "BANK_TRANSFER",
        "order_amount": "1400000",
        "currency": "VND",
        "order_invoice_number": "SEPAY_8f1c...-uuid",
        "order_description": "SmartRent Membership Gói Tiêu Chuẩn 1 Tháng",
        "success_url": "http://localhost:3000/payment/result?status=success",
        "error_url": "http://localhost:3000/payment/result?status=error",
        "cancel_url": "http://localhost:3000/payment/result?status=cancel",
        "merchant": "SP-TEST-...",
        "signature": "4eQSJiJExejDwyXTFvkTilLR/PAYmzwcs00N8asUg3U="
      }
    },
    "createdAt": "2026-06-06T22:30:00",
    "expiresAt": "2026-06-06T22:45:00"
  }
}
```

Key fields:
- **`providerData.checkoutUrl`** → POST the fields here (it's also mirrored in `paymentUrl`).
- **`providerData.fields`** → the **signed** form fields. POST them **exactly as given, in the same
  order** — the `signature` is computed over them and the gateway rejects any change.
- **`transactionRef`** → save it (state/localStorage); you'll poll it. It equals
  `order_invoice_number` in the fields.

> ⚠️ This is a **POST**, not a GET redirect — you can't just `window.location = checkoutUrl`. Build a
> hidden `<form method="POST">` with one hidden input per field and submit it.

---

## 3. Step 2 — Send the user to checkout

```jsx
function redirectToSePay(initData) {
  // initData = response.data from initiate-purchase
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
  form.submit(); // browser navigates to SePay's hosted checkout
}
```

The user completes payment on SePay, then SePay redirects the browser to your `success_url` /
`error_url` / `cancel_url`.

---

## 4. Step 3 — Result page + polling

On your `/payment/result` page, read the saved `transactionRef` and poll the backend until the
status is final (don't trust the redirect's `status` query param alone — the IPN is the source of
truth):

```jsx
import { useEffect, useState } from 'react';
import axios from 'axios';

export default function PaymentResult({ transactionRef, token }) {
  const [status, setStatus] = useState('PENDING');

  useEffect(() => {
    const poll = async () => {
      try {
        const res = await axios.get(`/v1/payments/transactions/${transactionRef}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        const s = res.data?.data?.status;
        setStatus(s);
        if (s && s !== 'PENDING') clearInterval(timer); // COMPLETED / FAILED / CANCELLED
      } catch (_) { /* keep polling */ }
    };
    const timer = setInterval(poll, 3000);
    poll();
    return () => clearInterval(timer);
  }, [transactionRef, token]);

  if (status === 'COMPLETED') return <h1>🎉 Thanh toán thành công!</h1>;
  if (status === 'CANCELLED' || status === 'FAILED') return <h1>Thanh toán đã hủy / thất bại</h1>;
  return <h1>⏳ Đang xác nhận thanh toán...</h1>;
}
```

---

## 5. Status reference

`GET /v1/payments/transactions/{txnRef}` → `data.status`:

| Status | Meaning | UI |
|---|---|---|
| `PENDING` | Awaiting payment / IPN | Spinner, keep polling |
| `COMPLETED` | Paid & matched (IPN received) | Show success, stop polling |
| `CANCELLED` | User cancelled / voided | Show cancelled |
| `FAILED` | Payment failed / expired | Show failed, offer retry |

---

## 6. Backend configuration (for reference / DevOps)

```env
SEPAY_ENV=sandbox                 # sandbox | production
SEPAY_MERCHANT_ID=...             # Payment Gateway Merchant ID (my.sepay.vn → Payment Gateway)
SEPAY_SECRET_KEY=...              # Payment Gateway Secret Key (signs checkout; verifies IPN)
SEPAY_PAYMENT_METHOD=BANK_TRANSFER  # BANK_TRANSFER | NAPAS_BANK_TRANSFER | CARD
SEPAY_CURRENCY=VND
SEPAY_SUCCESS_URL=https://www.smartrent.io.vn/payment/result?status=success
SEPAY_ERROR_URL=https://www.smartrent.io.vn/payment/result?status=error
SEPAY_CANCEL_URL=https://www.smartrent.io.vn/payment/result?status=cancel
SEPAY_IPN_URL=https://<your-api-host>/v1/payments/webhook/sepay
```

Environment-derived URLs (set automatically from `SEPAY_ENV`):

| | Sandbox | Production |
|---|---|---|
| Checkout init | `https://pay-sandbox.sepay.vn/v1/checkout/init` | `https://pay.sepay.vn/v1/checkout/init` |
| REST API | `https://pgapi-sandbox.sepay.vn/v1` | `https://pgapi.sepay.vn/v1` |

In the SePay dashboard (my.sepay.vn → **Payment Gateway → IPN / Webhooks**):
- Set the IPN URL to `https://<your-api-host>/v1/payments/webhook/sepay` (same as `SEPAY_IPN_URL`).
- Set the IPN auth type to **Secret Key** — SePay sends it as `X-Secret-Key: <SEPAY_SECRET_KEY>`,
  and the backend returns **401** if it doesn't match.

### Signature (how the backend signs the checkout)
`signature = base64( HMAC_SHA256( SEPAY_SECRET_KEY, "field1=value1,field2=value2,..." ) )`
over the present fields in this exact order:
`operation, payment_method, order_amount, currency, order_invoice_number, order_description,
customer_id, success_url, error_url, cancel_url, merchant`. This matches the official
`sepay-pg-node` SDK; the fields are POSTed in the same order plus `signature`.

### IPN payload (what the backend receives — FYI)
```json
{
  "timestamp": 1759134682,
  "notification_type": "ORDER_PAID",
  "order": {
    "order_invoice_number": "SEPAY_8f1c...-uuid",
    "order_status": "CAPTURED",
    "order_amount": "1400000.00"
  },
  "transaction": {
    "transaction_id": "68ba94ac80123",
    "transaction_status": "APPROVED",
    "transaction_amount": "1400000",
    "payment_method": "BANK_TRANSFER"
  }
}
```
The backend matches on `order.order_invoice_number` (= our `transactionRef`), requires
`notification_type=ORDER_PAID` + `transaction_status=APPROVED`, checks `transaction_amount >= order
amount`, then marks the transaction COMPLETED. `notification_type=TRANSACTION_VOID` cancels it.

---

## 7. Test mode (sandbox)

1. Register / log in at **my.sepay.vn** and open **Payment Gateway** → activate it
   (https://my.sepay.vn/pg/payment-methods) to get the **sandbox** MERCHANT ID + SECRET KEY.
2. Put them in `.env` with `SEPAY_ENV=sandbox`, set the IPN URL (auth type = Secret Key) to your
   dev backend, then **restart** the backend.
3. Call `initiate-purchase` → POST the returned `fields` to `checkoutUrl` → you land on the SePay
   **sandbox** checkout page.
4. Complete the test payment (BANK_TRANSFER). SePay fires the IPN → the transaction flips to
   `COMPLETED` → your polling UI shows success.

> Sandbox uses the same payload shape and the same `X-Secret-Key` auth as production. The
> `SePay_Payment.postman_collection.json` in this folder includes an IPN-simulation request.

---

## 8. Troubleshooting

### ❌ Checkout rejects the signature
- The `fields` were reordered or a value was changed before POSTing. POST them **exactly** as the
  backend returned them (same keys, same values, same order), including `signature`.

### ❌ IPN returns 401
- The `X-Secret-Key` value in the SePay dashboard doesn't match `SEPAY_SECRET_KEY` on the backend,
  or the dashboard auth type isn't set to **Secret Key**.

### ❌ Transaction stays `PENDING` after payment
- The **IPN isn't configured / not reachable** → check the IPN URL + Secret Key in the dashboard;
  the backend must be publicly reachable over HTTPS.
- **Amount mismatch** → the backend rejects underpayment (`SEPAY_AMOUNT_MISMATCH`).

---

## 9. Migration cheat-sheet (old SePay bank-transfer → SePay Payment Gateway)

| Was (SePay bank-transfer) | Now (SePay Payment Gateway) |
|---|---|
| Render `qrCodeData` VietQR image and poll | **POST signed `fields`** to `checkoutUrl`, redirect to hosted checkout |
| No redirect at all | Redirect back to `success_url`/`error_url`/`cancel_url` |
| Webhook auth `Authorization: Apikey <secret>` | IPN auth `X-Secret-Key: <secret>` |
| Flat webhook body, matched by transfer-content `code` | Nested IPN, matched by `order.order_invoice_number` |

> The initiate-payment endpoints, `transactionRef`, history (`/v1/payments/history`) and status
> (`/v1/payments/transactions/{txnRef}`) endpoints are **unchanged**.
