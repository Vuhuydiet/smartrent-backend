# Payment Integration Guide

This document explains how to integrate VNPAY payment in the frontend application.

## Payment Flow Overview

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │     │   Backend   │     │    VNPAY    │     │   Frontend  │
│  (Initiate) │────▶│  (Create)   │────▶│  (Payment)  │────▶│  (Callback) │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
      │                   │                   │                   │
      │ 1. Request        │                   │                   │
      │    payment        │                   │                   │
      │──────────────────▶│                   │                   │
      │                   │                   │                   │
      │ 2. Return         │                   │                   │
      │    paymentUrl     │                   │                   │
      │◀──────────────────│                   │                   │
      │                   │                   │                   │
      │ 3. Redirect to VNPAY                  │                   │
      │──────────────────────────────────────▶│                   │
      │                   │                   │                   │
      │                   │    4. User pays   │                   │
      │                   │                   │                   │
      │ 5. Redirect to frontend with params   │                   │
      │◀──────────────────────────────────────│                   │
      │                   │                   │                   │
      │ 6. Call callback endpoint             │                   │
      │──────────────────▶│                   │                   │
      │                   │                   │                   │
      │ 7. Return result  │                   │                   │
      │◀──────────────────│                   │                   │
```

## Step 1: Create Payment

Call the appropriate payment endpoint based on the payment type:

### For Membership Purchase
```typescript
const response = await fetch(`${API_URL}/v1/memberships/initiate-purchase`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    membershipId: 2, // Long type - membership package ID
    paymentProvider: 'VNPAY'
  })
});

const result = await response.json();
// result.data.paymentUrl contains the VNPAY payment URL
```

### For Push Purchase
```typescript
const response = await fetch(`${API_URL}/v1/pushes/push`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    listingId: 123, // Long type - listing ID
    useMembershipQuota: false, // false = direct payment
    paymentProvider: 'VNPAY'
  })
});

const result = await response.json();
// If payment required: result.data.paymentUrl contains the VNPAY payment URL
// If quota used: result.data contains push confirmation
```

## Step 2: Redirect to VNPAY

After receiving the payment URL, redirect the user to VNPAY:

```typescript
window.location.href = result.data.paymentUrl;
```

## Step 3: Handle Payment Result

After payment, VNPAY redirects the user back to your frontend at the configured return URL (e.g., `/payment/result`) with query parameters containing the payment result.

### Important: Filtering VNPay Parameters

When calling the backend callback endpoint, you should **only pass VNPay parameters** (those starting with `vnp_`). Any additional parameters in the URL (like `auth`, `returnUrl`, etc.) should be filtered out to ensure proper signature validation.

### Payment Result Page Component

```typescript
// pages/payment/result.tsx (Next.js example)
import { useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';

export default function PaymentResultPage() {
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<'loading' | 'success' | 'failed'>('loading');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const processPaymentResult = async () => {
      // Filter only VNPay parameters (starting with 'vnp_')
      const vnpParams = new URLSearchParams();
      searchParams.forEach((value, key) => {
        if (key.startsWith('vnp_')) {
          vnpParams.append(key, value);
        }
      });

      const queryString = vnpParams.toString();

      if (!queryString) {
        setStatus('failed');
        setMessage('No payment parameters found');
        return;
      }

      try {
        // Call the backend callback endpoint with only VNPay parameters
        const response = await fetch(
          `${process.env.NEXT_PUBLIC_API_URL}/v1/payments/callback/VNPAY?${queryString}`
        );

        const result = await response.json();

        if (result.code === '200000') {
          setStatus('success');
          setMessage('Payment completed successfully!');
        } else {
          setStatus('failed');
          setMessage(result.message || 'Payment failed');
        }
      } catch (error) {
        setStatus('failed');
        setMessage('Error processing payment result');
      }
    };

    processPaymentResult();
  }, [searchParams]);

  return (
    <div>
      {status === 'loading' && <p>Processing payment...</p>}
      {status === 'success' && <p>✅ {message}</p>}
      {status === 'failed' && <p>❌ {message}</p>}
    </div>
  );
}
```

### Alternative: Pass All Parameters (Backend Handles Filtering)

The backend now automatically filters out non-VNPay parameters during signature validation. So you can also pass all parameters as-is:

```typescript
// Alternative approach - backend handles filtering
const queryString = window.location.search;
const response = await fetch(
  `${process.env.NEXT_PUBLIC_API_URL}/v1/payments/callback/VNPAY${queryString}`
);
```

## API Reference

### Callback Endpoint

**Endpoint:** `GET /v1/payments/callback/VNPAY`

**Authentication:** Not required (public endpoint)

**Query Parameters:** All parameters from VNPAY redirect (pass them as-is)

**Response:**
```json
// Success
{
  "code": "200000",
  "message": "Payment completed successfully",
  "data": {
    "transactionRef": "d1171b46-02ce-4d68-98b5-f1f3aeca9c5a",
    "providerTransactionId": "15356501",
    "status": "COMPLETED",
    "success": true,
    "signatureValid": true,
    "message": "Payment successful"
  }
}

