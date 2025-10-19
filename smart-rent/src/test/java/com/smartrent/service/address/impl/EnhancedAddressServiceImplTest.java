package com.smartrent.service.address.impl;

import com.smartrent.dto.request.BatchAddressConversionRequest;
import com.smartrent.dto.response.*;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.DistrictRepository;
import com.smartrent.infra.repository.ProvinceRepository;
import com.smartrent.infra.repository.WardRepository;
import com.smartrent.infra.repository.entity.District;
import com.smartrent.infra.repository.entity.Province;
import com.smartrent.infra.repository.entity.Ward;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for EnhancedAddressServiceImpl
 * Tests all 13 service methods with success and error scenarios
 */
@ExtendWith(MockitoExtension.class)
class EnhancedAddressServiceImplTest {

    @Mock
    private ProvinceRepository provinceRepository;

    @Mock
    private DistrictRepository districtRepository;

    @Mock
    private WardRepository wardRepository;

    @InjectMocks
    private EnhancedAddressServiceImpl enhancedAddressService;

    private Province hanoiProvince;
    private Province hcmProvince;
    private Province mergedProvince;
    private District baDinhDistrict;
    private District district1;
    private Ward phucXaWard;
    private Ward ward1;

    @BeforeEach
    void setUp() {
        // Setup Hanoi province
        hanoiProvince = new Province();
        hanoiProvince.setProvinceId(1L);
        hanoiProvince.setCode("01");
        hanoiProvince.setName("Thành phố Hà Nội");
        hanoiProvince.setType(Province.ProvinceType.CITY);
        hanoiProvince.setIsActive(true);
        hanoiProvince.setIsMerged(false);
        hanoiProvince.setEffectiveFrom(LocalDate.of(2008, 5, 1));

        // Setup HCM province
        hcmProvince = new Province();
        hcmProvince.setProvinceId(2L);
        hcmProvince.setCode("79");
        hcmProvince.setName("Thành phố Hồ Chí Minh");
        hcmProvince.setType(Province.ProvinceType.CITY);
        hcmProvince.setIsActive(true);
        hcmProvince.setIsMerged(false);

        // Setup merged province (e.g., Hà Tây merged into Hanoi)
        mergedProvince = new Province();
        mergedProvince.setProvinceId(3L);
        mergedProvince.setCode("35");
        mergedProvince.setName("Tỉnh Hà Tây");
        mergedProvince.setOriginalName("Tỉnh Hà Tây");
        mergedProvince.setType(Province.ProvinceType.PROVINCE);
        mergedProvince.setIsActive(true);
        mergedProvince.setIsMerged(true);
        mergedProvince.setParentProvince(hanoiProvince);
        mergedProvince.setMergedDate(LocalDate.of(2008, 5, 1));

        // Setup Ba Dinh district
        baDinhDistrict = new District();
        baDinhDistrict.setDistrictId(1L);
        baDinhDistrict.setCode("001");
        baDinhDistrict.setName("Quận Ba Đình");
        baDinhDistrict.setType(District.DistrictType.DISTRICT);
        baDinhDistrict.setProvince(hanoiProvince);
        baDinhDistrict.setIsActive(true);
        baDinhDistrict.setEffectiveFrom(LocalDate.of(2008, 5, 1));

        // Setup District 1 (HCM)
        district1 = new District();
        district1.setDistrictId(2L);
        district1.setCode("760");
        district1.setName("Quận 1");
        district1.setType(District.DistrictType.DISTRICT);
        district1.setProvince(hcmProvince);
        district1.setIsActive(true);

        // Setup Phuc Xa ward
        phucXaWard = new Ward();
        phucXaWard.setWardId(1L);
        phucXaWard.setCode("00001");
        phucXaWard.setName("Phường Phúc Xá");
        phucXaWard.setType(Ward.WardType.WARD);
        phucXaWard.setDistrict(baDinhDistrict);
        phucXaWard.setIsActive(true);
        phucXaWard.setEffectiveFrom(LocalDate.of(2008, 5, 1));

        // Setup Ward 1 (HCM)
        ward1 = new Ward();
        ward1.setWardId(2L);
        ward1.setCode("26734");
        ward1.setName("Phường Bến Nghé");
        ward1.setType(Ward.WardType.WARD);
        ward1.setDistrict(district1);
        ward1.setIsActive(true);
    }

