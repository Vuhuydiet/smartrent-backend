# SmartRent VNPay Payment API Documentation

## Overview

This document describes the VNPay payment integration API endpoints for SmartRent. The system implements a dual payment model:
- **Quota-based**: Free for membership holders (uses membership benefits)
- **Direct Payment**: Pay-per-action via VNPay for non-members or when quota is exhausted

## Base URL
```
http://localhost:8080/v1
```

## Authentication
All endpoints require authentication via `X-User-Id` header:
```
X-User-Id: user123
```

---

## Payment Endpoints

### 1. Initiate Membership Purchase

**Endpoint**: `POST /payments/membership`

**Description**: Creates a pending transaction and returns VNPay payment URL for membership purchase.

**Request Headers**:
```
X-User-Id: user123
Content-Type: application/json
```

**Request Body**:
```json
{
  "membershipId": 1,
  "paymentProvider": "VNPAY",
  "returnUrl": "http://localhost:3000/payment/result"
}
```

**Response** (200 OK):
```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=140000000&...",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "orderInfo": "Membership Standard Package",
  "amount": 1400000
}
```

**Flow**:
1. Frontend calls this endpoint
2. Backend creates PENDING transaction
3. Backend generates VNPay payment URL
4. Frontend redirects user to `paymentUrl`
5. User completes payment on VNPay
6. VNPay redirects to callback URL

---

### 2. VNPay Payment Callback

**Endpoint**: `GET /payments/callback`

**Description**: Handles VNPay payment callback after user completes payment. This endpoint is called by VNPay.

**Query Parameters**:
- `vnp_TxnRef` (string, required): Transaction reference (our transaction ID)
- `vnp_Amount` (number, required): Payment amount in smallest currency unit
- `vnp_ResponseCode` (string, required): VNPay response code (00 = success)
- `vnp_TransactionNo` (string, required): VNPay transaction number
- `vnp_SecureHash` (string, required): HMAC-SHA512 signature
- ... (other VNPay parameters)

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Payment processed successfully",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "result": {
    "userMembershipId": 123,
    "status": "ACTIVE",
    "expiresAt": "2025-11-12T10:30:00"
  }
}
```

**VNPay Response Codes**:
- `00`: Success
- `07`: Suspicious transaction
- `09`: Card not registered for internet banking
- `10`: Incorrect OTP
- `11`: Timeout
- `24`: Transaction cancelled
- Other codes: See VNPay documentation

---

### 3. Get Transaction History

**Endpoint**: `GET /payments/history`

**Description**: Retrieves payment transaction history for the current user.

**Request Headers**:
```
X-User-Id: user123
```

**Query Parameters**:
- `transactionType` (string, optional): Filter by type (MEMBERSHIP_PURCHASE, POST_FEE, BOOST_FEE)

**Response** (200 OK):
```json
[
  {
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user123",
    "transactionType": "MEMBERSHIP_PURCHASE",
    "amount": 1400000,
    "status": "COMPLETED",
    "paymentProvider": "VNPAY",
    "providerTransactionId": "14123456",
    "createdAt": "2025-10-12T10:30:00",
    "updatedAt": "2025-10-12T10:35:00"
  }
]
```

---

### 4. Get Transaction by ID

**Endpoint**: `GET /payments/transactions/{transactionId}`

**Description**: Retrieves details of a specific transaction.

**Path Parameters**:
- `transactionId` (string, required): Transaction ID

**Response** (200 OK):
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user123",
  "transactionType": "POST_FEE",
  "amount": 600000,
  "status": "COMPLETED",
  "paymentProvider": "VNPAY",
  "referenceType": "LISTING",
  "referenceId": "VIP-30",
  "additionalInfo": "VIP Post - 30 days"
}
```

---

### 5. Query Payment Status

**Endpoint**: `GET /payments/transactions/{transactionId}/status`

**Description**: Queries payment status from VNPay directly.

**Path Parameters**:
- `transactionId` (string, required): Transaction ID

**Response** (200 OK):
```json
{
  "success": true,
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 600000,
  "responseCode": "00",
  "message": "Transaction successful"
}
```

---

## Listing Endpoints

### 6. Create VIP/Premium Listing

**Endpoint**: `POST /listings/vip`

**Description**: Creates a VIP or Premium listing with dual payment model.

**Request Headers**:
```
X-User-Id: user123
Content-Type: application/json
```

