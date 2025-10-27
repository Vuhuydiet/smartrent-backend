# Address System Refactoring - Complete Guide

## Overview
This document describes the **unified address system** that supports both the **legacy address structure** (before July 1, 2025) and the **new address structure** (after July 1, 2025) through a single, comprehensive API.

**Key Features:**
- Single unified controller (`AddressController`) at `/v1/addresses`
- Backward-compatible with existing frontend code
- Comprehensive Swagger documentation
- Support for both ID-based (legacy) and code-based (new) operations
- Seamless transition between old and new administrative structures

## Database Schema

### Legacy Structure (Before July 1, 2025)
The legacy structure uses **integer IDs** and follows this hierarchy:
```
Province → District → Ward → Street
```

**Tables:**
- `province` - Legacy provinces (integer ID, `_name`, `name_en`, `_code`)
- `district` - Legacy districts (integer ID, `_name`, `name_en`, `_prefix`, `_province_id`)
- `ward` - Legacy wards (integer ID, `_name`, `name_en`, `_prefix`, `_province_id`, `_district_id`)
- `street` - Legacy streets (integer ID, `_name`, `name_en`, `_prefix`, `_province_id`, `_district_id`, `_ward_id`)
- `project` - Project/location data (integer ID, `_name`, coordinates)

### New Structure (After July 1, 2025)
The new structure uses **VARCHAR codes** as primary keys and eliminates the district level:
```
Province → Ward
```

**Tables:**
- `provinces` - New provinces (VARCHAR code PK, linked to `administrative_units`)
- `wards` - New wards (VARCHAR code PK, linked to `provinces` and `administrative_units`)
- `administrative_regions` - Geographic regions of Vietnam (Northeast, Southeast, etc.)
- `administrative_units` - Administrative unit types (Municipality, Province, Ward, Commune, etc.)

### Mapping Tables
These tables enable conversion between legacy and new structures:
- `province_mapping` - Maps legacy province ID to new province code
- `district_ward_mapping` - Maps legacy district ID to new ward code
- `ward_mapping` - Maps legacy ward ID to new ward code (includes merge type)
- `street_mapping` - Maps legacy street ID to new province/ward codes

## Entity Classes

### New Structure Entities
- `NewProvince` - Provinces with VARCHAR code PK
- `NewWard` - Wards with VARCHAR code PK
- `AdministrativeRegion` - Geographic regions
- `AdministrativeUnit` - Administrative unit types

### Legacy Structure Entities
- `LegacyProvince` - Provinces with integer ID
- `LegacyDistrict` - Districts with integer ID
- `LegacyWard` - Wards with integer ID
- `LegacyStreet` - Streets with integer ID
- `Project` - Project/location data

### Mapping Entities
- `ProvinceMapping` - Province conversion mapping
- `DistrictWardMapping` - District to ward conversion
- `WardMapping` - Ward conversion with merge types
- `StreetMapping` - Street conversion mapping

## Repository Layer

### New Structure Repositories
- `NewProvinceRepository` - CRUD + search for new provinces
- `NewWardRepository` - CRUD + search for new wards
- `AdministrativeRegionRepository` - Administrative regions
- `AdministrativeUnitRepository` - Administrative units

### Legacy Structure Repositories
- `LegacyProvinceRepository` - CRUD + search for legacy provinces
- `LegacyDistrictRepository` - CRUD + search for legacy districts
- `LegacyWardRepository` - CRUD + search for legacy wards
- `LegacyStreetRepository` - CRUD + search for legacy streets

### Mapping Repositories
- `ProvinceMappingRepository` - Province mapping queries
- `WardMappingRepository` - Ward mapping queries

## Service Layer

### AddressService Interface
Provides methods for:
- **New structure operations**: provinces and wards
- **Legacy structure operations**: provinces, districts, wards, streets
- **Address conversion**: legacy to new structure

### AddressServiceImpl
Implementation with methods for:
- Pagination support for all list operations
- Keyword search functionality
- Full address retrieval
- Address conversion with mapping

## REST API - Unified Controller

### AddressController
Base path: `/v1/addresses`

