# Address System Flow and Entity Documentation

## Overview

The SmartRent application implements a comprehensive 4-tier hierarchical address system for Vietnam's administrative divisions:

```
Province (Tỉnh/Thành phố)
    ↓
District (Quận/Huyện/Thị xã)
    ↓
Ward (Phường/Xã/Thị trấn)
    ↓
Street (Đường phố)
    ↓
Address (Địa chỉ cụ thể)
```

## Entity Hierarchy

### 1. Province Entity
**File:** `src/main/java/com/smartrent/infra/repository/entity/Province.java`

**Purpose:** Represents the top-level administrative division (provinces and cities).

**Key Fields:**
- `provinceId` (Long) - Primary key
- `name` (String) - Province name (e.g., "Hà Nội", "Thành phố Hồ Chí Minh")
- `code` (String) - Unique province code
- `type` (ProvinceType) - Enum: `PROVINCE` or `CITY`
- `isActive` (Boolean) - Whether the province is currently active
- `effectiveFrom` / `effectiveTo` (LocalDate) - Validity period
- `parentProvince` (Province) - Self-referencing for merged provinces
- `mergedProvinces` (List<Province>) - Child provinces that were merged
- `isMerged` (Boolean) - Whether this province was merged into another
- `mergedDate` (LocalDate) - Date of merger
- `originalName` (String) - Name before merger

**Relationships:**
- One-to-Many with `District`
- Self-referencing for province mergers (parent-child)

**Special Features:**
- **Province Merger Support**: Handles administrative reorganization where provinces are merged
- Helper methods:
  - `isParentProvince()` - Check if this province has merged children
  - `isMergedProvince()` - Check if this province was merged into another
  - `getDisplayName()` - Returns parent province name if merged, otherwise own name
  - `getAllMergedProvinces()` - Get all merged child provinces

**Indexes:**
- `idx_name` - Fast name lookups
- `idx_is_active` - Filter active provinces
- `idx_effective_period` - Query by validity period
- `idx_parent_province` - Merged province queries
- `idx_is_merged` - Filter merged provinces

---

### 2. District Entity
**File:** `src/main/java/com/smartrent/infra/repository/entity/District.java`

**Purpose:** Represents the second-level administrative division (districts, towns, cities).

**Key Fields:**
- `districtId` (Long) - Primary key
- `name` (String) - District name
- `code` (String) - District code (unique within province)
- `type` (DistrictType) - Enum: `DISTRICT`, `TOWN`, or `CITY`
- `province` (Province) - Parent province (Many-to-One)
- `isActive` (Boolean) - Whether the district is active
- `effectiveFrom` / `effectiveTo` (LocalDate) - Validity period

**Relationships:**
- Many-to-One with `Province`
- One-to-Many with `Ward`

**Indexes:**
- `idx_province_id` - Fast lookup by province
- `idx_name` - Name searches
- `idx_is_active` - Filter active districts
- `idx_effective_period` - Query by validity period

**Constraints:**
- `unique_province_district_code` - District code must be unique within each province

---

### 3. Ward Entity
**File:** `src/main/java/com/smartrent/infra/repository/entity/Ward.java`

**Purpose:** Represents the third-level administrative division (wards, communes, townships).

**Key Fields:**
- `wardId` (Long) - Primary key
- `name` (String) - Ward name
- `code` (String) - Ward code (unique within district)
- `type` (WardType) - Enum: `WARD`, `COMMUNE`, or `TOWNSHIP`
- `district` (District) - Parent district (Many-to-One)
- `isActive` (Boolean) - Whether the ward is active
- `effectiveFrom` / `effectiveTo` (LocalDate) - Validity period

**Relationships:**
- Many-to-One with `District`
- One-to-Many with `Street`

**Indexes:**
- `idx_district_id` - Fast lookup by district
- `idx_name` - Name searches
- `idx_is_active` - Filter active wards
- `idx_effective_period` - Query by validity period

