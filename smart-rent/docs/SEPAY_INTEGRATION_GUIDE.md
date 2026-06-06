# SePay Integration Guide for Frontend

> SmartRent now uses **SePay** (https://sepay.vn) as its payment gateway. VNPay/PayOS have been removed.
> SePay is **not** a hosted-checkout gateway — there is **no redirect, no return/cancel page**.
> The user pays by **bank transfer via a VietQR code**, and SePay tells our backend (webhook) the moment
> the money arrives. Your job on the frontend: **show the QR, then poll until it's paid.**

---

## 1. The 30-second mental model

```
Frontend                         SePay / Bank                     Backend
   |                                  |                              |
   | 1. POST /initiate-purchase ----------------------------------->|
   |    { ..., paymentProvider: "SEPAY" }                          |
   |<--- { transactionRef, qrCodeData, providerData{bank, amount, transferContent} }
   |                                  |                              |
   | 2. Render the VietQR + bank info |                              |
   |                                  |                              |
   | 3. user transfers money -------->|                              |
   |                                  |--- 4. webhook (POST) ------->|  ← matches code, marks COMPLETED,
   |                                  |        Authorization: Apikey |    activates the purchase
   |                                  |                              |
   | 5. poll GET /transactions/{txnRef} every ~3s ---------------->|
   |<------------- { status: "COMPLETED" } -----------------------|
   | 6. show success                  |                              |
```

There is **no browser redirect**. The only confirmation is the **webhook** (backend-only). The frontend
discovers success by **polling the transaction status**.

| Mechanism | Who calls it | Purpose | Do **you** (frontend) handle it? |
|---|---|---|---|
| **Webhook** (`POST /v1/payments/webhook/sepay`) | SePay → backend | Source of truth. Verifies the `Apikey` header, matches the transfer, activates the purchase. | ❌ No — backend only |
| **Polling** (`GET /v1/payments/transactions/{txnRef}`) | Your app → backend | Find out when the transfer landed so you can show "success". | ✅ Yes |

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

(Membership upgrade, pay-per-post, VIP listing and repost work the same way — pass `"paymentProvider": "SEPAY"` when not using membership quota.)

### Response
```json
{
  "code": "200000",
  "message": "Success",
  "data": {
    "transactionRef": "8f1c...-uuid",
    "provider": "SEPAY",
    "amount": 299000,
    "currency": "VND",
    "paymentUrl":  "https://qr.sepay.vn/img?acc=0123456789&bank=MBBank&amount=299000&des=SR8KD9QW1A",
    "qrCodeData":  "https://qr.sepay.vn/img?acc=0123456789&bank=MBBank&amount=299000&des=SR8KD9QW1A",
    "providerData": {
      "accountNumber": "0123456789",
      "bankCode": "MBBank",
      "accountName": "CONG TY SMARTRENT",
      "amount": 299000,
      "transferContent": "SR8KD9QW1A",
      "qrUrl": "https://qr.sepay.vn/img?acc=0123456789&bank=MBBank&amount=299000&des=SR8KD9QW1A"
    },
    "createdAt": "2026-06-06T22:30:00",
    "expiresAt": "2026-06-06T22:45:00"
  }
}
```

Key fields:
- **`qrCodeData` / `paymentUrl`** → a ready-to-use **VietQR image URL**. Just drop it into an `<img>`.
- **`providerData`** → the bank details + the **`transferContent`** code. Show these so users who can't scan can transfer manually. **The `transferContent` MUST appear in the transfer description** — it's how the backend matches the payment. (The QR already pre-fills it.)
- **`transactionRef`** → save it (state/localStorage); you'll poll it.

> ⚠️ Don't `window.location = paymentUrl` like the old PayOS flow — that just opens a PNG. With SePay you **render** the QR inside your page.

---

## 3. Step 2 — Show the QR and poll

```jsx
import { useEffect, useRef, useState } from 'react';
import axios from 'axios';

export default function SePayCheckout({ initData, token }) {
  // initData = response.data from the initiate-purchase call
  const { transactionRef, qrCodeData, providerData, amount, expiresAt } = initData;
  const [status, setStatus] = useState('PENDING');
  const timer = useRef(null);

  useEffect(() => {
    const poll = async () => {
      try {
        const res = await axios.get(`/v1/payments/transactions/${transactionRef}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        const s = res.data?.data?.status;
        setStatus(s);
        if (s && s !== 'PENDING') clearInterval(timer.current); // COMPLETED / FAILED / CANCELLED
      } catch (_) { /* keep polling */ }
    };

    timer.current = setInterval(poll, 3000); // every 3s
    poll();
    // stop polling once the QR window expires
    const stopAt = new Date(expiresAt).getTime() - Date.now();
    const hardStop = setTimeout(() => clearInterval(timer.current), Math.max(stopAt, 0));
    return () => { clearInterval(timer.current); clearTimeout(hardStop); };
  }, [transactionRef, token, expiresAt]);

  if (status === 'COMPLETED') return <div><h1>🎉 Thanh toán thành công!</h1><p>Dịch vụ của bạn đã được kích hoạt.</p></div>;
  if (status === 'CANCELLED' || status === 'FAILED') return <div><h1>Thanh toán đã hủy / thất bại</h1></div>;

  return (
    <div className="sepay-checkout">
      <h2>Quét mã VietQR để thanh toán</h2>
      <img src={qrCodeData} alt="VietQR" width={256} height={256} />

      <p>Hoặc chuyển khoản thủ công:</p>
      <ul>
        <li>Ngân hàng: <b>{providerData.bankCode}</b></li>
        <li>Số tài khoản: <b>{providerData.accountNumber}</b></li>
        <li>Chủ tài khoản: <b>{providerData.accountName}</b></li>
        <li>Số tiền: <b>{amount.toLocaleString('vi-VN')} đ</b></li>
        <li>Nội dung CK: <b>{providerData.transferContent}</b> ⚠️ bắt buộc giữ nguyên</li>
      </ul>

      <p>⏳ Đang chờ thanh toán... (tự động cập nhật)</p>
    </div>
  );
}
```

That's the whole flow. When the user transfers (scan or manual), SePay fires the webhook, the backend
marks the transaction `COMPLETED`, and your next poll flips the screen to success.

> **Important:** tell the user to keep the **transfer content (`transferContent`) exactly as shown**. If
> they overwrite it, the backend can't match the transfer automatically.

---

## 4. Status reference

`GET /v1/payments/transactions/{txnRef}` → `data.status`:

| Status | Meaning | UI |
|---|---|---|
| `PENDING` | Waiting for the transfer | Keep showing the QR + spinner |
| `COMPLETED` | Money received & matched | Show success, stop polling |
| `CANCELLED` | User/you cancelled it | Show cancelled |
| `FAILED` | Expired / not paid | Show failed, offer retry |

---

## 5. Cancelling (optional)

If the user closes the QR, you can mark the pending transaction cancelled:

**`POST /v1/payments/cancel/{transactionRef}?reason=User%20closed`** — Bearer token

(Not required — a pending transaction simply stays pending/expires; the webhook is idempotent and safe.)

---

## 6. Backend configuration (for reference / DevOps)

Frontend devs don't set these, but here's what the backend expects:

```env
SEPAY_MERCHANT_ID=...          # SePay Merchant ID (account/channel identifier)
SEPAY_SECRET_KEY=...           # SePay Secret Key — sent back in the IPN 'Authorization: Apikey <secret>' header
SEPAY_ACCOUNT_NUMBER=0123456789
SEPAY_BANK_CODE=MBBank         # VietQR bank short name (see https://qr.sepay.vn/banks.json)
SEPAY_ACCOUNT_NAME=CONG TY SMARTRENT
SEPAY_CODE_PREFIX=SR           # prefix for the per-order transfer code
SEPAY_IPN_URL=https://<your-api-host>/v1/payments/webhook/sepay
```

In the SePay dashboard (my.sepay.vn → **Cấu hình IPN / Webhooks**):
- Set the IPN/webhook URL to `https://<your-api-host>/v1/payments/webhook/sepay` (same as `SEPAY_IPN_URL`).
- Set authentication to **API Key** and use your **Secret Key** as the value — SePay sends it as
  `Authorization: Apikey <SEPAY_SECRET_KEY>`, and the backend rejects the IPN with 401 if it doesn't match.
