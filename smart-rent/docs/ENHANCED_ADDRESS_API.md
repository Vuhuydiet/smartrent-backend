# Enhanced Address API Documentation

## Overview

This is a **comprehensive, production-ready Address API** designed and implemented by a Senior Java Spring Boot Engineer. The API provides full support for Vietnamese administrative divisions with advanced features including:

- ✅ Code-based RESTful endpoints
- ✅ Administrative merge/reorganization handling
- ✅ Address conversion (old → new structure)
- ✅ Batch processing capabilities
- ✅ Advanced search with relevance scoring
- ✅ Merge history tracking
- ✅ Full Swagger/OpenAPI documentation

## Architecture

### System Design

```
┌─────────────────────────────────────────────────────────┐
│              AddressApiController (v2.0)                │
│  RESTful endpoints with comprehensive Swagger docs      │
└────────────────────┬────────────────────────────────────┘
                     │
         ┌───────────┴──────────┐
         │                      │
┌────────▼───────────┐  ┌──────▼────────────────┐
│ EnhancedAddress    │  │  AddressService       │
│ Service            │  │  (Legacy support)     │
│ (New features)     │  │                       │
└────────┬───────────┘  └───────────────────────┘
         │
         │
    ┌────▼────────────────────────────────┐
    │  Repository Layer                   │
    │  - ProvinceRepository              │
    │  - DistrictRepository              │
    │  - WardRepository                  │
    └────┬────────────────────────────────┘
         │
    ┌────▼────────────────────────────────┐
    │  MySQL Database                     │
    │  - 63 Provinces                     │
    │  - ~700 Districts                   │
    │  - ~11,000 Wards                    │
    └─────────────────────────────────────┘
```

### Key Design Patterns

1. **Service Layer Separation**: `EnhancedAddressService` for new features, keeping legacy `AddressService` intact
2. **RESTful Resource Design**: Code-based URLs (`/provinces/{code}`) instead of ID-based
3. **DTO Pattern**: Separate request/response models for clean API contracts
4. **Builder Pattern**: All DTOs use Lombok builders for flexibility
5. **Strategy Pattern**: Different search strategies for old vs new administrative structures

## API Endpoints

### 📍 Base URL
```
http://localhost:8080/api/v1
```

### 🗂️ Endpoint Categories

| Category | Endpoints | Purpose |
|----------|-----------|---------|
| **Current Structure** | `/provinces`, `/provinces/{code}/districts`, `/districts/{code}/wards`, `/full-address` | Active administrative divisions |
| **New Structure** | `/new-provinces`, `/new-provinces/{code}/wards`, `/new-full-address` | Includes merged entities |
| **Search** | `/search-address`, `/search-new-address` | Find locations with relevance scoring |
| **Conversion** | `/convert/address`, `/convert/batch` | Convert old addresses to new structure |
| **History** | `/merge-history/province/{code}`, `/merge-history/ward/{code}` | Track administrative changes |

---

## Detailed Endpoint Documentation

### 1. Current Administrative Structure

#### GET `/api/v1/provinces`
Get all active provinces (excludes merged provinces)

**Response:**
```json
{
  "data": [
    {
      "provinceId": 1,
      "name": "Thành phố Hà Nội",
      "code": "01",
      "type": "CITY",
      "displayName": "Hà Nội",
      "isActive": true,
      "isMerged": false,
      "isParentProvince": true
    }
  ],
  "message": "Successfully retrieved 63 provinces"
}
```

---

#### GET `/api/v1/provinces/{provinceCode}/districts`
Get all districts for a province

**Parameters:**
- `provinceCode` (path): Province code (e.g., "01" for Hanoi)

**Example:**
```bash
GET /api/v1/provinces/01/districts
```

**Response:**
```json
{
  "data": [
    {
      "districtId": 1,
      "name": "Quận Ba Đình",
      "code": "001",
      "type": "DISTRICT",
      "provinceId": 1,
      "provinceName": "Thành phố Hà Nội",
      "isActive": true
    }
  ],
  "message": "Successfully retrieved 12 districts"
}
```

---

#### GET `/api/v1/districts/{districtCode}/wards`
Get all wards for a district

**Parameters:**
- `districtCode` (path): District code (e.g., "001")

