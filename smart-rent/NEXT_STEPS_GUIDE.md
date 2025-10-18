# Next Steps Guide - Vietnamese Administrative Units API

## Quick Start: Continuing the Implementation

This guide provides clear instructions for completing the remaining phases of the Vietnamese administrative units API implementation.

---

## Current Status

✅ **Completed:**
- Phase 1: Data Model Enhancement
- Phase 2: Database Migration
- Phase 3: DTOs Creation
- Phase 4: Repository Layer

🚧 **Next:** Phase 5 - Service Layer Implementation

---

## Phase 5: Service Layer Implementation

### Step 1: Run the Database Migration

First, apply the new database schema:

```bash
# Run Flyway migration
./gradlew flywayMigrate

# Or with custom database
./gradlew flywayMigrate -PdbUrl=jdbc:mysql://localhost:3306/smartrent -PdbUser=root -PdbPassword=yourpassword
```

**Verify migration:**
```sql
-- Check that new columns exist
DESCRIBE wards;
DESCRIBE provinces;
DESCRIBE districts;

-- Check that new table was created
SHOW TABLES LIKE 'address_conversion_mappings';

-- Verify configuration data
SELECT * FROM administrative_config;
```

### Step 2: Create Service Interface

Create: `src/main/java/com/smartrent/service/administrative/AdministrativeUnitService.java`

```java
package com.smartrent.service.administrative;

import com.smartrent.dto.request.administrative.*;
import com.smartrent.dto.response.administrative.*;
import java.util.List;

public interface AdministrativeUnitService {
    // Old structure APIs
    List<ProvinceOldResponse> getOldProvinces();
    List<DistrictOldResponse> getOldDistricts(Long provinceId);
    List<WardOldResponse> getOldWards(Long districtId);

    // New structure APIs
    List<ProvinceNewResponse> getNewProvinces();
    List<WardNewResponse> getNewWards(Long provinceId);

    // Conversion APIs
    AddressConversionResponse convertAddress(AddressConversionRequest request);
    BatchConversionResponse convertBatch(BatchConversionRequest request);

    // Merge history APIs
    ProvinceMergeHistoryResponse getProvinceMergeHistory(String code);
    WardMergeHistoryResponse getWardMergeHistory(String code);
}
```

### Step 3: Create MapStruct Mapper

Create: `src/main/java/com/smartrent/mapper/AdministrativeUnitMapper.java`

```java
package com.smartrent.mapper;

import com.smartrent.dto.response.administrative.*;
import com.smartrent.infra.repository.entity.*;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring")
public interface AdministrativeUnitMapper {

    // Old structure mappings
    @Mapping(source = "type", target = "type")
    ProvinceOldResponse toOldResponse(Province province);

    @Mapping(source = "province.provinceId", target = "provinceId")
    @Mapping(source = "province.name", target = "provinceName")
    DistrictOldResponse toOldResponse(District district);

    @Mapping(source = "district.districtId", target = "districtId")
    @Mapping(source = "district.name", target = "districtName")
    @Mapping(source = "district.province.provinceId", target = "provinceId")
    @Mapping(source = "district.province.name", target = "provinceName")
    WardOldResponse toOldResponse(Ward ward);

    // New structure mappings
    @Mapping(target = "mergedFromProvinceIds", expression = "java(getMergedProvinceIds(province))")
    @Mapping(target = "mergedFromProvinceNames", expression = "java(getMergedProvinceNames(province))")
    @Mapping(target = "wardCount", ignore = true)
    ProvinceNewResponse toNewResponse(Province province);

    @Mapping(source = "district.province.provinceId", target = "provinceId")
    @Mapping(source = "district.province.name", target = "provinceName")
    @Mapping(target = "mergedFromWardIds", expression = "java(getMergedWardIds(ward))")
    @Mapping(target = "mergedFromWardNames", expression = "java(getMergedWardNames(ward))")
    @Mapping(target = "formerDistrictName", source = "district.name")
    WardNewResponse toNewResponse(Ward ward);

    // Helper methods (implement in abstract class)
    default List<Long> getMergedProvinceIds(Province province) {
        if (province.getMergedProvinces() == null) return List.of();
        return province.getMergedProvinces().stream()
            .map(Province::getProvinceId)
            .toList();
    }

    default List<String> getMergedProvinceNames(Province province) {
        if (province.getMergedProvinces() == null) return List.of();
        return province.getMergedProvinces().stream()
            .map(Province::getOriginalName)
            .toList();
    }

    default List<Long> getMergedWardIds(Ward ward) {
        if (ward.getMergedWards() == null) return List.of();
        return ward.getMergedWards().stream()
            .map(Ward::getWardId)
            .toList();
    }

    default List<String> getMergedWardNames(Ward ward) {
        if (ward.getMergedWards() == null) return List.of();
        return ward.getMergedWards().stream()
            .map(Ward::getOriginalName)
            .toList();
    }
}
```

### Step 4: Implement Service

Create: `src/main/java/com/smartrent/service/administrative/impl/AdministrativeUnitServiceImpl.java`

