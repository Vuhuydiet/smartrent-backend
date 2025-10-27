# Exception Handling - Address System

## Overview
The address system uses the project's standard exception handling pattern with `DomainException` and `DomainCode`.

## New Exception Class

### ResourceNotFoundException
**Location**: `com.smartrent.infra.exception.ResourceNotFoundException`

**Purpose**: Generic exception for all resource not found scenarios in the address system

**Usage**:
```java
// Simple message
throw new ResourceNotFoundException("Province not found with code: " + code);

// With specific domain code
throw new ResourceNotFoundException(DomainCode.PROVINCE_NOT_FOUND);

// With specific code and parameters
throw new ResourceNotFoundException(DomainCode.RESOURCE_NOT_FOUND, "Province with code: " + code);
```

## New Domain Codes

Added to `DomainCode` enum in the 4xxx (Not Found) section:

| Code | Constant | HTTP Status | Message |
|------|----------|-------------|---------|
| 4009 | RESOURCE_NOT_FOUND | 404 | Resource not found: %s |
| 4010 | STREET_NOT_FOUND | 404 | Street not found |
| 4011 | PROVINCE_MAPPING_NOT_FOUND | 404 | Province mapping not found |
| 4012 | WARD_MAPPING_NOT_FOUND | 404 | Ward mapping not found |

### Existing Address-Related Codes
| Code | Constant | Message |
|------|----------|---------|
| 4005 | ADDRESS_NOT_FOUND | Address not found |
| 4006 | PROVINCE_NOT_FOUND | Province not found |
| 4007 | DISTRICT_NOT_FOUND | District not found |
| 4008 | WARD_NOT_FOUND | Ward not found |

## How It Works

### Exception Flow
```
AddressServiceImpl
    ↓ throws ResourceNotFoundException
DomainException (parent class)
    ↓ caught by
GlobalExceptionHandler
    ↓ returns
ApiResponse<Void> with error details
```

### Example in Service
```java
public NewProvinceResponse getNewProvinceByCode(String code) {
    NewProvince province = newProvinceRepository.findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Province not found with code: " + code
        ));
    return toNewProvinceResponse(province);
}
```

### Response Format
```json
{
  "code": "4009",
  "message": "Resource not found: Province with code: 99",
  "data": null,
  "success": false
}
```

## Best Practices

### 1. Use Specific Domain Codes When Available
```java
// Good - specific
throw new ResourceNotFoundException(DomainCode.PROVINCE_NOT_FOUND);

// OK - generic with details
throw new ResourceNotFoundException("Province not found with code: " + code);
```

### 2. Include Context in Error Messages
```java
// Good - includes what was searched
throw new ResourceNotFoundException("Ward not found with code: " + code);

// Bad - too generic
throw new ResourceNotFoundException("Not found");
```

### 3. Use in Repository Optional Handling
```java
// Pattern used in AddressServiceImpl
Entity entity = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Entity not found: " + id));
```

## Global Exception Handler

The `GlobalExceptionHandler` automatically handles all exceptions:

```java
@ExceptionHandler(DomainException.class)
public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException exception) {
    return ResponseEntity.status(exception.getDomainCode().getStatus())
        .body(ApiResponse.<Void>builder()
            .code(exception.getDomainCode().getValue())
            .message(exception.getMessage())
            .build());
}
```

Since `ResourceNotFoundException` extends `DomainException`, it's handled automatically without additional configuration.

## Testing

### Unit Test Example
```java
@Test
void testGetProvinceByCode_NotFound() {
    when(newProvinceRepository.findByCode("99"))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> addressService.getNewProvinceByCode("99"));
}
```

### Integration Test Example
```java
@Test
void testGetProvinceByCode_NotFound_ReturnsNotFound() {
    mockMvc.perform(get("/v1/addresses/new-provinces/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("4009"))
        .andExpect(jsonPath("$.message").exists());
}
```

## Error Code Reference

All error codes follow the pattern:
- **1xxx**: Internal server errors (developer errors)
- **2xxx**: Bad request (client input errors)
- **3xxx**: Conflict (existing/duplicate data)
- **4xxx**: Not found errors ← **Address system uses these**
- **5xxx**: Unauthorized (unauthenticated)
- **6xxx**: Forbidden (no permission)
- **7xxx**: Payment errors
- **8xxx**: File storage errors
- **9xxx**: External service errors

## Related Files

- **Exception Class**: `src/main/java/com/smartrent/infra/exception/ResourceNotFoundException.java`
- **Domain Codes**: `src/main/java/com/smartrent/infra/exception/model/DomainCode.java`
- **Global Handler**: `src/main/java/com/smartrent/infra/exception/GlobalExceptionHandler.java`
- **Service Usage**: `src/main/java/com/smartrent/service/address/AddressServiceImpl.java`

---

**Status**: ✅ Implemented and Ready
**Date**: 2025-01-26