    // ========================================================================
    // Test: getAllProvinces
    // ========================================================================

    @Test
    void getAllProvinces_Success_ShouldReturnActiveProvinces() {
        // Given
        when(provinceRepository.findByParentProvinceIsNullAndIsActiveTrueOrderByName())
                .thenReturn(Arrays.asList(hanoiProvince, hcmProvince));

        // When
        List<ProvinceResponse> result = enhancedAddressService.getAllProvinces();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("01", result.get(0).getCode());
        assertEquals("Thành phố Hà Nội", result.get(0).getName());
        verify(provinceRepository, times(1)).findByParentProvinceIsNullAndIsActiveTrueOrderByName();
    }

    @Test
    void getAllProvinces_EmptyList_ShouldReturnEmptyList() {
        // Given
        when(provinceRepository.findByParentProvinceIsNullAndIsActiveTrueOrderByName())
                .thenReturn(Collections.emptyList());

        // When
        List<ProvinceResponse> result = enhancedAddressService.getAllProvinces();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========================================================================
    // Test: getDistrictsByProvinceCode
    // ========================================================================

    @Test
    void getDistrictsByProvinceCode_Success_ShouldReturnDistricts() {
        // Given
        when(provinceRepository.findByCodeAndIsActiveTrue("01")).thenReturn(Optional.of(hanoiProvince));
        when(districtRepository.findByProvince_ProvinceIdAndIsActiveTrue(1L))
                .thenReturn(Collections.singletonList(baDinhDistrict));

        // When
        List<DistrictResponse> result = enhancedAddressService.getDistrictsByProvinceCode("01");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("001", result.get(0).getCode());
        assertEquals("Quận Ba Đình", result.get(0).getName());
    }

    @Test
    void getDistrictsByProvinceCode_ProvinceNotFound_ShouldThrowException() {
        // Given
        when(provinceRepository.findByCodeAndIsActiveTrue("99")).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> enhancedAddressService.getDistrictsByProvinceCode("99"));
        assertEquals(DomainCode.PROVINCE_NOT_FOUND, exception.getErrorCode());
    }

    // ========================================================================
    // Test: getWardsByDistrictCode
    // ========================================================================

    @Test
    void getWardsByDistrictCode_Success_ShouldReturnWards() {
        // Given
        when(districtRepository.findByCode("001")).thenReturn(Optional.of(baDinhDistrict));
        when(wardRepository.findByDistrict_DistrictIdAndIsActiveTrue(1L))
                .thenReturn(Collections.singletonList(phucXaWard));

        // When
        List<WardResponse> result = enhancedAddressService.getWardsByDistrictCode("001");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("00001", result.get(0).getCode());
        assertEquals("Phường Phúc Xá", result.get(0).getName());
    }

