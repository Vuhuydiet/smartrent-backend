# Vietnamese Administrative Units API - Refactoring Plan

## Executive Summary

This document outlines the complete refactoring plan to transform the current SmartRent address system into a comprehensive Vietnamese administrative units API similar to https://tinhthanhpho.com/api-docs.

**Goal:** Create a robust API that handles Vietnam's administrative reorganization (effective July 1, 2025) with support for:
- Old administrative structure (before July 1, 2025)
- New administrative structure (after July 1, 2025)
- Address conversion between old and new structures
- Merge history tracking
- Full backward compatibility

---

## Current System Analysis

### ✅ Strengths
1. **Solid Foundation**:
   - 5-tier hierarchical structure (Province → District → Ward → Street → Address)
   - Self-referencing province mergers already implemented
   - Comprehensive indexing for performance

2. **Existing Features**:
   - Province merger support (`parentProvince`, `isMerged` flags)
   - Temporal validity (`effectiveFrom`, `effectiveTo`)
   - Active/inactive status management
   - Full-text address search
   - Geographic queries (lat/lng)

3. **Well-Architected**:
   - Clean layered architecture (Controller → Service → Repository → Entity)
   - DTO pattern implementation
   - MapStruct for entity-DTO mapping
   - Swagger documentation

### ⚠️ Gaps to Address

1. **Missing Administrative Reorganization Support**:
   - No distinction between old/new districts (districts were dissolved in new structure)
   - Ward mergers not fully tracked
   - No conversion APIs for old → new address mapping

2. **API Structure**:
   - Current endpoints don't separate old/new structures
   - No merge history endpoints
   - No batch conversion support

3. **Data Model Enhancements Needed**:
   - Ward merger tracking (similar to provinces)
   - Administrative structure version field
   - Conversion mapping table for backward compatibility

---

## Refactoring Strategy

### Phase 1: Data Model Enhancement ✅ (Current Task)

#### 1.1 Update Ward Entity
Add self-referencing merger support (similar to Province):

```java
@Entity
public class Ward {
    // ... existing fields ...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_ward_id")
    Ward parentWard;  // For merged wards

    @OneToMany(mappedBy = "parentWard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Ward> mergedWards;

    @Builder.Default
    @Column(name = "is_merged", nullable = false)
    Boolean isMerged = false;

    @Column(name = "merged_date")
    LocalDate mergedDate;

    @Column(name = "original_name", length = 100)
    String originalName;

    // Helper methods
    public boolean isParentWard() { ... }
    public boolean isMergedWard() { ... }
    public String getDisplayName() { ... }
}
```

#### 1.2 Add Administrative Structure Version
Track which administrative structure version each entity belongs to:

```java
public enum AdministrativeStructure {
    OLD,    // Before July 1, 2025
    NEW,    // After July 1, 2025
    BOTH    // Valid in both structures
}
```

Add to Province, District, Ward:
```java
@Enumerated(EnumType.STRING)
@Column(name = "structure_version", nullable = false)
@Builder.Default
AdministrativeStructure structureVersion = AdministrativeStructure.BOTH;
```

#### 1.3 Create Address Conversion Mapping Table
For tracking old → new address conversions:

```java
@Entity
@Table(name = "address_conversion_mappings")
public class AddressConversionMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long mappingId;

    // Old structure (before July 1, 2025)
    @ManyToOne
    @JoinColumn(name = "old_province_id")
    Province oldProvince;

    @ManyToOne
    @JoinColumn(name = "old_district_id")
    District oldDistrict;

    @ManyToOne
    @JoinColumn(name = "old_ward_id")
    Ward oldWard;

    // New structure (after July 1, 2025)
    @ManyToOne
    @JoinColumn(name = "new_province_id")
    Province newProvince;

    // No district in new structure - wards directly under provinces

    @ManyToOne
    @JoinColumn(name = "new_ward_id")
    Ward newWard;

    @Column(name = "conversion_note", columnDefinition = "TEXT")
    String conversionNote;

    @CreationTimestamp
    LocalDateTime createdAt;
}
```

---

### Phase 2: Database Migration

#### 2.1 Create Migration: V13__Add_administrative_reorganization_support.sql

