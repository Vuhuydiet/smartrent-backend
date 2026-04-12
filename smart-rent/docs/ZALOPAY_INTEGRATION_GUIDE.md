# ZaloPay Integration Guide for Frontend

## 1. Payment Flow Overview

```
Frontend                    ZaloPay                     Backend
   |                           |                           |
   |-- POST /initiate-purchase -->                         |
   |<-- { paymentUrl } --------                           |
   |                           |                           |
   |-- redirect browser ------->                          |
   |      (user pays)          |                           |
   |                           |-- IPN (server-to-server) ->|
   |                           |                           | (updates DB)
   |<-- redirect to /payment/result?... ----------------  |
   |                           |                           |
   |-- GET /v1/payments/callback/ZALOPAY?... -----------> |
   |<-- { success, transactionRef, status } ------------- |
```

There are **two independent mechanisms** ZaloPay uses after payment:

| Mechanism | Direction | Purpose |
|---|---|---|
| **IPN** (`callback_url`) | ZaloPay server → your backend | Updates transaction in DB |
| **Browser redirect** (`redirecturl`) | ZaloPay → user browser → your frontend | Shows result to user |

> **Important:** Frontend should call the backend callback endpoint **after being redirected back**, even if IPN already ran. The backend handles duplicate processing safely.

---

## 2. API Endpoints

### 2.1 Initiate Purchase

**`POST /v1/memberships/initiate-purchase`** — requires Bearer token

```json
// Request
{
  "membershipId": 1,
  "paymentProvider": "ZALOPAY"
}

// Response 200
{
  "code": "0000",
  "message": "Success",
  "data": {
    "transactionRef": "40125b04-0c29-44b8-b9a1-abc123",
    "paymentUrl": "https://sb-openapi.zalopay.vn/v2/...",
    "provider": "ZALOPAY",
    "amount": 2800000,
    "currency": "VND",
    "createdAt": "2026-04-12T22:30:00",
    "expiresAt": "2026-04-12T22:45:00"
  }
}
```

**Store `transactionRef` in `localStorage`** — you need it on the result page!

```js
localStorage.setItem('pendingTransactionRef', data.transactionRef);
window.location.href = data.paymentUrl;
```

---

### 2.2 Process Callback (After Redirect)

**`GET /v1/payments/callback/ZALOPAY`** — **PUBLIC**, no auth required

Call this on the `/payment/result` page, forwarding **all** query params ZaloPay sent:

```js
// On /payment/result page
const params = Object.fromEntries(new URLSearchParams(window.location.search));
const response = await axios.get('/v1/payments/callback/ZALOPAY', { params });
```

ZaloPay redirect params (what you receive in the URL):

| Param | Example | Meaning |
|---|---|---|
| `status` | `1` | `1` = success, `2` = failed |
| `apptransid` | `260412_40125b04...` | ZaloPay order ID |
| `amount` | `2800000` | Amount in VND |
| `checksum` | `2de9e270...` | Signature to verify |
| `bankcode` | `SBIS` | Bank used |
| `pmcid` | `39` | Payment method ID |
| `appid` | `2554` | App ID |
| `discountamount` | `0` | Discount applied |

**Response:**

```json
// Success
{
  "code": "200000",
  "message": "Payment completed successfully",
  "data": {
    "transactionRef": "40125b04-0c29-44b8-b...",
    "status": "COMPLETED",
    "success": true,
    "signatureValid": true
  }
}

// Failed payment (user cancelled/error)
{
  "code": "400000",
  "message": "Payment failed: ...",
  "data": {
    "transactionRef": "40125b04-0c29-44b8-b...",
    "status": "FAILED",
    "success": false
  }
}
```

---

### 2.3 Query Transaction Status (Optional)

**`GET /v1/payments/transactions/{txnRef}`** — requires Bearer token

Use this to double-check the transaction status if needed (e.g., after IPN but before frontend callback).

```js
const txnRef = localStorage.getItem('pendingTransactionRef');
const res = await axios.get(`/v1/payments/transactions/${txnRef}`, {
  headers: { Authorization: `Bearer ${token}` }
});
```

---

## 3. Frontend Implementation

### 3.1 CRITICAL — Make `/payment/result` a Public Route

> ⚠️ **This is the most common cause of "no API called after redirect".**
>
> ZaloPay redirects to `/payment/result` **without an auth token** in the URL.
> If your router's auth guard protects this route, it will redirect to `/login?auth=login&returnUrl=...`
> and the payment result component will never mount — so no API is ever called.

**React Router example:**

```jsx
// In your route definitions, make /payment/result public:
<Route path="/payment/result" element={<PaymentResultPage />} /> // NO auth wrapper!

// NOT this:
<PrivateRoute path="/payment/result" element={<PaymentResultPage />} /> // ❌ WRONG
```

**Next.js middleware example:**

