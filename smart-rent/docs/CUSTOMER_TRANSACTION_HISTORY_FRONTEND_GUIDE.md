# Customer Transaction History - Frontend Guide

## Purpose

This guide describes the customer-facing transaction history experience for SmartRent. It is written for React frontend implementation and assumes the backend is Spring Boot with MySQL, Redis, VNPay, and future payment gateway support.

The goal is a practical graduation-project feature that still feels production-oriented: customers can review every payment, filter/search history, inspect payment details, and understand payment results after returning from VNPay.

## User Scope

Customer users can only see their own transactions. Never send a `userId` from the frontend for customer history. The backend should derive the customer from the JWT principal.

Recommended base path:

```http
/v1/me/transactions
```

If the current backend keeps `/v1/payments/history?userId=...` temporarily, treat it as legacy and migrate toward `/v1/me/transactions` because it prevents user-id spoofing.

## Status Model

Use these UI statuses:

| Status | Meaning | UI Treatment |
| --- | --- | --- |
| `PENDING` | Payment created but gateway result is not final | Yellow or neutral badge |
| `SUCCESS` | Payment confirmed and business action completed or queued | Green badge |
| `FAILED` | Gateway rejected payment or signature/result failed | Red badge |
| `CANCELLED` | User or system cancelled before success | Gray badge |
| `REFUNDED` | Successful payment was refunded | Blue or purple badge |

Backend note: the current code uses `COMPLETED`. For the new API, either expose `SUCCESS` to the frontend and map `COMPLETED -> SUCCESS`, or migrate the enum from `COMPLETED` to `SUCCESS` with a database migration. Do not expose both in new UI filters unless the backend needs a transition period.

## Payment Types

Recommended values:

| Type | Display Label |
| --- | --- |
| `ROOM_RENT` | Room rent |
| `DEPOSIT` | Deposit |
| `MONTHLY_INVOICE` | Monthly invoice |
| `UTILITY_BILL` | Utility bill |
| `SERVICE_FEE` | Service fee |
| `MEMBERSHIP_PURCHASE` | Membership purchase |
| `MEMBERSHIP_UPGRADE` | Membership upgrade |
| `POST_FEE` | Listing post fee |
| `PUSH_FEE` | Listing push fee |
| `REPOST_FEE` | Repost fee |
| `REFUND` | Refund |

For graduation scope, implement only the types currently supported by the backend plus the rental types required by the invoice module. The UI should render unknown values as title case instead of crashing.

## Page UX

Route suggestion:

```text
/account/transactions
```

Primary layout:

```text
Header: Transaction History
Toolbar:
  Search input: transaction code or invoice code
  Status select
  Payment type select
  Date range picker
  Reset filters button

Content:
  Desktop: table
  Mobile: transaction cards

Pagination:
  Page size selector: 10, 20, 50
  Prev/Next buttons
  Current page and total pages
```

Table columns:

| Column | Data |
| --- | --- |
| Transaction | `transactionCode`, payment type, invoice code |
| Amount | VND formatted amount |
| Method | Gateway and method, for example `VNPay - ATM` |
| Status | Badge |
| Room | Room name/code and address if available |
| Landlord | Name and phone if available |
| Created | Created time |
| Completed | Completed time or `-` |
| Actions | View details |

Empty states:

| State | Message |
| --- | --- |
| No transactions ever | `No payments yet.` |
| Filters return nothing | `No transactions match your filters.` |
| Payment pending | Show a small `Check status` action |
| Failed transaction | Show failure reason in details |

Loading strategy:

* Use skeleton rows for first load.
* Use subtle table opacity or row placeholders when changing filters.
* Debounce search input by 300-500 ms.
* Keep previous data visible while fetching the next page.

## API Contract

### List My Transactions

```http
GET /v1/me/transactions?page=1&size=20&status=SUCCESS&type=MONTHLY_INVOICE&fromDate=2026-05-01&toDate=2026-05-31&q=INV-202605
Authorization: Bearer <access_token>
```

