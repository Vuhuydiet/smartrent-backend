# Address Flow Guide

This document explains the address architecture in SmartRent, including the dual-structure system and migration path between old and new address formats.

## Table of Contents

- [Overview](#overview)
- [Address Structures](#address-structures)
- [Data Model](#data-model)
- [Address Flow](#address-flow)
- [Mapping Between Structures](#mapping-between-structures)
- [Migration Strategy](#migration-strategy)
- [API Usage](#api-usage)
- [Examples](#examples)

## Overview

SmartRent supports two administrative address structures for Vietnam:

1. **Legacy Structure**: 63 provinces, 3-tier hierarchy (Province → District → Ward)
   - Used before July 1, 2025
   - Integer-based IDs
   - ~700 districts, ~11,000 wards

2. **New Structure**: 34 provinces, 2-tier hierarchy (Province → Ward)
   - Effective from July 1, 2025
   - String-based codes
   - District level eliminated, wards reorganized (~2,000 wards)

The system maintains **both structures simultaneously** for backward compatibility and smooth transition.

## Address Structures

### Legacy Structure (OLD)

```
LegacyProvince (63 provinces)
├── Primary Key: Integer ID
├── Fields: id, name, nameEn, code
└── Children
    ├── District (~700 districts)
    │   ├── Primary Key: Integer ID
    │   ├── Fields: id, name, nameEn, prefix
    │   └── Children
    │       └── LegacyWard (~11,000 wards)
    │           ├── Primary Key: Integer ID
    │           └── Fields: id, name, nameEn, prefix
    └── Street
        ├── Primary Key: Integer ID
        ├── Fields: id, name, nameEn, prefix
        └── Foreign Keys: provinceId, districtId
```

**Example**: Hà Nội (Province #1) → Ba Đình (District #5) → Phường Phúc Xá (Ward #169)

### New Structure (NEW)

```
Province (34 provinces)
├── Primary Key: VARCHAR code (e.g., "01")
├── Fields: code, name, nameEn, fullName, fullNameEn, codeName
├── FK: administrativeUnitId
└── Children
    └── Ward (~2,000 wards)
        ├── Primary Key: VARCHAR code (e.g., "00004")
        ├── Fields: code, name, nameEn, fullName, fullNameEn, codeName
        └── FK: provinceCode, administrativeUnitId
```

**Example**: Hà Nội (Code "01") → Phường Phúc Xá (Code "00169")

**Key Difference**: District level removed, wards directly under provinces.

## Data Model

### Core Entities

#### 1. Address Entity
**File**: `src/main/java/com/smartrent/infra/repository/entity/Address.java`

```java
@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    // Legacy formatted address (3-tier)
    @Column(name = "full_address", columnDefinition = "TEXT")
    private String fullAddress;

    // New formatted address (2-tier)
    @Column(name = "full_newaddress", columnDefinition = "TEXT")
    private String fullNewAddress;

    // Coordinates
    @Column(precision = 10, scale = 8)
    private Double latitude;

    @Column(precision = 11, scale = 8)
    private Double longitude;

    // One-to-many with Listings
    @OneToMany(mappedBy = "address")
    private Set<Listing> listings;

    // Key methods
    public String getDisplayAddress() {
        return fullNewAddress != null ? fullNewAddress : fullAddress;
    }

    public boolean isNewStructure() {
        return fullNewAddress != null;
    }
}
```

**Purpose**: Stores both formatted address strings, allowing listings to exist with either structure.

#### 2. AddressMetadata Entity
**File**: `src/main/java/com/smartrent/infra/repository/entity/AddressMetadata.java`

```java
@Entity
@Table(name = "address_metadata")
public class AddressMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long metadataId;

    @Column(unique = true)
    private Long addressId;  // FK to Address

    @Enumerated(EnumType.STRING)
    private AddressType addressType;  // OLD or NEW

    // --- OLD STRUCTURE FIELDS ---
    private Integer provinceId;
    private Integer districtId;
    private Integer wardId;

    // --- NEW STRUCTURE FIELDS ---
    private String newProvinceCode;
    private String newWardCode;

    // --- COMMON FIELDS ---
    private Integer streetId;
    private Integer projectId;
    private String streetNumber;
}
```

**Purpose**: Enables efficient querying by administrative units without parsing address strings.

**Indexes**:
- `idx_address_type` - Filter by OLD/NEW
- `idx_old_province`, `idx_old_district`, `idx_old_ward` - Legacy structure queries
- `idx_new_province`, `idx_new_ward` - New structure queries
- `idx_street`, `idx_project` - Common queries

### Mapping Entities

#### ProvinceMapping
**File**: `src/main/java/com/smartrent/infra/repository/entity/ProvinceMapping.java`

Maps legacy provinces to new provinces (63 → 34 consolidation).

```java
@Entity
@Table(name = "province_mapping")
public class ProvinceMapping {
    @ManyToOne
    @JoinColumn(name = "province_legacy_id")
    private LegacyProvince legacyProvince;

    @ManyToOne
    @JoinColumn(name = "province_new_code")
    private Province newProvince;

    private LocalDate effectiveDate;  // 2025-07-01
}
```

**Example**:
- Legacy Hà Nội (ID: 1) → New Hà Nội (Code: "01")
- Legacy Hải Phòng (ID: 31) → New Hải Phòng (Code: "02")

#### DistrictWardMapping
**File**: `src/main/java/com/smartrent/infra/repository/entity/DistrictWardMapping.java`

Maps legacy districts to new wards (districts eliminated in new structure).

```java
@Entity
@Table(name = "district_ward_mapping")
public class DistrictWardMapping {
    @ManyToOne
    @JoinColumn(name = "district_legacy_id")
    private District legacyDistrict;

    @ManyToOne
    @JoinColumn(name = "ward_new_code")
    private Ward newWard;

    private LocalDate effectiveDate;  // 2025-07-01
}
```

**Example**: Legacy Ba Đình District (ID: 5) → New Ba Đình Ward (Code: "00004")

#### WardMapping
**File**: `src/main/java/com/smartrent/infra/repository/entity/WardMapping.java`

Maps legacy wards to new wards with merge/split tracking.

```java
@Entity
@Table(name = "ward_mapping")
public class WardMapping {
    @ManyToOne
    @JoinColumn(name = "ward_legacy_id")
    private LegacyWard legacyWard;

    @ManyToOne
    @JoinColumn(name = "ward_new_code")
    private Ward newWard;

    @Enumerated(EnumType.STRING)
    private MergeType mergeType;  // UNCHANGED, MERGED_WITH_OTHERS, SPLIT_TO_MULTIPLE, RENAMED

    private LocalDate effectiveDate;
}
```

**Merge Types**:
- `UNCHANGED`: Ward kept same boundaries
- `MERGED_WITH_OTHERS`: Multiple old wards merged into one new ward
- `SPLIT_TO_MULTIPLE`: One old ward split into multiple new wards
- `RENAMED`: Same boundaries, different name

**Examples**:
- Legacy Phường Phúc Xá (ID: 169) → New Phường Phúc Xá (Code: "00169") [UNCHANGED]
- Legacy Phường A + Phường B → New Phường AB (Code: "00XXX") [MERGED_WITH_OTHERS]

#### StreetMapping
**File**: `src/main/java/com/smartrent/infra/repository/entity/StreetMapping.java`

Maps legacy streets to new administrative structure.

```java
@Entity
@Table(name = "street_mapping")
public class StreetMapping {
    @ManyToOne
    @JoinColumn(name = "street_legacy_id")
    private Street legacyStreet;

    @ManyToOne
    @JoinColumn(name = "new_province_code")
    private Province newProvince;

    @ManyToOne
    @JoinColumn(name = "new_ward_code")
    private Ward newWard;  // Optional - some streets span multiple wards

    private LocalDate effectiveDate;
}
```

## Address Flow

### 1. Address Creation Flow

```
User Input (AddressCreationRequest)
    ├── addressType: OLD or NEW
    ├── Structure-specific fields
    └── Common fields (streetId, projectId, streetNumber, lat/lng)

    ↓

AddressCreationService.createAddress()
    ├── Validate request fields
    ├── Build formatted address string
    │   ├── If OLD: buildOldAddressString()
    │   │   → "streetNumber + street, ward, district, province"
    │   └── If NEW: buildNewAddressString()
    │       → "streetNumber + street, ward, province"
    ├── Create Address entity
    │   ├── Set fullAddress (if OLD) or fullNewAddress (if NEW)
    │   └── Set coordinates
    ├── Save Address
    └── Create AddressMetadata
        ├── Set addressType
        ├── Set structure-specific fields
        └── Set common fields

    ↓

Database
    ├── addresses table (addressId, fullAddress, fullNewAddress, lat, lng)
    └── address_metadata table (metadataId, addressId, addressType, ...)
```

### 2. Address Query Flow

#### Legacy Structure Query
```
GET /v1/addresses/provinces/1/districts/5/wards
    ↓
AddressService.getWardsByDistrictId(5)
    ↓
LegacyWardRepository.findByDistrictId(5)
    ↓
Returns: List<LegacyWard>
```

#### New Structure Query
```
GET /v1/addresses/new-provinces/01/wards
    ↓
NewAddressService.getWardsByNewProvince("01", keyword, page, limit)
    ↓
WardRepository.findByProvinceCode("01", pageable)
    ↓
Returns: Page<Ward>
```

#### Listing Search by Location
```
GET /v1/listings?provinceId=1&districtId=5
    ↓
ListingService.searchListings(filters)
    ↓
AddressMetadataRepository.findByProvinceIdAndDistrictId(1, 5)
    ↓
Returns: List<AddressMetadata>
    ↓
Extract addressIds
    ↓
ListingRepository.findByAddressIdIn(addressIds)
    ↓
Returns: List<Listing>
```

### 3. Address Conversion Flow

#### Legacy → New Conversion
```
Input: provinceId=1, districtId=5, wardId=169
    ↓
AddressService.convertLegacyToNew(1, 5, 169)
    ↓
Step 1: Province Mapping
    ProvinceMappingRepository.findByLegacyProvinceId(1)
    → newProvinceCode = "01"

Step 2: Ward Mapping
    WardMappingRepository.findByLegacyWardId(169)
    → newWardCode = "00169"
    → mergeType = UNCHANGED

    ↓
Returns: {newProvinceCode: "01", newWardCode: "00169", mergeType: "UNCHANGED"}
```

#### New → Legacy Conversion
```
Input: newProvinceCode="01", newWardCode="00169"
    ↓
AddressService.convertNewToLegacy("01", "00169")
    ↓
Step 1: Province Mapping
    ProvinceMappingRepository.findByNewProvinceCode("01")
    → provinceId = 1

Step 2: Ward Mapping
    WardMappingRepository.findByNewWardCode("00169")
    → wardId = 169
    → Get legacy ward details
    → districtId = 5

    ↓
Returns: {provinceId: 1, districtId: 5, wardId: 169}
```

### 4. Display Address Flow

```
Listing Entity
    ↓
@ManyToOne Address
    ↓
address.getDisplayAddress()
    ├── If fullNewAddress != null
    │   └── Return fullNewAddress (NEW structure)
    └── Else
        └── Return fullAddress (OLD structure)
```

**Logic**: Automatically uses new format when available, falls back to legacy format.

## Mapping Between Structures

### Province Level (63 → 34)

**Consolidation Examples**:

| Legacy Provinces | New Province |
|-----------------|--------------|
| Hà Nội (ID: 1) | Hà Nội (Code: "01") |
| Hải Phòng (ID: 31) | Hải Phòng (Code: "02") |
| Hồ Chí Minh (ID: 79) | Hồ Chí Minh (Code: "79") |
| Hà Tây + Hà Nội | Hà Nội (Code: "01") |

**Key Points**:
- 63 legacy provinces merged into 34 new provinces
- Some provinces unchanged (major cities)
- Some provinces merged (e.g., Hà Tây absorbed into Hà Nội)
- ProvinceMapping provides complete mapping

### District → Ward Level

**District Elimination Examples**:

| Legacy Structure | New Structure |
|-----------------|---------------|
| Hà Nội → Ba Đình District → Phường Phúc Xá | Hà Nội → Phường Phúc Xá |
| Hà Nội → Hoàn Kiếm District → Phường Hàng Bạc | Hà Nội → Phường Hàng Bạc |

**Key Points**:
- District level completely removed
- Wards promoted to second tier
- District names may become ward names (e.g., Ba Đình)
- DistrictWardMapping provides district → new ward mapping

### Ward Level (~11,000 → ~2,000)

**Ward Transformation Examples**:

1. **UNCHANGED**: Ward keeps same boundaries
   ```
   Legacy: Phường Phúc Xá (ID: 169, District: Ba Đình)
   New: Phường Phúc Xá (Code: "00169", Province: Hà Nội)
   ```

2. **MERGED_WITH_OTHERS**: Multiple wards merged
   ```
   Legacy: Phường A (ID: 100) + Phường B (ID: 101)
   New: Phường AB (Code: "00200")
   ```

3. **SPLIT_TO_MULTIPLE**: One ward split
   ```
   Legacy: Phường X (ID: 150)
   New: Phường X1 (Code: "00300") + Phường X2 (Code: "00301")
   ```

4. **RENAMED**: Same boundaries, new name
   ```
   Legacy: Phường Cũ (ID: 175)
   New: Phường Mới (Code: "00350")
   ```

## Migration Strategy

### For Writing Address Migration

When migrating existing addresses from OLD to NEW structure:

```sql
-- Step 1: Identify addresses to migrate
SELECT
    a.address_id,
    am.province_id,
    am.district_id,
    am.ward_id
FROM addresses a
JOIN address_metadata am ON a.address_id = am.address_id
WHERE am.address_type = 'OLD'
  AND a.full_newaddress IS NULL;

-- Step 2: Map to new structure using mapping tables
SELECT
    a.address_id,
    pm.province_new_code AS new_province_code,
    wm.ward_new_code AS new_ward_code,
    wm.merge_type
FROM addresses a
JOIN address_metadata am ON a.address_id = am.address_id
LEFT JOIN province_mapping pm ON am.province_id = pm.province_legacy_id
LEFT JOIN ward_mapping wm ON am.ward_id = wm.ward_legacy_id
WHERE am.address_type = 'OLD';

-- Step 3: Build new address strings
-- (Use AddressCreationService.buildNewAddressString() in Java)

-- Step 4: Update Address entity
UPDATE addresses a
JOIN (
    -- Subquery with new address data
) new_data ON a.address_id = new_data.address_id
SET
    a.full_newaddress = new_data.formatted_new_address;

-- Step 5: Create new AddressMetadata entries
INSERT INTO address_metadata (
    address_id,
    address_type,
    new_province_code,
    new_ward_code,
    street_id,
    project_id,
    street_number
)
SELECT
    a.address_id,
    'NEW',
    pm.province_new_code,
    wm.ward_new_code,
    am.street_id,
    am.project_id,
    am.street_number
FROM addresses a
JOIN address_metadata am ON a.address_id = am.address_id
JOIN province_mapping pm ON am.province_id = pm.province_legacy_id
JOIN ward_mapping wm ON am.ward_id = wm.ward_legacy_id
WHERE am.address_type = 'OLD';
```

### Migration Considerations

1. **Merge Conflicts**:
   - When `mergeType = MERGED_WITH_OTHERS`, multiple old addresses → same new address
   - Listings at these addresses should be grouped under new ward
   - Coordinates may need averaging if multiple old wards merged

2. **Split Conflicts**:
   - When `mergeType = SPLIT_TO_MULTIPLE`, one old address → multiple new addresses
   - Need additional data (exact coordinates, street number) to determine correct new ward
   - May require manual review

3. **Street Mapping**:
   - Streets may span multiple wards in new structure
   - Use StreetMapping.newWardCode when available
   - Fall back to coordinates for disambiguation

4. **Coordinate Validation**:
   - Vietnam boundaries: 8.0-23.5°N, 102.0-110.0°E
   - Use coordinates to verify ward assignment
   - Coordinates remain unchanged during migration

### Migration Service Pattern

**Recommended Approach**:

```java
@Service
public class AddressMigrationService {

    public void migrateAddressToNewStructure(Long addressId) {
        // 1. Load existing address and metadata
        Address address = addressRepository.findById(addressId);
        AddressMetadata oldMetadata = metadataRepository.findByAddressId(addressId);

        if (oldMetadata.getAddressType() != AddressType.OLD) {
            throw new IllegalStateException("Address already migrated");
        }

        // 2. Map to new structure
        ProvinceMapping provinceMapping = provinceMappingRepo
            .findByLegacyProvinceId(oldMetadata.getProvinceId());
        WardMapping wardMapping = wardMappingRepo
            .findByLegacyWardId(oldMetadata.getWardId());

        // 3. Handle merge/split cases
        String newWardCode = resolveNewWardCode(
            wardMapping,
            address.getLatitude(),
            address.getLongitude()
        );

        // 4. Build new address string
        String newAddressString = buildNewAddressString(
            provinceMapping.getNewProvince(),
            wardMapping.getNewWard(),
            oldMetadata.getStreetId(),
            oldMetadata.getStreetNumber()
        );

        // 5. Update Address entity
        address.setFullNewAddress(newAddressString);
        addressRepository.save(address);

        // 6. Create new AddressMetadata
        AddressMetadata newMetadata = new AddressMetadata();
        newMetadata.setAddressId(addressId);
        newMetadata.setAddressType(AddressType.NEW);
        newMetadata.setNewProvinceCode(provinceMapping.getNewProvinceCode());
        newMetadata.setNewWardCode(newWardCode);
        newMetadata.setStreetId(oldMetadata.getStreetId());
        newMetadata.setProjectId(oldMetadata.getProjectId());
        newMetadata.setStreetNumber(oldMetadata.getStreetNumber());
        metadataRepository.save(newMetadata);
    }

    private String resolveNewWardCode(WardMapping mapping, Double lat, Double lng) {
        if (mapping.getMergeType() == MergeType.UNCHANGED ||
            mapping.getMergeType() == MergeType.RENAMED) {
            return mapping.getNewWardCode();
        }

        if (mapping.getMergeType() == MergeType.SPLIT_TO_MULTIPLE) {
            // Find all possible new wards
            List<WardMapping> splits = wardMappingRepo
                .findAllByLegacyWardId(mapping.getLegacyWardId());

            // Use coordinates to determine correct ward
            return findClosestWard(splits, lat, lng);
        }

        // MERGED_WITH_OTHERS - straightforward mapping
        return mapping.getNewWardCode();
    }
}
```

## API Usage

### Creating an Address (Old Structure)

```http
POST /v1/addresses
Content-Type: application/json

{
  "addressType": "OLD",
  "provinceId": 1,
  "districtId": 5,
  "wardId": 169,
  "streetId": 1234,
  "streetNumber": "123",
  "latitude": 21.0285,
  "longitude": 105.8542
}
```

**Response**:
```json
{
  "addressId": 1,
  "fullAddress": "123 Phố Phúc Xá, Phường Phúc Xá, Quận Ba Đình, Hà Nội",
  "displayAddress": "123 Phố Phúc Xá, Phường Phúc Xá, Quận Ba Đình, Hà Nội",
  "addressType": "OLD",
  "provinceName": "Hà Nội",
  "districtName": "Ba Đình",
  "wardName": "Phúc Xá"
}
```

### Creating an Address (New Structure)

```http
POST /v1/addresses
Content-Type: application/json

{
  "addressType": "NEW",
  "newProvinceCode": "01",
  "newWardCode": "00169",
  "streetId": 1234,
  "streetNumber": "123",
  "latitude": 21.0285,
  "longitude": 105.8542
}
```

**Response**:
```json
{
  "addressId": 2,
  "fullNewAddress": "123 Phố Phúc Xá, Phường Phúc Xá, Hà Nội",
  "displayAddress": "123 Phố Phúc Xá, Phường Phúc Xá, Hà Nội",
  "addressType": "NEW",
  "newProvinceName": "Hà Nội",
  "newWardName": "Phúc Xá"
}
```

### Converting Legacy to New

```http
GET /v1/addresses/convert/legacy-to-new?provinceId=1&districtId=5&wardId=169
```

**Response**:
```json
{
  "newProvinceCode": "01",
  "newProvinceName": "Hà Nội",
  "newWardCode": "00169",
  "newWardName": "Phúc Xá",
  "mergeType": "UNCHANGED",
  "effectiveDate": "2025-07-01"
}
```

### Searching Listings by Location (Old Structure)

```http
GET /v1/listings?provinceId=1&districtId=5&page=0&size=20
```

### Searching Listings by Location (New Structure)

```http
GET /v1/listings?newProvinceCode=01&newWardCode=00169&page=0&size=20
```

## Examples

### Example 1: Complete Migration of a Listing Address

**Before Migration**:
```
Address ID: 1001
Full Address: "45 Đường Láng, Phường Láng Thượng, Quận Đống Đa, Hà Nội"
Address Type: OLD
Province: Hà Nội (ID: 1)
District: Đống Đa (ID: 6)
Ward: Láng Thượng (ID: 201)
Coordinates: (21.0245, 105.8112)
```

**Mapping Lookup**:
```java
// Province: 1 (Hà Nội) → "01" (Hà Nội)
ProvinceMapping: legacyId=1 → newCode="01"

// Ward: 201 (Láng Thượng) → "00201" (Láng Thượng)
WardMapping: legacyId=201 → newCode="00201", mergeType=UNCHANGED
```

**After Migration**:
```
Address ID: 1001
Full Address: "45 Đường Láng, Phường Láng Thượng, Quận Đống Đa, Hà Nội" (kept)
Full New Address: "45 Đường Láng, Phường Láng Thượng, Hà Nội" (added)
Address Type: OLD (original metadata kept)
Coordinates: (21.0245, 105.8112) (unchanged)

New AddressMetadata Created:
- Address Type: NEW
- Province: "01" (Hà Nội)
- Ward: "00201" (Láng Thượng)
- Street ID: same
- Street Number: "45"
```

**Result**: Address now queryable by both OLD and NEW structures.

### Example 2: Handling Merged Wards

**Scenario**: Two legacy wards merged into one new ward.

**Before Migration**:
```
Address A:
- Ward: Phường Hàng Bạc (ID: 301)
- District: Hoàn Kiếm (ID: 7)

Address B:
- Ward: Phường Hàng Đào (ID: 302)
- District: Hoàn Kiếm (ID: 7)
```

**Mapping**:
```java
WardMapping A: legacyId=301 → newCode="00700", mergeType=MERGED_WITH_OTHERS
WardMapping B: legacyId=302 → newCode="00700", mergeType=MERGED_WITH_OTHERS
```

**After Migration**:
```
Both addresses now map to:
- Province: "01" (Hà Nội)
- Ward: "00700" (Hoàn Kiếm)

Old distinction preserved in fullAddress field.
Listings searchable by new ward code "00700".
```

### Example 3: Handling Split Wards

**Scenario**: One legacy ward split into two new wards.

**Before Migration**:
```
Address A:
- Ward: Phường Cũ (ID: 400)
- Coordinates: (21.0300, 105.8500)

Address B:
- Ward: Phường Cũ (ID: 400)
- Coordinates: (21.0320, 105.8520)
```

**Mapping**:
```java
WardMapping Option 1: legacyId=400 → newCode="00800", mergeType=SPLIT_TO_MULTIPLE
WardMapping Option 2: legacyId=400 → newCode="00801", mergeType=SPLIT_TO_MULTIPLE
```

**Migration Logic**:
```java
// Use coordinates to determine correct new ward
Ward ward800 = wardRepository.findByCode("00800");
Ward ward801 = wardRepository.findByCode("00801");

// Calculate distance to ward centroids or boundaries
// Assign each address to closest new ward

Address A → Ward "00800"
Address B → Ward "00801"
```

**After Migration**:
```
Address A:
- New Ward: "00800"
- Coordinates: (21.0300, 105.8500)

Address B:
- New Ward: "00801"
- Coordinates: (21.0320, 105.8520)
```

## File Reference

### Entities
- `entity/Address.java` - Main address storage with dual format support
- `entity/AddressMetadata.java` - Structured metadata for querying
- `entity/Province.java` / `entity/LegacyProvince.java` - Province hierarchies
- `entity/Ward.java` / `entity/LegacyWard.java` - Ward hierarchies
- `entity/District.java` - Legacy district level (eliminated in new structure)
- `entity/ProvinceMapping.java` - Province conversion (63→34)
- `entity/WardMapping.java` - Ward conversion with merge/split tracking
- `entity/DistrictWardMapping.java` - District elimination mapping
- `entity/StreetMapping.java` - Street updates for new structure

### Services
- `service/address/AddressService.java` - Legacy operations (3-tier)
- `service/address/NewAddressService.java` - New operations (2-tier)
- `service/address/AddressCreationService.java` - Address creation logic

### Controllers
- `controller/AddressController.java` - REST API endpoints for both structures

### Migrations
- `V21__Create_address_new.sql` - Creates all address tables
- `V22__Create_provinces_database_old.sql` - Populates legacy data (63 provinces)
- `V23__Create_provinces_database.sql` - Populates new data (34 provinces)
- `V24__Create_address_metadata_table.sql` - Creates metadata table with indexes

## Summary

The SmartRent address system supports a complex dual-structure architecture:

1. **Legacy (OLD)**: 63 provinces, 3-tier (Province→District→Ward), integer IDs
2. **New (NEW)**: 34 provinces, 2-tier (Province→Ward), string codes

**Key Features**:
- Addresses store both formats (`fullAddress` + `fullNewAddress`)
- AddressMetadata enables efficient querying for both structures
- Mapping tables provide bidirectional conversion
- `getDisplayAddress()` intelligently selects appropriate format
- Both APIs coexist for backward compatibility

**For Migration**:
1. Use mapping tables to convert administrative units
2. Handle merge/split cases using `mergeType` enum
3. Use coordinates to resolve ambiguities
4. Preserve original data while adding new format
5. Create new AddressMetadata entries for NEW structure queries

This architecture ensures smooth transition from legacy to new administrative structure while maintaining backward compatibility and data integrity.