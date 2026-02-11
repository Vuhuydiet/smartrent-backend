# VIP Listing Payment Integration Guide

This document explains how to integrate VIP listing creation with VNPAY payment in the frontend application.

## Overview

When creating a VIP listing (SILVER, GOLD, DIAMOND), users have two options:
1. **Use Membership Quota** - If user has an active membership with available quota
2. **Direct Payment** - Pay via VNPAY for the listing

## Payment Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │     │   Backend   │     │    VNPAY    │     │   Frontend  │
│  (Create)   │────▶│  (Process)  │────▶│  (Payment)  │────▶│  (Callback) │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
      │                   │                   │                   │
      │ 1. POST /v1/      │                   │                   │
      │    listings       │                   │                   │
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
      │                   │ 7. Create listing │                   │
      │                   │    from cache     │                   │
      │ 8. Return result  │                   │                   │
      │◀──────────────────│                   │                   │
```

## Step 1: Create Listing

### API Endpoint
```
POST /v1/listings
Authorization: Bearer {accessToken}
Content-Type: application/json
```

> **Note:** The `/v1/listings/vip` endpoint is deprecated. Use `/v1/listings` for all listing types (NORMAL, SILVER, GOLD, DIAMOND).

### Request Body
```typescript
interface ListingCreationRequest {
  // Required fields
  title: string;                    // Listing title
  description: string;              // Listing description
  listingType: 'RENT' | 'SALE' | 'SHARE';
  vipType: 'NORMAL' | 'SILVER' | 'GOLD' | 'DIAMOND';
  categoryId: number;
  productType: 'ROOM' | 'APARTMENT' | 'HOUSE' | 'OFFICE' | 'STUDIO';
  price: number;
  priceUnit: 'MONTH' | 'DAY' | 'YEAR';
  address: {
    legacy?: {                      // Use for old address system
      provinceId: number;
      districtId: number;
      wardId: number;
      street?: string;
    };
    newAddress?: {                  // Use for new address system
      provinceCode: string;
      wardCode: string;
      street?: string;
    };
    latitude?: number;
    longitude?: number;
  };

  // Optional fields
  area?: number;
  bedrooms?: number;
  bathrooms?: number;
  direction?: string;
  furnishing?: string;
  roomCapacity?: number;
  amenityIds?: number[];
  mediaIds?: number[];

  // Payment options
  useMembershipQuota?: boolean;     // true = use quota, false = direct payment
  benefitIds?: number[];            // Required when useMembershipQuota=true (vipType inferred from benefit)
  durationDays?: number;            // 10, 15, or 30 days (default: 30)
  paymentProvider?: 'VNPAY';        // Payment provider (default: VNPAY)
}
```

### Example Request
```typescript
const response = await fetch(`${API_URL}/v1/listings`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    title: 'Cho thuê căn hộ 2PN Q7',
    description: 'Căn hộ đẹp, view sông, đầy đủ nội thất',
    listingType: 'RENT',
    vipType: 'GOLD',
    categoryId: 1,
    productType: 'APARTMENT',
    price: 15000000,
    priceUnit: 'MONTH',
    address: {
      legacy: {
        provinceId: 79,
        districtId: 760,
        wardId: 26734,
        street: 'Nguyễn Văn Linh'
      },
      latitude: 10.7456,
      longitude: 106.6789
    },
    area: 75.5,
    bedrooms: 2,
    bathrooms: 2,
    amenityIds: [1, 2, 3],
    mediaIds: [101, 102, 103],
    useMembershipQuota: false,
    durationDays: 30,
    paymentProvider: 'VNPAY'
  })
});

const result = await response.json();
```

### Response - Payment Required
```json
{
  "code": "200000",
  "message": "Success",
  "data": {
    "listingId": null,
    "paymentRequired": true,
    "transactionId": "d1171b46-02ce-4d68-98b5-f1f3aeca9c5a",
    "amount": 2689500,
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=...",
    "message": "Payment required. Complete payment to activate VIP listing."
  }
}
```

### Response - Quota Used (No Payment)
```json
{
  "code": "200000",
  "message": "Success",
  "data": {
    "listingId": 12345,
    "status": "CREATED",
    "paymentRequired": false
  }
}
```

## Step 2: Redirect to VNPAY

After receiving the payment URL, redirect the user to VNPAY:

```typescript
if (result.data.paymentRequired) {
  window.location.href = result.data.paymentUrl;
}
```

## Step 3: Handle Payment Result

After payment, VNPAY redirects the user back to your frontend at the configured return URL with query parameters.

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
        const response = await fetch(
          `${process.env.NEXT_PUBLIC_API_URL}/v1/payments/callback/VNPAY?${queryString}`
        );
        const result = await response.json();

        if (result.code === '200000' && result.data.success) {
          setStatus('success');
          setMessage('Payment completed! Your VIP listing has been created.');
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
    <div className="payment-result">
      {status === 'loading' && <p>Processing payment...</p>}
      {status === 'success' && (
        <div>
          <p>✅ {message}</p>
          <a href="/my-listings">View My Listings</a>
        </div>
      )}
      {status === 'failed' && <p>❌ {message}</p>}
    </div>
  );
}
```

## VIP Tier Pricing

| VIP Tier | Daily Rate | 10 Days | 15 Days (11% off) | 30 Days (18.5% off) |
|----------|------------|---------|-------------------|---------------------|
| SILVER   | 50,000 VND | 500,000 | 667,500 | 1,222,500 |
| GOLD     | 110,000 VND | 1,100,000 | 1,468,500 | 2,689,500 |
| DIAMOND  | 280,000 VND | 2,800,000 | 3,738,000 | 6,846,000 |

## API Reference

### Callback Endpoint

**Endpoint:** `GET /v1/payments/callback/VNPAY`

**Authentication:** Not required (public endpoint)

**Query Parameters:** All parameters from VNPAY redirect

**Response:**
```json
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
```

### Query Transaction Status

**Endpoint:** `GET /v1/payments/transactions/{transactionRef}`

**Authentication:** Required (Bearer token)

**Response:**
```json
{
  "code": "200000",
  "data": {
    "transactionId": "d1171b46-02ce-4d68-98b5-f1f3aeca9c5a",
    "status": "COMPLETED",
    "amount": 2689500,
    "transactionType": "POST_FEE"
  }
}
```

## Error Handling

### Common Error Codes

| Code | Description |
|------|-------------|
| `400000` | Bad request - Invalid parameters |
| `400001` | Invalid signature |
| `401000` | Unauthorized - Invalid or missing token |
| `404000` | Resource not found |
| `500000` | Internal server error |

### VNPay Response Codes

| Code | Description |
|------|-------------|
| `00` | Success |
| `24` | Transaction cancelled by user |
| `51` | Insufficient balance |
| `11` | Payment timeout |
| `99` | Other errors |

## Best Practices

1. **Always validate payment status** - Don't trust frontend-only validation
2. **Handle timeout** - VNPay payments expire after 15 minutes
3. **Implement retry logic** - For network failures during callback
4. **Store transaction reference** - For tracking and support purposes
5. **Show clear feedback** - Display payment status to users immediately