```sql
-- Add ward merger support (similar to provinces)
ALTER TABLE wards
ADD COLUMN parent_ward_id BIGINT AFTER district_id,
ADD COLUMN is_merged BOOLEAN NOT NULL DEFAULT FALSE AFTER is_active,
ADD COLUMN merged_date DATE AFTER is_merged,
ADD COLUMN original_name VARCHAR(100) AFTER merged_date,
ADD COLUMN structure_version ENUM('OLD', 'NEW', 'BOTH') NOT NULL DEFAULT 'BOTH' AFTER original_name;

ALTER TABLE wards
ADD CONSTRAINT fk_wards_parent FOREIGN KEY (parent_ward_id) REFERENCES wards(ward_id) ON DELETE SET NULL,
ADD INDEX idx_parent_ward (parent_ward_id),
ADD INDEX idx_structure_version (structure_version);

-- Add structure version to provinces
ALTER TABLE provinces
ADD COLUMN structure_version ENUM('OLD', 'NEW', 'BOTH') NOT NULL DEFAULT 'BOTH' AFTER original_name;

ALTER TABLE provinces
ADD INDEX idx_structure_version (structure_version);

-- Add structure version to districts
ALTER TABLE districts
ADD COLUMN structure_version ENUM('OLD', 'NEW', 'BOTH') NOT NULL DEFAULT 'BOTH' AFTER effective_to;

ALTER TABLE districts
ADD INDEX idx_structure_version (structure_version);

-- Create address conversion mapping table
CREATE TABLE address_conversion_mappings (
    mapping_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    old_province_id BIGINT,
    old_district_id BIGINT,
    old_ward_id BIGINT,
    new_province_id BIGINT,
    new_ward_id BIGINT,
    conversion_note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_old_address (old_province_id, old_district_id, old_ward_id),
    INDEX idx_new_address (new_province_id, new_ward_id),

    CONSTRAINT fk_mapping_old_province FOREIGN KEY (old_province_id) REFERENCES provinces(province_id),
    CONSTRAINT fk_mapping_old_district FOREIGN KEY (old_district_id) REFERENCES districts(district_id),
    CONSTRAINT fk_mapping_old_ward FOREIGN KEY (old_ward_id) REFERENCES wards(ward_id),
    CONSTRAINT fk_mapping_new_province FOREIGN KEY (new_province_id) REFERENCES provinces(province_id),
    CONSTRAINT fk_mapping_new_ward FOREIGN KEY (new_ward_id) REFERENCES wards(ward_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add metadata table for tracking reorganization effective date
CREATE TABLE administrative_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO administrative_config (config_key, config_value, description) VALUES
('REORGANIZATION_DATE', '2025-07-01', 'Effective date of administrative reorganization'),
('CURRENT_STRUCTURE', 'NEW', 'Current administrative structure in use (OLD/NEW)');
```

---

### Phase 3: DTOs Creation

#### 3.1 Old Structure DTOs

```java
// ProvinceOldResponse.java
@Getter @Setter @Builder
public class ProvinceOldResponse {
    Long provinceId;
    String name;
    String code;
    String type;
    Boolean isActive;
    LocalDate effectiveFrom;
    LocalDate effectiveTo;
}

// DistrictOldResponse.java
@Getter @Setter @Builder
public class DistrictOldResponse {
    Long districtId;
    String name;
    String code;
    String type;
    Long provinceId;
    String provinceName;
    Boolean isActive;
    LocalDate effectiveFrom;
    LocalDate effectiveTo;
}

// WardOldResponse.java
@Getter @Setter @Builder
public class WardOldResponse {
    Long wardId;
    String name;
    String code;
    String type;
    Long districtId;
    String districtName;
    Long provinceId;
    String provinceName;
    Boolean isActive;
    LocalDate effectiveFrom;
    LocalDate effectiveTo;
}
```

#### 3.2 New Structure DTOs

```java
// ProvinceNewResponse.java
@Getter @Setter @Builder
public class ProvinceNewResponse {
    Long provinceId;
    String name;
    String code;
    String type;
    Boolean isActive;
    List<Long> mergedFromProvinceIds;  // Shows which old provinces were merged
    LocalDate effectiveFrom;
}

// WardNewResponse.java (Direct under province, no district)
@Getter @Setter @Builder
public class WardNewResponse {
    Long wardId;
    String name;
    String code;
    String type;
    Long provinceId;
    String provinceName;
    Boolean isActive;
    List<Long> mergedFromWardIds;  // Shows which old wards were merged
    LocalDate effectiveFrom;
}
```

