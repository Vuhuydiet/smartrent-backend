# Admin Transaction Management - Frontend And Backend Design Guide

## Purpose

This guide describes the admin transaction management feature for SmartRent. It includes the frontend contract, recommended backend schema, service flow, payment lifecycle, concurrency strategy, and implementation plan.

The design is intentionally practical for a graduation project: one Spring Boot backend, one MySQL database, Redis where it helps, and provider abstractions for VNPay now plus future gateways later.

## Admin Capabilities

Admins should be able to:

* View transactions from all customers.
* Filter by customer, landlord, status, gateway, date range, and payment type.
* Search by transaction code, VNPay transaction code, or invoice code.
* View full transaction detail.
* Export filtered transactions as CSV.
* See revenue and payment statistics.
* Investigate failed, pending, cancelled, and refunded payments.

Recommended admin routes:

```text
/admin/transactions
/admin/transactions/:transactionId
/admin/payments/dashboard
```

## Recommended API Naming

Use separate customer and admin APIs:

```http
GET /v1/me/transactions
GET /v1/me/transactions/{transactionId}

GET /v1/admin/transactions
GET /v1/admin/transactions/{transactionId}
GET /v1/admin/transactions/export
GET /v1/admin/transactions/statistics
GET /v1/admin/transactions/revenue-series
```

Why: customer APIs use the authenticated user automatically. Admin APIs intentionally expose cross-user filters and require admin authorization.

## Admin List API

```http
GET /v1/admin/transactions?page=1&size=20&status=SUCCESS&gateway=VNPAY&type=MONTHLY_INVOICE&customerId=...&landlordId=...&fromDate=2026-05-01&toDate=2026-05-31&q=INV-202605
Authorization: Bearer <admin_access_token>
```

Query parameters:

| Name | Type | Notes |
| --- | --- | --- |
| `page` | number | 1-based, default `1`. |
| `size` | number | Default `20`, max `100`. |
| `status` | string | `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `REFUNDED`. |
| `gateway` | string | `VNPAY`, later `ZALOPAY`, `MOMO`, etc. |
| `type` | string | Payment type. |
| `customerId` | string | UUID. |
| `landlordId` | string | UUID. |
| `fromDate` | date | Inclusive. |
| `toDate` | date | Inclusive. |
| `q` | string | Transaction code, gateway transaction code, invoice code. |
| `sort` | string | Default `createdAt,desc`. |

Response:

```json
{
  "code": "200000",
  "message": "Transactions retrieved successfully",
  "data": {
    "page": 1,
    "size": 20,
    "totalElements": 1284,
    "totalPages": 65,
    "data": [
      {
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
        "customer": {
          "customerId": "13ad9071-279a-4758-9caf-9758d259187d",
          "name": "Tran Thi B",
          "phone": "0912345678"
        },
        "landlord": {
          "landlordId": "9a2d1bdc-1121-4431-a278-a4fb5f3ef222",
          "name": "Nguyen Van A",
          "phone": "0901234567"
        },
        "invoice": {
          "invoiceId": "8f5f1a4b-4d1f-4d2a-9e10-7f1c95b22121",
          "invoiceCode": "INV-202605-0009"
        },
        "room": {
          "roomId": 102,
          "roomCode": "A102",
          "roomName": "Room A102"
        },
        "failureReason": null
      }
    ]
  }
}
```

## Admin Detail API

```http
GET /v1/admin/transactions/{transactionId}
Authorization: Bearer <admin_access_token>
```

Include raw provider fields only for admins:

```json
{
  "code": "200000",
  "message": "Transaction retrieved successfully",
  "data": {
    "transactionId": "6f5d6e8a-7ff7-4c40-95af-a7c65f4ff111",
    "transactionCode": "TXN-20260516-000128",
    "idempotencyKey": "invoice:8f5f1a4b:attempt:1",
    "amount": 2500000,
    "currency": "VND",
    "paymentGateway": "VNPAY",
    "paymentMethod": "ATM",
    "gatewayTransactionCode": "15356501",
    "gatewayResponseCode": "00",
    "status": "SUCCESS",
    "paymentType": "MONTHLY_INVOICE",
    "createdAt": "2026-05-16T10:15:30",
    "completedAt": "2026-05-16T10:17:02",
    "expiredAt": "2026-05-16T10:30:30",
    "customer": {
      "customerId": "13ad9071-279a-4758-9caf-9758d259187d",
      "name": "Tran Thi B",
      "email": "tenant@example.com",
      "phone": "0912345678"
    },
    "landlord": {
      "landlordId": "9a2d1bdc-1121-4431-a278-a4fb5f3ef222",
      "name": "Nguyen Van A",
      "phone": "0901234567"
    },
    "invoice": {
      "invoiceId": "8f5f1a4b-4d1f-4d2a-9e10-7f1c95b22121",
      "invoiceCode": "INV-202605-0009",
      "status": "PAID"
    },
    "room": {
      "roomId": 102,
      "roomCode": "A102",
      "roomName": "Room A102",
      "address": "12 Nguyen Trai, District 1, Ho Chi Minh City"
    },
    "failureReason": null,
    "providerPayload": {
      "vnp_TxnRef": "TXN-20260516-000128",
      "vnp_TransactionNo": "15356501",
      "vnp_ResponseCode": "00"
    },
    "timeline": [
      {
        "status": "PENDING",
        "at": "2026-05-16T10:15:30",
        "actorType": "SYSTEM",
        "note": "Payment created"
      },
      {
        "status": "SUCCESS",
        "at": "2026-05-16T10:17:02",
        "actorType": "GATEWAY",
        "note": "VNPay callback accepted"
      }
    ]
  }
}
```

## Statistics APIs

### Summary

```http
GET /v1/admin/transactions/statistics?fromDate=2026-05-01&toDate=2026-05-31
```

```json
{
  "code": "200000",
  "message": "Transaction statistics retrieved successfully",
  "data": {
    "totalRevenue": 245000000,
    "totalTransactions": 1294,
    "successfulPayments": 1180,
    "failedPayments": 72,
    "pendingPayments": 35,
    "cancelledPayments": 5,
    "refundedPayments": 2,
    "successRate": 91.19,
    "averageSuccessfulAmount": 207627
  }
}
```

### Revenue Series

```http
GET /v1/admin/transactions/revenue-series?groupBy=DAY&fromDate=2026-05-01&toDate=2026-05-31
```

```json
{
  "code": "200000",
  "message": "Revenue series retrieved successfully",
  "data": [
    {
      "period": "2026-05-01",
      "revenue": 8500000,
      "successfulCount": 42
    },
    {
      "period": "2026-05-02",
      "revenue": 9200000,
      "successfulCount": 49
    }
  ]
}
```

Supported `groupBy`: `DAY`, `MONTH`.

### Export

```http
GET /v1/admin/transactions/export?status=SUCCESS&fromDate=2026-05-01&toDate=2026-05-31
Accept: text/csv
```

CSV is enough for graduation scope. Excel can be added later with Apache POI.

Recommended columns:

```text
Transaction Code, Invoice Code, Customer Name, Customer Phone, Landlord Name, Room, Type, Gateway, Gateway Transaction Code, Status, Amount, Created At, Completed At, Failure Reason
```

## Admin Frontend UX

Dashboard widgets:

* Total revenue.
* Successful payments.
* Failed payments.
* Pending payments.
* Refunded payments.
* Success rate.

Main table columns:

| Column | Notes |
| --- | --- |
| Transaction | Code, gateway transaction code |
| Invoice | Invoice code and room |
| Customer | Name and phone |
| Landlord | Name and phone |
| Type | Payment type label |
| Gateway | `VNPAY`, future gateways |
| Amount | VND |
| Status | Badge |
| Created | Newest first |
| Completed | Empty for pending/failed |
| Actions | View detail |

Filter UX:

* Search input with debounce.
* Selects for status, gateway, and payment type.
* Customer and landlord async search boxes.
* Date range picker with quick presets: today, last 7 days, this month.
* Export button uses the current filters.
* Reset filters button.

Detail view:

* Summary section with amount, status, gateway.
* Customer/landlord/room/invoice panels.
* Provider information for debugging.
* Timeline/audit section.
* Failure reason section if failed.

## Recommended Database Schema

Use a dedicated transaction table. The current `transactions` table is a good start, but rental payments need more fields and stronger uniqueness.

```sql
CREATE TABLE payment_transactions (
  id CHAR(36) PRIMARY KEY,
  transaction_code VARCHAR(40) NOT NULL,
  idempotency_key VARCHAR(120) NOT NULL,

  customer_id CHAR(36) NOT NULL,
  landlord_id CHAR(36) NULL,
  invoice_id CHAR(36) NULL,
  room_id BIGINT NULL,
  listing_id BIGINT NULL,

  payment_type VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL,
  amount DECIMAL(15, 0) NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'VND',

  payment_gateway VARCHAR(30) NOT NULL,
  payment_method VARCHAR(50) NULL,
  gateway_transaction_code VARCHAR(100) NULL,
  gateway_response_code VARCHAR(30) NULL,
  gateway_bank_code VARCHAR(50) NULL,

  failure_reason VARCHAR(500) NULL,
  order_info VARCHAR(500) NULL,
  provider_payload JSON NULL,

  customer_name_snapshot VARCHAR(150) NULL,
  customer_phone_snapshot VARCHAR(30) NULL,
  landlord_name_snapshot VARCHAR(150) NULL,
  landlord_phone_snapshot VARCHAR(30) NULL,
  room_name_snapshot VARCHAR(150) NULL,
  room_code_snapshot VARCHAR(50) NULL,
  room_address_snapshot VARCHAR(500) NULL,
  invoice_code_snapshot VARCHAR(50) NULL,

  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  completed_at DATETIME(6) NULL,
  expired_at DATETIME(6) NULL,
  version BIGINT NOT NULL DEFAULT 0,

  UNIQUE KEY uk_transaction_code (transaction_code),
  UNIQUE KEY uk_idempotency_key (idempotency_key),
  UNIQUE KEY uk_gateway_transaction (payment_gateway, gateway_transaction_code),
  KEY idx_customer_created (customer_id, created_at DESC),
  KEY idx_admin_created (created_at DESC),
  KEY idx_status_created (status, created_at DESC),
  KEY idx_type_created (payment_type, created_at DESC),
  KEY idx_gateway_created (payment_gateway, created_at DESC),
  KEY idx_invoice (invoice_id),
  KEY idx_landlord_created (landlord_id, created_at DESC),
  KEY idx_search_codes (transaction_code, invoice_code_snapshot, gateway_transaction_code)
);
```

Audit table:

```sql
CREATE TABLE payment_transaction_audits (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  transaction_id CHAR(36) NOT NULL,
  old_status VARCHAR(20) NULL,
  new_status VARCHAR(20) NOT NULL,
  actor_type VARCHAR(20) NOT NULL,
  actor_id VARCHAR(36) NULL,
  reason VARCHAR(500) NULL,
  provider_event_id VARCHAR(120) NULL,
  created_at DATETIME(6) NOT NULL,
  KEY idx_audit_transaction (transaction_id, created_at)
);
```

Optional invoice relationship:

```sql
CREATE TABLE invoices (
  id CHAR(36) PRIMARY KEY,
  invoice_code VARCHAR(50) NOT NULL,
  customer_id CHAR(36) NOT NULL,
  landlord_id CHAR(36) NOT NULL,
  room_id BIGINT NOT NULL,
  amount DECIMAL(15, 0) NOT NULL,
  status VARCHAR(20) NOT NULL,
  due_date DATE NULL,
  paid_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_invoice_code (invoice_code),
  KEY idx_customer_status (customer_id, status),
  KEY idx_landlord_status (landlord_id, status)
);
```

Why snapshot fields: transaction history must remain readable even if a room name, landlord phone, or invoice description changes later. Keep foreign keys for navigation, but render history from snapshots where possible.

Soft delete: do not soft delete financial transactions. Financial rows should be immutable except for controlled status changes. If something must be hidden, add admin-only visibility rules instead of deleting.

## Entity Design

Recommended package structure:

```text
com.smartrent.payment
  controller
    CustomerTransactionController
    AdminTransactionController
    PaymentCallbackController
  service
    TransactionQueryService
    PaymentCommandService
    PaymentCallbackService
    TransactionExportService
    TransactionStatisticsService
  repository
    PaymentTransactionRepository
    PaymentTransactionAuditRepository
  entity
    PaymentTransaction
    PaymentTransactionAudit
  dto
    request
    response
  enums
    PaymentStatus
    PaymentType
    PaymentGateway
