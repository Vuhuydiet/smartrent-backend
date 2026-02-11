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
import java.util.Optional;
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
    private final AddressMappingRepository addressMappingRepository;
    private final StreetRepository streetRepository;
    private final ProjectRepository projectRepository;
    private final com.smartrent.mapper.AddressMapper addressMapper;

    // =====================================================================
    // New structure methods (not from interface - public API)
    // =====================================================================

    public Page<NewProvinceResponse> getAllNewProvinces(Pageable pageable) {
        return newProvinceRepository.findAll(pageable)
                .map(addressMapper::toNewProvinceResponse);
    }

    public Page<NewProvinceResponse> searchNewProvinces(String keyword, Pageable pageable, String code) {
        // Search provinces by keyword (code parameter is unused - provinces don't have parent codes)
        return newProvinceRepository.searchByKeyword(keyword, pageable)
                .map(addressMapper::toNewProvinceResponse);
    }

    public NewProvinceResponse getNewProvinceByCode(String code) {
        Province province = newProvinceRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with code: " + code));
        return addressMapper.toNewProvinceResponse(province);
    }

    public Page<NewWardResponse> getNewWardsByProvince(String provinceCode, Pageable pageable) {
        return newWardRepository.findByProvinceCode(provinceCode, pageable)
                .map(addressMapper::toNewWardResponse);
    }

    public Page<NewWardResponse> searchNewWardsByProvince(String provinceCode, String keyword, Pageable pageable) {
        return newWardRepository.searchByProvinceAndKeyword(provinceCode, keyword, pageable)
                .map(addressMapper::toNewWardResponse);
    }

    public NewWardResponse getNewWardByCode(String code) {
        Ward ward = newWardRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + code));
        return addressMapper.toNewWardResponse(ward);
    }

    public NewFullAddressResponse getNewFullAddress(String provinceCode, String wardCode) {
        Province province = newProvinceRepository.findByCode(provinceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with code: " + provinceCode));
        Ward ward = newWardRepository.findByCode(wardCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + wardCode));

        return NewFullAddressResponse.builder()
                .province(addressMapper.toNewProvinceResponse(province))
                .ward(addressMapper.toNewWardResponse(ward))
                .build();
    }

    // =====================================================================
    // Legacy structure methods (not from interface - public API)
    // =====================================================================

    public Page<LegacyProvinceResponse> getAllLegacyProvinces(Pageable pageable) {
        return legacyProvinceRepository.findAll(pageable)
                .map(addressMapper::toLegacyProvinceResponse);
    }

    public Page<LegacyProvinceResponse> searchLegacyProvinces(String keyword, Pageable pageable) {
        return legacyProvinceRepository.searchByKeyword(keyword, pageable)
                .map(addressMapper::toLegacyProvinceResponse);
    }

    public LegacyProvinceResponse getLegacyProvinceById(Integer id) {
        LegacyProvince province = legacyProvinceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy province not found with id: " + id));
        return addressMapper.toLegacyProvinceResponse(province);
    }

    public Page<LegacyDistrictResponse> getLegacyDistrictsByProvince(Integer provinceId, Pageable pageable) {
        // Get province to find its code
        LegacyProvince province = legacyProvinceRepository.findById(provinceId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy province not found with id: " + provinceId));

        return legacyDistrictRepository.findByProvinceCode(province.getCode(), pageable)
                .map(addressMapper::toLegacyDistrictResponse);
    }

    public Page<LegacyDistrictResponse> searchLegacyDistrictsByProvince(Integer provinceId, String keyword, Pageable pageable) {
        // Get province to find its code
        LegacyProvince province = legacyProvinceRepository.findById(provinceId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy province not found with id: " + provinceId));

        return legacyDistrictRepository.searchByProvinceAndKeyword(province.getCode(), keyword, pageable)
                .map(addressMapper::toLegacyDistrictResponse);
    }

    public LegacyDistrictResponse getLegacyDistrictById(Integer id) {
        District district = legacyDistrictRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy district not found with id: " + id));
        return addressMapper.toLegacyDistrictResponse(district);
    }

    public Page<LegacyWardResponse> getLegacyWardsByDistrict(Integer districtId, Pageable pageable) {
        // Get district to find its code
        District district = legacyDistrictRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy district not found with id: " + districtId));

        return legacyWardRepository.findByDistrictCode(district.getCode(), pageable)
                .map(addressMapper::toLegacyWardResponse);
    }

    public Page<LegacyWardResponse> searchLegacyWardsByDistrict(Integer districtId, String keyword, Pageable pageable) {
        // Get district to find its code
        District district = legacyDistrictRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy district not found with id: " + districtId));

        return legacyWardRepository.searchByDistrictAndKeyword(district.getCode(), keyword, pageable)
                .map(addressMapper::toLegacyWardResponse);
    }

    public LegacyWardResponse getLegacyWardById(Integer id) {
        LegacyWard ward = legacyWardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy ward not found with id: " + id));
        return addressMapper.toLegacyWardResponse(ward);
    }

    public FullAddressResponse getLegacyFullAddress(Integer provinceId, Integer districtId, Integer wardId) {
        LegacyProvince province = legacyProvinceRepository.findById(provinceId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy province not found with id: " + provinceId));
        District district = legacyDistrictRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy district not found with id: " + districtId));
        LegacyWard ward = legacyWardRepository.findById(wardId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy ward not found with id: " + wardId));

        return FullAddressResponse.builder()
                .province(addressMapper.toLegacyProvinceResponse(province))
                .district(addressMapper.toLegacyDistrictResponse(district))
                .ward(addressMapper.toLegacyWardResponse(ward))
                .build();
    }

    // =====================================================================
    // Address conversion methods
    // =====================================================================

    @Override
    public AddressConversionResponse convertLegacyToNew(Integer legacyProvinceId, Integer legacyDistrictId, Integer legacyWardId) {
        // Get legacy address
        FullAddressResponse legacyAddress = getLegacyFullAddress(legacyProvinceId, legacyDistrictId, legacyWardId);

        // Get province, district, ward codes
        LegacyProvince legacyProvince = legacyProvinceRepository.findById(legacyProvinceId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy province not found with id: " + legacyProvinceId));
        District legacyDistrict = legacyDistrictRepository.findById(legacyDistrictId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy district not found with id: " + legacyDistrictId));
        LegacyWard legacyWard = legacyWardRepository.findById(legacyWardId)
                .orElseThrow(() -> new ResourceNotFoundException("Legacy ward not found with id: " + legacyWardId));

        // Find best mapping (polygon contains > default > nearest)
        AddressMapping mapping = addressMappingRepository.findBestByLegacyAddress(
                        legacyProvince.getCode(),
                        legacyDistrict.getCode(),
                        legacyWard.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Address mapping not found for legacy address"));

        // Get new address from mapping
        Province newProvince = mapping.getNewProvince();
        Ward newWard = mapping.getNewWard();

        NewFullAddressResponse newAddress = NewFullAddressResponse.builder()
                .province(addressMapper.toNewProvinceResponse(newProvince))
                .ward(addressMapper.toNewWardResponse(newWard))
                .build();

        String conversionNote = String.format("Converted from legacy structure. " +
                        "Divided: %s, Merged: %s, Default: %s",
                mapping.getIsDividedWard(), mapping.getIsMergedWard(), mapping.getIsDefaultNewWard());

        return AddressConversionResponse.builder()
                .legacyAddress(legacyAddress)
                .newAddress(newAddress)
                .conversionNote(conversionNote)
                .build();
    }

    @Override
    public AddressMergeHistoryResponse getMergeHistory(String newProvinceCode, String newWardCode) {
        log.info("Getting merge history - provinceCode: {}, wardCode: {}", newProvinceCode, newWardCode);

        // Get new address
        Province newProvince = newProvinceRepository.findByCode(newProvinceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with code: " + newProvinceCode));
        Ward newWard = newWardRepository.findByCode(newWardCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + newWardCode));

        NewFullAddressResponse newAddress = NewFullAddressResponse.builder()
                .province(addressMapper.toNewProvinceResponse(newProvince))
                .ward(addressMapper.toNewWardResponse(newWard))
                .build();

        // Get all legacy addresses that were merged into this new address
        List<AddressMapping> allMappings = addressMappingRepository.findByNewAddress(newProvinceCode, newWardCode);

        if (allMappings.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format("No merge history found for new address: province=%s, ward=%s",
                            newProvinceCode, newWardCode));
        }

        // Build legacy source mappings
        List<AddressMergeHistoryResponse.LegacyAddressMapping> legacySources = allMappings.stream()
                .map(mapping -> {
                    // Get legacy address components
                    LegacyProvince legacyProvince = mapping.getLegacyProvince();
                    District legacyDistrict = mapping.getLegacyDistrict();
                    LegacyWard legacyWard = mapping.getLegacyWard();

                    FullAddressResponse legacyAddress = FullAddressResponse.builder()
                            .province(addressMapper.toLegacyProvinceResponse(legacyProvince))
                            .district(legacyDistrict != null ? addressMapper.toLegacyDistrictResponse(legacyDistrict) : null)
                            .ward(legacyWard != null ? addressMapper.toLegacyWardResponse(legacyWard) : null)
                            .build();

                    // Build merge description
                    String mergeDescription = buildMergeDescription(mapping);

                    return AddressMergeHistoryResponse.LegacyAddressMapping.builder()
                            .legacyAddress(legacyAddress)
                            .isMergedProvince(mapping.getIsMergedProvince())
                            .isMergedWard(mapping.getIsMergedWard())
                            .isDividedWard(mapping.getIsDividedWard())
                            .isDefault(mapping.getIsDefaultNewWard())
                            .mergeDescription(mergeDescription)
                            .build();
                })
                .collect(Collectors.toList());

        // Build summary note
        String mergeNote;
        if (allMappings.size() == 1) {
            AddressMapping mapping = allMappings.get(0);
            if (Boolean.TRUE.equals(mapping.getIsDividedWard())) {
                mergeNote = "This new ward was created by dividing a legacy ward into multiple new wards.";
            } else {
                mergeNote = "This new ward has a 1:1 mapping with the legacy ward (no merge).";
            }
        } else {
            long mergedCount = allMappings.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsMergedWard()))
                    .count();
            mergeNote = String.format("This new ward was created by merging %d legacy wards. " +
                    "%d were merged, %d were divided.",
                    allMappings.size(), mergedCount, allMappings.size() - mergedCount);
        }

        return AddressMergeHistoryResponse.builder()
                .newAddress(newAddress)
                .legacySources(legacySources)
                .totalMergedCount(allMappings.size())
                .mergeNote(mergeNote)
                .build();
    }

    private String buildMergeDescription(AddressMapping mapping) {
        if (Boolean.TRUE.equals(mapping.getIsMergedProvince()) && Boolean.TRUE.equals(mapping.getIsMergedWard())) {
            return "Both province and ward were merged";
        } else if (Boolean.TRUE.equals(mapping.getIsMergedProvince())) {
            return "Province was merged";
        } else if (Boolean.TRUE.equals(mapping.getIsMergedWard())) {
            return "Ward was merged";
        } else if (Boolean.TRUE.equals(mapping.getIsDividedWard())) {
            return "Ward was divided into multiple new wards";
        } else {
            return "Direct 1:1 mapping (no merge or division)";
        }
    }

    // =====================================================================
    // Old AddressService interface methods (for backward compatibility)
    // =====================================================================

    @Override
    public List<LegacyProvinceResponse> getAllProvinces() {
        return legacyProvinceRepository.findAll().stream()
                .map(addressMapper::toLegacyProvinceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegacyProvinceResponse> getParentProvinces() {
        return getAllProvinces(); // All provinces are parent provinces in legacy structure
    }

    @Override
    public LegacyProvinceResponse getProvinceById(Integer provinceId) {
        LegacyProvince province = legacyProvinceRepository.findById(provinceId)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + provinceId));
        return addressMapper.toLegacyProvinceResponse(province);
    }

    @Override
    public List<LegacyProvinceResponse> searchProvinces(String searchTerm) {
        return legacyProvinceRepository.findByNameContainingIgnoreCase(searchTerm).stream()
                .map(addressMapper::toLegacyProvinceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegacyDistrictResponse> getDistrictsByProvinceId(Integer provinceId) {
        // Get province to find its code
        LegacyProvince province = legacyProvinceRepository.findById(provinceId)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + provinceId));

        return legacyDistrictRepository.findByProvinceCode(province.getCode()).stream()
                .map(addressMapper::toLegacyDistrictResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LegacyDistrictResponse getDistrictById(Integer districtId) {
        District district = legacyDistrictRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District not found with id: " + districtId));
        return addressMapper.toLegacyDistrictResponse(district);
    }

    @Override
    public List<LegacyDistrictResponse> searchDistricts(String searchTerm, Integer provinceId) {
        if (provinceId != null) {
            // Get province to find its code
            LegacyProvince province = legacyProvinceRepository.findById(provinceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + provinceId));

            return legacyDistrictRepository.findByProvinceCode(province.getCode()).stream()
                    .filter(d -> d.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                    .map(addressMapper::toLegacyDistrictResponse)
                    .collect(Collectors.toList());
        }
        return legacyDistrictRepository.findAll().stream()
                .filter(d -> d.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                .map(addressMapper::toLegacyDistrictResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegacyWardResponse> getWardsByDistrictId(Integer districtId) {
        // Get district to find its code
        District district = legacyDistrictRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District not found with id: " + districtId));

        return legacyWardRepository.findByDistrictCode(district.getCode()).stream()
                .map(addressMapper::toLegacyWardResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LegacyWardResponse getWardById(Integer wardId) {
        LegacyWard ward = legacyWardRepository.findById(wardId)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with id: " + wardId));
        return addressMapper.toLegacyWardResponse(ward);
    }

    @Override
    public List<LegacyWardResponse> searchWards(String searchTerm, Integer districtId) {
        if (districtId != null) {
            // Get district to find its code
            District district = legacyDistrictRepository.findById(districtId)
                    .orElseThrow(() -> new ResourceNotFoundException("District not found with id: " + districtId));

            return legacyWardRepository.findByDistrictCode(district.getCode()).stream()
                    .filter(w -> w.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                    .map(addressMapper::toLegacyWardResponse)
                    .collect(Collectors.toList());
        }
        return legacyWardRepository.findAll().stream()
                .filter(w -> w.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                .map(addressMapper::toLegacyWardResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse getAddressById(Integer addressId) {
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