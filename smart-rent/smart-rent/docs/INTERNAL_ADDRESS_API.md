# Internal Address API Documentation

## Overview

This document describes the **internal address API** system for the SmartRent application. The system uses Vietnamese administrative location data stored in the local database (imported from tinhthanhpho.com).

## Architecture

```
┌─────────────────────────────────────┐
│      Frontend / API Client          │
└──────────────┬──────────────────────┘
               │ REST API
    ┌──────────▼──────────┐
    │ AddressController   │
    └──────────┬──────────┘
               │
    ┌──────────▼──────────┐
    │   AddressService    │
    └──────────┬──────────┘
               │
    ┌──────────▼──────────────────────┐
    │   JPA Repositories              │
    │  - ProvinceRepository           │
    │  - DistrictRepository           │
    │  - WardRepository               │
    │  - StreetRepository             │
    │  - AddressRepository            │
    └──────────┬──────────────────────┘
               │
    ┌──────────▼──────────┐
    │   MySQL Database    │
    │  (~11,700 records)  │
    └─────────────────────┘
```

## Data Model

### Hierarchy

```
Province (63)
  ├── District (~700)
  │     ├── Ward (~11,000)
  │     │     ├── Street (variable)
  │     │     │     └── Address (variable)
```

### Entity Relationships

```sql
provinces
  - province_id (PK)
  - name, code, type
  - parent_province_id (FK) -- for merged provinces

districts
  - district_id (PK)
  - province_id (FK)
  - name, code, type

wards
  - ward_id (PK)
  - district_id (FK)
  - name, code, type

streets
  - street_id (PK)
  - ward_id (FK)
  - name

addresses
  - address_id (PK)
  - street_id, ward_id, district_id, province_id (FKs)
  - street_number, full_address
  - latitude, longitude
```

## API Endpoints

### Province Endpoints

```
GET  /v1/addresses/provinces
     → Get all parent provinces (for dropdown)

GET  /v1/addresses/provinces/{provinceId}
     → Get specific province by ID

GET  /v1/addresses/provinces/search?q={keyword}
     → Search provinces by name
```

### District Endpoints

```
GET  /v1/addresses/provinces/{provinceId}/districts
     → Get all districts for a province

GET  /v1/addresses/districts/{districtId}
     → Get specific district by ID
```

### Ward Endpoints

```
GET  /v1/addresses/districts/{districtId}/wards
     → Get all wards for a district

GET  /v1/addresses/wards/{wardId}
     → Get specific ward by ID
```

### Street Endpoints

```
GET  /v1/addresses/wards/{wardId}/streets
     → Get all streets for a ward

GET  /v1/addresses/streets/{streetId}
     → Get specific street by ID
```

### Address Endpoints

```
POST /v1/addresses
     → Create a new address
     Body: {
       "streetNumber": "123",
       "streetId": 1,
       "wardId": 1,
       "districtId": 1,
       "provinceId": 1,
       "latitude": 21.0285,
       "longitude": 105.8542
     }

GET  /v1/addresses/{addressId}
     → Get specific address by ID

GET  /v1/addresses/search?q={keyword}
     → Search addresses by full address text

GET  /v1/addresses/suggest?q={keyword}
     → Get address suggestions (autocomplete)
     → Returns top 10 matches

GET  /v1/addresses/nearby?latitude={lat}&longitude={lng}&radius={km}
     → Find addresses within radius
     → Default radius: 5km
```

## Usage Examples

### Frontend Flow: Listing Creation with Address Selection

#### Step 1: User selects Province

```javascript
// Fetch provinces
const provinces = await fetch('/v1/addresses/provinces')
  .then(res => res.json());

// User selects: "Thành phố Hà Nội" (provinceId = 1)
```

#### Step 2: User selects District

```javascript
// Fetch districts for selected province
const districts = await fetch('/v1/addresses/provinces/1/districts')
  .then(res => res.json());

// User selects: "Quận Ba Đình" (districtId = 1)
```