```

If you keep the existing global package style, use:

```text
com.smartrent.controller
com.smartrent.service.transaction
com.smartrent.infra.repository
com.smartrent.infra.repository.entity
com.smartrent.dto.request
com.smartrent.dto.response
```

Do not split into microservices for this project. A clean modular package inside the existing Spring Boot app is enough.

## Service Layer Design

Recommended services:

| Service | Responsibility |
| --- | --- |
| `PaymentCommandService` | Create payment attempts, cancel pending transactions, request refunds. |
| `PaymentCallbackService` | Validate gateway callback, apply idempotent status transition, trigger business completion. |
| `TransactionQueryService` | Customer/admin list and detail queries. |
| `TransactionStatisticsService` | Revenue and count aggregation. |
| `TransactionExportService` | CSV export from filtered query. |
| `PaymentProvider` | Gateway abstraction for VNPay and future providers. |

Repository design:

* Use `JpaRepository` for simple lookup.
* Use `JpaSpecificationExecutor` or QueryDSL for dynamic filters.
* Keep list endpoints `@Transactional(readOnly = true)`.
* Use DTO projections for admin tables to avoid loading large object graphs.
* Fetch detail with explicit joins or separate targeted queries.

## Transaction Boundaries

Payment creation:

* One database transaction.
* Create `PENDING` transaction.
* Store idempotency key.
* Generate payment URL.
* Do not mark invoice paid here.

Callback processing:

* One short database transaction for status update and audit insert.
* Lock transaction row using optimistic version or pessimistic write lock.
* Validate allowed transition.
* Commit before triggering heavy business actions if they can be retried.

Business completion:

* Mark invoice paid, activate membership, publish listing, or push listing.
* Prefer same transaction for simple invoice payment.
* For complex side effects, use an internal event after transaction success.

## Payment Lifecycle

```text
Customer clicks Pay
  -> Backend creates PENDING transaction
  -> Backend returns VNPay URL
  -> Customer pays on VNPay
  -> VNPay redirects browser and/or sends IPN
  -> Backend validates signature
  -> Backend locks transaction row
  -> If already final, return existing result
  -> Update PENDING to SUCCESS/FAILED/CANCELLED
  -> Insert audit row
  -> Apply business effect, for example mark invoice PAID
  -> Frontend displays backend-confirmed status