**Constraints:**
- `unique_district_ward_code` - Ward code must be unique within each district

---

### 4. Street Entity
**File:** `src/main/java/com/smartrent/infra/repository/entity/Street.java`

**Purpose:** Represents a street within a ward.

**Key Fields:**
- `streetId` (Long) - Primary key
- `name` (String) - Street name
- `ward` (Ward) - Parent ward (Many-to-One)
- `isActive` (Boolean) - Whether the street is active

**Relationships:**
- Many-to-One with `Ward`
- One-to-Many with `Address`

**Indexes:**
- `idx_ward_id` - Fast lookup by ward
- `idx_name` - Name searches
- `idx_is_active` - Filter active streets

---

### 5. Address Entity
**File:** `src/main/java/com/smartrent/infra/repository/entity/Address.java`

**Purpose:** Represents a complete physical address with specific street number.

**Key Fields:**
- `addressId` (Long) - Primary key
- `streetNumber` (String) - House/building number
- `street` (Street) - Parent street (Many-to-One)
- `ward` (Ward) - Direct reference to ward (Many-to-One)
- `district` (District) - Direct reference to district (Many-to-One)
- `province` (Province) - Direct reference to province (Many-to-One)
- `fullAddress` (String, TEXT) - Complete formatted address
- `latitude` / `longitude` (BigDecimal) - Geographic coordinates
- `isVerified` (Boolean) - Whether the address has been verified

**Relationships:**
- Many-to-One with `Street`, `Ward`, `District`, `Province`
- One-to-Many with `Listing` (property listings at this address)

**Indexes:**
- `idx_street_id` - Lookup by street
- `idx_ward_id` - Lookup by ward
- `idx_district_id` - Lookup by district
- `idx_province_id` - Lookup by province
- `idx_coordinates` - Geographic queries (latitude, longitude)
- `idx_is_verified` - Filter verified addresses

**Helper Methods:**
- `getFullAddressDisplay()` - Builds formatted address string
  - Format: `{streetNumber} {street}, {ward}, {district}, {province}`
  - Uses `province.getDisplayName()` to handle merged provinces

---

## Data Transfer Objects (DTOs)

### AddressCreationRequest
**File:** `src/main/java/com/smartrent/dto/request/AddressCreationRequest.java`

```java
{
  "streetNumber": "123",    // Required
  "streetId": 1,            // Required
  "wardId": 1,              // Required
  "districtId": 1,          // Required
  "provinceId": 1,          // Required
  "latitude": 21.0285,      // Optional
  "longitude": 105.8542     // Optional
}
```

### AddressResponse
**File:** `src/main/java/com/smartrent/dto/response/AddressResponse.java`

```java
{
  "addressId": 1,
  "streetNumber": "123",
  "streetId": 1,
  "streetName": "Nguyễn Trãi",
  "wardId": 1,
  "wardName": "Thanh Xuân",
  "districtId": 1,
  "districtName": "Thanh Xuân",
  "provinceId": 1,
  "provinceName": "Hà Nội",
  "fullAddress": "123 Nguyễn Trãi, Thanh Xuân, Thanh Xuân, Hà Nội",
  "latitude": 21.0285,
  "longitude": 105.8542,
  "isVerified": false
}
```

---

## API Endpoints Flow

**Base URL:** `/v1/addresses`

### 1. Browse Provinces → Districts → Wards → Streets

```
GET /provinces
  → Returns list of all active parent provinces

GET /provinces/{provinceId}/districts
  → Returns districts for a province (including merged provinces)

GET /districts/{districtId}/wards
  → Returns wards for a district

GET /wards/{wardId}/streets
  → Returns streets for a ward
```

### 2. Create Complete Address

```
POST /addresses
Body: AddressCreationRequest
  → Creates or returns existing address
  → Auto-generates fullAddress field
  → Returns AddressResponse
```

### 3. Search Functionality