```js
// middleware.js
export function middleware(request) {
  const publicPaths = [
    '/payment/result',  // ← must be here
    '/login',
    '/register',
  ];
  const isPublic = publicPaths.some(p => request.nextUrl.pathname.startsWith(p));
  if (isPublic) return NextResponse.next();
  // ... rest of auth logic
}
```

---

### 3.2 Payment Initiation

```js
// PaymentButton.jsx (or similar)
const handleZaloPayCheckout = async (membershipId) => {
  const res = await axios.post('/v1/memberships/initiate-purchase', {
    membershipId,
    paymentProvider: 'ZALOPAY'
  }, {
    headers: { Authorization: `Bearer ${accessToken}` }
  });

  const { transactionRef, paymentUrl } = res.data.data;

  // Save ref so result page can retrieve it
  localStorage.setItem('pendingTransactionRef', transactionRef);

  // Redirect to ZaloPay
  window.location.href = paymentUrl;
};
```

---

### 3.3 Payment Result Page

```jsx
// PaymentResultPage.jsx
import { useEffect, useState } from 'react';
import axios from 'axios';

export default function PaymentResultPage() {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const processResult = async () => {
      try {
        // Forward all ZaloPay query params to backend
        const params = Object.fromEntries(
          new URLSearchParams(window.location.search)
        );

        // Only call if ZaloPay params are present (avoid calling on direct nav)
        if (!params.apptransid) {
          setResult({ error: 'No payment data found' });
          return;
        }

        const res = await axios.get('/v1/payments/callback/ZALOPAY', { params });
        setResult(res.data);
      } catch (err) {
        console.error('Payment callback error:', err);
        setResult({ error: err.message });
      } finally {
        // Cleanup
        localStorage.removeItem('pendingTransactionRef');
        setLoading(false);
      }
    };

    processResult();
  }, []);

  if (loading) return <div>Đang xử lý thanh toán...</div>;

  const success = result?.data?.success;
  const status = result?.data?.status;

  return (
    <div>
      {success ? (
        <div>
          <h1>Thanh toán thành công! 🎉</h1>
          <p>Gói của bạn đã được kích hoạt.</p>
        </div>
      ) : (
        <div>
          <h1>Thanh toán thất bại</h1>
          <p>Trạng thái: {status}</p>
          <p>{result?.message}</p>
        </div>
      )}
    </div>
  );
}
```

---

## 4. Reading ZaloPay `status` Param

The `status` query param in the redirect URL:

| Value | Meaning |
|---|---|
| `1` | Payment **successful** |
| `2` | Payment **failed** (user cancelled, timeout, or error) |

> **Do NOT rely solely on the redirect `status` param.** Always call the backend callback endpoint (`/v1/payments/callback/ZALOPAY`) which verifies the `checksum` and updates the database.

---

## 5. Sandbox Testing

### App
Download **ZaloPay Sandbox** app, or use the web interface shown during the redirect.

### Test Bank Accounts
```
ATM card number: 970433 + any 10-13 digits (e.g., 9704330001234567)
OTP:             123456
```

### Required `.env` Variables
```env
ZALOPAY_APP_ID=2554
ZALOPAY_KEY1=sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn
ZALOPAY_KEY2=trMrHtvjo6myautxDUiAcYsVtaeQ8nhf
ZALOPAY_RETURN_URL=https://www.smartrent.io.vn/payment/result
ZALOPAY_IPN_URL=https://dev.api.smartrent.io.vn/v1/payments/callback/ZALOPAY
```

> `ZALOPAY_RETURN_URL` now has a safe default in `application.yml` — but set it explicitly in production.

---

## 6. Troubleshooting

### ❌ "No API called after ZaloPay redirect" / page shows login
**Cause:** Auth guard is intercepting `/payment/result` — URL will contain `&auth=login&returnUrl=...`  
**Fix:** Make `/payment/result` a **public route** (see §3.1)

### ❌ `apptransid` not found in database
**Cause:** The `apptransid` format is `yyMMdd_<first20charsOfUUID>`. The backend stores it in `additional_info`.  
**Fix:** This is handled automatically. If it still fails, check backend logs for `"Generated appTransId:"`.

### ❌ `checksum` validation fails  
**Cause:** `ZALOPAY_KEY2` is wrong, or params were modified.  
**Note:** The current backend always returns `signatureValid: true` (validation is stubbed). Implement full validation in production using `key2` against `appid|apptransid|pmcid|bankcode|amount|discountamount|status`.

### ❌ ZaloPay `return_code: 2` on order creation  
**Cause:** MAC signature is wrong. Check backend logs for `"ZaloPay MAC source:"`.  
**Fix:** Verify `ZALOPAY_KEY1`, `ZALOPAY_APP_ID` match sandbox credentials.

### ❌ IPN never arrives  
**Cause:** `ZALOPAY_IPN_URL` must be a **publicly reachable** HTTPS URL (not localhost).  
**Fix:** Set `ZALOPAY_IPN_URL` to your deployed backend URL, e.g. `https://dev.api.smartrent.io.vn/v1/payments/callback/ZALOPAY`.