Query parameters:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `page` | number | No | 1-based. Default `1`. |
| `size` | number | No | Default `20`, max `100`. |
| `status` | string | No | `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `REFUNDED`. |
| `type` | string | No | Payment type enum. |
| `fromDate` | date | No | Inclusive, `YYYY-MM-DD`. |
| `toDate` | date | No | Inclusive, `YYYY-MM-DD`. |
| `q` | string | No | Search transaction code, gateway code, or invoice code. |
| `sort` | string | No | Default `createdAt,desc`. Frontend should normally omit. |

Recommended response:

```json
{
  "code": "200000",
  "message": "Transactions retrieved successfully",
  "data": {
    "page": 1,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "data": [
      {
        "transactionId": "6f5d6e8a-7ff7-4c40-95af-a7c65f4ff111",
        "transactionCode": "TXN-20260516-000128",
        "amount": 2500000,
        "currency": "VND",
        "paymentGateway": "VNPAY",
        "paymentMethod": "ATM",
        "status": "SUCCESS",
        "paymentType": "MONTHLY_INVOICE",
        "createdAt": "2026-05-16T10:15:30",
        "completedAt": "2026-05-16T10:17:02",
        "invoice": {
          "invoiceId": "8f5f1a4b-4d1f-4d2a-9e10-7f1c95b22121",
          "invoiceCode": "INV-202605-0009"
        },
        "room": {
          "roomId": 102,
          "roomCode": "A102",
          "roomName": "Room A102",
          "address": "12 Nguyen Trai, District 1, Ho Chi Minh City"
        },
        "landlord": {
          "landlordId": "9a2d1bdc-1121-4431-a278-a4fb5f3ef222",
          "name": "Nguyen Van A",
          "phone": "0901234567"
        },
        "failureReason": null
      }
    ]
  }
}
```

### Get Transaction Detail

```http
GET /v1/me/transactions/{transactionId}
Authorization: Bearer <access_token>
```

Response:

```json
{
  "code": "200000",
  "message": "Transaction retrieved successfully",
  "data": {
    "transactionId": "6f5d6e8a-7ff7-4c40-95af-a7c65f4ff111",
    "transactionCode": "TXN-20260516-000128",
    "amount": 2500000,
    "currency": "VND",
    "paymentGateway": "VNPAY",
    "paymentMethod": "ATM",
    "gatewayTransactionCode": "15356501",
    "status": "SUCCESS",
    "paymentType": "MONTHLY_INVOICE",
    "createdAt": "2026-05-16T10:15:30",
    "completedAt": "2026-05-16T10:17:02",
    "invoice": {
      "invoiceId": "8f5f1a4b-4d1f-4d2a-9e10-7f1c95b22121",
      "invoiceCode": "INV-202605-0009",
      "period": "2026-05",
      "description": "Monthly rent and utility bill"
    },
    "room": {
      "roomId": 102,
      "roomCode": "A102",
      "roomName": "Room A102",
      "address": "12 Nguyen Trai, District 1, Ho Chi Minh City"
    },
    "landlord": {
      "landlordId": "9a2d1bdc-1121-4431-a278-a4fb5f3ef222",
      "name": "Nguyen Van A",
      "phone": "0901234567"
    },
    "failureReason": null,
    "timeline": [
      {
        "status": "PENDING",
        "at": "2026-05-16T10:15:30",
        "note": "Payment created"
      },
      {
        "status": "SUCCESS",
        "at": "2026-05-16T10:17:02",
        "note": "VNPay confirmed payment"
      }
    ]
  }
}
```

### Check Payment Status After Redirect

The existing callback flow can remain:

```http
GET /v1/payments/callback/VNPAY?<vnpay_params>
```

After the callback returns, query the transaction detail:

```http
GET /v1/me/transactions/{transactionId}
```

Do not trust only the browser redirect result. The transaction page should show the backend-confirmed state.

## React Data Model

```ts
export type TransactionStatus =
  | 'PENDING'
  | 'SUCCESS'
  | 'FAILED'
  | 'CANCELLED'
  | 'REFUNDED';