#### 3.3 Conversion DTOs

```java
// AddressConversionRequest.java
@Getter @Setter
public class AddressConversionRequest {
    @NotNull Long oldProvinceId;
    @NotNull Long oldDistrictId;
    @NotNull Long oldWardId;
    String streetNumber;
    String streetName;
}

// AddressConversionResponse.java
@Getter @Setter @Builder
public class AddressConversionResponse {
    // Old structure
    OldAddressInfo oldAddress;

    // New structure
    NewAddressInfo newAddress;

    // Conversion metadata
    Boolean conversionSuccessful;
    String conversionNote;
    LocalDate conversionDate;
}

// OldAddressInfo.java
@Getter @Setter @Builder
public class OldAddressInfo {
    String provinceName;
    String districtName;
    String wardName;
    String fullAddress;
}

// NewAddressInfo.java
@Getter @Setter @Builder
public class NewAddressInfo {
    String provinceName;
    String wardName;  // No district
    String fullAddress;
}

// BatchConversionRequest.java
@Getter @Setter
public class BatchConversionRequest {
    @NotNull @Size(min = 1, max = 100)
    List<AddressConversionRequest> addresses;
}

// BatchConversionResponse.java
@Getter @Setter @Builder
public class BatchConversionResponse {
    List<AddressConversionResponse> conversions;
    Integer totalRequested;
    Integer successfulConversions;
    Integer failedConversions;
}
```

#### 3.4 Merge History DTOs

```java
// ProvinceMergeHistoryResponse.java
@Getter @Setter @Builder
public class ProvinceMergeHistoryResponse {
    Long provinceId;
    String currentName;
    String code;

    // Merger information
    Boolean isMerged;
    LocalDate mergedDate;
    String originalName;

    // Parent info (if merged into another)
    Long parentProvinceId;
    String parentProvinceName;

    // Child provinces (if this is a parent)
    List<MergedProvinceInfo> mergedProvinces;
}

// WardMergeHistoryResponse.java
@Getter @Setter @Builder
public class WardMergeHistoryResponse {
    Long wardId;
    String currentName;
    String code;

    // Merger information
    Boolean isMerged;
    LocalDate mergedDate;
    String originalName;

    // Parent info
    Long parentWardId;
    String parentWardName;

    // Child wards
    List<MergedWardInfo> mergedWards;

    // Location change
    String oldDistrictName;
    String newProvinceName;
}
```

---

### Phase 4: Repository Layer Updates

#### 4.1 Add to ProvinceRepository

```java
@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {
    // ... existing methods ...

    // New structure queries
    List<Province> findByStructureVersionInAndIsActiveTrueOrderByName(
        List<AdministrativeStructure> structures);

    Optional<Province> findByCodeAndStructureVersionInAndIsActiveTrue(
        String code, List<AdministrativeStructure> structures);

    // Find provinces valid at specific date
    @Query("SELECT p FROM provinces p WHERE p.isActive = true " +
           "AND (p.effectiveFrom IS NULL OR p.effectiveFrom <= :date) " +
           "AND (p.effectiveTo IS NULL OR p.effectiveTo > :date)")
    List<Province> findActiveAtDate(@Param("date") LocalDate date);
}
```

#### 4.2 Add to WardRepository

```java
@Repository
public interface WardRepository extends JpaRepository<Ward, Long> {
    // ... existing methods ...

    // New structure: wards directly under provinces
    @Query("SELECT w FROM wards w WHERE w.district.province.provinceId = :provinceId " +
           "AND w.structureVersion IN :structures AND w.isActive = true ORDER BY w.name")
    List<Ward> findByProvinceIdAndStructureVersionInAndIsActiveTrueOrderByName(
        @Param("provinceId") Long provinceId,
        @Param("structures") List<AdministrativeStructure> structures);

    // Get parent wards (for merged wards)
    List<Ward> findByParentWardIsNullAndStructureVersionInAndIsActiveTrueOrderByName(
        List<AdministrativeStructure> structures);

    // Get merged wards for a parent
    List<Ward> findByParentWardWardIdAndIsActiveTrueOrderByName(Long parentWardId);

    // Find by code with structure version
    Optional<Ward> findByCodeAndStructureVersionInAndIsActiveTrue(
        String code, List<AdministrativeStructure> structures);
}
```

