# OTP Phone Verification Module

Complete Spring Boot module for OTP phone verification targeting Vietnam users with Zalo ZNS as primary channel and SMS as fallback.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Production Deployment](#production-deployment)
- [Operational Notes](#operational-notes)
- [Troubleshooting](#troubleshooting)

## Features

- ✅ **Dual Channel Support**: Zalo ZNS (primary) + SMS (fallback)
- ✅ **Automatic Fallback**: Seamless fallback from Zalo to SMS on failure
- ✅ **Secure OTP Generation**: Cryptographically secure 6-digit codes with BCrypt hashing
- ✅ **Rate Limiting**: Per-phone (5/hour) and per-IP (20/hour) limits
- ✅ **Vietnam Phone Validation**: E.164 format with Vietnam country code validation
- ✅ **Redis Storage**: Production-ready with in-memory fallback for testing
- ✅ **Retry Logic**: Exponential backoff for transient failures
- ✅ **Metrics & Monitoring**: Micrometer metrics for observability
- ✅ **Comprehensive Tests**: Unit and integration tests with WireMock

## Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ OtpController   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────┐
│   OtpService    │─────▶│ RateLimit    │
└────────┬────────┘      │   Service    │
         │               └──────────────┘
         │
         ├──────────────┬──────────────┐
         ▼              ▼              ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐
│  OtpStore    │ │  Zalo    │ │  Twilio  │
│   (Redis)    │ │ Provider │ │ Provider │
└──────────────┘ └──────────┘ └──────────┘
```

## Prerequisites

### Required

- Java 17+
- Spring Boot 3.x
- Redis 6+ (for production)
- Gradle 7+

### Provider Accounts

#### Zalo ZNS (Primary Channel)

1. **Register Zalo Official Account (OA)**
   - Visit: https://oa.zalo.me/
   - Complete business verification
   - Get OA ID

2. **Register ZNS Template**
   - Visit: https://zns.zalo.me/
   - Create OTP template with variables: `otp_code`, `expire_time`, `app_name`
   - Wait for approval (1-3 business days)
   - Get Template ID

3. **Get Access Token**
   - Use OAuth 2.0 flow or App credentials
   - Documentation: https://developers.zalo.me/docs/api/official-account-api/xac-thuc-va-uy-quyen/cach-1-xac-thuc-voi-oauth-post-4307

#### Twilio SMS (Fallback Channel)

1. **Create Twilio Account**
   - Visit: https://www.twilio.com/
   - Get Account SID and Auth Token

2. **Register Sender in Vietnam**
   - Vietnam requires sender registration
   - Submit brand name and use case
   - Alternative: Use local SMS providers (VIETGUYS, VNPT, FPT, Viettel)

## Installation

### 1. Clone and Build

```bash
cd smart-rent
./gradlew clean build
```

### 2. Start Redis

```bash
docker-compose -f docker-compose-otp.yml up -d
```

### 3. Configure Environment Variables

Create `.env` file or set environment variables:

```bash
# Zalo ZNS Configuration
export ZALO_OTP_ENABLED=true
export ZALO_ACCESS_TOKEN=your_zalo_access_token
export ZALO_OA_ID=your_oa_id
export ZALO_TEMPLATE_ID=your_template_id

# Optional Zalo Configuration (with defaults)
export ZALO_API_ENDPOINT=https://business.openapi.zalo.me/message/template
export ZALO_MAX_RETRY_ATTEMPTS=3
export ZALO_RETRY_BACKOFF_SECONDS=1
export ZALO_REQUEST_TIMEOUT_SECONDS=10
export ZALO_APP_NAME=SmartRent
export ZALO_DEFAULT_EXPIRY_MINUTES=5

# Twilio SMS Configuration
export TWILIO_OTP_ENABLED=true
export TWILIO_ACCOUNT_SID=your_account_sid
export TWILIO_AUTH_TOKEN=your_auth_token
export TWILIO_FROM_NUMBER=+1234567890

# Optional Twilio Configuration (with default)
export TWILIO_MESSAGE_TEMPLATE="SmartRent - Your verification code is %s. Valid for 5 minutes. Do not share this code."

# Redis Configuration (optional, defaults to localhost:6379)
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379

# OTP Store Type (redis or memory)
export OTP_STORE_TYPE=redis
```

### 4. Run Application

```bash
./gradlew bootRun
```

## Configuration

### application.yml

```yaml
otp:
  code-length: 6
  ttl-seconds: 300  # 5 minutes
  max-verification-attempts: 5

  store:
    type: redis  # or 'memory' for testing

  rate-limit:
    max-sends-per-phone: 5
    max-sends-per-ip: 20
    window-seconds: 3600

  providers:
    zalo:
      enabled: ${ZALO_OTP_ENABLED:false}
      access-token: ${ZALO_ACCESS_TOKEN:}
      oa-id: ${ZALO_OA_ID:}
      template-id: ${ZALO_TEMPLATE_ID:}
      api-endpoint: ${ZALO_API_ENDPOINT:https://business.openapi.zalo.me/message/template}
      max-retry-attempts: ${ZALO_MAX_RETRY_ATTEMPTS:3}
      retry-backoff-seconds: ${ZALO_RETRY_BACKOFF_SECONDS:1}
      request-timeout-seconds: ${ZALO_REQUEST_TIMEOUT_SECONDS:10}
      app-name: ${ZALO_APP_NAME:SmartRent}
      default-expiry-minutes: ${ZALO_DEFAULT_EXPIRY_MINUTES:5}

    twilio:
      enabled: ${TWILIO_OTP_ENABLED:false}
      account-sid: ${TWILIO_ACCOUNT_SID:}
      auth-token: ${TWILIO_AUTH_TOKEN:}
      from-number: ${TWILIO_FROM_NUMBER:}
      message-template: "${TWILIO_MESSAGE_TEMPLATE:SmartRent - Your verification code is %s. Valid for 5 minutes. Do not share this code.}"
```

### Configuration Reference

#### Zalo ZNS Provider Configuration

| Property | Environment Variable | Default | Description |
|----------|---------------------|---------|-------------|
| `enabled` | `ZALO_OTP_ENABLED` | `false` | Enable/disable Zalo provider |
| `access-token` | `ZALO_ACCESS_TOKEN` | - | Zalo API access token (required) |
| `oa-id` | `ZALO_OA_ID` | - | Official Account ID (required) |
| `template-id` | `ZALO_TEMPLATE_ID` | - | Template ID for OTP messages (required) |
| `api-endpoint` | `ZALO_API_ENDPOINT` | `https://business.openapi.zalo.me/message/template` | Zalo API endpoint |
| `max-retry-attempts` | `ZALO_MAX_RETRY_ATTEMPTS` | `3` | Maximum retry attempts for failed requests |
| `retry-backoff-seconds` | `ZALO_RETRY_BACKOFF_SECONDS` | `1` | Exponential backoff duration in seconds |
| `request-timeout-seconds` | `ZALO_REQUEST_TIMEOUT_SECONDS` | `10` | Request timeout in seconds |
| `app-name` | `ZALO_APP_NAME` | `SmartRent` | Application name in OTP message |
| `default-expiry-minutes` | `ZALO_DEFAULT_EXPIRY_MINUTES` | `5` | Default OTP expiry time in minutes |

#### Twilio SMS Provider Configuration

| Property | Environment Variable | Default | Description |
|----------|---------------------|---------|-------------|
| `enabled` | `TWILIO_OTP_ENABLED` | `false` | Enable/disable Twilio provider |
| `account-sid` | `TWILIO_ACCOUNT_SID` | - | Twilio account SID (required) |
| `auth-token` | `TWILIO_AUTH_TOKEN` | - | Twilio auth token (required) |
| `from-number` | `TWILIO_FROM_NUMBER` | - | Phone number to send from (E.164 format, required) |
| `message-template` | `TWILIO_MESSAGE_TEMPLATE` | `SmartRent - Your verification code is %s...` | SMS message template (use %s for OTP code) |

## API Endpoints

### 1. Send OTP

**Endpoint**: `POST /otp/send`

**Request**:
```json
{
  "phone": "0912345678",
  "preferredChannels": ["zalo", "sms"]
}
```

**Response** (200 OK):
```json
{
  "code": "0",
  "message": "OTP sent successfully",
  "data": {
    "channel": "zalo",
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "ttlSeconds": 300,
    "maskedPhone": "+8491***5678"
  }
}
```

**Headers**:
- `X-RateLimit-Remaining-Phone`: Remaining attempts for this phone
- `X-RateLimit-Remaining-IP`: Remaining attempts for this IP

### 2. Verify OTP

**Endpoint**: `POST /otp/verify`

**Request**:
```json
{
  "phone": "0912345678",
  "code": "123456",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response** (200 OK - Success):
```json
{
  "code": "0",
  "message": "OTP verified successfully",
  "data": {
    "verified": true,
    "message": "OTP verified successfully"
  }
}
```

**Response** (400 Bad Request - Failed):
```json
{
  "code": "10007",
  "message": "Invalid OTP code",
  "data": {
    "verified": false,
    "message": "Invalid OTP code",
    "remainingAttempts": 4
  }
}
```

### Error Codes

| Code  | Description                          |
|-------|--------------------------------------|
| 10001 | Invalid phone number format          |
| 10002 | Non-Vietnam phone number             |
| 10003 | Rate limit exceeded                  |
| 10004 | OTP generation failed                |
| 10005 | OTP send failed (all channels)       |
| 10006 | OTP not found or expired             |
| 10007 | Invalid OTP code                     |
| 10008 | Verification attempts exceeded       |
| 10009 | OTP already verified                 |
| 10010 | Provider error                       |
| 10011 | Invalid request ID                   |

## Testing

### Run Unit Tests

```bash
./gradlew test
```

### Run Integration Tests

```bash
./gradlew integrationTest
```

### Manual Testing with cURL

```bash
# Send OTP
curl -X POST http://localhost:8080/otp/send \
  -H "Content-Type: application/json" \
  -d '{"phone": "0912345678"}'

# Verify OTP
curl -X POST http://localhost:8080/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "0912345678",
    "code": "123456",
    "requestId": "your-request-id"
  }'
```

## Production Deployment

### Security Checklist

- [ ] Enable HTTPS/TLS for all endpoints
- [ ] Store secrets in secure vault (AWS Secrets Manager, HashiCorp Vault)
- [ ] Enable CSRF protection if using session-based auth
- [ ] Add CAPTCHA on send endpoint to prevent abuse
- [ ] Monitor rate limit violations
- [ ] Set up alerts for high failure rates
- [ ] Rotate Zalo access tokens regularly
- [ ] Use Redis with authentication and encryption
- [ ] Enable Redis persistence (AOF + RDB)
- [ ] Set up Redis cluster for high availability

### Environment Variables (Production)

```bash
# Use secrets manager instead of environment variables
ZALO_ACCESS_TOKEN=<from-secrets-manager>
TWILIO_AUTH_TOKEN=<from-secrets-manager>
SPRING_DATA_REDIS_PASSWORD=<from-secrets-manager>
```

### Monitoring

Monitor these metrics:

- `otp.send.success` - Successful OTP sends
- `otp.send.failure` - Failed OTP sends
- `otp.verify.success` - Successful verifications
- `otp.verify.failure` - Failed verifications
- `otp.fallback` - Fallback to secondary channel

### Scaling Considerations

1. **Redis Cluster**: Use Redis Cluster for horizontal scaling
2. **Rate Limiting**: Adjust limits based on traffic patterns
3. **Provider Quotas**: Monitor Zalo/Twilio quotas and costs
4. **Connection Pooling**: Configure WebClient connection pool
5. **Async Processing**: Consider async OTP sending for high volume

## Operational Notes

### Zalo ZNS Limitations

- **Template Approval**: Required before production use (1-3 days)
- **Rate Limits**: Check Zalo documentation for current limits
- **Access Token Expiry**: Tokens expire, implement refresh logic
- **OA Verification**: Business verification required
- **Cost**: ZNS messages are charged per message

### SMS Fallback

- **Vietnam Regulations**: Sender registration mandatory
- **Local Providers**: Consider VIETGUYS, VNPT, FPT, Viettel for better delivery
- **Cost**: SMS typically more expensive than ZNS
- **Delivery Time**: SMS may be slower than ZNS

### Rate Limiting

- **Phone Limit**: 5 OTPs per hour per phone (prevent spam)
- **IP Limit**: 20 OTPs per hour per IP (prevent abuse)
- **Adjust**: Modify limits in `application.yml` based on use case

### OTP Security

- **TTL**: 5 minutes (balance security and UX)
- **Attempts**: 5 verification attempts (prevent brute force)
- **Hashing**: BCrypt with 10 rounds (secure but performant)
- **Code Length**: 6 digits (1 million combinations)

## Troubleshooting

### Issue: Zalo returns error -124 (Invalid access token)

**Solution**: 
- Verify access token is valid and not expired
- Refresh access token using OAuth flow
- Check OA ID matches the access token

### Issue: SMS not delivered in Vietnam

**Solution**:
- Verify sender is registered with Vietnamese authorities
- Consider switching to local SMS provider
- Check Twilio account has Vietnam enabled

### Issue: Rate limit errors

**Solution**:
- Check Redis is running and accessible
- Verify rate limit configuration
- Consider increasing limits for legitimate high-volume use cases

### Issue: OTP not found or expired

**Solution**:
- Check Redis TTL configuration
- Verify system clocks are synchronized
- Check Redis persistence settings

### Issue: High failure rate

**Solution**:
- Check provider status pages (Zalo, Twilio)
- Verify network connectivity to provider APIs
- Review application logs for specific errors
- Check provider account balance/quotas

## Support

For issues or questions:
- Check logs: `logs/smart-rent.log`
- Review metrics: `/actuator/metrics`
- Health check: `/actuator/health`

## License

Internal use only - SmartRent Platform

