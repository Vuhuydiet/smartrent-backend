# Final Fixes Required - Address Migration Update

## Status: 90% Complete ✅

Đã hoàn thành 9/12 tasks chính, còn 32 compilation errors cần fix.

## ✅ Completed (9 tasks)

1. **Entities** - 100% done
   - LegacyProvince, District, LegacyWard
   - Province, Ward
   - AddressMapping (NEW)

2. **Repositories** - 100% done
   - All repositories updated for new entity structure
   - AddressMappingRepository created

3. **DTOs** - 100% done
   - All 5 Response DTOs updated

4. **Mappers** - 100% done
   - AddressMapperImpl updated

5. **Service Layer** - 75% done
   - AddressServiceImpl: Added AddressMapper dependency
   - Updated public API methods to use addressMapper
   - Fixed getLegacyDistrictsByProvince/searchLegacyDistrictsByProvince
   - Fixed getLegacyWardsByDistrict/searchLegacyWardsByDistrict
   - Updated convertLegacyToNew to use AddressMappingRepository

## ❌ Remaining Issues (3 tasks, ~32 errors)

### 1. AddressServiceImpl - Delete Duplicate Mapper Methods (Lines 299-416)

**Location**: `src/main/java/com/smartrent/service/address/impl/AddressServiceImpl.java:299-416`

**What to do**: Delete these methods (already duplicated in AddressMapperImpl):
- `toNewProvinceResponse()` (line 300-311)
- `toNewWardResponse()` (line 313-326)
- `toLegacyProvinceResponse()` (line 328-335)
- `toLegacyDistrictResponse()` (line 337-346)
- `toLegacyWardResponse()` (line 348-359)
- `toLegacyStreetResponse()` (line 361-387)
- `toLegacyProjectResponse()` (line 389-416)

**Then replace all usages**:
- `this::toLegacyProvinceResponse` → `addressMapper::toLegacyProvinceResponse`
- `this::toLegacyDistrictResponse` → `addressMapper::toLegacyDistrictResponse`
- `this::toLegacyWardResponse` → `addressMapper::toLegacyWardResponse`
- `this::toLegacyStreetResponse` → `addressMapper::toLegacyStreetResponse`
- `this::toLegacyProjectResponse` → `addressMapper::toLegacyProjectResponse`

### 2. AddressServiceImpl - Fix Interface Methods (Lines 422-690)

**Methods using wrong repository calls**:

**Lines 449-450** - `getDistrictsByProvinceId()`:
```java
// OLD:
return legacyDistrictRepository.findByProvinceId(provinceId).stream()

// NEW:
LegacyProvince province = legacyProvinceRepository.findById(provinceId)
    .orElseThrow(() -> new ResourceNotFoundException("Province not found"));
return legacyDistrictRepository.findByProvinceCode(province.getCode()).stream()
```

**Lines 464-465** - `searchDistricts()`:
```java
// OLD:
return legacyDistrictRepository.findByProvinceId(provinceId).stream()

// NEW:
LegacyProvince province = legacyProvinceRepository.findById(provinceId)
    .orElseThrow(...);
return legacyDistrictRepository.findByProvinceCode(province.getCode()).stream()
```

**Lines 477-478** - `getWardsByDistrictId()`:
```java
// OLD:
return legacyWardRepository.findByDistrictId(districtId).stream()

// NEW:
District district = legacyDistrictRepository.findById(districtId)
    .orElseThrow(...);
return legacyWardRepository.findByDistrictCode(district.getCode()).stream()
```

**Lines 492-493** - `searchWards()`:
```java
// OLD:
return legacyWardRepository.findByDistrictId(districtId).stream()

// NEW:
District district = legacyDistrictRepository.findById(districtId)
    .orElseThrow(...);
return legacyWardRepository.findByDistrictCode(district.getCode()).stream()
```

### 3. AddressServiceImpl - Fix convertNewToLegacy Method (Lines 217-297)

**Current code** uses old repositories (wardMappingRepository, districtWardMappingRepository)