#### 4.3 Create AddressConversionMappingRepository

```java
@Repository
public interface AddressConversionMappingRepository
        extends JpaRepository<AddressConversionMapping, Long> {

    // Find conversion by old address
    Optional<AddressConversionMapping> findByOldProvinceProvinceIdAndOldDistrictDistrictIdAndOldWardWardId(
        Long oldProvinceId, Long oldDistrictId, Long oldWardId);

    // Find all conversions for a province
    List<AddressConversionMapping> findByOldProvinceProvinceIdOrNewProvinceProvinceId(
        Long oldProvinceId, Long newProvinceId);

    // Find reverse conversion (new → old)
    List<AddressConversionMapping> findByNewProvinceProvinceIdAndNewWardWardId(
        Long newProvinceId, Long newWardId);
}
```

---

### Phase 5: Service Layer Implementation

#### 5.1 Create AdministrativeUnitService

```java
public interface AdministrativeUnitService {
    // Old structure APIs
    List<ProvinceOldResponse> getOldProvinces();
    List<DistrictOldResponse> getOldDistricts(Long provinceId);
    List<WardOldResponse> getOldWards(Long districtId);
    OldFullAddressResponse getOldFullAddress(Long provinceId, Long districtId, Long wardId);

    // New structure APIs
    List<ProvinceNewResponse> getNewProvinces();
    List<WardNewResponse> getNewWards(Long provinceId);
    NewFullAddressResponse getNewFullAddress(Long provinceId, Long wardId);

    // Conversion APIs
    AddressConversionResponse convertAddress(AddressConversionRequest request);
    BatchConversionResponse convertBatch(BatchConversionRequest request);

    // Merge history APIs
    ProvinceMergeHistoryResponse getProvinceMergeHistory(String code);
    WardMergeHistoryResponse getWardMergeHistory(String code);
}
```

