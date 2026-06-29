package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.AddressMappingRepository;
import com.smartrent.infra.repository.LegacyDistrictRepository;
import com.smartrent.infra.repository.LegacyProvinceRepository;
import com.smartrent.infra.repository.entity.District;
import com.smartrent.service.listing.ListingQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Regression for the chatbot "tìm trọ quận 1 → 0 results" bug.
 *
 * <p>The AI tool knows districts by their GSO administrative code (760 = Quận 1),
 * but {@link com.smartrent.infra.repository.specification.ListingSpecification}
 * matches {@code addresses.legacy_district_id} against the legacy_districts
 * surrogate PK (Quận 1 = 541). Sending the code 760 as districtId matched no
 * address, so every Quận 1 listing was filtered out. The service must resolve a
 * districtCode to the surrogate PK before building the specification.
 */
@ExtendWith(MockitoExtension.class)
class ListingServiceImplDistrictCodeTest {

    @Mock
    LegacyDistrictRepository legacyDistrictRepository;
    @Mock
    LegacyProvinceRepository legacyProvinceRepository;
    @Mock
    AddressMappingRepository addressMappingRepository;
    @Mock
    ListingQueryService listingQueryService;

    @InjectMocks
    ListingServiceImpl service;

    @Test
    void resolvesDistrictCodeToLegacyDistrictSurrogatePk() {
        // Quận 1: GSO district_code "760" → legacy_districts PK 541 (see V37 seed / DB).
        when(legacyDistrictRepository.findByCode("760"))
                .thenReturn(Optional.of(District.builder().id(541).code("760").build()));
        lenient().when(addressMappingRepository.findNewWardCodesByLegacyDistrictId(541))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<ListingFilterRequest> captor =
                ArgumentCaptor.forClass(ListingFilterRequest.class);
        when(listingQueryService.executeQuery(captor.capture())).thenReturn(Page.empty());

        service.searchListings(ListingFilterRequest.builder().districtCode("760").build());

        // The specification only understands the surrogate PK, so the resolved
        // districtId — not the raw GSO code — is what reaches the query.
        assertEquals(Integer.valueOf(541), captor.getValue().getDistrictId());
    }

    @Test
    void throwsBadRequestOnUnknownDistrictCode() {
        // An unknown code must fail loudly (400) instead of silently dropping the
        // district filter and returning over-broad/zero results that hide the
        // AI's mistake.
        when(legacyDistrictRepository.findByCode("999")).thenReturn(Optional.empty());
        lenient().when(listingQueryService.executeQuery(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Page.empty());

        DomainException ex = assertThrows(DomainException.class, () ->
                service.searchListings(ListingFilterRequest.builder().districtCode("999").build()));
        assertEquals(DomainCode.INVALID_DISTRICT_CODE, ex.getDomainCode());
    }

    @Test
    void throwsBadRequestOnUnknownProvinceCode() {
        // A province code that resolves to no legacy id AND no direct province is
        // genuinely unknown → 400, not a silent 0-result search.
        when(addressMappingRepository.findNewCodeToLegacyIdPairs(anyList()))
                .thenReturn(Collections.emptyList());
        when(legacyProvinceRepository.findByCodeIn(anyList()))
                .thenReturn(Collections.emptyList());
        lenient().when(listingQueryService.executeQuery(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Page.empty());

        DomainException ex = assertThrows(DomainException.class, () ->
                service.searchListings(ListingFilterRequest.builder().provinceCode("ZZ").build()));
        assertEquals(DomainCode.INVALID_PROVINCE_CODE, ex.getDomainCode());
    }
}
