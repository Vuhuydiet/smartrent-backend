# AddressService Implementation Update Plan

## Issues Found

### 1. Repository Dependencies (Line 36-38)
**Current:**
```java
private final ProvinceMappingRepository provinceMappingRepository;
private final WardMappingRepository wardMappingRepository;
private final DistrictWardMappingRepository districtWardMappingRepository;
```

**Should be:**
```java
private final AddressMappingRepository addressMappingRepository;
// WardMappingRepository can be kept if still used elsewhere
// Remove ProvinceMappingRepository and DistrictWardMappingRepository
```

### 2. Legacy Methods Using Old Repository Calls

**Lines 112, 117:** `findByProvinceId` → `findByProvinceCode` (needs province code, not ID)
**Lines 128, 133:** `findByDistrictId` → `findByDistrictCode` (needs district code, not ID)

### 3. Mapper Methods Need Complete Rewrite

**toNewProvinceResponse (Line 277-288):**
- Remove: `nameEn`, `fullName`, `fullNameEn`, `codeName`, `administrativeUnit`
- Add: `shortName`, `key`, `latitude`, `longitude`, `alias`

**toNewWardResponse (Line 290-303):**
- Remove: `nameEn`, `fullName`, `fullNameEn`, `codeName`, `administrativeUnit`
- Use denormalized fields: `provinceCode`, `provinceName` (already in Ward entity)
- Add: `shortName`, `key`, `type`, `latitude`, `longitude`

**toLegacyProvinceResponse (Line 305-312):**
- Remove: `nameEn`
- Add: `shortName`, `key`

**toLegacyDistrictResponse (Line 314-323):**
- Remove: `nameEn`, `prefix`
- Use `district.getProvinceCode()` instead of `district.getProvince().getId()`
- Use denormalized `provinceName` from District entity

**toLegacyWardResponse (Line 325-336):**
- Remove: `nameEn`, `prefix`
- Use denormalized fields directly: `provinceCode`, `provinceName`, `districtCode`, `districtName`

### 4. Address Conversion Methods

**convertLegacyToNew (Line 163-192):**
Replace with AddressMappingRepository:
```java
AddressMapping mapping = addressMappingRepository.findBestByLegacyAddress(
    legacyProvince.getCode(),
    legacyDistrict.getCode(),
    legacyWard.getCode()
).orElseThrow(...);
```

**convertNewToLegacy (Line 195-275):**
Replace with AddressMappingRepository:
```java
AddressMapping mapping = addressMappingRepository.findDefaultByNewAddress(
    newProvinceCode,
    newWardCode
).orElseThrow(...);
```

## Action Plan

1. Update repository dependencies
2. Fix all `findByProvinceId` → `findByProvinceCode`
3. Fix all `findByDistrictId` → `findByDistrictCode`
4. Rewrite all mapper methods
5. Update conversion methods to use AddressMappingRepository
6. Build and test
7. Fix any remaining compilation errors

## Files to Update

- `AddressServiceImpl.java` - Main service implementation
- Response DTOs - May need to add/remove fields to match new entity structure