#### 5.2 Implementation: AdministrativeUnitServiceImpl

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AdministrativeUnitServiceImpl implements AdministrativeUnitService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final AddressConversionMappingRepository conversionRepository;
    private final AdministrativeUnitMapper mapper;

    private static final LocalDate REORGANIZATION_DATE = LocalDate.of(2025, 7, 1);

    @Override
    public List<ProvinceOldResponse> getOldProvinces() {
        log.info("Getting provinces from old structure");
        List<AdministrativeStructure> oldStructures =
            Arrays.asList(AdministrativeStructure.OLD, AdministrativeStructure.BOTH);

        return provinceRepository
            .findByStructureVersionInAndIsActiveTrueOrderByName(oldStructures)
            .stream()
            .filter(p -> p.getEffectiveTo() == null ||
                        p.getEffectiveTo().isAfter(REORGANIZATION_DATE.minusDays(1)))
            .map(mapper::toOldResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<ProvinceNewResponse> getNewProvinces() {
        log.info("Getting provinces from new structure");
        List<AdministrativeStructure> newStructures =
            Arrays.asList(AdministrativeStructure.NEW, AdministrativeStructure.BOTH);

        return provinceRepository
            .findByParentProvinceIsNullAndIsActiveTrueOrderByName()
            .stream()
            .filter(p -> newStructures.contains(p.getStructureVersion()))
            .filter(p -> p.getEffectiveFrom() == null ||
                        !p.getEffectiveFrom().isAfter(REORGANIZATION_DATE))
            .map(mapper::toNewResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressConversionResponse convertAddress(AddressConversionRequest request) {
        log.info("Converting address: province={}, district={}, ward={}",
            request.getOldProvinceId(), request.getOldDistrictId(), request.getOldWardId());

        // Look up conversion mapping
        Optional<AddressConversionMapping> mapping = conversionRepository
            .findByOldProvinceProvinceIdAndOldDistrictDistrictIdAndOldWardWardId(
                request.getOldProvinceId(),
                request.getOldDistrictId(),
                request.getOldWardId());

        if (mapping.isEmpty()) {
            throw new ConversionNotFoundException(
                "No conversion mapping found for the specified address");
        }

        AddressConversionMapping conv = mapping.get();

        // Build response
        return AddressConversionResponse.builder()
            .oldAddress(buildOldAddressInfo(conv, request))
            .newAddress(buildNewAddressInfo(conv, request))
            .conversionSuccessful(true)
            .conversionNote(conv.getConversionNote())
            .conversionDate(REORGANIZATION_DATE)
            .build();
    }

    @Override
    public ProvinceMergeHistoryResponse getProvinceMergeHistory(String code) {
        Province province = provinceRepository.findByCodeAndIsActiveTrue(code)
            .orElseThrow(() -> new ProvinceNotFoundException("Province not found: " + code));

        return ProvinceMergeHistoryResponse.builder()
            .provinceId(province.getProvinceId())
            .currentName(province.getName())
            .code(province.getCode())
            .isMerged(province.getIsMerged())
            .mergedDate(province.getMergedDate())
            .originalName(province.getOriginalName())
            .parentProvinceId(province.getParentProvince() != null ?
                province.getParentProvince().getProvinceId() : null)
            .parentProvinceName(province.getParentProvince() != null ?
                province.getParentProvince().getName() : null)
            .mergedProvinces(province.getMergedProvinces().stream()
                .map(mapper::toMergedProvinceInfo)
                .collect(Collectors.toList()))
            .build();
    }

    // ... other methods ...
}
```

---

### Phase 6: Controller Implementation

#### 6.1 Create AdministrativeUnitController

```java
@RestController
@RequestMapping("/api/v1/administrative-units")
@Tag(name = "Administrative Units", description = "Vietnamese administrative unit APIs supporting old and new structures")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdministrativeUnitController {

    AdministrativeUnitService service;

    // ==================== OLD STRUCTURE APIs ====================

    @GetMapping("/provinces")
    @Operation(summary = "Get all provinces (old structure before July 1, 2025)")
    public ApiResponse<List<ProvinceOldResponse>> getOldProvinces() {
        return ApiResponse.<List<ProvinceOldResponse>>builder()
            .data(service.getOldProvinces())
            .build();
    }

    @GetMapping("/provinces/{provinceId}/districts")
    @Operation(summary = "Get districts in a province (old structure)")
    public ApiResponse<List<DistrictOldResponse>> getOldDistricts(
            @PathVariable Long provinceId) {
        return ApiResponse.<List<DistrictOldResponse>>builder()
            .data(service.getOldDistricts(provinceId))
            .build();
    }

    @GetMapping("/districts/{districtId}/wards")
    @Operation(summary = "Get wards in a district (old structure)")
    public ApiResponse<List<WardOldResponse>> getOldWards(
            @PathVariable Long districtId) {
        return ApiResponse.<List<WardOldResponse>>builder()
            .data(service.getOldWards(districtId))
            .build();
    }

    @GetMapping("/full-address")
    @Operation(summary = "Get complete address details (old structure)")
    public ApiResponse<OldFullAddressResponse> getOldFullAddress(
            @RequestParam Long provinceId,
            @RequestParam Long districtId,
            @RequestParam Long wardId) {
        return ApiResponse.<OldFullAddressResponse>>builder()
            .data(service.getOldFullAddress(provinceId, districtId, wardId))
            .build();
    }

    // ==================== NEW STRUCTURE APIs ====================

    @GetMapping("/new-provinces")
    @Operation(summary = "Get all provinces (new structure after July 1, 2025)")
    public ApiResponse<List<ProvinceNewResponse>> getNewProvinces() {
        return ApiResponse.<List<ProvinceNewResponse>>builder()
            .data(service.getNewProvinces())
            .build();
    }

    @GetMapping("/new-provinces/{provinceId}/wards")
    @Operation(summary = "Get wards directly under province (new structure - no districts)")
    public ApiResponse<List<WardNewResponse>> getNewWards(
            @PathVariable Long provinceId) {
        return ApiResponse.<List<WardNewResponse>>builder()
            .data(service.getNewWards(provinceId))
            .build();
    }

    @GetMapping("/new-full-address")
    @Operation(summary = "Get complete address details (new structure)")
    public ApiResponse<NewFullAddressResponse> getNewFullAddress(
            @RequestParam Long provinceId,
            @RequestParam Long wardId) {
        return ApiResponse.<NewFullAddressResponse>>builder()
            .data(service.getNewFullAddress(provinceId, wardId))
            .build();
    }

    // ==================== CONVERSION APIs ====================

    @PostMapping("/convert/address")
    @Operation(summary = "Convert single address from old to new structure")
    public ApiResponse<AddressConversionResponse> convertAddress(
            @RequestBody @Valid AddressConversionRequest request) {
        return ApiResponse.<AddressConversionResponse>>builder()
            .data(service.convertAddress(request))
            .build();
    }

    @PostMapping("/convert/batch")
    @Operation(summary = "Batch convert addresses (max 100 per request)")
    public ApiResponse<BatchConversionResponse> convertBatch(
            @RequestBody @Valid BatchConversionRequest request) {
        return ApiResponse.<BatchConversionResponse>>builder()
            .data(service.convertBatch(request))
            .build();
    }

    // ==================== MERGE HISTORY APIs ====================

    @GetMapping("/merge-history/province/{code}")
    @Operation(summary = "Get province merger history")
    public ApiResponse<ProvinceMergeHistoryResponse> getProvinceMergeHistory(
            @PathVariable String code) {
        return ApiResponse.<ProvinceMergeHistoryResponse>>builder()
            .data(service.getProvinceMergeHistory(code))
            .build();
    }

    @GetMapping("/merge-history/ward/{code}")
    @Operation(summary = "Get ward merger history")
    public ApiResponse<WardMergeHistoryResponse> getWardMergeHistory(
            @PathVariable String code) {
        return ApiResponse.<WardMergeHistoryResponse>>builder()
            .data(service.getWardMergeHistory(code))
            .build();
    }
}
```

---

### Phase 7: Exception Handling

#### 7.1 Create Custom Exceptions

```java
// ConversionNotFoundException.java
@Getter
public class ConversionNotFoundException extends AppException {
    public ConversionNotFoundException(String message) {
        super(DomainCode.ADDRESS_CONVERSION_NOT_FOUND, message);
    }
}

// ProvinceNotFoundException.java
@Getter
public class ProvinceNotFoundException extends AppException {
    public ProvinceNotFoundException(String message) {
        super(DomainCode.PROVINCE_NOT_FOUND, message);
    }
}

// WardNotFoundException.java
@Getter
public class WardNotFoundException extends AppException {
    public WardNotFoundException(String message) {
        super(DomainCode.WARD_NOT_FOUND, message);
    }
}
```

#### 7.2 Add Error Codes to DomainCode

```java
public enum DomainCode {
    // ... existing codes ...

    // Administrative unit errors (7xxx)
    ADDRESS_CONVERSION_NOT_FOUND(7001, "Address conversion mapping not found"),
    INVALID_ADMINISTRATIVE_STRUCTURE(7002, "Invalid administrative structure version"),
    PROVINCE_MERGE_CONFLICT(7003, "Province merger conflict detected"),
    WARD_MERGE_CONFLICT(7004, "Ward merger conflict detected"),
    BATCH_CONVERSION_LIMIT_EXCEEDED(7005, "Batch conversion limit exceeded (max 100)");
}
```

---

### Phase 8: Testing Strategy

#### 8.1 Unit Tests

```java
@SpringBootTest
class AdministrativeUnitServiceTest {

    @Test
    void testGetOldProvinces_ShouldReturnOldStructureProvinces() {
        // Given: Provinces with OLD and BOTH structure versions
        // When: Call getOldProvinces()
        // Then: Should return provinces from old structure
    }

    @Test
    void testConvertAddress_ValidMapping_ShouldReturnConversion() {
        // Given: Valid old address with conversion mapping
        // When: Call convertAddress()
        // Then: Should return converted new address
    }

    @Test
    void testConvertAddress_NoMapping_ShouldThrowException() {
        // Given: Old address without conversion mapping
        // When: Call convertAddress()
        // Then: Should throw ConversionNotFoundException
    }

    @Test
    void testGetProvinceMergeHistory_MergedProvince_ShouldReturnHistory() {
        // Given: Merged province (e.g., Hà Tây → Hà Nội)
        // When: Call getProvinceMergeHistory()
        // Then: Should return parent province info
    }
}
```

#### 8.2 Integration Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AdministrativeUnitControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testOldStructureEndpoints_ShouldWorkCorrectly() throws Exception {
        // Test full flow: provinces → districts → wards
    }

    @Test
    void testNewStructureEndpoints_ShouldSkipDistricts() throws Exception {
        // Test new flow: provinces → wards (no districts)
    }

    @Test
    void testBatchConversion_100Addresses_ShouldSucceed() throws Exception {
        // Test batch conversion with maximum allowed size
    }
}
```

---

### Phase 9: Documentation

#### 9.1 OpenAPI/Swagger Documentation

Add comprehensive Swagger annotations to all endpoints with:
- Request/response examples
- Error response examples
- Parameter descriptions
- Tags and grouping

#### 9.2 API Usage Guide

Create `ADMINISTRATIVE_API_GUIDE.md` with:
- Complete API reference
- Usage examples for each endpoint
- Migration guide from old to new structure
- Best practices for address conversion
- Rate limiting and authentication info

---

## Implementation Timeline

### Week 1: Data Model & Migration
- [ ] Update Ward entity with merger support
- [ ] Add AdministrativeStructure enum
- [ ] Create AddressConversionMapping entity
- [ ] Write and test database migration V13

### Week 2: DTOs & Mappers
- [ ] Create all old structure DTOs
- [ ] Create all new structure DTOs
- [ ] Create conversion and merge history DTOs
- [ ] Implement MapStruct mappers

### Week 3: Repository & Service Layer
- [ ] Update repository interfaces
- [ ] Implement AdministrativeUnitService
- [ ] Add address conversion logic
- [ ] Implement merge history queries

### Week 4: Controller & Testing
- [ ] Create AdministrativeUnitController
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update Swagger documentation

### Week 5: Data Population & Validation
- [ ] Populate conversion mapping table
- [ ] Import real Vietnam administrative data
- [ ] Validate all endpoints with real data
- [ ] Performance testing

---

## Backward Compatibility Strategy

### Maintain Existing Endpoints
- Keep current `/v1/addresses/*` endpoints unchanged
- Add new `/api/v1/administrative-units/*` endpoints
- Document migration path for API consumers

### Data Migration
- Existing addresses remain valid
- Add structure_version to existing records (default: BOTH)
- Create conversion mappings for all existing addresses

### Graceful Deprecation
- Mark old endpoints as deprecated in Swagger
- Provide 6-month sunset timeline
- Send deprecation warnings in API responses

---

## Success Criteria

### Functional Requirements ✅
- [ ] All endpoints from tinhthanhpho.com API replicated
- [ ] Old and new administrative structures fully supported
- [ ] Address conversion working bi-directionally
- [ ] Merge history tracking complete
- [ ] Batch conversion supporting 100 addresses

### Non-Functional Requirements ✅
- [ ] All endpoints respond within 200ms (p95)
- [ ] 100% test coverage for service layer
- [ ] API documentation complete with examples
- [ ] Zero breaking changes to existing APIs

### Data Requirements ✅
- [ ] All 63 Vietnam provinces loaded
- [ ] All districts (old structure) loaded
- [ ] All wards (old + new structure) loaded
- [ ] Complete conversion mapping table

---

## Risk Mitigation

### Technical Risks
1. **Data Inconsistency**:
   - Mitigation: Comprehensive data validation scripts
   - Regular audits of conversion mappings

2. **Performance Degradation**:
   - Mitigation: Database indexing strategy
   - Caching layer for frequently accessed data

3. **Breaking Changes**:
   - Mitigation: Strict API versioning
   - Comprehensive integration tests

### Business Risks
1. **Incomplete Conversion Data**:
   - Mitigation: Manual review process for edge cases
   - Fallback to approximate conversions with warnings

2. **User Confusion**:
   - Mitigation: Clear documentation
   - API response includes metadata about structure version

---

## Next Steps

1. **Approve refactoring plan**
2. **Begin Phase 1: Data Model Enhancement**
3. **Create database migration**
4. **Implement core service logic**
5. **Expose REST APIs**

---

**Document Version:** 1.0
**Created:** 2025-10-07
**Status:** Pending Approval