**Request Body (Using Quota)**:
```json
{
  "title": "Luxury Apartment in District 1",
  "description": "Beautiful 2BR apartment",
  "listingType": "RENT",
  "vipType": "VIP",
  "categoryId": 1,
  "productType": "APARTMENT",
  "price": 15000000,
  "priceUnit": "MONTH",
  "addressId": 123,
  "area": 80.5,
  "bedrooms": 2,
  "bathrooms": 2,
  "useMembershipQuota": true,
  "durationDays": 30
}
```

**Response (Quota Used)** (200 OK):
```json
{
  "data": {
    "listingId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Luxury Apartment in District 1",
    "vipType": "VIP",
    "postSource": "QUOTA",
    "status": "ACTIVE",
    "createdAt": "2025-10-12T10:30:00"
  }
}
```

**Request Body (Direct Payment)**:
```json
{
  "title": "Premium Villa in District 2",
  "description": "Stunning 4BR villa",
  "listingType": "RENT",
  "vipType": "PREMIUM",
  "categoryId": 1,
  "productType": "HOUSE",
  "price": 50000000,
  "priceUnit": "MONTH",
  "addressId": 456,
  "area": 250.0,
  "bedrooms": 4,
  "bathrooms": 3,
  "useMembershipQuota": false,
  "durationDays": 30,
  "paymentProvider": "VNPAY",
  "returnUrl": "http://localhost:3000/payment/result"
}
```

**Response (Payment Required)** (200 OK):
```json
{
  "data": {
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=...",
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "orderInfo": "Premium Post - 30 days",
    "amount": 1800000
  }
}
```

**VIP Types**:
- `NORMAL`: 90,000 VND/30 days
- `VIP`: 600,000 VND/30 days
- `PREMIUM`: 1,800,000 VND/30 days (auto-creates shadow NORMAL listing)

---

### 7. Check Posting Quota

**Endpoint**: `GET /listings/quota-check`

**Description**: Check available VIP and Premium posting quota for current user.

**Request Headers**:
```
X-User-Id: user123
```

**Query Parameters**:
- `vipType` (string, optional): Specific type to check (VIP or PREMIUM)

**Response (All Quotas)** (200 OK):
```json
{
  "data": {
    "vipPosts": {
      "totalAvailable": 5,
      "totalUsed": 2,
      "totalGranted": 7
    },
    "premiumPosts": {
      "totalAvailable": 2,
      "totalUsed": 1,
      "totalGranted": 3
    },
    "boosts": {
      "totalAvailable": 10,
      "totalUsed": 3,
      "totalGranted": 13
    }
  }
}
```

---

## Error Responses

### Insufficient Quota (402 Payment Required)
```json
{
  "code": "QUOTA_001",
  "message": "INSUFFICIENT_QUOTA",
  "data": {
    "userId": "user123",
    "benefitType": "VIP_POSTS",
    "required": 1,
    "available": 0,
    "message": "Insufficient VIP_POSTS quota for user user123"
  }
}
```

### Payment Failed (402 Payment Required)
```json
{
  "code": "PAYMENT_001",
  "message": "PAYMENT_FAILED",
  "data": {
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "responseCode": "24",
    "reason": "Transaction cancelled by user"
  }
}
```

### Invalid Callback (400 Bad Request)
```json
{
  "code": "PAYMENT_002",
  "message": "INVALID_PAYMENT_CALLBACK",
  "data": {
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "reason": "Invalid signature"
  }
}
```

### Duplicate Transaction (409 Conflict)
```json
{
  "code": "PAYMENT_003",
  "message": "DUPLICATE_TRANSACTION",
  "data": {
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "providerTransactionId": "14123456"
  }
}
```

---

## Pricing

| Item | Price (VND) | Duration |
|------|-------------|----------|
| Normal Post | 90,000 | 30 days |
| VIP Post | 600,000 | 30 days |
| Premium Post | 1,800,000 | 30 days |
| Boost | 40,000 | Per time |
| Basic Membership | 700,000 | 30 days |
| Standard Membership | 1,400,000 | 30 days |
| Advanced Membership | 2,800,000 | 30 days |

---

## Swagger/OpenAPI

Access interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

---

## Testing with VNPay Sandbox

**Sandbox URL**: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html

**Test Cards**:
- Card Number: 9704198526191432198
- Card Holder: NGUYEN VAN A
- Expiry Date: 07/15
- OTP: 123456

**Configuration**:
```yaml
vnpay:
  tmn-code: YOUR_TMN_CODE
  hash-secret: YOUR_HASH_SECRET
  url: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  return-url: http://localhost:8080/v1/payments/callback
  version: 2.1.0
```

