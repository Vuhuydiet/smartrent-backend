# SmartRent API Error Code Reference

This document provides a comprehensive reference for all error codes used in the SmartRent API, including their meanings, HTTP status codes, and resolution strategies.

## Table of Contents

- [Error Code Structure](#error-code-structure)
- [Error Code Categories](#error-code-categories)
- [Internal Server Errors (1xxx)](#internal-server-errors-1xxx)
- [Client Input Errors (2xxx)](#client-input-errors-2xxx)
- [Resource Conflict Errors (3xxx)](#resource-conflict-errors-3xxx)
- [Not Found Errors (4xxx)](#not-found-errors-4xxx)
- [Authentication Errors (5xxx)](#authentication-errors-5xxx)
- [Authorization Errors (6xxx)](#authorization-errors-6xxx)

## Error Code Structure

All SmartRent API responses follow a consistent error format:

```json
{
  "code": "error_code",
  "message": "ERROR_MESSAGE_CONSTANT",
  "data": null
}
```

### Response Components

- **code**: 4-digit error code indicating the specific error type
- **message**: Constant error message for programmatic handling
- **data**: Additional error context (usually null for errors)

### HTTP Status Codes

Error codes are mapped to appropriate HTTP status codes:

- **1xxx**: 500 Internal Server Error
- **2xxx**: 400 Bad Request
- **3xxx**: 409 Conflict (or 400 Bad Request)
- **4xxx**: 404 Not Found
- **5xxx**: 401 Unauthorized
- **6xxx**: 403 Forbidden

## Error Code Categories

### Category Overview

| Category | Range | Description | HTTP Status |
|----------|-------|-------------|-------------|
| Unknown | 9999 | Undefined errors | 500 |
| Internal | 1xxx | Server-side errors | 500 |
| Input | 2xxx | Client validation errors | 400 |
| Conflict | 3xxx | Resource conflicts | 409/400 |
| Not Found | 4xxx | Missing resources | 404 |
| Authentication | 5xxx | Auth failures | 401 |
| Authorization | 6xxx | Permission denied | 403 |

## Internal Server Errors (1xxx)

These errors indicate server-side issues that require developer attention.

### 9999 - UNKNOWN_ERROR
```json
{
  "code": "9999",
  "message": "Unknown error",
  "data": null
}
```
- **HTTP Status**: 500 Internal Server Error
- **Cause**: Unexpected server error
- **Resolution**: Check server logs, contact support

### 1001 - INVALID_KEY
```json
{
  "code": "1001",
  "message": "Invalid key",
  "data": null
}
```
- **HTTP Status**: 500 Internal Server Error
- **Cause**: Invalid JWT signing key or configuration
- **Resolution**: Check JWT configuration, verify signing keys

### 1002 - CANNOT_SEND_EMAIL
```json
{
  "code": "1002",
  "message": "Cannot send email",
  "data": null
}
```
- **HTTP Status**: 500 Internal Server Error
- **Cause**: Email service failure (circuit breaker open)
- **Resolution**: Check email service status, verify API keys

## Client Input Errors (2xxx)

These errors indicate invalid client input that needs correction.

### 2001 - EMPTY_INPUT
```json
{
  "code": "2001",
  "message": "This field should not be null or empty",
  "data": null
}
```
- **HTTP Status**: 400 Bad Request
- **Cause**: Required field is null or empty
- **Resolution**: Provide valid input for all required fields

### 2002 - INVALID_PASSWORD
```json
{
  "code": "2002",
  "message": "Password should be at least 8 characters, contain at least one uppercase letter, one lowercase letter, one number, and one special character",
  "data": null
}
```
- **HTTP Status**: 400 Bad Request
- **Cause**: Password doesn't meet security requirements
- **Resolution**: Use password matching pattern: `^(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&*(){}\\[\\]!~`|])(?=.*\\d).*$`

### 2003 - INVALID_ROLE
```json
{
  "code": "2003",
  "message": "Invalid role",
  "data": null
}
```
- **HTTP Status**: 400 Bad Request
- **Cause**: Specified role doesn't exist
- **Resolution**: Use valid roles: SA, UA, CM, SPA, FA, MA

### 2004 - INVALID_EMAIL
```json
{
  "code": "2004",
  "message": "Invalid email",
  "data": null
}
```
- **HTTP Status**: 400 Bad Request
- **Cause**: Email format is invalid
- **Resolution**: Use valid email format matching RFC 5322

### 2005 - INVALID_PHONE
```json
{
  "code": "2005",
  "message": "Invalid phone number",
  "data": null
}
```
- **HTTP Status**: 400 Bad Request
- **Cause**: Phone number format is invalid
- **Resolution**: Provide valid phone code (1-5 chars) and number (5-20 chars)

### 2006 - VERIFY_CODE_EXPIRED
```json
{
  "code": "2006",
  "message": "Verify code expired",
  "data": null
}
```
- **HTTP Status**: 400 Bad Request
- **Cause**: Verification code has expired (>60 seconds)
- **Resolution**: Request a new verification code

### 2007 - INCORRECT_PASSWORD
```json
{
  "code": "2007",
  "message": "Incorrect password",
  "data": null
}
```
- **HTTP Status**: 400 Bad Request
- **Cause**: Current password is incorrect
- **Resolution**: Provide the correct current password

## Resource Conflict Errors (3xxx)

These errors indicate conflicts with existing resources.

### 3001 - EMAIL_EXISTING
```json
{
  "code": "3001",
  "message": "Email already exists",
  "data": null
}
```
- **HTTP Status**: 409 Conflict
- **Cause**: Email address is already registered
- **Resolution**: Use a different email or login with existing account

### 3002 - PHONE_EXISTING
```json
{
  "code": "3002",
  "message": "Phone already exists",
  "data": null
}
```
- **HTTP Status**: 409 Conflict
- **Cause**: Phone number is already registered
- **Resolution**: Use a different phone number

### 3003 - DOCUMENT_EXISTING
```json
{
  "code": "3003",
  "message": "Document already exists",
  "data": null
}
```
- **HTTP Status**: 409 Conflict
- **Cause**: ID document number is already registered
- **Resolution**: Use a different document number

### 3004 - TAX_NUMBER_EXISTING
```json
{
  "code": "3004",
  "message": "Tax number already exists",
  "data": null
}
```
- **HTTP Status**: 400 Bad Request
- **Cause**: Tax number is already registered
- **Resolution**: Use a different tax number

## Not Found Errors (4xxx)

These errors indicate requested resources don't exist.

### 4001 - USER_NOT_FOUND
```json
{
  "code": "4001",
  "message": "User not found",
  "data": null
}
```
- **HTTP Status**: 404 Not Found
- **Cause**: User with specified ID doesn't exist
- **Resolution**: Verify user ID or create new user account

### 4002 - VERIFY_CODE_NOT_FOUND
```json
{
  "code": "4002",
  "message": "Verify code not found",
  "data": null
}
```
- **HTTP Status**: 404 Not Found
- **Cause**: Verification code doesn't exist or has been used
- **Resolution**: Request a new verification code

## Authentication Errors (5xxx)

These errors indicate authentication failures.

### 5001 - UNAUTHENTICATED
```json
{
  "code": "5001",
  "message": "Unauthenticated",
  "data": null
}
```
- **HTTP Status**: 401 Unauthorized
- **Cause**: No valid authentication token provided
- **Resolution**: Login to obtain access token

### 5002 - INVALID_EMAIL_PASSWORD
```json
{
  "code": "5002",
  "message": "Invalid email or password",
  "data": null
}
```
- **HTTP Status**: 401 Unauthorized
- **Cause**: Email or password is incorrect
- **Resolution**: Verify credentials or reset password

### 5003 - INVALID_TOKEN
```json
{
  "code": "5003",
  "message": "Invalid token",
  "data": null
}
```
- **HTTP Status**: 401 Unauthorized
- **Cause**: JWT token is invalid, expired, or malformed
- **Resolution**: Refresh token or login again

### 5004 - USER_NOT_VERIFIED
```json
{
  "code": "5004",
  "message": "User not verified",
  "data": null
}
```
- **HTTP Status**: 401 Unauthorized
- **Cause**: User account hasn't been email verified
- **Resolution**: Complete email verification process

## Authorization Errors (6xxx)

These errors indicate insufficient permissions.

### 6001 - UNAUTHORIZED
```json
{
  "code": "6001",
  "message": "Don't have permission",
  "data": null
}
```
- **HTTP Status**: 403 Forbidden
- **Cause**: User doesn't have required permissions
- **Resolution**: Contact admin for proper role assignment