**Example:**
```bash
GET /api/v1/districts/001/wards
```

**Response:**
```json
{
  "data": [
    {
      "wardId": 1,
      "name": "Phường Phúc Xá",
      "code": "00001",
      "type": "WARD",
      "districtId": 1,
      "districtName": "Quận Ba Đình",
      "provinceId": 1,
      "provinceName": "Hà Nội",
      "isActive": true
    }
  ],
  "message": "Successfully retrieved 14 wards"
}
```

---

#### GET `/api/v1/full-address`
Build complete address from codes

**Parameters:**
- `provinceCode` (query): Province code
- `districtCode` (query): District code
- `wardCode` (query): Ward code

**Example:**
```bash
GET /api/v1/full-address?provinceCode=01&districtCode=001&wardCode=00001
```

**Response:**
```json
{
  "data": {
    "province": {
      "code": "01",
      "name": "Thành phố Hà Nội",
      "type": "CITY"
    },
    "district": {
      "code": "001",
      "name": "Quận Ba Đình",
      "type": "DISTRICT"
    },
    "ward": {
      "code": "00001",
      "name": "Phường Phúc Xá",
      "type": "WARD"
    },
    "fullAddressText": "Phường Phúc Xá, Quận Ba Đình, Hà Nội"
  },
  "message": "Successfully built full address"
}
```

---

### 2. New Administrative Structure (with Merged Entities)

#### GET `/api/v1/new-provinces`
Get ALL provinces including merged ones

**Use Case:** Historical data, conversion, reporting

**Response:**
```json
{
  "data": [
    {
      "provinceId": 1,
      "name": "Thành phố Hà Nội",
      "code": "01",
      "type": "CITY",
      "isActive": true,
      "isMerged": false,
      "isParentProvince": true
    },
    {
      "provinceId": 35,
      "name": "Tỉnh Hà Tây",
      "code": "35",
      "type": "PROVINCE",
      "isActive": true,
      "isMerged": true,
      "isParentProvince": false,
      "parentProvinceId": 1,
      "originalName": "Hà Tây"
    }
  ],
  "message": "Successfully retrieved 70 provinces (including merged)"
}
```

---

#### GET `/api/v1/new-provinces/{provinceCode}/wards`
Get wards directly by province (flattened hierarchy)

**Use Case:** Simplified selection without district level

**Example:**
```bash
GET /api/v1/new-provinces/01/wards
```

**Response:**
```json
{
  "data": [
    {
      "wardId": 1,
      "name": "Phường Phúc Xá",
      "code": "00001",
      "districtId": 1,
      "districtName": "Quận Ba Đình",
      "provinceId": 1,
      "provinceName": "Hà Nội"
    }
    // ... all wards across all districts in the province
  ],
  "message": "Successfully retrieved 584 wards"
}
```

---

### 3. Search Endpoints

#### GET `/api/v1/search-address`
Search in current/active administrative structure

**Parameters:**
- `q` (query): Search query
- `limit` (query, optional): Max results (default: 20, max: 100)

**Example:**
```bash
GET /api/v1/search-address?q=Ba%20Đình&limit=10
```

**Response:**
```json
{
  "data": {
    "query": "Ba Đình",
    "totalResults": 15,
    "matches": [
      {
        "provinceCode": "01",
        "provinceName": "Thành phố Hà Nội",
        "districtCode": "001",
        "districtName": "Quận Ba Đình",
        "wardCode": "00001",
        "wardName": "Phường Phúc Xá",
        "fullAddress": "Phường Phúc Xá, Quận Ba Đình, Hà Nội",
        "matchScore": 0.95,
        "isActive": true,
        "isMerged": false
      }
    ]
  },
  "message": "Found 15 matches"
}
```

**Match Score Algorithm:**
- `1.0` = Exact match
- `0.9` = Starts with query
- `0.5-0.8` = Contains query (scored by position and length)

---

#### GET `/api/v1/search-new-address`
Search including merged/historical entities

**Use Case:** Finding old addresses for conversion

**Example:**
```bash
GET /api/v1/search-new-address?q=Hà%20Tây&limit=20
```

**Response:** Similar to `/search-address` but includes merged entities

---

### 4. Address Conversion