// Failed Payment
{
  "code": "400000",
  "message": "Payment failed: User cancelled",
  "data": {
    "transactionRef": "d1171b46-02ce-4d68-98b5-f1f3aeca9c5a",
    "status": "FAILED",
    "success": false,
    "signatureValid": true
  }
}

// Invalid Signature
{
  "code": "400001",
  "message": "Invalid signature",
  "data": {
    "success": false,
    "signatureValid": false
  }
}
```

## Troubleshooting

### Invalid Signature Error

If you receive an "Invalid signature" error, check the following:

1. **Non-VNPay Parameters**: Ensure you're not passing non-VNPay parameters (like `auth`, `returnUrl`, etc.) to the callback endpoint. The backend filters these out automatically, but it's best practice to only send `vnp_*` parameters.

2. **URL Encoding**: The backend handles URL decoding automatically. Do not manually encode/decode the parameters before sending them.

3. **Hash Secret**: Verify that the `VNPAY_HASH_SECRET` environment variable matches the secret provided by VNPay.

4. **Parameter Tampering**: Ensure the query parameters from VNPay redirect are passed exactly as received without modification.

### VNPay Parameters Reference

The following parameters are sent by VNPay in the callback:

| Parameter | Description |
|-----------|-------------|
| `vnp_Amount` | Payment amount (in VND × 100) |
| `vnp_BankCode` | Bank code used for payment |
| `vnp_BankTranNo` | Bank transaction number |
| `vnp_CardType` | Card type (ATM, VISA, etc.) |
| `vnp_OrderInfo` | Order description |
| `vnp_PayDate` | Payment date (yyyyMMddHHmmss) |
| `vnp_ResponseCode` | Response code (00 = success) |
| `vnp_TmnCode` | Merchant terminal code |
| `vnp_TransactionNo` | VNPay transaction number |
| `vnp_TransactionStatus` | Transaction status (00 = success) |
| `vnp_TxnRef` | Your transaction reference |
| `vnp_SecureHash` | Signature for verification |

### Response Codes

| Code | Description |
|------|-------------|
| `00` | Success |
| `07` | Deducted but suspected fraud |
| `09` | Transaction failed: Card not registered for Internet Banking |
| `10` | Transaction failed: Incorrect card info 3+ times |
| `11` | Transaction failed: Payment timeout |
| `12` | Transaction failed: Card locked |
| `13` | Transaction failed: Incorrect OTP |
| `24` | Transaction cancelled by user |
| `51` | Transaction failed: Insufficient balance |
| `65` | Transaction failed: Daily limit exceeded |
| `75` | Bank under maintenance |
| `79` | Transaction failed: Incorrect payment password |
| `99` | Other errors |
