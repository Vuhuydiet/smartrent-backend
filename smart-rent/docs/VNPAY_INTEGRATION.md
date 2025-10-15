# VNPay Integration Documentation

## Overview

This document describes the VNPay payment gateway integration implemented in the SmartRent backend application. The integration follows Spring Boot best practices and design patterns including Service Layer Pattern, Repository Pattern, and DTO Pattern.

## Architecture

### Design Patterns Used

1. **Service Layer Pattern**: `PaymentService` interface with `PaymentServiceImpl` implementation
2. **Repository Pattern**: `PaymentRepository` for database operations
3. **DTO Pattern**: Separate request/response DTOs for API communication
4. **Connector Pattern**: `VNPayConnector` using Feign client for external API calls
5. **Configuration Pattern**: `VNPayProperties` for configuration management

### Components

#### Entities
- `Payment`: JPA entity for storing payment transactions
- `VNPayTransactionStatus`: Enum for transaction statuses
- `VNPayTransactionType`: Enum for transaction types

#### DTOs
- `VNPayPaymentRequest`: Request DTO for creating payments
- `VNPayCallbackRequest`: Request DTO for VNPay callbacks
- `VNPayPaymentResponse`: Response DTO for payment creation
- `VNPayCallbackResponse`: Response DTO for callback processing
- `PaymentHistoryResponse`: Response DTO for payment history

#### Services
- `PaymentService`: Core payment service interface with VNPay functionality
- `PaymentServiceImpl`: Implementation of payment service with all VNPay logic

#### Controllers
- `PaymentController`: REST controller for payment endpoints

#### Utilities
- `VNPayUtil`: Utility class for VNPay-specific operations (signature generation, validation, etc.)

## Configuration

### Environment Variables

Add the following environment variables to your `.env` file:

```bash
# VNPay Configuration
VNPAY_TMN_CODE=your_vnpay_tmn_code_here
VNPAY_HASH_SECRET=your_vnpay_hash_secret_here

# VNPay URLs (Sandbox for development)
VNPAY_PAYMENT_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_QUERY_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
VNPAY_REFUND_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/refund

# Callback URLs
VNPAY_RETURN_URL=http://localhost:8080/api/v1/payments/vnpay/callback
VNPAY_IPN_URL=http://localhost:8080/api/v1/payments/vnpay/ipn
```

### Production Configuration

For production, update the URLs to:
```bash
VNPAY_PAYMENT_URL=https://vnpayment.vn/paymentv2/vpcpay.html
VNPAY_QUERY_URL=https://vnpayment.vn/merchant_webapi/api/transaction
VNPAY_REFUND_URL=https://vnpayment.vn/merchant_webapi/api/refund
```

## Database Schema

The integration creates a `payments` table with the following structure:

```sql
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    listing_id BIGINT,
    transaction_ref VARCHAR(100) NOT NULL UNIQUE,
    vnpay_transaction_id VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    transaction_type VARCHAR(20) NOT NULL DEFAULT 'PAYMENT',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- ... other columns
);
```

## API Endpoints

### 1. Create Payment

**POST** `/api/v1/payments/vnpay/create`

Creates a new VNPay payment and returns the payment URL.

**Request Body:**
```json
{
    "amount": 100000,
    "orderInfo": "Payment for listing rental deposit",
    "listingId": 12345,
    "bankCode": "NCB",
    "language": "vn",
    "returnUrl": "http://localhost:3000/payment/result",
    "notes": "Additional notes",
    "metadata": "{\"key\": \"value\"}"
}
```

**Response:**
```json
{
    "code": "200000",
    "message": "Payment created successfully",
    "data": {
        "paymentId": 1,
        "transactionRef": "TXN1703501234567_ABC123DE",
        "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=...",
        "amount": 100000,
        "currency": "VND",
        "orderInfo": "Payment for listing rental deposit",
        "createdAt": "2023-12-15T14:00:00",
        "expiresAt": "2023-12-15T14:15:00"
    }
}
```

### 2. VNPay Callback