#### Step 3: User selects Ward

```javascript
// Fetch wards for selected district
const wards = await fetch('/v1/addresses/districts/1/wards')
  .then(res => res.json());

// User selects: "Phường Phúc Xá" (wardId = 1)
```

#### Step 4: User selects/enters Street

```javascript
// Fetch streets for selected ward
const streets = await fetch('/v1/addresses/wards/1/streets')
  .then(res => res.json());

// User selects: "Phố Phúc Xá" (streetId = 1)
// Or user types street name if not in list
```

#### Step 5: Create Address

```javascript
// Create address with street number
const address = await fetch('/v1/addresses', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    streetNumber: "123",
    streetId: 1,
    wardId: 1,
    districtId: 1,
    provinceId: 1,
    latitude: 21.0285,
    longitude: 105.8542
  })
}).then(res => res.json());

// Response: { addressId: 456, fullAddress: "123 Phố Phúc Xá, Phường Phúc Xá, Quận Ba Đình, Hà Nội" }
```

#### Step 6: Create Listing with Address

```javascript
// Create listing with addressId
const listing = await fetch('/v1/listings', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    title: "Cho thuê căn hộ 2PN",
    description: "Căn hộ đẹp, full nội thất",
    price: 10000000,
    priceUnit: "VND/month",
    addressId: 456,  // ← Address ID from step 5
    area: 75.5,
    bedrooms: 2,
    bathrooms: 1,
    // ... other fields
  })
}).then(res => res.json());
```

### Alternative: Address Autocomplete

```javascript
// User types partial address
const suggestions = await fetch('/v1/addresses/suggest?q=123%20Phúc%20Xá')
  .then(res => res.json());

// Returns matching addresses:
// [
//   { addressId: 456, fullAddress: "123 Phố Phúc Xá, Phường Phúc Xá, Quận Ba Đình, Hà Nội" },
//   { addressId: 457, fullAddress: "123A Phố Phúc Xá, Phường Phúc Xá, Quận Ba Đình, Hà Nội" }
// ]

// User selects from suggestions
const selectedAddressId = 456;

// Create listing directly with selected address ID
```

## Data Import Process

### One-Time Setup

1. **Run the data fetcher script** (see `TINHTHANHPHO_DATA_IMPORT_GUIDE.md`):
   ```bash
   node scripts/fetch-tinhthanhpho-data.js
   ```

2. **Generated SQL file** contains ~11,700 INSERT statements:
   - 63 provinces
   - ~700 districts
   - ~11,000 wards

3. **Apply migration**:
   ```bash
   ./gradlew flywayMigrate
   ```

4. **Verify data**:
   ```sql
   SELECT COUNT(*) FROM provinces; -- Expected: 63
   SELECT COUNT(*) FROM districts; -- Expected: ~700
   SELECT COUNT(*) FROM wards; -- Expected: ~11,000
   ```

## Integration with Listing Service

### ListingCreationRequest

The `ListingCreationRequest` already includes `addressId`:

```java
@NotNull
Long addressId;  // ← Address must be created before listing
```

### Listing Creation Flow

```java
@Override
@Transactional
public ListingCreationResponse createListing(ListingCreationRequest request) {
    // 1. Validate addressId exists
    Address address = addressRepository.findById(request.getAddressId())
        .orElseThrow(() -> new AppException(DomainCode.ADDRESS_NOT_FOUND));

    // 2. Create listing with address reference
    Listing listing = listingMapper.toEntity(request);
    listing.setAddress(address);

    // 3. Location pricing is automatically calculated
    //    based on address's ward/district/province

    Listing saved = listingRepository.save(listing);
    return listingMapper.toCreationResponse(saved);
}
```

### Listing Response with Location

