# OTP Module Configuration Changes

## Summary

Moved hardcoded configuration values from provider classes to `application.yml` for better configurability and maintainability.

## Changes Made

### 1. ZaloProperties.java

**Added new configuration properties:**

```java
// API Configuration
private String apiEndpoint = "https://business.openapi.zalo.me/message/template";

// Retry Configuration
private int maxRetryAttempts = 3;
private int retryBackoffSeconds = 1;
private int requestTimeoutSeconds = 10;

// Message Configuration
private String appName = "SmartRent";
private int defaultExpiryMinutes = 5;
```

**Benefits:**
- API endpoint can be changed without code modification (useful for testing/staging)
- Retry behavior can be tuned per environment
- Timeout can be adjusted based on network conditions
- App name and expiry time can be customized per deployment

### 2. TwilioProperties.java

**Added new configuration property:**

```java
// Message template with %s placeholder for OTP code
private String messageTemplate = "SmartRent - Your verification code is %s. Valid for 5 minutes. Do not share this code.";
```

**Benefits:**
- SMS message can be customized without code changes
- Different messages for different environments (dev/staging/prod)
- Easy to update message for compliance or branding changes

### 3. ZaloZnsProvider.java

**Removed hardcoded constants:**

```java
// BEFORE
private static final String ZALO_API_ENDPOINT = "https://business.openapi.zalo.me/message/template";
private static final int MAX_RETRY_ATTEMPTS = 3;

// AFTER - Using configuration properties
this.webClient = webClientBuilder
    .baseUrl(zaloProperties.getApiEndpoint())  // From config
    .build();

.retryWhen(Retry.backoff(
    zaloProperties.getMaxRetryAttempts(),      // From config
    Duration.ofSeconds(zaloProperties.getRetryBackoffSeconds())  // From config
))
.timeout(Duration.ofSeconds(zaloProperties.getRequestTimeoutSeconds()))  // From config
```

**Updated template data:**

```java
// BEFORE
templateData.put("expire_time", "5 phút");
templateData.put("app_name", "SmartRent");

// AFTER
templateData.put("expire_time", zaloProperties.getDefaultExpiryMinutes() + " phút");
templateData.put("app_name", zaloProperties.getAppName());
```

### 4. TwilioVerifyProvider.java

**Updated message building:**

```java
// BEFORE
String template = "SmartRent - Your verification code is %s. Valid for 5 minutes. Do not share this code.";

// AFTER
String template = twilioProperties.getMessageTemplate();
```

### 5. application.yml

**Added new configuration properties:**

```yaml
otp:
  providers:
    zalo:
      enabled: ${ZALO_OTP_ENABLED:false}
      access-token: ${ZALO_ACCESS_TOKEN:}
      oa-id: ${ZALO_OA_ID:}
      template-id: ${ZALO_TEMPLATE_ID:}
      # New properties
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
      # New property
      message-template: "${TWILIO_MESSAGE_TEMPLATE:SmartRent - Your verification code is %s. Valid for 5 minutes. Do not share this code.}"
```

### 6. OTP_MODULE_README.md

**Updated documentation with:**
- New environment variables in setup section
- Configuration reference table with all properties
- Default values for each property
- Description of each configuration option

## Environment Variables

### New Optional Variables (with defaults)

```bash
# Zalo Configuration
export ZALO_API_ENDPOINT=https://business.openapi.zalo.me/message/template
export ZALO_MAX_RETRY_ATTEMPTS=3
export ZALO_RETRY_BACKOFF_SECONDS=1
export ZALO_REQUEST_TIMEOUT_SECONDS=10
export ZALO_APP_NAME=SmartRent
export ZALO_DEFAULT_EXPIRY_MINUTES=5

# Twilio Configuration
export TWILIO_MESSAGE_TEMPLATE="SmartRent - Your verification code is %s. Valid for 5 minutes. Do not share this code."
```

## Use Cases

### 1. Testing with Mock Zalo Server

```bash
# Point to local WireMock server
export ZALO_API_ENDPOINT=http://localhost:8089/message/template
export ZALO_MAX_RETRY_ATTEMPTS=1
export ZALO_REQUEST_TIMEOUT_SECONDS=5
```

### 2. High-Latency Network

```bash
# Increase timeout and retries for poor network conditions
export ZALO_REQUEST_TIMEOUT_SECONDS=30
export ZALO_MAX_RETRY_ATTEMPTS=5
export ZALO_RETRY_BACKOFF_SECONDS=2
```

### 3. Custom Branding

```bash
# Different app name per environment
export ZALO_APP_NAME="SmartRent Dev"
export TWILIO_MESSAGE_TEMPLATE="[DEV] SmartRent - Your verification code is %s. Valid for 5 minutes."
```

### 4. Compliance Requirements

```bash
# Adjust expiry time for regulatory compliance
export ZALO_DEFAULT_EXPIRY_MINUTES=3
```

## Migration Guide

### For Existing Deployments

No action required! All new properties have sensible defaults that match the previous hardcoded values.

### For New Deployments

1. **Minimal Configuration** (uses all defaults):
   ```bash
   export ZALO_OTP_ENABLED=true
   export ZALO_ACCESS_TOKEN=your_token
   export ZALO_OA_ID=your_oa_id
   export ZALO_TEMPLATE_ID=your_template_id
   ```

2. **Custom Configuration** (override defaults):
   ```bash
   export ZALO_OTP_ENABLED=true
   export ZALO_ACCESS_TOKEN=your_token
   export ZALO_OA_ID=your_oa_id
   export ZALO_TEMPLATE_ID=your_template_id
   export ZALO_APP_NAME="My Custom App"
   export ZALO_MAX_RETRY_ATTEMPTS=5
   ```

## Testing

All existing tests pass without modification:
- ✅ OtpUtilTest (3 tests)
- ✅ PhoneNumberUtilTest (13 tests)
- ✅ InMemoryOtpStoreTest (6 tests)
- ✅ OtpServiceTest (8 tests)
- ✅ OtpIntegrationTest (5 tests)

**Total: 35 tests passing**

## Benefits

1. **Flexibility**: Configuration can be changed without code deployment
2. **Environment-specific**: Different settings for dev/staging/prod
3. **Testing**: Easy to configure for integration tests
4. **Maintainability**: All configuration in one place
5. **Documentation**: Clear defaults and descriptions
6. **Backward Compatible**: Existing deployments work without changes
7. **12-Factor App**: Follows best practices for cloud-native applications

## Files Modified

1. `src/main/java/com/smartrent/config/otp/ZaloProperties.java` - Added 6 properties
2. `src/main/java/com/smartrent/config/otp/TwilioProperties.java` - Added 1 property
3. `src/main/java/com/smartrent/service/otp/provider/ZaloZnsProvider.java` - Use config properties
4. `src/main/java/com/smartrent/service/otp/provider/TwilioVerifyProvider.java` - Use config property
5. `src/main/resources/application.yml` - Added configuration properties
6. `OTP_MODULE_README.md` - Updated documentation

## Verification

```bash
# Build and test
./gradlew clean build

# Run OTP tests
./gradlew test --tests "com.smartrent.service.otp.*"

# Verify configuration
./gradlew bootRun
# Check logs for: "Zalo ZNS provider initialized" or "Twilio SMS provider initialized"
```

---

**Date**: 2025-11-02  
**Status**: ✅ Complete and Tested  
**Impact**: Low (backward compatible, all tests passing)

