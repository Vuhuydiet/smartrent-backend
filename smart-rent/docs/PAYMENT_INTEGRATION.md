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
const response = await fetch(`${API_URL}/v1/memberships/purchase`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    packageId: 'package-uuid',
    provider: 'VNPAY'
  })
});

const result = await response.json();
// result.data.paymentUrl contains the VNPAY payment URL
```

### For Push Purchase
```typescript
const response = await fetch(`${API_URL}/v1/push/purchase`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    listingId: 'listing-uuid',
    pushDetailId: 'push-detail-uuid',
    provider: 'VNPAY'
  })
});

const result = await response.json();
// result.data.paymentUrl contains the VNPAY payment URL
```

## Step 2: Redirect to VNPAY

After receiving the payment URL, redirect the user to VNPAY:

```typescript
window.location.href = result.data.paymentUrl;
```

## Step 3: Handle Payment Result

After payment, VNPAY redirects the user back to your frontend at the configured return URL (e.g., `/payment/result`) with query parameters containing the payment result.

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
      // Get the full query string from the URL
      const queryString = window.location.search;
      
      if (!queryString) {
        setStatus('failed');
        setMessage('No payment parameters found');
        return;
      }

      try {
        // Call the backend callback endpoint with the same query parameters
        const response = await fetch(
          `${process.env.NEXT_PUBLIC_API_URL}/v1/payments/callback/VNPAY${queryString}`
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
  }, []);

  return (
    <div>
      {status === 'loading' && <p>Processing payment...</p>}
      {status === 'success' && <p>✅ {message}</p>}
      {status === 'failed' && <p>❌ {message}</p>}
    </div>
  );
}
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