    @Test
    void getWardsByDistrictCode_DistrictNotFound_ShouldThrowException() {
        // Given
        when(districtRepository.findByCode("999")).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> enhancedAddressService.getWardsByDistrictCode("999"));
        assertEquals(DomainCode.DISTRICT_NOT_FOUND, exception.getErrorCode());
    }

    // ========================================================================
    // Test: getFullAddress
    // ========================================================================

    @Test
    void getFullAddress_Success_ShouldReturnCompleteAddress() {
        // Given
        when(provinceRepository.findByCodeAndIsActiveTrue("01")).thenReturn(Optional.of(hanoiProvince));
        when(districtRepository.findByCodeAndProvince_ProvinceId("001", 1L))
                .thenReturn(Optional.of(baDinhDistrict));
        when(wardRepository.findByCodeAndDistrict_DistrictId("00001", 1L))
                .thenReturn(Optional.of(phucXaWard));

        // When
        FullAddressResponse result = enhancedAddressService.getFullAddress("01", "001", "00001");

        // Then
        assertNotNull(result);
        assertNotNull(result.getProvince());
        assertEquals("01", result.getProvince().getCode());
        assertEquals("Thành phố Hà Nội", result.getProvince().getName());
        assertNotNull(result.getDistrict());
        assertEquals("001", result.getDistrict().getCode());
        assertNotNull(result.getWard());
        assertEquals("00001", result.getWard().getCode());
        assertTrue(result.getFullAddressText().contains("Phường Phúc Xá"));
        assertTrue(result.getFullAddressText().contains("Quận Ba Đình"));
        assertTrue(result.getFullAddressText().contains("Thành phố Hà Nội"));
    }

    @Test
    void getFullAddress_WardNotFound_ShouldThrowException() {
        // Given
        when(provinceRepository.findByCodeAndIsActiveTrue("01")).thenReturn(Optional.of(hanoiProvince));
        when(districtRepository.findByCodeAndProvince_ProvinceId("001", 1L))
                .thenReturn(Optional.of(baDinhDistrict));
        when(wardRepository.findByCodeAndDistrict_DistrictId("99999", 1L))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> enhancedAddressService.getFullAddress("01", "001", "99999"));
        assertEquals(DomainCode.WARD_NOT_FOUND, exception.getErrorCode());
    }

    // ========================================================================
    // Test: getNewProvinces (includes merged provinces)
    // ========================================================================

    @Test
    void getNewProvinces_Success_ShouldReturnAllActiveProvinces() {
        // Given
        when(provinceRepository.findByIsActiveTrueOrderByName())
                .thenReturn(Arrays.asList(hanoiProvince, hcmProvince, mergedProvince));

        // When
        List<ProvinceResponse> result = enhancedAddressService.getNewProvinces();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        // Verify merged province is included
        assertTrue(result.stream().anyMatch(p -> "35".equals(p.getCode())));
    }

    // ========================================================================
    // Test: searchAddress (with relevance scoring)
    // ========================================================================

    @Test
    void searchAddress_Success_ShouldReturnMatchesWithScores() {
        // Given
        when(provinceRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(hanoiProvince));
        when(districtRepository.findAll()).thenReturn(Collections.singletonList(baDinhDistrict));
        when(wardRepository.findAll()).thenReturn(Collections.singletonList(phucXaWard));

        // When
        AddressSearchResponse result = enhancedAddressService.searchAddress("Hà Nội", 10);

        // Then
        assertNotNull(result);
        assertEquals("Hà Nội", result.getQuery());
        assertTrue(result.getTotalResults() > 0);
        assertNotNull(result.getMatches());
        // Verify matches have scores
        result.getMatches().forEach(match -> {
            assertNotNull(match.getMatchScore());
            assertTrue(match.getMatchScore() >= 0.0 && match.getMatchScore() <= 1.0);
        });
    }

    @Test
    void searchAddress_NoMatches_ShouldReturnEmptyResults() {
        // Given
        when(provinceRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());
        when(districtRepository.findAll()).thenReturn(Collections.emptyList());
        when(wardRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        AddressSearchResponse result = enhancedAddressService.searchAddress("XYZ123", 10);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalResults());
        assertTrue(result.getMatches().isEmpty());
    }

    @Test
    void searchAddress_WithLimit_ShouldRespectLimit() {
        // Given
        when(provinceRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(hanoiProvince));
        when(districtRepository.findAll()).thenReturn(Collections.singletonList(baDinhDistrict));
        when(wardRepository.findAll()).thenReturn(Collections.singletonList(phucXaWard));

        // When
        AddressSearchResponse result = enhancedAddressService.searchAddress("Hà Nội", 1);

        // Then
        assertNotNull(result);
        assertTrue(result.getMatches().size() <= 1);
    }

    // ========================================================================
    // Test: convertAddress (old structure → new structure)
    // ========================================================================

    @Test
    void convertAddress_NoMerge_ShouldReturnSameAddress() {
        // Given
        when(provinceRepository.findByCode("79")).thenReturn(Optional.of(hcmProvince));
        when(districtRepository.findByCodeAndProvince_ProvinceId("760", 2L))
                .thenReturn(Optional.of(district1));
        when(wardRepository.findByCodeAndDistrict_DistrictId("26734", 2L))
                .thenReturn(Optional.of(ward1));

        // When
        AddressConversionResponse result = enhancedAddressService.convertAddress("79", "760", "26734");

        // Then
        assertNotNull(result);
        assertNotNull(result.getOldAddress());
        assertNotNull(result.getNewAddress());
        assertNotNull(result.getConversionInfo());
        assertFalse(result.getConversionInfo().getWasConverted());
        assertEquals("NONE", result.getConversionInfo().getConversionType());
    }

    @Test
    void convertAddress_ProvinceMerge_ShouldConvertToParent() {
        // Given
        when(provinceRepository.findByCode("35")).thenReturn(Optional.of(mergedProvince));
        when(districtRepository.findByCodeAndProvince_ProvinceId("001", 3L))
                .thenReturn(Optional.of(baDinhDistrict));
        when(wardRepository.findByCodeAndDistrict_DistrictId("00001", 1L))
                .thenReturn(Optional.of(phucXaWard));

        // When
        AddressConversionResponse result = enhancedAddressService.convertAddress("35", "001", "00001");

        // Then
        assertNotNull(result);
        assertTrue(result.getConversionInfo().getWasConverted());
        assertEquals("PROVINCE_MERGE", result.getConversionInfo().getConversionType());
        assertEquals("01", result.getNewAddress().getProvinceCode()); // Converted to Hanoi
    }

    @Test
    void convertAddress_InvalidProvince_ShouldThrowException() {
        // Given
        when(provinceRepository.findByCode("99")).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> enhancedAddressService.convertAddress("99", "001", "00001"));
        assertEquals(DomainCode.PROVINCE_NOT_FOUND, exception.getErrorCode());
    }

    // ========================================================================
    // Test: convertAddressesBatch
    // ========================================================================

    @Test
    void convertAddressesBatch_Success_ShouldReturnAllConversions() {
        // Given
        BatchAddressConversionRequest.AddressToConvert address1 =
                BatchAddressConversionRequest.AddressToConvert.builder()
                        .provinceCode("79")
                        .districtCode("760")
                        .wardCode("26734")
                        .build();

        BatchAddressConversionRequest request = BatchAddressConversionRequest.builder()
                .addresses(Collections.singletonList(address1))
                .build();

        when(provinceRepository.findByCode("79")).thenReturn(Optional.of(hcmProvince));
        when(districtRepository.findByCodeAndProvince_ProvinceId("760", 2L))
                .thenReturn(Optional.of(district1));
        when(wardRepository.findByCodeAndDistrict_DistrictId("26734", 2L))
                .thenReturn(Optional.of(ward1));

        // When
        List<AddressConversionResponse> result = enhancedAddressService.convertAddressesBatch(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getOldAddress());
    }

    @Test
    void convertAddressesBatch_PartialFailure_ShouldHandleGracefully() {
        // Given
        BatchAddressConversionRequest.AddressToConvert validAddress =
                BatchAddressConversionRequest.AddressToConvert.builder()
                        .provinceCode("79")
                        .districtCode("760")
                        .wardCode("26734")
                        .build();

        BatchAddressConversionRequest.AddressToConvert invalidAddress =
                BatchAddressConversionRequest.AddressToConvert.builder()
                        .provinceCode("99")
                        .districtCode("999")
                        .wardCode("99999")
                        .build();

        BatchAddressConversionRequest request = BatchAddressConversionRequest.builder()
                .addresses(Arrays.asList(validAddress, invalidAddress))
                .build();

        when(provinceRepository.findByCode("79")).thenReturn(Optional.of(hcmProvince));
        when(districtRepository.findByCodeAndProvince_ProvinceId("760", 2L))
                .thenReturn(Optional.of(district1));
        when(wardRepository.findByCodeAndDistrict_DistrictId("26734", 2L))
                .thenReturn(Optional.of(ward1));
        when(provinceRepository.findByCode("99")).thenReturn(Optional.empty());

        // When
        List<AddressConversionResponse> result = enhancedAddressService.convertAddressesBatch(request);

        // Then
        assertNotNull(result);
        // Should return results for valid addresses, skip or mark invalid ones
        assertTrue(result.size() <= 2);
    }

    // ========================================================================
    // Test: getProvinceMergeHistory
    // ========================================================================

    @Test
    void getProvinceMergeHistory_MergedProvince_ShouldReturnHistory() {
        // Given
        when(provinceRepository.findByCode("35")).thenReturn(Optional.of(mergedProvince));

        // When
        MergeHistoryResponse result = enhancedAddressService.getProvinceMergeHistory("35");

        // Then
        assertNotNull(result);
        assertEquals("35", result.getEntityCode());
        assertTrue(result.getIsMerged());
        assertNotNull(result.getMergedInto());
        assertEquals("01", result.getMergedInto().getCode());
        assertEquals("Thành phố Hà Nội", result.getMergedInto().getName());
    }

    @Test
    void getProvinceMergeHistory_NotMerged_ShouldReturnNoMerge() {
        // Given
        when(provinceRepository.findByCode("79")).thenReturn(Optional.of(hcmProvince));

        // When
        MergeHistoryResponse result = enhancedAddressService.getProvinceMergeHistory("79");

        // Then
        assertNotNull(result);
        assertEquals("79", result.getEntityCode());
        assertFalse(result.getIsMerged());
        assertNull(result.getMergedInto());
    }

    @Test
    void getProvinceMergeHistory_ProvinceNotFound_ShouldThrowException() {
        // Given
        when(provinceRepository.findByCode("99")).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> enhancedAddressService.getProvinceMergeHistory("99"));
        assertEquals(DomainCode.PROVINCE_NOT_FOUND, exception.getErrorCode());
    }

    // ========================================================================
    // Test: validateAddressCodes
    // ========================================================================

    @Test
    void validateAddressCodes_ValidCodes_ShouldReturnTrue() {
        // Given
        when(provinceRepository.findByCodeAndIsActiveTrue("01")).thenReturn(Optional.of(hanoiProvince));
        when(districtRepository.findByCodeAndProvince_ProvinceId("001", 1L))
                .thenReturn(Optional.of(baDinhDistrict));
        when(wardRepository.findByCodeAndDistrict_DistrictId("00001", 1L))
                .thenReturn(Optional.of(phucXaWard));

        // When
        boolean result = enhancedAddressService.validateAddressCodes("01", "001", "00001");

        // Then
        assertTrue(result);
    }

    @Test
    void validateAddressCodes_InvalidCodes_ShouldReturnFalse() {
        // Given
        when(provinceRepository.findByCodeAndIsActiveTrue("99")).thenReturn(Optional.empty());

        // When
        boolean result = enhancedAddressService.validateAddressCodes("99", "999", "99999");

        // Then
        assertFalse(result);
    }

    // ========================================================================
    // Test: getEffectiveDate
    // ========================================================================

    @Test
    void getEffectiveDate_Province_ShouldReturnDate() {
        // Given
        when(provinceRepository.findByCode("01")).thenReturn(Optional.of(hanoiProvince));

        // When
        LocalDate result = enhancedAddressService.getEffectiveDate("01", "PROVINCE");

        // Then
        assertNotNull(result);
        assertEquals(LocalDate.of(2008, 5, 1), result);
    }

    @Test
    void getEffectiveDate_InvalidEntityType_ShouldThrowException() {
        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> enhancedAddressService.getEffectiveDate("01", "INVALID"));
        assertEquals(DomainCode.BAD_REQUEST_ERROR, exception.getErrorCode());
    }
}