```
GET /provinces/search?q={term}
  → Search provinces by name or original name

GET /addresses/search?q={term}
  → Search addresses by full address text

GET /addresses/suggest?q={partial}
  → Auto-complete suggestions (max 10 results)
```

### 4. Geographic Queries

```
GET /addresses/nearby?latitude={lat}&longitude={lng}&radius={km}
  → Find addresses within radius (default 5km)
  → Uses Haversine distance approximation
```

### 5. Individual Entity Retrieval

```
GET /provinces/{id}
GET /districts/{id}
GET /wards/{id}
GET /streets/{id}
GET /addresses/{id}
  → Get specific entity by ID
```

---

## Service Layer Logic

**File:** `src/main/java/com/smartrent/service/address/impl/AddressServiceImpl.java`

### Key Service Methods

#### 1. `getParentProvinces()`
- Returns only parent provinces (not merged children)
- Used for dropdowns and province selection
- Filters: `isActive = true` AND `parentProvince IS NULL`

#### 2. `getDistrictsByProvinceId(Long provinceId)`
- Gets districts for a province
- **Also includes districts from merged child provinces**
- Handles province reorganization transparently

#### 3. `createAddress(AddressCreationRequest request)`
- **Checks for existing address** (prevents duplicates)
- Validates all foreign keys (street, ward, district, province exist)
- Auto-generates `fullAddress` using `buildFullAddress()` helper
- Sets `isVerified = false` by default
- Returns existing address if found, creates new if not

#### 4. `getNearbyAddresses(BigDecimal lat, BigDecimal lng, Double radiusKm)`
- Converts radius to lat/lng delta
- Approximations:
  - 1 degree latitude ≈ 111 km
  - 1 degree longitude ≈ 111 km × cos(latitude)
- Queries database for addresses within bounding box

#### 5. `suggestAddresses(String partialAddress)`
- Auto-complete functionality
- Searches `fullAddress` field with case-insensitive partial match
- Limits results to 10 suggestions

---

## Address Flow Scenarios

### Scenario 1: User Creating a Listing Address

**Step-by-step UI flow:**

1. **Select Province**
   ```
   GET /v1/addresses/provinces
   → User selects: "Hà Nội" (provinceId: 1)
   ```

2. **Select District**
   ```
   GET /v1/addresses/provinces/1/districts
   → User selects: "Thanh Xuân" (districtId: 5)
   ```

3. **Select Ward**
   ```
   GET /v1/addresses/districts/5/wards
   → User selects: "Khương Mai" (wardId: 15)
   ```

4. **Select Street**
   ```
   GET /v1/addresses/wards/15/streets
   → User selects: "Nguyễn Trãi" (streetId: 102)
   ```

5. **Enter Street Number and Create Address**
   ```
   POST /v1/addresses
   {
     "streetNumber": "123",
     "streetId": 102,
     "wardId": 15,
     "districtId": 5,
     "provinceId": 1,
     "latitude": 21.0028,
     "longitude": 105.8198
   }
   → Returns addressId: 456
   ```

6. **Use addressId when creating listing**

---

### Scenario 2: Searching for an Address

**Option A: Hierarchical Search**
```
1. GET /v1/addresses/provinces/search?q=Hà Nội
   → Select province
2. GET /v1/addresses/provinces/1/districts
   → Select district
3. Continue drilling down...
```

**Option B: Full Text Search**
```
GET /v1/addresses/search?q=123 Nguyễn Trãi
→ Returns all matching addresses
```

**Option C: Auto-complete**
```
GET /v1/addresses/suggest?q=123 Nguy
→ Returns top 10 suggestions
```

---

### Scenario 3: Finding Nearby Listings

```
1. User's current location: (21.0285, 105.8542)
2. GET /v1/addresses/nearby?latitude=21.0285&longitude=105.8542&radius=3
3. Returns all addresses within 3km
4. Use returned addressIds to find listings
```

---

## Database Relationships Diagram