```json
{
  "listingId": 123,
  "title": "Cho thuê căn hộ 2PN",
  "address": {
    "addressId": 456,
    "fullAddress": "123 Phố Phúc Xá, Phường Phúc Xá, Quận Ba Đình, Hà Nội",
    "ward": "Phường Phúc Xá",
    "district": "Quận Ba Đình",
    "province": "Hà Nội",
    "latitude": 21.0285,
    "longitude": 105.8542
  },
  "locationPricing": {
    "wardPricing": {
      "averagePrice": 15000000
    },
    "districtPricing": {
      "averagePrice": 14000000
    },
    "provincePricing": {
      "averagePrice": 12000000
    }
  }
}
```

## Repository Methods

### ProvinceRepository

```java
// Get all active provinces
List<Province> findByIsActiveTrueOrderByName();

// Get parent provinces only (for dropdown)
List<Province> findByParentProvinceIsNullAndIsActiveTrueOrderByName();

// Find by code (for data sync)
Optional<Province> findByCode(String code);

// Search provinces
List<Province> findByNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCaseAndIsActiveTrueOrderByName(
    String nameSearchTerm, String originalNameSearchTerm);
```

### DistrictRepository

```java
// Get districts by province
List<District> findByProvince_ProvinceIdAndIsActiveTrue(Long provinceId);

// Find by code
Optional<District> findByCode(String code);

// Search districts within province
List<District> findByNameContainingIgnoreCaseAndProvinceProvinceIdAndIsActiveTrueOrderByName(
    String searchTerm, Long provinceId);
```

### WardRepository

```java
// Get wards by district
List<Ward> findByDistrict_DistrictIdAndIsActiveTrue(Long districtId);

// Find by code
Optional<Ward> findByCode(String code);

// Search wards within district
List<Ward> findByNameContainingIgnoreCaseAndDistrictDistrictIdAndIsActiveTrueOrderByName(
    String searchTerm, Long districtId);
```

### StreetRepository

```java
// Get streets by ward
List<Street> findByWardWardIdAndIsActiveTrueOrderByName(Long wardId);

// Search streets within ward
List<Street> findByNameContainingIgnoreCaseAndWardWardIdAndIsActiveTrueOrderByName(
    String searchTerm, Long wardId);
```

### AddressRepository

```java
// Find by components (prevent duplicates)
Optional<Address> findByStreetNumberAndStreetStreetIdAndWardWardIdAndDistrictDistrictIdAndProvinceProvinceId(
    String streetNumber, Long streetId, Long wardId, Long districtId, Long provinceId);

// Search by full address
List<Address> findByFullAddressContainingIgnoreCaseOrderByFullAddress(String searchTerm);

// Find nearby addresses
List<Address> findNearbyAddresses(
    BigDecimal latitude, BigDecimal longitude,
    BigDecimal latDelta, BigDecimal lngDelta);
```

## Performance Optimization

### Indexing Strategy

```sql
-- Already indexed in migration files
CREATE INDEX idx_province_code ON provinces(code);
CREATE INDEX idx_province_name ON provinces(name);
CREATE INDEX idx_district_code ON districts(code);
CREATE INDEX idx_district_province_id ON districts(province_id);
CREATE INDEX idx_ward_code ON wards(code);
CREATE INDEX idx_ward_district_id ON wards(district_id);
CREATE INDEX idx_street_ward_id ON streets(ward_id);
CREATE INDEX idx_address_full_address ON addresses(full_address);
CREATE INDEX idx_address_lat_lng ON addresses(latitude, longitude);
```

### Query Optimization Tips

1. **Use specific queries**: Don't fetch all provinces when you only need one
2. **Limit results**: Use `LIMIT` for search/autocomplete
3. **Eager vs Lazy loading**: Use `@ManyToOne(fetch = FetchType.LAZY)` by default
4. **Batch fetching**: For listing pages, use `@BatchSize(size = 10)`

## Testing

### Unit Tests

