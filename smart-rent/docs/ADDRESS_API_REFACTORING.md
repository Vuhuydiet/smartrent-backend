# Address API Refactoring - tinhthanhpho.com Integration

## Overview

This document describes the refactored address API flow that integrates Vietnamese administrative location data from [tinhthanhpho.com](https://tinhthanhpho.com) into the SmartRent listing system.

## Architecture

### Component Diagram

```
┌─────────────────────┐
│  AddressController  │ ← REST API Endpoints
└──────────┬──────────┘
           │
           ├─────────────────────────────┐
           │                             │
    ┌──────▼────────────┐    ┌──────────▼────────────────┐
    │ AddressService    │    │ AddressLocationService    │
    │ (Local DB Logic)  │    │ (External API + Cache)    │
    └──────┬────────────┘    └──────────┬────────────────┘
           │                             │
    ┌──────▼────────────┐    ┌──────────▼────────────────┐
    │ Local Database    │    │  TinhThanhPhoConnector    │
    │  (MySQL)          │    │  (Feign Client)           │
    └───────────────────┘    └──────────┬────────────────┘
                                        │
                             ┌──────────▼────────────────┐
                             │  tinhthanhpho.com API     │
                             │  (External Service)       │
                             └───────────────────────────┘
                                        │
                             ┌──────────▼────────────────┐
                             │  Redis Cache              │
                             │  (24h TTL)                │
                             └───────────────────────────┘
```

## Key Components

### 1. TinhThanhPhoConnector (Feign Client)

**Location:** `src/main/java/com/smartrent/infra/connector/TinhThanhPhoConnector.java`

Feign client for integrating with tinhthanhpho.com API.

**Key Endpoints:**
- `GET /api/tinh-thanh-pho` - Get all provinces
- `GET /api/quan-huyen/tinh-thanh-pho/{provinceId}` - Get districts by province
- `GET /api/phuong-xa/quan-huyen/{districtId}` - Get wards by district

**Configuration:**
- Connection timeout: 5000ms
- Read timeout: 10000ms
- Base URL: configurable via `${TINHTHANHPHO_API_URL}`

### 2. AddressLocationService

**Location:** `src/main/java/com/smartrent/service/address/AddressLocationService.java`

Service layer that handles external API calls with resilience patterns.

**Features:**
- **Circuit Breaker**: Protects against external API failures
- **Retry Mechanism**: 3 attempts with exponential backoff
- **Caching**: Redis cache with 24-hour TTL
- **Fallback**: Falls back to local database when API unavailable

**Resilience4j Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      tinhthanhpho:
        failureRateThreshold: 60
        waitDurationInOpenState: 120s
        slidingWindowSize: 20
  retry:
    instances:
      tinhthanhpho:
        maxAttempts: 3
        waitDuration: 2s
```

### 3. Data Models

#### External API DTOs

**ProvinceApiResponse**
```json
{
  "id": "01",
  "name": "Thành phố Hà Nội",
  "code": "01",
  "codeName": "thanh_pho_ha_noi",
  "fullName": "Thành phố Hà Nội"
}
```

**DistrictApiResponse**
```json
{
  "id": "001",
  "name": "Quận Ba Đình",
  "code": "001",
  "provinceId": "01",
  "fullName": "Quận Ba Đình"
}
```

**WardApiResponse**
```json
{
  "id": "00001",
  "name": "Phường Phúc Xá",
  "code": "00001",
  "districtId": "001",
  "fullName": "Phường Phúc Xá"
}
```

### 4. Enhanced Response DTOs

Updated `ProvinceResponse`, `DistrictResponse`, and `WardResponse` to include:
- `externalId`: ID from tinhthanhpho.com API
- `fullName`: Full administrative name
- `codeName`: URL-friendly code name

## API Endpoints

### Local Database Endpoints (Existing)

```
GET  /v1/addresses/provinces
GET  /v1/addresses/provinces/{id}
GET  /v1/addresses/provinces/{id}/districts
GET  /v1/addresses/districts/{id}
GET  /v1/addresses/districts/{id}/wards
GET  /v1/addresses/wards/{id}
```

### External API Integration Endpoints (New)

```
GET  /v1/addresses/external/provinces
GET  /v1/addresses/external/provinces/{externalId}/districts
GET  /v1/addresses/external/districts/{externalId}/wards
POST /v1/addresses/sync/provinces
POST /v1/addresses/cache/clear
```

## Caching Strategy

### Redis Configuration

```yaml
spring:
  cache:
    cacheNames:
      - "locationCache"
    redis:
      expires:
        "[locationCache]": 24h
```

### Cache Keys

- `all-provinces` → List of all provinces
- `province-{externalId}` → Province details
- `districts-province-{provinceExternalId}` → Districts by province
- `district-{externalId}` → District details
- `wards-district-{districtExternalId}` → Wards by district
- `ward-{externalId}` → Ward details

## Error Handling

### New Error Codes

```java
ADDRESS_NOT_FOUND("4005", HttpStatus.NOT_FOUND, "Address not found")
PROVINCE_NOT_FOUND("4006", HttpStatus.NOT_FOUND, "Province not found")
DISTRICT_NOT_FOUND("4007", HttpStatus.NOT_FOUND, "District not found")
WARD_NOT_FOUND("4008", HttpStatus.NOT_FOUND, "Ward not found")
EXTERNAL_SERVICE_ERROR("9001", HttpStatus.SERVICE_UNAVAILABLE, "External service error")
TOO_MANY_REQUESTS("9002", HttpStatus.TOO_MANY_REQUESTS, "Too many requests to external service")
BAD_REQUEST_ERROR("9003", HttpStatus.BAD_REQUEST, "Bad request to external service")
```

### Circuit Breaker States

1. **CLOSED**: Normal operation, calling external API
2. **OPEN**: Too many failures, falling back to local database
3. **HALF_OPEN**: Testing if external API recovered

## Data Synchronization

### Sync Process

1. **Manual Sync** via API endpoint:
   ```
   POST /v1/addresses/sync/provinces
   ```

2. **Sync Flow:**
   ```
   External API → Fetch Data → Map to Entities → Save to DB
   ```

3. **Sync Methods:**
   - `syncProvincesFromExternalApi()` - Sync all provinces
   - `syncDistrictsFromExternalApi(provinceId)` - Sync districts for province
   - `syncWardsFromExternalApi(districtId)` - Sync wards for district

### Data Mapping

The service automatically determines entity types from Vietnamese names:
- "Thành phố" → `CITY`
- "Tỉnh" → `PROVINCE`
- "Quận" → `DISTRICT`
- "Huyện" → `TOWN`
- "Phường" → `WARD`
- "Xã" → `COMMUNE`
- "Thị trấn" → `TOWNSHIP`

## Integration with Listing System

The location pricing information is already integrated into the listing response:

```json
{
  "listingId": 123,
  "locationPricing": {
    "wardPricing": {
      "locationId": 1001,
      "locationName": "Phường Bến Nghé",
      "averagePrice": 1500000
    },
    "districtPricing": {...},
    "provincePricing": {...}
  }
}
```

## Configuration

### Environment Variables

Add to your `.env` or `application-local.yaml`:

```yaml
TINHTHANHPHO_API_URL: "https://tinhthanhpho.com"
```

### Feign Configuration

```yaml
feign:
  client:
    config:
      tinhthanhpho:
        url: "${TINHTHANHPHO_API_URL:https://tinhthanhpho.com}"
        connectTimeout: 5000
        readTimeout: 10000
```

## Testing

### Test External API Integration

```bash
# Get provinces from external API
curl http://localhost:8080/v1/addresses/external/provinces

# Get districts for a province
curl http://localhost:8080/v1/addresses/external/provinces/01/districts

# Sync provinces to local database (requires auth)
curl -X POST http://localhost:8080/v1/addresses/sync/provinces \
  -H "Authorization: Bearer {token}"

# Clear caches (requires auth)
curl -X POST http://localhost:8080/v1/addresses/cache/clear \
  -H "Authorization: Bearer {token}"
```

### Monitor Circuit Breaker

```bash
# Check circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers
```

## Performance Considerations

1. **First Request**: Slower due to API call (~1-2s)
2. **Cached Requests**: Very fast (~10-50ms)
3. **Fallback Mode**: Moderate speed, uses local DB (~100-200ms)
4. **Cache Warming**: Consider warming cache on application startup

## Best Practices

1. **Use External API endpoints** for real-time data
2. **Sync periodically** to keep local database up to date
3. **Monitor circuit breaker** health via actuator endpoints
4. **Clear cache** after syncing new data
5. **Handle fallbacks gracefully** in UI when external API is down

## Future Enhancements

1. **Scheduled Sync**: Auto-sync data periodically
2. **Webhook Support**: Real-time updates from tinhthanhpho.com
3. **Search Optimization**: Full-text search on location names
4. **Geolocation**: Integrate lat/long from external API
5. **Historical Data**: Track administrative changes over time

## Troubleshooting

### External API Not Responding

**Symptom:** All external API calls return errors

**Solution:**
1. Check network connectivity
2. Verify `TINHTHANHPHO_API_URL` configuration
3. Check circuit breaker status
4. System automatically falls back to local database

### Cache Not Working

**Symptom:** Every request hits external API

**Solution:**
1. Verify Redis is running
2. Check Redis connection in `application.yml`
3. Verify cache configuration

### Sync Failing

**Symptom:** Sync endpoint returns errors

**Solution:**
1. Check external API availability
2. Verify database permissions
3. Check application logs for specific errors

## Related Files

### Core Implementation
- `TinhThanhPhoConnector.java` - Feign client
- `TinhThanhPhoFeignConfig.java` - Feign configuration
- `TinhThanhPhoErrorDecoder.java` - Error handling
- `AddressLocationService.java` - Service interface
- `AddressLocationServiceImpl.java` - Service implementation
- `AddressController.java` - REST endpoints

### Models
- `ProvinceApiResponse.java` - External API province DTO
- `DistrictApiResponse.java` - External API district DTO
- `WardApiResponse.java` - External API ward DTO
- `ProvinceResponse.java` - Enhanced response DTO
- `DistrictResponse.java` - Enhanced response DTO
- `WardResponse.java` - Enhanced response DTO

### Configuration
- `application.yml` - Main configuration
- `application-local.yaml` - Local environment config

### Exceptions
- `AppException.java` - Generic application exception
- `DomainCode.java` - Error code enumeration

## Conclusion

This refactoring provides a robust, resilient, and performant address API system that:
- Integrates with external Vietnamese location API
- Falls back gracefully when external service unavailable
- Caches data for optimal performance
- Maintains local database for offline capability
- Follows enterprise patterns (Circuit Breaker, Retry, Caching)