```
┌─────────────────┐
│   Province      │
│  (Tỉnh/TP)      │
│                 │ ─┐ Self-referencing
│ • provinceId    │ ←┘ (province mergers)
│ • name          │
│ • type          │
│ • parentProvince│
└────────┬────────┘
         │ 1:N
         ↓
┌─────────────────┐
│   District      │
│  (Quận/Huyện)   │
│                 │
│ • districtId    │
│ • name          │
│ • type          │
│ • provinceId    │
└────────┬────────┘
         │ 1:N
         ↓
┌─────────────────┐
│     Ward        │
│   (Phường/Xã)   │
│                 │
│ • wardId        │
│ • name          │
│ • type          │
│ • districtId    │
└────────┬────────┘
         │ 1:N
         ↓
┌─────────────────┐
│     Street      │
│   (Đường phố)   │
│                 │
│ • streetId      │
│ • name          │
│ • wardId        │
└────────┬────────┘
         │ 1:N
         ↓
┌─────────────────┐          ┌─────────────┐
│    Address      │          │   Listing   │
│  (Địa chỉ)      │ 1:N      │ (Bài đăng)  │
│                 ├─────────→│             │
│ • addressId     │          │ • addressId │
│ • streetNumber  │          └─────────────┘
│ • streetId      │
│ • wardId        │ ─┐
│ • districtId    │  │ Denormalized references
│ • provinceId    │  │ for faster queries
│ • fullAddress   │ ←┘
│ • lat/lng       │
└─────────────────┘
```

---

## Special Features

### 1. Province Merger Handling
When provinces are merged:
- Original province marked as `isMerged = true`
- Points to `parentProvince` (the merged-into province)
- `originalName` preserved for historical records
- `getDisplayName()` automatically returns parent province name
- Queries automatically include districts from merged provinces

**Example:**
```
Hà Tây → Merged into Hà Nội (2008)
- Hà Tây: isMerged=true, parentProvinceId=1 (Hà Nội)
- When querying Hà Nội's districts, Hà Tây's districts are included
```

### 2. Denormalized Address References
The `Address` entity stores direct references to all levels:
- `streetId` (required for hierarchy)
- `wardId` (denormalized)
- `districtId` (denormalized)
- `provinceId` (denormalized)

**Benefits:**
- Faster queries (no joins needed)
- Efficient filtering by province/district/ward
- Better index performance

### 3. Full Address Generation
The `fullAddress` field is auto-generated and stored:
- Generated during address creation
- Format: `{number} {street}, {ward}, {district}, {province}`
- Enables full-text search
- Used for display and auto-complete

### 4. Geographic Search
Supports coordinate-based queries:
- Stores `latitude` and `longitude` as `BigDecimal` (high precision)
- `getNearbyAddresses()` uses bounding box approximation
- Indexed for performance (`idx_coordinates`)

---

## Validation and Business Rules

### Address Creation Rules:
1. **Uniqueness**: Check if address already exists before creating
   - Unique by: `streetNumber + streetId + wardId + districtId + provinceId`
   - Returns existing address if found (idempotent)

2. **Foreign Key Validation**: All referenced entities must exist
   - Street must exist and be active
   - Ward must exist and be active
   - District must exist and be active
   - Province must exist and be active

3. **Hierarchy Consistency**:
   - Street must belong to specified ward
   - Ward must belong to specified district
   - District must belong to specified province
   - (Enforced at database level via foreign keys)

4. **Default Values**:
   - `isVerified = false` (requires manual verification)
   - `fullAddress` auto-generated
   - Timestamps auto-managed

---

## Performance Optimizations

### 1. Database Indexes
All entities have comprehensive indexing:
- **Name indexes**: Fast text searches
- **Foreign key indexes**: Efficient joins
- **Active status indexes**: Filter inactive entities
- **Coordinate indexes**: Geographic queries
- **Unique constraints**: Prevent duplicates

### 2. Lazy Loading
All entity relationships use `FetchType.LAZY`:
- Prevents N+1 query problems
- Loads related entities only when accessed
- Reduces memory footprint

