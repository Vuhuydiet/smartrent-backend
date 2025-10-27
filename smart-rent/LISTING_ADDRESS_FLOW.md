# Listing Creation with Address - Flow Analysis & Recommendations

## Current Database Schema

### Addresses Table (from V7 migration)
```sql
CREATE TABLE addresses (
    address_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_address TEXT,           -- For old address structure
    full_newaddress TEXT,         -- For new address structure
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Current Entity Issue

The `Address.java` entity does NOT match the database schema:

**Database has:**
- `full_address` (TEXT)
- `full_newaddress` (TEXT)

**Entity has:**
- Relationships to `Street`, `Ward`, `District`, `Province` (NOT in database!)
- `fullAddress` field
- Missing `full_newaddress` field

## Required Flow for Listing Creation

### Frontend Flow

1. **Select Address Type**: User chooses between:
   - Old Address Structure (63 provinces)
   - New Address Structure (34 provinces)

2. **Old Address Flow**:
   ```
   GET /v1/addresses/provinces → Select Province
   ↓
   GET /v1/addresses/provinces/{provinceId}/districts → Select District
   ↓
   GET /v1/addresses/districts/{districtId}/wards → Select Ward
   ↓
   GET /v1/addresses/districts/{districtId}/streets → Select Street (Optional)
   ↓
   GET /v1/addresses/districts/{districtId}/projects → Select Project (Optional)
   ```

3. **New Address Flow**:
   ```
   GET /v1/addresses/new-provinces → Select Province
   ↓
   GET /v1/addresses/new-provinces/{provinceCode}/wards → Select Ward
   ↓
   GET /v1/addresses/provinces/{provinceId}/streets → Select Street (Optional)
   ↓
   GET /v1/addresses/provinces/{provinceId}/projects → Select Project (Optional)
   ```

4. **Submit Listing** with address data:
   ```json
   {
     "title": "Beautiful apartment",
     "price": 5000000,
     "address": {
       "addressType": "OLD" | "NEW",

       // For OLD address
       "provinceId": 1,
       "districtId": 5,
       "wardId": 45,
       "streetId": 123,      // Optional
       "projectId": 456,     // Optional
       "streetNumber": "123",

       // For NEW address
       "newProvinceCode": "01",
       "newWardCode": "00004",
       "streetId": 123,      // Optional
       "projectId": 456,     // Optional
       "streetNumber": "123",

       "latitude": 21.0285,
       "longitude": 105.8542
     }
   }
   ```

## Backend Requirements

### 1. Update Address Entity

The Address entity needs to be fixed to match the database:

```java
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    Long addressId;

    // Old address structure (stored as formatted text)
    @Column(name = "full_address", columnDefinition = "TEXT")
    String fullAddress;

    // New address structure (stored as formatted text)
    @Column(name = "full_newaddress", columnDefinition = "TEXT")
    String fullNewAddress;

    @Column(name = "latitude", precision = 10, scale = 8)
    BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    BigDecimal longitude;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "address")
    List<Listing> listings;
}
```

### 2. Create Address Metadata Table (Optional - Better Approach)

For better querying and data integrity, create a new table to store address component references:

```sql
CREATE TABLE address_metadata (
    metadata_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    address_id BIGINT NOT NULL,
    address_type ENUM('OLD', 'NEW') NOT NULL,

    -- Old address structure
    province_id INT,
    district_id INT,
    ward_id INT,
    street_id INT,
    project_id INT,

    -- New address structure
    new_province_code VARCHAR(10),
    new_ward_code VARCHAR(10),

    street_number VARCHAR(20),

    CONSTRAINT fk_address_metadata_address
        FOREIGN KEY (address_id) REFERENCES addresses(address_id) ON DELETE CASCADE,

    INDEX idx_address_type (address_type),
    INDEX idx_old_province (province_id),
    INDEX idx_old_district (district_id),
    INDEX idx_old_ward (ward_id),
    INDEX idx_new_province (new_province_code),
    INDEX idx_new_ward (new_ward_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 3. Create DTOs for Address Creation

```java
@Data
public class AddressCreationRequest {
    private AddressType addressType; // OLD or NEW

    // Old address fields
    private Integer provinceId;
    private Integer districtId;
    private Integer wardId;

    // New address fields
    private String newProvinceCode;
    private String newWardCode;

    // Common fields
    private Integer streetId;      // Optional
    private Integer projectId;     // Optional
    private String streetNumber;   // e.g., "123", "45A"

    // Location
    private BigDecimal latitude;
    private BigDecimal longitude;

    public enum AddressType {
        OLD, NEW
    }
}
```

### 4. Create Address Service

```java
@Service
public class AddressCreationService {

    public Address createAddress(AddressCreationRequest request) {
        Address address = new Address();

        // Set coordinates
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        if (request.getAddressType() == AddressType.OLD) {
            // Build and set old address format
            String fullAddress = buildOldAddress(request);
            address.setFullAddress(fullAddress);
            address.setFullNewAddress(null);
        } else {
            // Build and set new address format
            String fullNewAddress = buildNewAddress(request);
            address.setFullAddress(null);
            address.setFullNewAddress(fullNewAddress);
        }

        return addressRepository.save(address);
    }

    private String buildOldAddress(AddressCreationRequest request) {
        StringBuilder sb = new StringBuilder();

        // Add street number
        if (request.getStreetNumber() != null) {
            sb.append(request.getStreetNumber()).append(" ");
        }

        // Add street
        if (request.getStreetId() != null) {
            Street street = streetRepository.findById(request.getStreetId())
                .orElseThrow(() -> new ResourceNotFoundException("Street not found"));
            sb.append(street.getPrefix()).append(" ")
              .append(street.getName()).append(", ");
        }

        // Add project (if selected instead of street)
        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            sb.append(project.getName()).append(", ");
        }

        // Add ward
        LegacyWard ward = legacyWardRepository.findById(request.getWardId())
            .orElseThrow(() -> new ResourceNotFoundException("Ward not found"));
        sb.append(ward.getPrefix()).append(" ")
          .append(ward.getName()).append(", ");

        // Add district
        District district = legacyDistrictRepository.findById(request.getDistrictId())
            .orElseThrow(() -> new ResourceNotFoundException("District not found"));
        sb.append(district.getPrefix()).append(" ")
          .append(district.getName()).append(", ");

        // Add province
        LegacyProvince province = legacyProvinceRepository.findById(request.getProvinceId())
            .orElseThrow(() -> new ResourceNotFoundException("Province not found"));
        sb.append(province.getName());

        return sb.toString();
    }

    private String buildNewAddress(AddressCreationRequest request) {
        StringBuilder sb = new StringBuilder();

        // Add street number
        if (request.getStreetNumber() != null) {
            sb.append(request.getStreetNumber()).append(" ");
        }

        // Add street or project
        if (request.getStreetId() != null) {
            Street street = streetRepository.findById(request.getStreetId())
                .orElseThrow(() -> new ResourceNotFoundException("Street not found"));
            sb.append(street.getPrefix()).append(" ")
              .append(street.getName()).append(", ");
        } else if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            sb.append(project.getName()).append(", ");
        }

        // Add ward
        Ward ward = newWardRepository.findByCode(request.getNewWardCode())
            .orElseThrow(() -> new ResourceNotFoundException("Ward not found"));
        sb.append(ward.getFullName()).append(", ");

        // Add province
        Province province = newProvinceRepository.findByCode(request.getNewProvinceCode())
            .orElseThrow(() -> new ResourceNotFoundException("Province not found"));
        sb.append(province.getFullName());

        return sb.toString();
    }
}
```

### 5. API Endpoints Summary

**Public APIs** (already configured):

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/v1/addresses/provinces` | Get all 63 provinces (old) |
| GET | `/v1/addresses/provinces/{id}/districts` | Get districts by province |
| GET | `/v1/addresses/districts/{id}/wards` | Get wards by district |
| GET | `/v1/addresses/provinces/{id}/streets` | Get streets by province |
| GET | `/v1/addresses/districts/{id}/streets` | Get streets by district |
| GET | `/v1/addresses/provinces/{id}/projects` | Get projects by province |
| GET | `/v1/addresses/districts/{id}/projects` | Get projects by district |
| GET | `/v1/addresses/new-provinces` | Get all 34 provinces (new) |
| GET | `/v1/addresses/new-provinces/{code}/wards` | Get wards by new province |

## Implementation Checklist

- [ ] Fix Address entity to match database schema
- [ ] Create AddressCreationRequest DTO
- [ ] Implement AddressCreationService with buildOldAddress() and buildNewAddress()
- [ ] Create AddressMetadata entity (optional but recommended)
- [ ] Update ListingCreationRequest to include AddressCreationRequest
- [ ] Implement listing creation service to handle address creation
- [ ] Add validation for address fields
- [ ] Add integration tests for both address types
- [ ] Update API documentation with examples

## Example Full Address Outputs

**Old Address Format:**
```
123 Đường Nguyễn Trãi, Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội
```

**New Address Format:**
```
123 Đường Nguyễn Trãi, Phường Ba Đình, Thành phố Hà Nội
```

## Notes

1. **Data Integrity**: Store both formatted text AND component references (via AddressMetadata)
2. **Search**: Having component references allows filtering listings by province/district/ward
3. **Migration**: Existing listings only have full_address filled, need migration strategy
4. **Validation**: Ensure address components are valid before creating listing
5. **Geocoding**: Consider integrating with Google Maps API for lat/long validation