Key methods to implement:

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdministrativeUnitServiceImpl implements AdministrativeUnitService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final AddressConversionMappingRepository conversionRepository;
    private final AdministrativeUnitMapper mapper;

    private static final LocalDate REORGANIZATION_DATE = LocalDate.of(2025, 7, 1);

    @Override
    public List<ProvinceOldResponse> getOldProvinces() {
        List<AdministrativeStructure> oldStructures =
            Arrays.asList(AdministrativeStructure.OLD, AdministrativeStructure.BOTH);

        return provinceRepository
            .findByStructureVersionInAndIsActiveTrueOrderByName(oldStructures)
            .stream()
            .map(mapper::toOldResponse)
            .collect(Collectors.toList());
    }

    @Override
    public AddressConversionResponse convertAddress(AddressConversionRequest request) {
        // 1. Validate old address exists
        // 2. Look up conversion mapping
        // 3. Build conversion response
        // 4. Handle edge cases (no mapping found, etc.)
    }

    // ... implement other methods
}
```

### Step 5: Create Custom Exceptions

Create in `src/main/java/com/smartrent/infra/exception/`:

```java
// ConversionNotFoundException.java
public class ConversionNotFoundException extends AppException {
    public ConversionNotFoundException(String message) {
        super(DomainCode.ADDRESS_CONVERSION_NOT_FOUND, message);
    }
}

// Add to DomainCode.java:
ADDRESS_CONVERSION_NOT_FOUND(7001, "Address conversion mapping not found"),
INVALID_ADMINISTRATIVE_STRUCTURE(7002, "Invalid administrative structure version"),
BATCH_CONVERSION_LIMIT_EXCEEDED(7005, "Batch conversion limit exceeded (max 100)");
```

---

## Phase 6: Controller Implementation

### Step 1: Create Controller

Create: `src/main/java/com/smartrent/controller/AdministrativeUnitController.java`

```java
@RestController
@RequestMapping("/api/v1/administrative-units")
@Tag(name = "Administrative Units", description = "Vietnamese administrative unit APIs")
@RequiredArgsConstructor
public class AdministrativeUnitController {

    private final AdministrativeUnitService service;

    @GetMapping("/provinces")
    @Operation(summary = "Get all provinces (old structure)")
    public ApiResponse<List<ProvinceOldResponse>> getOldProvinces() {
        return ApiResponse.<List<ProvinceOldResponse>>builder()
            .data(service.getOldProvinces())
            .build();
    }

    // ... implement other endpoints
}
```

### Step 2: Test Endpoints

After implementing the controller:

```bash
# Start the application
./gradlew bootRun --args='--spring.profiles.active=local'

# Test endpoints
curl http://localhost:8080/api/v1/administrative-units/provinces
curl http://localhost:8080/api/v1/administrative-units/new-provinces
```

---

## Phase 7: Data Population

### Step 1: Prepare Sample Data

Create SQL script: `src/main/resources/db/data/sample_administrative_data.sql`

```sql
-- Insert sample provinces
INSERT INTO provinces (name, code, type, structure_version, is_active, effective_from)
VALUES
    ('Hà Nội', 'HN', 'CITY', 'BOTH', TRUE, '1954-10-10'),
    ('Hồ Chí Minh', 'HCM', 'CITY', 'BOTH', TRUE, '1976-07-02');

-- Insert sample districts (old structure only)
INSERT INTO districts (name, code, type, province_id, structure_version, is_active)
VALUES
    ('Quận Ba Đình', 'BAD', 'DISTRICT', 1, 'OLD', TRUE),
    ('Quận Hoàn Kiếm', 'HKM', 'DISTRICT', 1, 'OLD', TRUE);

-- Insert sample wards
INSERT INTO wards (name, code, type, district_id, structure_version, is_active)
VALUES
    ('Phường Điện Biên', 'DBF', 'WARD', 1, 'BOTH', TRUE),
    ('Phường Hàng Bạc', 'HBC', 'WARD', 2, 'BOTH', TRUE);

-- Insert sample conversion mappings
INSERT INTO address_conversion_mappings
    (old_province_id, old_district_id, old_ward_id, new_province_id, new_ward_id,
     conversion_accuracy, conversion_note)
VALUES
    (1, 1, 1, 1, 1, 100, 'Direct mapping - no changes'),
    (1, 2, 2, 1, 2, 100, 'District dissolved, ward remains');
```

### Step 2: Import Real Data

For production, you'll need:

1. **Official Vietnam Administrative Data**
   - Download from: https://danhmuchanhchinh.gso.gov.vn/
   - Or use: https://github.com/daohoangson/dvhcvn

2. **Conversion Mapping Data**
   - Create based on official reorganization documents
   - Verify with local authorities

3. **Import Script**
```bash
# Import administrative data
mysql -u root -p smartrent < vietnam_administrative_data.sql