### 3. Query Optimization
- `getParentProvinces()`: Single query with filter
- `getDistrictsByProvinceId()`: Two queries (direct + merged)
- Search endpoints return limited results
- Auto-complete limited to 10 suggestions

### 4. Caching Opportunities
Consider caching (not currently implemented):
- Province/district/ward lists (rarely change)
- Street lists by ward (relatively stable)
- Address lookups by coordinates

---

## Error Handling

Common exceptions thrown:
```java
RuntimeException("Province not found with id: {id}")
RuntimeException("District not found with id: {id}")
RuntimeException("Ward not found with id: {id}")
RuntimeException("Street not found")
RuntimeException("Address not found: {fullAddress}")
```

*Note: These should be replaced with custom exceptions mapped to error codes (see ERROR_CODES.md)*

---

## Testing Considerations

### Test Scenarios:
1. **Province Merger Logic**
   - Create merged province relationships
   - Verify `getParentProvinces()` excludes merged children
   - Verify `getDistrictsByProvinceId()` includes merged province districts

2. **Address Deduplication**
   - Create same address twice
   - Verify only one record created
   - Verify returned addressId is consistent

3. **Geographic Queries**
   - Test boundary conditions (equator, poles)
   - Verify radius calculations
   - Test with various coordinate systems

4. **Full Address Generation**
   - Test with/without street number
   - Test with merged provinces
   - Verify format consistency

---

## Migration Notes

### Database Setup:
1. Flyway migrations create all tables
2. Hierarchical structure enforced via foreign keys
3. Unique constraints prevent duplicates
4. Indexes optimize query performance

### Data Import:
For initial data population:
1. Import provinces (top-level)
2. Import districts (with provinceId references)
3. Import wards (with districtId references)
4. Import streets (with wardId references)
5. Addresses created on-demand by users/system

### Historical Data:
- Use `effectiveFrom` / `effectiveTo` for administrative changes
- Mark old entities as `isActive = false`
- Use province merger feature for reorganizations

---

## Future Enhancements

Potential improvements:
1. **Address Verification**: Integrate with mapping services (Google Maps, OpenStreetMap)
2. **Geocoding**: Auto-populate lat/lng from address
3. **Address Normalization**: Standardize address formats
4. **Caching Layer**: Redis cache for administrative divisions
5. **Postal Codes**: Add zip/postal code support
6. **Multi-language**: Support English translations
7. **Address History**: Track address changes over time
8. **Bulk Import**: API for importing multiple addresses

---

## Related Files

### Controllers:
- `src/main/java/com/smartrent/controller/AddressController.java`

### Services:
- `src/main/java/com/smartrent/service/address/AddressService.java` (interface)
- `src/main/java/com/smartrent/service/address/impl/AddressServiceImpl.java`

### Repositories:
- `src/main/java/com/smartrent/infra/repository/ProvinceRepository.java`
- `src/main/java/com/smartrent/infra/repository/DistrictRepository.java`
- `src/main/java/com/smartrent/infra/repository/WardRepository.java`
- `src/main/java/com/smartrent/infra/repository/StreetRepository.java`
- `src/main/java/com/smartrent/infra/repository/AddressRepository.java`

### DTOs:
- `src/main/java/com/smartrent/dto/request/AddressCreationRequest.java`
- `src/main/java/com/smartrent/dto/response/AddressResponse.java`
- `src/main/java/com/smartrent/dto/response/ProvinceResponse.java`
- `src/main/java/com/smartrent/dto/response/DistrictResponse.java`
- `src/main/java/com/smartrent/dto/response/WardResponse.java`
- `src/main/java/com/smartrent/dto/response/StreetResponse.java`

### Mappers:
- `src/main/java/com/smartrent/mapper/AddressMapper.java`
- `src/main/java/com/smartrent/mapper/impl/AddressMapperImpl.java`

---

**Generated:** 2025-10-07
**Version:** 1.0
**Status:** Complete