```java
@Test
void testGetDistrictsByProvince() {
    // Given
    Long provinceId = 1L; // Hanoi

    // When
    List<DistrictResponse> districts = addressService.getDistrictsByProvinceId(provinceId);

    // Then
    assertThat(districts).isNotEmpty();
    assertThat(districts).allMatch(d -> d.getProvinceId().equals(provinceId));
}
```

### Integration Tests

```java
@Test
@Sql("/test-data/addresses.sql")
void testCreateListing WithAddress() {
    // Given
    Address address = addressRepository.save(createTestAddress());
    ListingCreationRequest request = createListingRequest(address.getAddressId());

    // When
    ListingCreationResponse response = listingService.createListing(request);

    // Then
    assertThat(response.getAddressId()).isEqualTo(address.getAddressId());
}
```

## Common Use Cases

### Use Case 1: Simple Listing Creation

```
1. User fills listing form
2. User selects Province → District → Ward → Street
3. User enters street number
4. System creates Address (or reuses existing)
5. System creates Listing with addressId
```

### Use Case 2: Address Autocomplete

```
1. User starts typing address
2. System shows suggestions after 3 characters
3. User selects from dropdown
4. System uses selected addressId for listing
```

### Use Case 3: Map-based Selection

```
1. User clicks on map
2. System gets lat/lng coordinates
3. System finds nearby addresses
4. User selects closest match or creates new
5. System creates listing with address
```

## Error Handling

### Common Errors

| Error Code | HTTP Status | Description | Solution |
|------------|-------------|-------------|----------|
| 4005 | 404 | ADDRESS_NOT_FOUND | Ensure addressId exists |
| 4006 | 404 | PROVINCE_NOT_FOUND | Check provinceId |
| 4007 | 404 | DISTRICT_NOT_FOUND | Check districtId |
| 4008 | 404 | WARD_NOT_FOUND | Check wardId |

### Example Error Response

```json
{
  "code": "4005",
  "message": "Address not found",
  "timestamp": "2025-01-18T10:30:00"
}
```

## Best Practices

1. **Always validate addressId** before creating listings
2. **Use autocomplete** for better UX (reduces errors)
3. **Cache province/district/ward lists** in frontend
4. **Reuse existing addresses** when possible (check before creating)
5. **Include lat/lng** when creating addresses (for map features)
6. **Handle missing streets** gracefully (allow manual entry)

## Maintenance

### Annual Updates

Vietnamese administrative divisions change occasionally. To update:

1. **Re-run data fetcher script** with latest API data
2. **Generate new migration file** (e.g., `V19__Update_locations_2026.sql`)
3. **Mark old locations as inactive** instead of deleting
4. **Update merged provinces** using `parent_province_id` FK

### Data Integrity Checks

```sql
-- Find orphaned districts (province doesn't exist)
SELECT d.* FROM districts d
LEFT JOIN provinces p ON d.province_id = p.province_id
WHERE p.province_id IS NULL;

-- Find wards without districts
SELECT w.* FROM wards w
LEFT JOIN districts d ON w.district_id = d.district_id
WHERE d.district_id IS NULL;

-- Find addresses without location data
SELECT * FROM addresses
WHERE province_id IS NULL OR district_id IS NULL OR ward_id IS NULL;
```

## Troubleshooting

### Issue: Dropdown shows no provinces

**Solution**: Run data import migration

### Issue: Address creation fails with validation error

**Solution**: Ensure all location IDs (province, district, ward, street) exist and are active

### Issue: Search returns no results

**Solution**: Check if `fullAddress` field is properly populated during address creation

### Issue: Nearby search returns wrong addresses

**Solution**: Verify latitude/longitude are stored correctly (not swapped)

## Related Documentation

- `TINHTHANHPHO_DATA_IMPORT_GUIDE.md` - How to import location data
- `V18__Import_tinhthanhpho_location_data.sql` - Migration file template
- API docs: http://localhost:8080/swagger-ui.html

## Support

For issues or questions:
1. Check application logs
2. Verify database has location data
3. Test API endpoints directly (Swagger UI)
4. Review error codes in `DomainCode.java`