- Optionally configure the "payment code" pattern to your prefix (`SR`) so SePay fills the webhook `code`
  field directly — the backend also parses the transfer content as a fallback, so this is optional.

---

## 7. Test mode (sandbox)

SePay has a full Test Mode (isolated from live — no real money). **You don't need a bank account in
sandbox** — leave `SEPAY_ACCOUNT_NUMBER` empty and trigger transfers from the dashboard. (With no
account number the initiate response omits `qrCodeData`/`paymentUrl` and just returns the
`transferContent` code, which is all you need to simulate.)

1. Log in to **my.sepay.vn** and toggle **Test Mode** (orange bar appears).
2. Add a test bank account and point your webhook at your dev backend (`.../v1/payments/webhook/sepay`).
3. Initiate a payment in the app → you get a QR + a `transferContent` code (e.g. `SR8KD9QW1A`).
4. In the dashboard: **Giao dịch → + Mô phỏng giao dịch** (Simulate transaction). Enter:
   - the **amount** = the order amount,
   - the **content** = the `transferContent` code from step 3,
   - `transferType` = **in** (incoming).
5. SePay fires the webhook to your backend → the transaction flips to `COMPLETED` → your polling UI shows success.

> Test Mode webhooks have the same payload shape and the same `Authorization: Apikey` auth as live.

