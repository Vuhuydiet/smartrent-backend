package com.smartrent.service.address.impl;

import com.smartrent.dto.request.AddressCreationRequest;
import com.smartrent.dto.response.*;
import com.smartrent.infra.exception.ResourceNotFoundException;
import com.smartrent.infra.repository.*;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.service.address.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified implementation of AddressService supporting both legacy and new address structures
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    private final ProvinceRepository newProvinceRepository;
    private final WardRepository newWardRepository;
    private final LegacyProvinceRepository legacyProvinceRepository;
    private final LegacyDistrictRepository legacyDistrictRepository;
    private final LegacyWardRepository legacyWardRepository;
    private final ProvinceMappingRepository provinceMappingRepository;
    private final WardMappingRepository wardMappingRepository;

    // =====================================================================
    // New structure methods (not from interface - public API)
    // =====================================================================

    public Page<NewProvinceResponse> getAllNewProvinces(Pageable pageable) {
        return newProvinceRepository.findAll(pageable)
                .map(this::toNewProvinceResponse);
    }

    public Page<NewProvinceResponse> searchNewProvinces(String keyword, Pageable pageable, String code) {
        // Search provinces by keyword (code parameter is unused - provinces don't have parent codes)
        return newProvinceRepository.searchByKeyword(keyword, pageable)
                .map(this::toNewProvinceResponse);
    }

    public NewProvinceResponse getNewProvinceByCode(String code) {
        Province province = newProvinceRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with code: " + code));
        return toNewProvinceResponse(province);
    }

    public Page<NewWardResponse> getNewWardsByProvince(String provinceCode, Pageable pageable) {
        return newWardRepository.findByProvinceCode(provinceCode, pageable)
                .map(this::toNewWardResponse);
    }

    public Page<NewWardResponse> searchNewWardsByProvince(String provinceCode, String keyword, Pageable pageable) {
        return newWardRepository.searchByProvinceAndKeyword(provinceCode, keyword, pageable)
                .map(this::toNewWardResponse);
    }

    public NewWardResponse getNewWardByCode(String code) {
        Ward ward = newWardRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + code));
        return toNewWardResponse(ward);
    }

    public NewFullAddressResponse getNewFullAddress(String provinceCode, String wardCode) {
        Province province = newProvinceRepository.findByCode(provinceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with code: " + provinceCode));
        Ward ward = newWardRepository.findByCode(wardCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + wardCode));

        return NewFullAddressResponse.builder()
                .province(toNewProvinceResponse(province))
                .ward(toNewWardResponse(ward))
                .build();
    }

    // =====================================================================
    // Legacy structure methods (not from interface - public API)
    // =====================================================================

    public Page<LegacyProvinceResponse> getAllLegacyProvinces(Pageable pageable) {
        return legacyProvinceRepository.findAll(pageable)
                .map(this::toLegacyProvinceResponse);
    }

    public Page<LegacyProvinceResponse> searchLegacyProvinces(String keyword, Pageable pageable) {
        return legacyProvinceRepository.searchByKeyword(keyword, pageable)
                .map(this::toLegacyProvinceResponse);
    }

    public LegacyProvinceResponse getLegacyProvinceById(Integer id) {
        LegacyProvince province = legacyProvinceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy province not found with id: " + id));
        return toLegacyProvinceResponse(province);
    }

    public Page<LegacyDistrictResponse> getLegacyDistrictsByProvince(Integer provinceId, Pageable pageable) {
        return legacyDistrictRepository.findByProvinceId(provinceId, pageable)
                .map(this::toLegacyDistrictResponse);
    }

    public Page<LegacyDistrictResponse> searchLegacyDistrictsByProvince(Integer provinceId, String keyword, Pageable pageable) {
        return legacyDistrictRepository.searchByProvinceAndKeyword(provinceId, keyword, pageable)
                .map(this::toLegacyDistrictResponse);
    }

    public LegacyDistrictResponse getLegacyDistrictById(Integer id) {
        District district = legacyDistrictRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy district not found with id: " + id));
        return toLegacyDistrictResponse(district);
    }

    public Page<LegacyWardResponse> getLegacyWardsByDistrict(Integer districtId, Pageable pageable) {
        return legacyWardRepository.findByDistrictId(districtId, pageable)
                .map(this::toLegacyWardResponse);
    }

    public Page<LegacyWardResponse> searchLegacyWardsByDistrict(Integer districtId, String keyword, Pageable pageable) {
        return legacyWardRepository.searchByDistrictAndKeyword(districtId, keyword, pageable)
                .map(this::toLegacyWardResponse);
    }

    public LegacyWardResponse getLegacyWardById(Integer id) {
        LegacyWard ward = legacyWardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy ward not found with id: " + id));
        return toLegacyWardResponse(ward);
    }

    public FullAddressResponse getLegacyFullAddress(Integer provinceId, Integer districtId, Integer wardId) {
        LegacyProvince province = legacyProvinceRepository.findById(provinceId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy province not found with id: " + provinceId));
        District district = legacyDistrictRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy district not found with id: " + districtId));
        LegacyWard ward = legacyWardRepository.findById(wardId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy ward not found with id: " + wardId));

        return FullAddressResponse.builder()
                .province(toLegacyProvinceResponse(province))
                .district(toLegacyDistrictResponse(district))
                .ward(toLegacyWardResponse(ward))
                .build();
    }

    // =====================================================================
    // Address conversion methods
    // =====================================================================

    @Override
    public AddressConversionResponse convertLegacyToNew(Integer legacyProvinceId, Integer legacyDistrictId, Integer legacyWardId) {
        // Get legacy address
        FullAddressResponse legacyAddress = getLegacyFullAddress(legacyProvinceId, legacyDistrictId, legacyWardId);

        // Find province mapping
        ProvinceMapping provinceMapping = provinceMappingRepository.findByLegacyProvinceId(legacyProvinceId)
                .orElseThrow(() -> new ResourceNotFoundException("Province mapping not found for legacy province id: " + legacyProvinceId));

        // Find ward mapping
        WardMapping wardMapping = wardMappingRepository.findByLegacyWardId(legacyWardId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ward mapping not found for legacy ward id: " + legacyWardId));

        // Get new address
        Province newProvince = provinceMapping.getNewProvince();
        Ward newWard = wardMapping.getNewWard();

        NewFullAddressResponse newAddress = NewFullAddressResponse.builder()
                .province(toNewProvinceResponse(newProvince))
                .ward(toNewWardResponse(newWard))
                .build();

        String conversionNote = String.format("Converted from legacy structure. Merge type: %s",
                wardMapping.getMergeType());

        return AddressConversionResponse.builder()
                .legacyAddress(legacyAddress)
                .newAddress(newAddress)
                .conversionNote(conversionNote)
                .build();
    }

    @Override
    public AddressConversionResponse convertNewToLegacy(String newProvinceCode, String newWardCode) {
        // Get new address
        Province newProvince = newProvinceRepository.findByCode(newProvinceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with code: " + newProvinceCode));
        Ward newWard = newWardRepository.findByCode(newWardCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + newWardCode));

        NewFullAddressResponse newAddress = NewFullAddressResponse.builder()
                .province(toNewProvinceResponse(newProvince))
                .ward(toNewWardResponse(newWard))
                .build();

        // Find ward mapping (one new ward may map to multiple legacy wards)
        List<WardMapping> wardMappings = wardMappingRepository.findByNewWardCode(newWardCode);
        if (wardMappings.isEmpty()) {
            throw new ResourceNotFoundException("No legacy ward mapping found for new ward code: " + newWardCode);
        }

        // Get the first mapping (or you could return all possible mappings)
        WardMapping wardMapping = wardMappings.get(0);
        LegacyWard legacyWard = wardMapping.getLegacyWard();
        District legacyDistrict = legacyWard.getDistrict();
        LegacyProvince legacyProvince = legacyWard.getProvince();

        FullAddressResponse legacyAddress = FullAddressResponse.builder()
                .province(toLegacyProvinceResponse(legacyProvince))
                .district(toLegacyDistrictResponse(legacyDistrict))
                .ward(toLegacyWardResponse(legacyWard))
                .build();

        String conversionNote = wardMappings.size() > 1
                ? String.format("Converted to legacy structure. Note: This new ward maps to %d legacy wards. Showing first result. Merge type: %s",
                        wardMappings.size(), wardMapping.getMergeType())
                : String.format("Converted to legacy structure. Merge type: %s", wardMapping.getMergeType());

        return AddressConversionResponse.builder()
                .legacyAddress(legacyAddress)
                .newAddress(newAddress)
                .conversionNote(conversionNote)
                .build();
    }

    // Mapping methods
    private NewProvinceResponse toNewProvinceResponse(Province province) {
        return NewProvinceResponse.builder()
                .code(province.getCode())
                .name(province.getName())
                .nameEn(province.getNameEn())
                .fullName(province.getFullName())
                .fullNameEn(province.getFullNameEn())
                .codeName(province.getCodeName())
                .administrativeUnitType(province.getAdministrativeUnit() != null ?
                        province.getAdministrativeUnit().getFullName() : null)
                .build();
    }

    private NewWardResponse toNewWardResponse(Ward ward) {
        return NewWardResponse.builder()
                .code(ward.getCode())
                .name(ward.getName())
                .nameEn(ward.getNameEn())
                .fullName(ward.getFullName())
                .fullNameEn(ward.getFullNameEn())
                .codeName(ward.getCodeName())
                .provinceCode(ward.getProvince() != null ? ward.getProvince().getCode() : null)
                .provinceName(ward.getProvince() != null ? ward.getProvince().getName() : null)
                .administrativeUnitType(ward.getAdministrativeUnit() != null ?
                        ward.getAdministrativeUnit().getFullName() : null)
                .build();
    }

    private LegacyProvinceResponse toLegacyProvinceResponse(LegacyProvince province) {
        return LegacyProvinceResponse.builder()
                .id(province.getId())
                .name(province.getName())
                .nameEn(province.getNameEn())
                .code(province.getCode())
                .build();
    }

    private LegacyDistrictResponse toLegacyDistrictResponse(District district) {
        return LegacyDistrictResponse.builder()
                .id(district.getId())
                .name(district.getName())
                .nameEn(district.getNameEn())
                .prefix(district.getPrefix())
                .provinceId(district.getProvince() != null ? district.getProvince().getId() : null)
                .provinceName(district.getProvince() != null ? district.getProvince().getName() : null)
                .build();
    }

    private LegacyWardResponse toLegacyWardResponse(LegacyWard ward) {
        return LegacyWardResponse.builder()
                .id(ward.getId())
                .name(ward.getName())
                .nameEn(ward.getNameEn())
                .prefix(ward.getPrefix())
                .provinceId(ward.getProvince() != null ? ward.getProvince().getId() : null)
                .provinceName(ward.getProvince() != null ? ward.getProvince().getName() : null)
                .districtId(ward.getDistrict() != null ? ward.getDistrict().getId() : null)
                .districtName(ward.getDistrict() != null ? ward.getDistrict().getName() : null)
                .build();
    }

    // =====================================================================
    // Old AddressService interface methods (for backward compatibility)
    // =====================================================================

    @Override
    public List<LegacyProvinceResponse> getAllProvinces() {
        return legacyProvinceRepository.findAll().stream()
                .map(this::toLegacyProvinceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegacyProvinceResponse> getParentProvinces() {
        return getAllProvinces(); // All provinces are parent provinces in legacy structure
    }

    @Override
    public LegacyProvinceResponse getProvinceById(Long provinceId) {
        LegacyProvince province = legacyProvinceRepository.findById(provinceId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + provinceId));
        return toLegacyProvinceResponse(province);
    }

    @Override
    public List<LegacyProvinceResponse> searchProvinces(String searchTerm) {
        return legacyProvinceRepository.findByNameContainingIgnoreCase(searchTerm).stream()
                .map(this::toLegacyProvinceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegacyDistrictResponse> getDistrictsByProvinceId(Long provinceId) {
        return legacyDistrictRepository.findByProvinceId(provinceId.intValue()).stream()
                .map(this::toLegacyDistrictResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LegacyDistrictResponse getDistrictById(Long districtId) {
        District district = legacyDistrictRepository.findById(districtId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("District not found with id: " + districtId));
        return toLegacyDistrictResponse(district);
    }

    @Override
    public List<LegacyDistrictResponse> searchDistricts(String searchTerm, Long provinceId) {
        if (provinceId != null) {
            return legacyDistrictRepository.findByProvinceId(provinceId.intValue()).stream()
                    .filter(d -> d.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                    .map(this::toLegacyDistrictResponse)
                    .collect(Collectors.toList());
        }
        return legacyDistrictRepository.findAll().stream()
                .filter(d -> d.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                .map(this::toLegacyDistrictResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegacyWardResponse> getWardsByDistrictId(Long districtId) {
        return legacyWardRepository.findByDistrictId(districtId.intValue()).stream()
                .map(this::toLegacyWardResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LegacyWardResponse getWardById(Long wardId) {
        LegacyWard ward = legacyWardRepository.findById(wardId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with id: " + wardId));
        return toLegacyWardResponse(ward);
    }

    @Override
    public List<LegacyWardResponse> searchWards(String searchTerm, Long districtId) {
        if (districtId != null) {
            return legacyWardRepository.findByDistrictId(districtId.intValue()).stream()
                    .filter(w -> w.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                    .map(this::toLegacyWardResponse)
                    .collect(Collectors.toList());
        }
        return legacyWardRepository.findAll().stream()
                .filter(w -> w.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                .map(this::toLegacyWardResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegacyStreetResponse> getStreetsByWardId(Long wardId) {
        // Streets not implemented yet, return empty list
        return Collections.emptyList();
    }

    @Override
    public LegacyStreetResponse getStreetById(Long streetId) {
        throw new ResourceNotFoundException("Street functionality not yet implemented");
    }

    @Override
    public List<LegacyStreetResponse> searchStreets(String searchTerm, Long wardId) {
        return Collections.emptyList();
    }

    @Override
    public List<AddressResponse> getAddressesByStreetId(Long streetId) {
        return Collections.emptyList();
    }

    @Override
    public AddressResponse getAddressById(Long addressId) {
        throw new ResourceNotFoundException("Address functionality not yet implemented");
    }

    @Override
    public List<AddressResponse> searchAddresses(String searchTerm) {
        return Collections.emptyList();
    }

    @Override
    public List<AddressResponse> getNearbyAddresses(BigDecimal latitude, BigDecimal longitude, Double radiusKm) {
        return Collections.emptyList();
    }

    @Override
    public AddressResponse createAddress(AddressCreationRequest request) {
        throw new UnsupportedOperationException("Address creation not yet implemented");
    }

    @Override
    public AddressResponse autoCompleteAddress(String fullAddressText) {
        throw new UnsupportedOperationException("Address autocomplete not yet implemented");
    }

    @Override
    public List<AddressResponse> suggestAddresses(String partialAddress) {
        return Collections.emptyList();
    }
}