**Replace entire method** with:
```java
@Override
public AddressConversionResponse convertNewToLegacy(String newProvinceCode, String newWardCode) {
    log.info("Converting new address to legacy - provinceCode: {}, wardCode: {}", newProvinceCode, newWardCode);

    // Get new address
    Province newProvince = newProvinceRepository.findByCode(newProvinceCode)
            .orElseThrow(() -> new ResourceNotFoundException("Province not found with code: " + newProvinceCode));
    Ward newWard = newWardRepository.findByCode(newWardCode)
            .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + newWardCode));

    NewFullAddressResponse newAddress = NewFullAddressResponse.builder()
            .province(addressMapper.toNewProvinceResponse(newProvince))
            .ward(addressMapper.toNewWardResponse(newWard))
            .build();

    // Find default mapping for new address
    AddressMapping mapping = addressMappingRepository.findDefaultByNewAddress(newProvinceCode, newWardCode)
            .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("No address mapping found for new address: province=%s, ward=%s",
                            newProvinceCode, newWardCode)));

    // Get legacy address from mapping
    LegacyProvince legacyProvince = mapping.getLegacyProvince();
    District legacyDistrict = mapping.getLegacyDistrict();
    LegacyWard legacyWard = mapping.getLegacyWard();

    FullAddressResponse legacyAddress = FullAddressResponse.builder()
            .province(addressMapper.toLegacyProvinceResponse(legacyProvince))
            .district(legacyDistrict != null ? addressMapper.toLegacyDistrictResponse(legacyDistrict) : null)
            .ward(legacyWard != null ? addressMapper.toLegacyWardResponse(legacyWard) : null)
            .build();

    // Check if there are multiple legacy addresses for this new ward
    List<AddressMapping> allMappings = addressMappingRepository.findByNewAddress(newProvinceCode, newWardCode);
    String conversionNote = allMappings.size() > 1
            ? String.format("Converted to legacy structure. Note: This new ward was merged from %d legacy wards. " +
                    "Showing default/primary source. Divided: %s, Merged: %s",
                    allMappings.size(), mapping.getIsDividedWard(), mapping.getIsMergedWard())
            : String.format("Converted to legacy structure. Divided: %s, Merged: %s",
                    mapping.getIsDividedWard(), mapping.getIsMergedWard());

    return AddressConversionResponse.builder()
            .legacyAddress(legacyAddress)
            .newAddress(newAddress)
            .conversionNote(conversionNote)
            .build();
}
```

### 4. AddressCreationServiceImpl (8 errors)

**File**: `src/main/java/com/smartrent/service/address/impl/AddressCreationServiceImpl.java`

**Lines 97-98** - Replace `ward.getPrefix()` with `ward.getType()`:
```java
// OLD:
if (ward.getPrefix() != null && !ward.getPrefix().isEmpty()) {
    sb.append(ward.getPrefix()).append(" ");

// NEW:
if (ward.getType() != null && !ward.getType().isEmpty()) {
    sb.append(ward.getType()).append(" ");
```

**Lines 106-107** - Replace `district.getPrefix()` with `district.getType()`:
```java
// OLD:
if (district.getPrefix() != null && !district.getPrefix().isEmpty()) {
    sb.append(district.getPrefix()).append(" ");

// NEW:
if (district.getType() != null && !district.getType().isEmpty()) {
    sb.append(district.getType()).append(" ");
```

**Line 146** - Replace `ward.getFullName()` with `ward.getName()`:
```java
// OLD:
sb.append(ward.getFullName()).append(", ");

// NEW:
sb.append(ward.getName()).append(", ");
```

**Line 151** - Replace `province.getFullName()` with `province.getName()`:
```java
// OLD:
sb.append(province.getFullName());

// NEW:
sb.append(province.getName());
```

## Quick Fix Command (if needed)

Nếu muốn fix nhanh bằng sed commands:

```bash
cd "D:\DEV\datn\smartrent-backend\smart-rent\src\main\java\com\smartrent\service\address\impl"

# Fix AddressCreationServiceImpl
sed -i 's/ward\.getPrefix()/ward.getType()/g' AddressCreationServiceImpl.java
sed -i 's/district\.getPrefix()/district.getType()/g' AddressCreationServiceImpl.java
sed -i 's/ward\.getFullName()/ward.getName()/g' AddressCreationServiceImpl.java
sed -i 's/province\.getFullName()/province.getName()/g' AddressCreationServiceImpl.java
```

## Estimated Time to Complete

- Task 1 (Delete duplicate mappers): 10 minutes
- Task 2 (Fix interface methods): 15 minutes
- Task 3 (Fix convertNewToLegacy): 5 minutes
- Task 4 (Fix AddressCreationServiceImpl): 5 minutes

**Total**: ~35 minutes of focused work

## After Fixes

Run build to verify:
```bash
./gradlew build -x test
```

Expected result: 0 compilation errors ✅