# Import conversion mappings
mysql -u root -p smartrent < conversion_mappings.sql
```

---

## Phase 8: Testing

### Unit Tests Template

Create: `src/test/java/com/smartrent/service/AdministrativeUnitServiceTest.java`

```java
@SpringBootTest
class AdministrativeUnitServiceTest {

    @Autowired
    private AdministrativeUnitService service;

    @Test
    void testGetOldProvinces_ShouldReturnOldStructureProvinces() {
        // Given
        // When
        List<ProvinceOldResponse> provinces = service.getOldProvinces();

        // Then
        assertNotNull(provinces);
        assertTrue(provinces.size() > 0);
    }

    @Test
    void testConvertAddress_ValidMapping_ShouldReturnConversion() {
        // Test conversion logic
    }

    @Test
    void testConvertAddress_NoMapping_ShouldThrowException() {
        // Test error handling
    }
}
```

### Integration Tests Template

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AdministrativeUnitControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetOldProvinces_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/administrative-units/provinces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }
}
```

Run tests:
```bash
./gradlew test
./gradlew test jacocoTestReport
```

---

## Phase 9: Documentation

### Swagger Documentation

Access at: `http://localhost:8080/swagger-ui.html`

Ensure all endpoints have:
- Summary and description
- Request/response examples
- Error responses documented

### API Usage Examples

Create: `API_USAGE_EXAMPLES.md`

Include examples for:
1. Querying old structure (Province → District → Ward)
2. Querying new structure (Province → Ward)
3. Converting single address
4. Batch converting addresses
5. Viewing merge history

---

## Verification Checklist

Before considering the implementation complete:

### Functionality
- [ ] All old structure endpoints working
- [ ] All new structure endpoints working
- [ ] Address conversion working (single & batch)
- [ ] Merge history endpoints working
- [ ] Error handling working correctly

### Performance
- [ ] All queries < 200ms (test with 1000+ records)
- [ ] Batch conversion handles 100 addresses < 2s
- [ ] Database indexes being used (check EXPLAIN plans)

### Quality
- [ ] Unit tests passing (>80% coverage)
- [ ] Integration tests passing
- [ ] No compiler warnings
- [ ] Code follows project style guidelines

### Documentation
- [ ] All endpoints documented in Swagger
- [ ] API usage guide complete
- [ ] Migration guide for API consumers
- [ ] Postman collection created

### Data
- [ ] Sample data loaded and working
- [ ] Production data ready for import
- [ ] Conversion mappings validated

---

## Common Issues & Solutions

### Issue 1: Migration Fails

**Error:** `Table 'wards' already has column 'structure_version'`

**Solution:**
```sql
-- Check if column exists
SHOW COLUMNS FROM wards LIKE 'structure_version';

-- If exists, skip migration or drop and recreate:
ALTER TABLE wards DROP COLUMN structure_version;
-- Then rerun migration
```

### Issue 2: MapStruct Not Generating Implementation

**Solution:**
```bash
# Clean and rebuild
./gradlew clean build

# Check if mapper implementation was generated
ls build/generated/sources/annotationProcessor/java/main/com/smartrent/mapper/
```

### Issue 3: Circular Dependency in Service

**Solution:** Use `@Lazy` annotation:
```java
@RequiredArgsConstructor
public class AdministrativeUnitServiceImpl {
    private final @Lazy SomeOtherService otherService;
}
```

---

## Quick Commands Reference

```bash
# Build project
./gradlew clean build

# Run locally
./gradlew bootRun --args='--spring.profiles.active=local'

# Run tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# Run specific test
./gradlew test --tests AdministrativeUnitServiceTest

# Database migration
./gradlew flywayMigrate

# Check migration status
./gradlew flywayInfo

# API documentation
# http://localhost:8080/swagger-ui.html
```

---

## Need Help?

### Resources
1. **Refactoring Plan**: `REFACTORING_PLAN.md` - Complete architecture design
2. **Implementation Summary**: `IMPLEMENTATION_SUMMARY.md` - What's been done
3. **Address Documentation**: `ADDRESS_FLOW_DOCUMENTATION.md` - Current system
4. **Error Codes**: `docs/ERROR_CODES.md` - Error code reference
5. **Project Instructions**: `CLAUDE.md` - Development guidelines

### Reference API
- **tinhthanhpho.com API**: https://tinhthanhpho.com/api-docs

---

## Estimated Timeline

- **Phase 5 (Service Layer)**: 2-3 days
- **Phase 6 (Controller)**: 1-2 days
- **Phase 7 (Data Population)**: 2-3 days
- **Phase 8 (Testing)**: 2-3 days
- **Phase 9 (Documentation)**: 1 day

**Total: ~2 weeks for complete implementation**

---

## Success Criteria

When all phases are complete, you should be able to:

✅ Query provinces, districts, wards from old structure
✅ Query provinces, wards from new structure (no districts)
✅ Convert addresses from old → new structure
✅ View province and ward merger histories
✅ Handle 100+ batch conversions efficiently
✅ Access complete API documentation via Swagger

---

**Ready to start Phase 5!**

Good luck with the implementation! 🚀