**GET** `/api/v1/payments/vnpay/callback`

Handles VNPay return callback after payment completion. This endpoint redirects users to the frontend with payment results.

### 3. VNPay IPN

**POST** `/api/v1/payments/vnpay/ipn`

Handles VNPay Instant Payment Notification for server-to-server communication.

### 4. Query Transaction

**GET** `/api/v1/payments/vnpay/query/{transactionRef}`

Queries the current status of a transaction from VNPay.

### 5. Payment History

**GET** `/api/v1/payments/history?userId={userId}&page={page}&size={size}`

Retrieves paginated payment history for a user.

### 6. Payment History by Status

**GET** `/api/v1/payments/history/status/{status}?userId={userId}&page={page}&size={size}`

Retrieves paginated payment history filtered by status.

### 7. Cancel Payment

**POST** `/api/v1/payments/cancel/{transactionRef}?reason={reason}`

Cancels a pending payment.

## Payment Flow

### 1. Payment Creation Flow

1. Client sends payment request to `/api/v1/payments/vnpay/create`
2. System validates request and creates payment record
3. System generates VNPay payment URL with secure hash
4. Client redirects user to VNPay payment page
5. User completes payment on VNPay

### 2. Payment Callback Flow

1. VNPay redirects user to callback URL with payment result
2. System validates VNPay signature
3. System updates payment status in database
4. System redirects user to frontend with result

### 3. IPN Flow (Server-to-Server)

1. VNPay sends IPN notification to server
2. System validates signature and updates payment status
3. System responds with appropriate status code

## Security

### Signature Validation

All VNPay communications are secured using HMAC-SHA512 signatures:

```java
String secureHash = VNPayUtil.generateSecureHash(params, secretKey);
boolean isValid = VNPayUtil.validateSecureHash(params, secretKey, receivedHash);
```

### Best Practices

1. **Always validate signatures** for all VNPay responses
2. **Use HTTPS** for all callback URLs in production
3. **Store sensitive data securely** (hash secret, transaction details)
4. **Implement proper logging** for audit trails
5. **Handle errors gracefully** with appropriate user feedback

## Error Handling

The integration handles various error scenarios:

- Invalid signatures
- Network timeouts
- Invalid transaction states
- Duplicate transactions
- Bank-specific errors

Error codes are mapped to meaningful messages for users.

## Testing

### Sandbox Testing

Use VNPay sandbox environment for testing:
- TMN Code: Provided by VNPay
- Hash Secret: Provided by VNPay
- Test cards: Available in VNPay documentation

### Test Scenarios

1. Successful payment
2. Failed payment
3. Cancelled payment
4. Expired payment
5. Network errors
6. Invalid signatures

## Monitoring and Logging

The integration provides comprehensive logging at different levels:

- **INFO**: Normal operations (payment creation, callback processing)
- **WARN**: Non-critical issues (invalid signatures, not found errors)
- **ERROR**: Critical errors (service failures, database errors)

## Production Considerations

1. **Environment Variables**: Ensure all production environment variables are set
2. **Database**: Run migration scripts to create payment tables
3. **SSL/TLS**: Use HTTPS for all callback URLs
4. **Monitoring**: Set up monitoring for payment failures
5. **Backup**: Regular database backups for payment data
6. **Compliance**: Ensure PCI DSS compliance if applicable

## Troubleshooting

### Common Issues

1. **Invalid Signature**: Check hash secret and parameter ordering
2. **Callback Not Working**: Verify callback URL is accessible from VNPay servers
3. **Payment Not Found**: Check transaction reference format
4. **Database Errors**: Verify foreign key constraints and data types

### Debug Steps

1. Enable debug logging for VNPay service
2. Check VNPay dashboard for transaction details
3. Verify environment variables
4. Test with VNPay sandbox first

## Support

For VNPay-specific issues, refer to:
- VNPay Documentation: [https://vnpay.vn](https://vnpay.vn)
- VNPay Support: Contact through merchant dashboard

For integration issues, check the application logs and verify configuration.