```

Synchronous updates:

* `PENDING` is created synchronously before redirecting to VNPay.
* `CANCELLED` can be synchronous if the user cancels before gateway success.
* Manual admin notes/audit records are synchronous.

Asynchronous updates:

* `SUCCESS`, `FAILED`, and provider-driven `CANCELLED` should be updated from VNPay callback/IPN.
* `REFUNDED` should be updated after refund gateway confirmation.
* Timeout jobs can mark old `PENDING` transactions as `FAILED` or `CANCELLED` after querying the gateway.

## VNPay Callback Strategy

VNPay callback/IPN handling should:

1. Accept all `vnp_` params.
2. Verify HMAC signature before trusting any value.
3. Extract `vnp_TxnRef` as `transactionCode`.
4. Find the transaction.
5. Verify expected amount equals `vnp_Amount / 100`.
6. Verify gateway and merchant config.
7. Lock the transaction row.
8. If transaction is already final, return success without changing business state.
9. Map VNPay response:
   * `vnp_ResponseCode = 00` and transaction status success -> `SUCCESS`.
   * User cancelled or known gateway cancellation -> `CANCELLED`.
   * Other response codes -> `FAILED`.
10. Save gateway transaction code and provider payload.
11. Insert audit row.
12. Trigger invoice/listing/membership completion once.

Duplicate callback prevention:

* Unique key on `(payment_gateway, gateway_transaction_code)`.
* Unique `transaction_code`.
* Idempotent status transition: final states are not processed twice.
* Audit can record duplicate callbacks as a note if useful, but business effects must not run again.

## Idempotency Strategy

Use an `idempotency_key` per payable resource:

```text
invoice:{invoiceId}
deposit:{contractId}
membership:{userId}:{membershipId}:{yyyyMMddHHmm}
listing-post:{draftId}
push:{listingId}:{requestedSlot}
```

For invoices, only allow one active `PENDING` or `SUCCESS` transaction per invoice. If the user refreshes or clicks pay again:

* Existing `PENDING`: return the existing payment URL if not expired, or create a new attempt after cancelling/expiring the old one.
* Existing `SUCCESS`: return a conflict response such as `INVOICE_ALREADY_PAID`.
* Existing `FAILED/CANCELLED`: allow a new attempt with a new transaction code, but the invoice idempotency rule must still prevent double success.

## Race Condition Prevention

Use at least one of these:

* `@Version` optimistic locking on `PaymentTransaction`.
* `SELECT ... FOR UPDATE` repository method for callback processing.
* Conditional update query: update only when current status is `PENDING`.

Recommended for this project: use `@Version` plus a repository method with `@Lock(PESSIMISTIC_WRITE)` for callback by transaction code. It is easy to explain and reliable.

Example:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select t from PaymentTransaction t where t.transactionCode = :transactionCode")
Optional<PaymentTransaction> findByTransactionCodeForUpdate(String transactionCode);
```