#### GET `/api/v1/convert/address`
Convert single address from old to new structure

**Parameters:**
- `provinceCode` (query): Old province code
- `districtCode` (query): Old district code
- `wardCode` (query): Old ward code

**Example - Hà Tây (merged into Hanoi in 2008):**
```bash
GET /api/v1/convert/address?provinceCode=35&districtCode=001&wardCode=00001
```

**Response:**
```json
{
  "data": {
    "oldAddress": {
      "provinceCode": "35",
      "provinceName": "Tỉnh Hà Tây",
      "districtCode": "001",
      "districtName": "Huyện Hà Đông",
      "wardCode": "00001",
      "wardName": "Xã Vạn Phúc",
      "fullAddress": "Xã Vạn Phúc, Huyện Hà Đông, Hà Tây"
    },
    "newAddress": {
      "provinceCode": "01",
      "provinceName": "Thành phố Hà Nội",
      "districtCode": "268",
      "districtName": "Quận Hà Đông",
      "wardCode": "09589",
      "wardName": "Phường Vạn Phúc",
      "fullAddress": "Phường Vạn Phúc, Quận Hà Đông, Hà Nội"
    },
    "conversionInfo": {
      "wasConverted": true,
      "conversionType": "PROVINCE_MERGE",
      "effectiveDate": "2008-08-01",
      "notes": "Address was converted due to administrative reorganization"
    }
  },
  "message": "Address converted successfully"
}
```

**Conversion Types:**
- `NONE` - No conversion needed
- `PROVINCE_MERGE` - Province was merged (e.g., Hà Tây → Hà Nội)
- `WARD_MERGE` - Ward was merged or reorganized
- `DISTRICT_MERGE` - District was reorganized

---

#### POST `/api/v1/convert/batch`
Batch convert multiple addresses

**Request Body:**
```json
{
  "addresses": [
    {
      "provinceCode": "35",
      "districtCode": "001",
      "wardCode": "00001",
      "referenceId": "ADDR_001"
    },
    {
      "provinceCode": "35",
      "districtCode": "002",
      "wardCode": "00002",
      "referenceId": "ADDR_002"
    }
  ]
}
```

**Constraints:**
- Minimum: 1 address
- Maximum: 1000 addresses per batch

**Response:**
```json
{
  "data": [
    {
      "oldAddress": { ... },
      "newAddress": { ... },
      "conversionInfo": { ... }
    }
  ],
  "message": "Converted 142 out of 150 addresses"
}
```

---

### 5. Merge History

#### GET `/api/v1/merge-history/province/{code}`
Get province merge history

**Example:**
```bash
GET /api/v1/merge-history/province/35
```

**Response:**
```json
{
  "data": {
    "entityCode": "35",
    "entityName": "Tỉnh Hà Tây",
    "entityType": "PROVINCE",
    "isMerged": true,
    "mergeDate": "2008-08-01",
    "mergedInto": {
      "code": "01",
      "name": "Thành phố Hà Nội",
      "effectiveDate": "2008-08-01",
      "reason": "Administrative reorganization"
    },
    "mergedFrom": null
  },
  "message": "Successfully retrieved merge history"
}
```

---

#### GET `/api/v1/merge-history/ward/{code}`
Get ward merge history

**Example:**
```bash
GET /api/v1/merge-history/ward/00001
```

---

## Usage Examples

### Frontend Integration

#### Example 1: Cascading Dropdowns