**Legacy Structure Endpoints (63 provinces, ID-based):**
```
GET /provinces                                    - Get all provinces
GET /provinces/{provinceId}                       - Get province by ID
GET /provinces/search?q={query}                   - Search provinces
GET /provinces/{provinceId}/districts             - Get districts by province
GET /districts/{districtId}                       - Get district by ID
GET /districts/search?q={query}&provinceId={id}   - Search districts
GET /districts/{districtId}/wards                 - Get wards by district
GET /wards/{wardId}                               - Get ward by ID
GET /wards/search?q={query}&districtId={id}       - Search wards
```

**New Structure Endpoints (34 provinces, code-based, paginated):**
```
GET /new-provinces?keyword={kw}&page={p}&limit={l}                      - Get all new provinces
GET /new-provinces/{provinceCode}/wards?keyword={kw}&page={p}&limit={l} - Get wards by province
GET /new-full-address?provinceCode={code}&wardCode={code}               - Get full new address
GET /search-new-address?keyword={kw}&page={p}&limit={l}                 - Search new addresses
```

**Utility Endpoints:**
```
GET /health                                       - API health check
```

**Response Format:**
All endpoints return data wrapped in `ApiResponse<T>` or `PaginatedResponse<T>`:
```json
{
  "data": { ... },
  "message": "Success message",
  "success": true,
  "metadata": {  // For paginated responses only
    "total": 100,
    "page": 1,
    "limit": 20
  }
}
```

## DTOs (Data Transfer Objects)

### New Structure DTOs
- `NewProvinceResponse` - Province data with code, name, administrative unit
- `NewWardResponse` - Ward data with code, name, province info
- `NewFullAddressResponse` - Complete new address (province + ward)

### Legacy Structure DTOs
- `LegacyProvinceResponse` - Province data with ID, name, code
- `LegacyDistrictResponse` - District data with ID, name, province info
- `LegacyWardResponse` - Ward data with ID, name, district/province info
- `LegacyStreetResponse` - Street data with ID, name, location info
- `FullAddressResponse` - Complete legacy address (province + district + ward + street)

### Conversion DTOs
- `AddressConversionResponse` - Contains both legacy and new addresses with conversion notes

## Key Differences Between Legacy and New

| Aspect | Legacy (Before July 1, 2025) | New (After July 1, 2025) |
|--------|------------------------------|--------------------------|
| **Primary Key** | Integer ID | VARCHAR code |
| **Hierarchy** | Province → District → Ward → Street | Province → Ward |
| **District Level** | Exists | Eliminated |
| **Number of Provinces** | 63 | 37 (consolidated) |
| **Administrative Units** | Not explicitly defined | Linked to `administrative_units` table |
| **Column Naming** | Prefixed with `_` (e.g., `_name`) | Standard names (e.g., `name`) |

## Usage Examples

### Getting Legacy Provinces (for existing code)
```http
GET /v1/addresses/provinces
Response: List<AddressUnitDTO> wrapped in ApiResponse
```

### Getting New Provinces (paginated)
```http
GET /v1/addresses/new-provinces?keyword=Hà Nội&page=1&limit=20
Response: PaginatedResponse with metadata
```

### Cascading Address Selection (Legacy)
```http
# Step 1: Get all provinces
GET /v1/addresses/provinces

# Step 2: Get districts for selected province
GET /v1/addresses/provinces/1/districts

# Step 3: Get wards for selected district
GET /v1/addresses/districts/1/wards
```

### Cascading Address Selection (New Structure)
```http
# Step 1: Get all provinces
GET /v1/addresses/new-provinces?page=1&limit=20

# Step 2: Get wards for selected province (no district level)
GET /v1/addresses/new-provinces/01/wards?page=1&limit=20
```

### Searching Addresses
```http
# Legacy search
GET /v1/addresses/wards/search?q=Phúc Xá&districtId=1

# New structure search
GET /v1/addresses/search-new-address?keyword=Phúc Xá&page=1&limit=20
```

## Migration Notes

1. **Data Import**: Migration files V22 and V23 contain INSERT statements for legacy and new data
2. **Mapping Data**: Mapping tables must be populated to enable address conversion
3. **Backward Compatibility**: Both legacy and new APIs are available simultaneously
4. **Transition Period**: Applications can query both structures during migration

## Swagger Documentation

The unified AddressController includes comprehensive Swagger/OpenAPI documentation:

- **API Tag**: "Address API (Unified)"
- **Description**: Detailed explanation of both legacy and new structures
- **Example Responses**: JSON examples for each endpoint
- **Parameter Descriptions**: Clear guidance on query parameters
- **Response Codes**: 200 (success), 400 (bad request), 404 (not found)

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

## Migration Guide

### For Frontend Developers

**If your code currently uses:**
```javascript
// Old: GET /api/v1/addresses/provinces
// Still works! No changes needed for backward compatibility
fetch('/v1/addresses/provinces')
```

**To adopt new structure:**
```javascript
// New: GET /v1/addresses/new-provinces?page=1&limit=20
// Returns paginated response with 34 provinces
fetch('/v1/addresses/new-provinces?page=1&limit=20')
```

### For Backend Developers

**Service Layer:**
- Use `AddressServiceImpl` - it implements the old `AddressService` interface
- All legacy methods still work (getProvinceById, getDistrictsByProvinceId, etc.)
- New methods available for new structure (getAllNewProvinces, getNewWardsByProvince, etc.)

**Controller:**
- Single `AddressController` at `/v1/addresses`
- No need to change existing endpoint consumers
- New endpoints available with `/new-` prefix

## Cleanup Summary

### Files Removed
- ❌ `Province.java` (replaced by `LegacyProvince` and `NewProvince`)
- ❌ `District.java` (replaced by `LegacyDistrict`)
- ❌ `Ward.java` (replaced by `LegacyWard` and `NewWard`)
- ❌ `Street.java` (replaced by `LegacyStreet`)
- ❌ `LegacyAddressController.java` (merged into `AddressController`)
- ❌ `NewAddressController.java` (merged into `AddressController`)
- ❌ `AddressConversionController.java` (functionality in service layer)

### Files Added/Updated
- ✅ `LegacyProvince`, `LegacyDistrict`, `LegacyWard`, `LegacyStreet` entities
- ✅ `NewProvince`, `NewWard` entities
- ✅ `AdministrativeRegion`, `AdministrativeUnit` reference entities
- ✅ Mapping entities: `ProvinceMapping`, `DistrictWardMapping`, `WardMapping`, `StreetMapping`
- ✅ 10 repository interfaces
- ✅ `AddressServiceImpl` - unified implementation
- ✅ `AddressController` - remains at `/v1/addresses` with expanded functionality
- ✅ Response DTOs for legacy and new structures

## Future Enhancements

- [ ] Complete address conversion API endpoints
- [ ] Add caching for frequently accessed provinces/wards
- [ ] Implement batch conversion API
- [ ] Add reverse geocoding (coordinates to address)
- [ ] Support for address autocomplete
- [ ] Migration scripts for existing address references
- [ ] Archive old address data after full migration
- [ ] Performance optimization for search queries

## Related Files

- **Migrations**: `V21__Create_address_new.sql`, `V22__Create_provinces_database_old.sql`, `V23__Create_provinces_database.sql`
- **Entities**: `src/main/java/com/smartrent/infra/repository/entity/Legacy*.java`, `New*.java`, mapping entities
- **Repositories**: `src/main/java/com/smartrent/infra/repository/*Repository.java`
- **Services**: `src/main/java/com/smartrent/service/address/AddressServiceImpl.java`
- **Controller**: `src/main/java/com/smartrent/controller/AddressController.java` (unified)
- **DTOs**: `src/main/java/com/smartrent/dto/response/*Response.java`

## Testing

### Manual Testing via Swagger
1. Start the application: `./gradlew bootRun --args='--spring.profiles.active=local'`
2. Navigate to: `http://localhost:8080/swagger-ui.html`
3. Find "Address API (Unified)" section
4. Test legacy endpoints: `/v1/addresses/provinces`, `/provinces/{id}/districts`
5. Test new endpoints: `/v1/addresses/new-provinces`, `/new-provinces/{code}/wards`

### Sample cURL Commands
```bash
# Get all legacy provinces
curl -X GET "http://localhost:8080/v1/addresses/provinces"

# Get new provinces (paginated)
curl -X GET "http://localhost:8080/v1/addresses/new-provinces?page=1&limit=20"

# Search wards in new structure
curl -X GET "http://localhost:8080/v1/addresses/search-new-address?keyword=Ba%20Đình&page=1&limit=10"

# Health check
curl -X GET "http://localhost:8080/v1/addresses/health"
```