export type PaymentType =
  | 'ROOM_RENT'
  | 'DEPOSIT'
  | 'MONTHLY_INVOICE'
  | 'UTILITY_BILL'
  | 'SERVICE_FEE'
  | 'MEMBERSHIP_PURCHASE'
  | 'MEMBERSHIP_UPGRADE'
  | 'POST_FEE'
  | 'PUSH_FEE'
  | 'REPOST_FEE'
  | 'REFUND';

export interface CustomerTransactionItem {
  transactionId: string;
  transactionCode: string;
  amount: number;
  currency: 'VND';
  paymentGateway: 'VNPAY' | 'ZALOPAY' | 'CASH' | string;
  paymentMethod?: string | null;
  gatewayTransactionCode?: string | null;
  status: TransactionStatus;
  paymentType: PaymentType | string;
  createdAt: string;
  completedAt?: string | null;
  invoice?: {
    invoiceId: string;
    invoiceCode: string;
  } | null;
  room?: {
    roomId: number;
    roomCode?: string | null;
    roomName?: string | null;
    address?: string | null;
  } | null;
  landlord?: {
    landlordId: string;
    name: string;
    phone?: string | null;
  } | null;
  failureReason?: string | null;
}
```

## Suggested Component Structure

```text
features/transactions/
  api/customerTransactionsApi.ts
  hooks/useCustomerTransactions.ts
  pages/CustomerTransactionHistoryPage.tsx
  components/TransactionFilters.tsx
  components/TransactionTable.tsx
  components/TransactionMobileCard.tsx
  components/TransactionDetailDrawer.tsx
  components/TransactionStatusBadge.tsx
  utils/transactionFormatters.ts
```

Keep formatting helpers small:

```ts
export function formatVnd(amount: number) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}
```

## Payment Lifecycle UX

When a user initiates payment:

1. Backend creates a `PENDING` transaction and returns a payment URL.
2. Frontend redirects to VNPay.
3. VNPay redirects back to the frontend payment result page.
4. Frontend calls backend callback endpoint with VNPay params.
5. Backend validates signature and updates the transaction.
6. Frontend navigates to transaction detail or invoice detail.

For pending states:

* Poll the transaction detail every 3-5 seconds for up to 60 seconds.
* Stop polling when status becomes `SUCCESS`, `FAILED`, `CANCELLED`, or `REFUNDED`.
* Show `Payment is still being confirmed` if the result stays pending.
* Provide a `Refresh status` button.

## Backend Design Summary For Frontend Awareness

Recommended relationship:

```text
invoice 1 --- n payment_transaction
user/customer 1 --- n payment_transaction
landlord 1 --- n payment_transaction
room/listing 1 --- n invoice
payment_transaction 1 --- n payment_transaction_audit
```

The frontend should expect transaction rows to contain snapshot fields such as room name, landlord name, and invoice code. Snapshot data is important because room names, landlord profiles, or invoice descriptions can change later, while payment history should remain historically accurate.

## Reliability Rules Frontend Must Follow

* Disable the pay button while payment creation is in progress.
* Store the returned `transactionId` for the result page.
* Do not create a new payment automatically when the user refreshes the result page.
* If the same invoice already has a `PENDING` transaction, show `Continue payment` instead of creating another one.
* Treat frontend status as display-only. Backend is the source of truth.
* Never mark an invoice paid on the frontend without a backend `SUCCESS` transaction.

## Error Handling

Recommended display:

| Backend Case | UI |
| --- | --- |
| `401` | Redirect to login |
| `403` | Show no permission page |
| `404` transaction | Show `Transaction not found` |
| Invalid filter | Inline filter error |
| Network error | Toast + retry button |
| `PENDING` timeout | Inform user payment is being verified |

Failure reason should be visible in detail view, not crowded into every table row.

## Implementation Checklist

1. Add API client methods for list and detail endpoints.
2. Build filter state synchronized with URL query params.
3. Implement table for desktop and cards for mobile.
4. Add status badges and VND/date formatters.
5. Add detail drawer or detail route.
6. Add payment result polling flow.
7. Add empty, loading, and error states.
8. Test with statuses: `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `REFUNDED`.
9. Test repeated refresh on payment result page.
10. Test unauthorized access with expired token.