### Webhook payload (what the backend receives — FYI)
```json
{
  "id": 92704,
  "gateway": "MBBank",
  "transactionDate": "2026-06-06 11:08:33",
  "accountNumber": "0123456789",
  "code": "SR8KD9QW1A",
  "content": "SR8KD9QW1A thanh toan goi PRO",
  "transferType": "in",
  "transferAmount": 299000,
  "referenceCode": "FT26123456789",
  "description": "NGUYEN VAN A chuyen tien"
}
```
The backend matches on `code` (or any token in `content`), checks `transferType == "in"` and
`transferAmount >= order amount`, then marks the order paid.

---

## 8. Troubleshooting

### ❌ Transaction never leaves `PENDING` after the user paid
- The transfer **content didn't contain the code** (user overwrote it) → ask them to transfer again with the exact content, or reconcile manually in the SePay dashboard.
- The **webhook isn't configured / not reachable** → check the webhook URL + API key in the SePay dashboard; the backend must be publicly reachable over HTTPS.
- **Amount was less than required** → the backend rejects underpayment (`SEPAY_AMOUNT_MISMATCH`). The user must transfer the exact amount.

### ❌ Webhook returns 401
- The `Authorization: Apikey <...>` value in the SePay dashboard doesn't match `SEPAY_API_KEY` on the backend.

### ❌ QR shows the wrong bank/amount
- `SEPAY_ACCOUNT_NUMBER` / `SEPAY_BANK_CODE` misconfigured, or the bank short-name isn't a valid VietQR code (see https://qr.sepay.vn/banks.json).

---

## 9. Migration cheat-sheet (PayOS/VNPay → SePay)

| Was (PayOS / VNPay) | Now (SePay) |
|---|---|
| `paymentProvider: "PAYOS"` / `"VNPAY"` | `paymentProvider: "SEPAY"` |
| Redirect to a hosted `paymentUrl` checkout page | **Render** the VietQR `qrCodeData` in your page |
| `/payment/result` return + cancel pages | **None** — no redirect at all |
| Call `/payments/callback/{provider}` after redirect | **Don't** — instead **poll** `/payments/transactions/{txnRef}` |
| Gateway confirms via redirect + webhook | SePay confirms via **webhook only** |

> The initiate-payment endpoints, `transactionRef`, history (`/v1/payments/history`) and status
> (`/v1/payments/transactions/{txnRef}`) endpoints are **unchanged**.