## Retry And Timeout Strategy

* Keep `PENDING` transactions with `expiredAt`.
* Scheduled job runs every 5-10 minutes.
* For expired pending transactions, query VNPay once or twice.
* If gateway confirms success, mark `SUCCESS`.
* If gateway says not found/failed after retry window, mark `FAILED` or `CANCELLED`.
* Log every automatic transition in audit.

Redis is useful for:

* Short-lived idempotency locks around payment creation: `payment:create:{invoiceId}` with TTL 30-60 seconds.
* Rate limiting callback endpoint if needed.
* Caching dashboard stats for 1-5 minutes.

Do not use Redis as the source of truth for payment status. MySQL remains authoritative.

## Security And Authorization

Rules:

* Customer list/detail: authenticated user can only access own transactions.
* Admin list/detail/export/statistics: require `ADMIN` role.
* Provider callback/IPN: public endpoint but must verify provider signature.
* Do not expose full provider payload to customers.
* Mask phone/email in admin table if the admin role is limited. Full admin can see full values.
* Never accept amount, status, customer id, or landlord id from frontend during callback.
* Export endpoint should be audited.

Logging:

* Log transaction code, gateway, status transition, and response code.
* Do not log raw signatures, secrets, full card/bank account data, or JWTs.
* Use structured log fields where possible.

Audit logging:

* Record every status transition.
* Record admin exports.
* Record manual refund/cancel actions.
* Record actor type: `CUSTOMER`, `ADMIN`, `GATEWAY`, `SYSTEM`.

## Performance

Indexing:

* Customer page: `(customer_id, created_at DESC)`.
* Admin newest first: `(created_at DESC)`.
* Status filter: `(status, created_at DESC)`.
* Gateway filter: `(payment_gateway, created_at DESC)`.
* Type filter: `(payment_type, created_at DESC)`.
* Landlord filter: `(landlord_id, created_at DESC)`.
* Invoice lookup: `(invoice_id)`.
* Unique gateway callback: `(payment_gateway, gateway_transaction_code)`.

Search:

* Prefix search on `transaction_code`, `invoice_code_snapshot`, and `gateway_transaction_code` is enough.
* Avoid `LIKE '%keyword%'` on large tables unless using full-text indexes.
* Normalize codes to uppercase and search by prefix or exact match.

Pagination:

* Offset pagination is fine for graduation scope and admin dashboards.
* Enforce max page size `100`.
* Cursor pagination is only needed if the table grows very large or infinite scroll is required.

Avoid N+1:

* Return list rows from projection queries.
* Store display snapshots on transaction rows.
* Do not fetch room, user, landlord, invoice entities per row.

Caching:

* Cache statistics for 1-5 minutes.
* Do not cache user transaction lists unless there is a proven need.
* Invalidate stats cache after successful payment/refund if simple to implement; otherwise short TTL is acceptable.

## Admin React Structure

```text
features/adminTransactions/
  api/adminTransactionsApi.ts
  hooks/useAdminTransactions.ts
  hooks/useTransactionStatistics.ts
  pages/AdminTransactionsPage.tsx
  pages/AdminTransactionDetailPage.tsx
  components/AdminTransactionFilters.tsx
  components/AdminTransactionTable.tsx
  components/TransactionStatisticsCards.tsx
  components/RevenueChart.tsx
  components/TransactionTimeline.tsx
  components/ExportTransactionsButton.tsx
  utils/adminTransactionFormatters.ts
```

Suggested request type:

```ts
export interface AdminTransactionFilters {
  page: number;
  size: number;
  q?: string;
  status?: 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED' | 'REFUNDED';
  gateway?: 'VNPAY' | string;
  type?: string;
  customerId?: string;
  landlordId?: string;
  fromDate?: string;
  toDate?: string;
}
```

## Error Handling Strategy

Use consistent backend errors:

| Code | Meaning |
| --- | --- |
| `200000` | Success |
| `400000` | Invalid request |
| `401000` | Unauthorized |
| `403000` | Forbidden |
| `404000` | Not found |
| `409000` | Duplicate or already paid |
| `422000` | Invalid status transition |
| `500000` | Server error |

Admin UI behavior:

* `401`: redirect to admin login.
* `403`: show permission error.
* `409`: show conflict message and refresh detail.
* `422`: show status transition message.
* `500`: show retry action.

## Potential Pitfalls

* Letting frontend pass `userId` for customer history.
* Marking invoice paid when payment URL is created instead of when gateway confirms success.
* Processing VNPay browser redirect and IPN twice without idempotency.
* Missing unique constraint for gateway transaction code.
* Using `float`/`double` for money. Use `DECIMAL(15,0)` or Java `BigDecimal`.
* Displaying live room/landlord data only, causing old payment history to change unexpectedly.
* Exporting huge CSV files without filters. Require date range or stream the response.
* Treating Redis locks as enough without database constraints.
* Logging raw provider secrets or secure hashes.

## Step-By-Step Implementation Plan

1. Extend or create `payment_transactions` entity with status, gateway, invoice, room, landlord, snapshots, idempotency key, and version.
2. Add `payment_transaction_audits`.
3. Add Flyway/Liquibase migration with indexes and unique constraints.
4. Define enums: `PaymentStatus`, `PaymentType`, `PaymentGateway`.
5. Implement query DTOs and response DTOs for customer/admin list and detail.
6. Implement `TransactionQueryService` using specifications or QueryDSL.
7. Implement admin list/detail/statistics/export controllers.
8. Update payment creation to use idempotency key and `PENDING` status.
9. Update VNPay callback to lock by transaction code and process idempotently.
10. Add scheduled timeout reconciliation for old pending transactions.
11. Add admin audit logging for export and manual actions.
12. Add frontend transaction history page.
13. Add admin transaction dashboard and detail page.
14. Test duplicate callback, repeated pay clicks, payment failure, payment success, pending timeout, and export filters.

## Minimal Graduation Scope

Build this first:

* Customer list/detail with filters and search.
* Admin list/detail with filters and search.
* CSV export.
* Statistics summary and daily revenue chart.
* VNPay idempotent callback.
* Audit rows for status changes.
* Scheduled pending timeout job.

Defer these:

* Excel export.
* Partial refunds.
* Advanced full-text search.
* Cursor pagination.
* Separate payment microservice.
* Complex event bus. Spring events or simple service calls are enough.