```javascript
// React component
function AddressSelector() {
  const [provinces, setProvinces] = useState([]);
  const [districts, setDistricts] = useState([]);
  const [wards, setWards] = useState([]);

  const [selectedProvince, setSelectedProvince] = useState('');
  const [selectedDistrict, setSelectedDistrict] = useState('');
  const [selectedWard, setSelectedWard] = useState('');

  // Load provinces on mount
  useEffect(() => {
    fetch('/api/v1/provinces')
      .then(res => res.json())
      .then(data => setProvinces(data.data));
  }, []);

  // Load districts when province changes
  useEffect(() => {
    if (selectedProvince) {
      fetch(`/api/v1/provinces/${selectedProvince}/districts`)
        .then(res => res.json())
        .then(data => setDistricts(data.data));
    }
  }, [selectedProvince]);

  // Load wards when district changes
  useEffect(() => {
    if (selectedDistrict) {
      fetch(`/api/v1/districts/${selectedDistrict}/wards`)
        .then(res => res.json())
        .then(data => setWards(data.data));
    }
  }, [selectedDistrict]);

  return (
    <div>
      <select onChange={(e) => setSelectedProvince(e.target.value)}>
        <option value="">Select Province</option>
        {provinces.map(p => (
          <option key={p.code} value={p.code}>{p.name}</option>
        ))}
      </select>

      <select onChange={(e) => setSelectedDistrict(e.target.value)} disabled={!selectedProvince}>
        <option value="">Select District</option>
        {districts.map(d => (
          <option key={d.code} value={d.code}>{d.name}</option>
        ))}
      </select>

      <select onChange={(e) => setSelectedWard(e.target.value)} disabled={!selectedDistrict}>
        <option value="">Select Ward</option>
        {wards.map(w => (
          <option key={w.code} value={w.code}>{w.name}</option>
        ))}
      </select>
    </div>
  );
}
```

---

#### Example 2: Address Search with Autocomplete

```javascript
function AddressAutocomplete() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);

  const searchAddress = async (searchQuery) => {
    if (searchQuery.length < 2) return;

    const response = await fetch(
      `/api/v1/search-address?q=${encodeURIComponent(searchQuery)}&limit=10`
    );
    const data = await response.json();
    setResults(data.data.matches);
  };

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => searchAddress(query), 300);
    return () => clearTimeout(timer);
  }, [query]);

  return (
    <div>
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search address..."
      />
      <ul>
        {results.map((match, idx) => (
          <li key={idx} onClick={() => selectAddress(match)}>
            {match.fullAddress} (Score: {match.matchScore.toFixed(2)})
          </li>
        ))}
      </ul>
    </div>
  );
}
```

---

#### Example 3: Batch Address Conversion

```javascript
async function convertAddressesBulk(addresses) {
  const response = await fetch('/api/v1/convert/batch', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      addresses: addresses.map(addr => ({
        provinceCode: addr.provinceCode,
        districtCode: addr.districtCode,
        wardCode: addr.wardCode,
        referenceId: addr.id
      }))
    })
  });

  const result = await response.json();
  console.log(`Converted ${result.message}`);
  return result.data;
}

// Usage
const oldAddresses = [
  { id: 'ADDR_001', provinceCode: '35', districtCode: '001', wardCode: '00001' },
  { id: 'ADDR_002', provinceCode: '35', districtCode: '002', wardCode: '00002' }
];

const converted = await convertAddressesBulk(oldAddresses);
```

---

### Backend/Service Integration

#### Example: Listing Service Integration

```java
@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final EnhancedAddressService addressService;
    private final ListingRepository listingRepository;

    @Transactional
    public ListingResponse createListing(ListingCreationRequest request) {
        // Validate address codes
        boolean isValid = addressService.validateAddressCodes(
            request.getProvinceCode(),
            request.getDistrictCode(),
            request.getWardCode()
        );

        if (!isValid) {
            throw new InvalidAddressException("Invalid address codes");
        }

        // Get full address for display
        FullAddressResponse fullAddress = addressService.getFullAddress(
            request.getProvinceCode(),
            request.getDistrictCode(),
            request.getWardCode()
        );

        Listing listing = Listing.builder()
            .title(request.getTitle())
            .provinceCode(request.getProvinceCode())
            .districtCode(request.getDistrictCode())
            .wardCode(request.getWardCode())
            .fullAddress(fullAddress.getFullAddressText())
            .build();

        return listingMapper.toResponse(listingRepository.save(listing));
    }
}
```

---

## Performance Considerations

### Database Indexing

Ensure these indexes exist:

```sql
CREATE INDEX idx_province_code ON provinces(code);
CREATE INDEX idx_district_code ON districts(code);
CREATE INDEX idx_ward_code ON wards(code);
CREATE INDEX idx_province_parent ON provinces(parent_province_id);
CREATE INDEX idx_district_province ON districts(province_id);
CREATE INDEX idx_ward_district ON wards(district_id);
```

### Caching Strategy

Recommended caching for high-traffic scenarios:

```java
@Cacheable(value = "provinces", key = "'all'")
public List<ProvinceResponse> getAllProvinces() { ... }

@Cacheable(value = "districts", key = "#provinceCode")
public List<DistrictResponse> getDistrictsByProvinceCode(String provinceCode) { ... }

@Cacheable(value = "fullAddress", key = "#provinceCode + '-' + #districtCode + '-' + #wardCode")
public FullAddressResponse getFullAddress(...) { ... }
```

---

## Error Handling

### Error Response Format

```json
{
  "code": "4006",
  "message": "Province not found",
  "timestamp": "2025-01-18T15:30:00"
}
```

### Common Error Codes

| Code | HTTP Status | Message |
|------|-------------|---------|
| 4005 | 404 | Address not found |
| 4006 | 404 | Province not found |
| 4007 | 404 | District not found |
| 4008 | 404 | Ward not found |
| 2001 | 400 | Invalid input |
| 9001 | 500 | Internal server error |

---

## Testing

### Unit Test Example

```java
@Test
void testGetDistrictsByProvinceCode() {
    // Given
    String provinceCode = "01";

    // When
    List<DistrictResponse> districts = enhancedAddressService
        .getDistrictsByProvinceCode(provinceCode);

    // Then
    assertThat(districts).isNotEmpty();
    assertThat(districts).allMatch(d ->
        d.getProvinceId().equals(1L));
}
```

### Integration Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
class AddressApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAllProvinces() throws Exception {
        mockMvc.perform(get("/api/v1/provinces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].code").exists())
            .andExpect(jsonPath("$.message").value(containsString("provinces")));
    }

    @Test
    void testSearchAddress() throws Exception {
        mockMvc.perform(get("/api/v1/search-address")
                .param("q", "Ba Đình")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.matches").isArray());
    }
}
```

---

## API Versioning

Current version: **v1**

Future versions will use URL versioning:
- `/api/v1/...` - Current version
- `/api/v2/...` - Future version

---

## Swagger/OpenAPI Documentation

Access interactive API documentation:

```
http://localhost:8080/swagger-ui.html
```

All endpoints are fully documented with:
- ✅ Request/response schemas
- ✅ Parameter descriptions
- ✅ Example values
- ✅ Error responses
- ✅ Try-it-out functionality

---

## Migration Guide

### From Old Address API to Enhanced API

| Old Endpoint | New Endpoint | Notes |
|--------------|--------------|-------|
| `GET /v1/addresses/provinces` | `GET /api/v1/provinces` | Now code-based |
| `GET /v1/addresses/provinces/{id}/districts` | `GET /api/v1/provinces/{code}/districts` | Use code instead of ID |
| `GET /v1/addresses/districts/{id}/wards` | `GET /api/v1/districts/{code}/wards` | Use code instead of ID |
| N/A | `GET /api/v1/full-address` | New endpoint |
| N/A | `GET /api/v1/convert/address` | New feature |

---

## Support & Maintenance

### Health Check

```bash
GET /api/v1/health
```

Returns `200 OK` if API is operational.

### Logging

All endpoints log at INFO level:
```
2025-01-18 15:30:00 [INFO] GET /api/v1/provinces - Fetching all active provinces
2025-01-18 15:30:01 [INFO] GET /api/v1/convert/address - provinceCode=35, districtCode=001, wardCode=00001
```

### Monitoring Metrics

Key metrics to monitor:
- Request rate per endpoint
- Response time (p50, p95, p99)
- Error rate by error code
- Cache hit rate (if caching enabled)

---

## Changelog

### Version 2.0.0 (2025-01-18)
- ✨ Complete API refactoring with code-based endpoints
- ✨ Added merge history tracking
- ✨ Added address conversion features
- ✨ Added batch processing
- ✨ Enhanced search with relevance scoring
- ✨ Full Swagger documentation
- 🔧 Improved error handling
- 📚 Comprehensive documentation

---

## Contact & Support

For issues or questions:
- API Documentation: `/swagger-ui.html`
- GitHub Issues: [Link to your repo]
- Email: support@smartrent.com

---

**Designed & Implemented by:** Senior Java Spring Boot Engineer
**Version:** 2.0.0
**Last Updated:** January 18, 2025
