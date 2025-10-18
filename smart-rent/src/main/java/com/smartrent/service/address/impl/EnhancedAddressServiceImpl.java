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
import com.smartrent.service.address.EnhancedAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EnhancedAddressServiceImpl implements EnhancedAddressService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;

    @Override
    public List<ProvinceResponse> getAllProvinces() {
        log.info("Getting all active provinces");
        return provinceRepository.findByParentProvinceIsNullAndIsActiveTrueOrderByName()
                .stream()
                .map(this::mapToProvinceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DistrictResponse> getDistrictsByProvinceCode(String provinceCode) {
        log.info("Getting districts for province code: {}", provinceCode);

        Province province = provinceRepository.findByCode(provinceCode)
                .orElseThrow(() -> new AppException(DomainCode.PROVINCE_NOT_FOUND));

        return districtRepository.findByProvince_ProvinceIdAndIsActiveTrue(province.getProvinceId())
                .stream()
                .map(this::mapToDistrictResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardResponse> getWardsByDistrictCode(String districtCode) {
        log.info("Getting wards for district code: {}", districtCode);

        District district = districtRepository.findByCode(districtCode)
                .orElseThrow(() -> new AppException(DomainCode.DISTRICT_NOT_FOUND));

        return wardRepository.findByDistrict_DistrictIdAndIsActiveTrue(district.getDistrictId())
                .stream()
                .map(this::mapToWardResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FullAddressResponse getFullAddress(String provinceCode, String districtCode, String wardCode) {
        log.info("Building full address for codes: {}/{}/{}", provinceCode, districtCode, wardCode);

        Province province = provinceRepository.findByCode(provinceCode)
                .orElseThrow(() -> new AppException(DomainCode.PROVINCE_NOT_FOUND));

        District district = districtRepository.findByCode(districtCode)
                .orElseThrow(() -> new AppException(DomainCode.DISTRICT_NOT_FOUND));

        Ward ward = wardRepository.findByCode(wardCode)
                .orElseThrow(() -> new AppException(DomainCode.WARD_NOT_FOUND));

        return buildFullAddressResponse(province, district, ward);
    }

    @Override
    public List<ProvinceResponse> getNewProvinces() {
        log.info("Getting all provinces including merged ones");

        // Get all provinces (including those that were merged)
        return provinceRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToProvinceResponseWithMergeInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardResponse> getWardsByProvinceCode(String provinceCode) {
        log.info("Getting all wards for province code: {} (flattened)", provinceCode);

        Province province = provinceRepository.findByCode(provinceCode)
                .orElseThrow(() -> new AppException(DomainCode.PROVINCE_NOT_FOUND));

        // Get all districts for this province
        List<District> districts = districtRepository.findByProvince_ProvinceIdAndIsActiveTrue(province.getProvinceId());

        // Flatten wards from all districts
        return districts.stream()
                .flatMap(district -> wardRepository.findByDistrict_DistrictIdAndIsActiveTrue(district.getDistrictId()).stream())
                .map(this::mapToWardResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FullAddressResponse getNewFullAddress(String provinceCode, String districtCode, String wardCode) {
        log.info("Building new full address for codes: {}/{}/{}", provinceCode, districtCode, wardCode);

        // Similar to getFullAddress but considers merged entities
        Province province = provinceRepository.findByCode(provinceCode)
                .orElse(null);

        // If not found, check if it's a merged province
        if (province == null) {
            province = findMergedProvince(provinceCode);
        }

        District district = districtRepository.findByCode(districtCode)
                .orElseThrow(() -> new AppException(DomainCode.DISTRICT_NOT_FOUND));

        Ward ward = wardRepository.findByCode(wardCode)
                .orElse(null);

        // If not found, check merged wards
        if (ward == null) {
            ward = findMergedWard(wardCode);
        }

        return buildFullAddressResponse(province, district, ward);
    }

    @Override
    public AddressSearchResponse searchAddress(String query, Integer limit) {
        log.info("Searching addresses (current structure): {}", query);

        List<AddressSearchResponse.AddressMatch> matches = new ArrayList<>();
        int maxResults = limit != null ? limit : 20;

        // Search in provinces
        List<Province> provinces = provinceRepository.findByIsActiveTrue().stream()
                .filter(p -> matchesQuery(p.getName(), query) && !p.getIsMerged())
                .limit(maxResults)
                .toList();

        for (Province province : provinces) {
            // For each province, get districts
            List<District> districts = districtRepository.findByProvince_ProvinceIdAndIsActiveTrue(province.getProvinceId());

            for (District district : districts) {
                if (matchesQuery(district.getName(), query)) {
                    // Get wards for matching districts
                    List<Ward> wards = wardRepository.findByDistrict_DistrictIdAndIsActiveTrue(district.getDistrictId());

                    for (Ward ward : wards) {
                        if (matchesQuery(ward.getName(), query)) {
                            matches.add(buildAddressMatch(province, district, ward, query));

                            if (matches.size() >= maxResults) break;
                        }
                    }
                }
                if (matches.size() >= maxResults) break;
            }
            if (matches.size() >= maxResults) break;
        }

        // Sort by relevance score
        matches.sort(Comparator.comparingDouble(AddressSearchResponse.AddressMatch::getMatchScore).reversed());

        return AddressSearchResponse.builder()
                .query(query)
                .totalResults(matches.size())
                .matches(matches.stream().limit(maxResults).collect(Collectors.toList()))
                .build();
    }

    @Override
    public AddressSearchResponse searchNewAddress(String query, Integer limit) {
        log.info("Searching addresses (new structure including merged): {}", query);

        // Similar to searchAddress but includes merged entities
        List<AddressSearchResponse.AddressMatch> matches = new ArrayList<>();
        int maxResults = limit != null ? limit : 20;

        // Search in all provinces (including merged)
        List<Province> provinces = provinceRepository.findByIsActiveTrue().stream()
                .filter(p -> matchesQuery(p.getName(), query) || (p.getOriginalName() != null && matchesQuery(p.getOriginalName(), query)))
                .limit(maxResults * 2) // Get more to account for merged entities
                .toList();

        for (Province province : provinces) {
            List<District> districts = districtRepository.findByProvince_ProvinceIdAndIsActiveTrue(province.getProvinceId());

            for (District district : districts) {
                List<Ward> wards = wardRepository.findByDistrict_DistrictIdAndIsActiveTrue(district.getDistrictId());

                for (Ward ward : wards) {
                    String combinedText = String.format("%s %s %s",
                            ward.getName(), district.getName(), province.getName());

                    if (matchesQuery(combinedText, query)) {
                        AddressSearchResponse.AddressMatch match = buildAddressMatch(province, district, ward, query);
                        match.setIsMerged(province.getIsMerged() || ward.getCode().startsWith("MERGED_"));
                        matches.add(match);

                        if (matches.size() >= maxResults) break;
                    }
                }
                if (matches.size() >= maxResults) break;
            }
            if (matches.size() >= maxResults) break;
        }

        matches.sort(Comparator.comparingDouble(AddressSearchResponse.AddressMatch::getMatchScore).reversed());

        return AddressSearchResponse.builder()
                .query(query)
                .totalResults(matches.size())
                .matches(matches.stream().limit(maxResults).collect(Collectors.toList()))
                .build();
    }

    @Override
    public AddressConversionResponse convertAddress(String provinceCode, String districtCode, String wardCode) {
        log.info("Converting address: {}/{}/{}", provinceCode, districtCode, wardCode);

        Province province = provinceRepository.findByCode(provinceCode).orElse(null);
        District district = districtRepository.findByCode(districtCode).orElse(null);
        Ward ward = wardRepository.findByCode(wardCode).orElse(null);

        if (province == null || district == null || ward == null) {
            throw new AppException(DomainCode.ADDRESS_NOT_FOUND);
        }

        // Build old address
        AddressConversionResponse.OldAddress oldAddress = AddressConversionResponse.OldAddress.builder()
                .provinceCode(province.getCode())
                .provinceName(province.getName())
                .districtCode(district.getCode())
                .districtName(district.getName())
                .wardCode(ward.getCode())
                .wardName(ward.getName())
                .fullAddress(buildFullAddressString(province, district, ward))
                .build();

        // Check if conversion is needed
        boolean needsConversion = province.getIsMerged() || checkWardMerged(ward);
        String conversionType = "NONE";
        Province newProvince = province;
        Ward newWard = ward;
        LocalDate effectiveDate = null;

        if (province.getIsMerged() && province.getParentProvince() != null) {
            newProvince = province.getParentProvince();
            conversionType = "PROVINCE_MERGE";
            effectiveDate = province.getEffectiveFrom();
            needsConversion = true;
        }

        // Build new address
        AddressConversionResponse.NewAddress newAddress = AddressConversionResponse.NewAddress.builder()
                .provinceCode(newProvince.getCode())
                .provinceName(newProvince.getName())
                .districtCode(district.getCode())
                .districtName(district.getName())
                .wardCode(newWard.getCode())
                .wardName(newWard.getName())
                .fullAddress(buildFullAddressString(newProvince, district, newWard))
                .build();

        AddressConversionResponse.ConversionInfo conversionInfo = AddressConversionResponse.ConversionInfo.builder()
                .wasConverted(needsConversion)
                .conversionType(conversionType)
                .effectiveDate(effectiveDate)
                .notes(needsConversion ? "Address was converted due to administrative reorganization" : "No conversion needed")
                .build();

        return AddressConversionResponse.builder()
                .oldAddress(oldAddress)
                .newAddress(newAddress)
                .conversionInfo(conversionInfo)
                .build();
    }

    @Override
    public List<AddressConversionResponse> convertAddressesBatch(BatchAddressConversionRequest request) {
        log.info("Converting {} addresses in batch", request.getAddresses().size());

        return request.getAddresses().stream()
                .map(addr -> {
                    try {
                        return convertAddress(addr.getProvinceCode(), addr.getDistrictCode(), addr.getWardCode());
                    } catch (Exception e) {
                        log.error("Error converting address: {}/{}/{}",
                                addr.getProvinceCode(), addr.getDistrictCode(), addr.getWardCode(), e);
                        return null;
                    }
                })
                .filter(result -> result != null)
                .collect(Collectors.toList());
    }

    @Override
    public MergeHistoryResponse getProvinceMergeHistory(String provinceCode) {
        log.info("Getting merge history for province: {}", provinceCode);

        Province province = provinceRepository.findByCode(provinceCode)
                .orElseThrow(() -> new AppException(DomainCode.PROVINCE_NOT_FOUND));

        MergeHistoryResponse.MergeHistoryResponseBuilder builder = MergeHistoryResponse.builder()
                .entityCode(province.getCode())
                .entityName(province.getName())
                .entityType("PROVINCE")
                .isMerged(province.getIsMerged())
                .mergeDate(province.getEffectiveFrom());

        // If this province was merged into another
        if (province.getIsMerged() && province.getParentProvince() != null) {
            Province parent = province.getParentProvince();
            builder.mergedInto(MergeHistoryResponse.MergedInto.builder()
                    .code(parent.getCode())
                    .name(parent.getName())
                    .effectiveDate(province.getEffectiveFrom())
                    .reason("Administrative reorganization")
                    .build());
        }

        // If this province is a parent (has merged provinces)
        List<Province> mergedProvinces = provinceRepository
                .findByParentProvinceProvinceIdAndIsActiveTrueOrderByName(province.getProvinceId());

        if (!mergedProvinces.isEmpty()) {
            List<MergeHistoryResponse.MergedFrom> mergedFrom = mergedProvinces.stream()
                    .map(p -> MergeHistoryResponse.MergedFrom.builder()
                            .code(p.getCode())
                            .originalName(p.getOriginalName() != null ? p.getOriginalName() : p.getName())
                            .mergeDate(p.getEffectiveFrom())
                            .reason("Administrative reorganization")
                            .build())
                    .collect(Collectors.toList());
            builder.mergedFrom(mergedFrom);
        }

        return builder.build();
    }

    @Override
    public MergeHistoryResponse getWardMergeHistory(String wardCode) {
        log.info("Getting merge history for ward: {}", wardCode);

        Ward ward = wardRepository.findByCode(wardCode)
                .orElseThrow(() -> new AppException(DomainCode.WARD_NOT_FOUND));

        // Similar logic to province merge history
        return MergeHistoryResponse.builder()
                .entityCode(ward.getCode())
                .entityName(ward.getName())
                .entityType("WARD")
                .isMerged(false) // Extend Ward entity to support merge tracking
                .mergeDate(ward.getEffectiveFrom())
                .build();
    }

    @Override
    public boolean validateAddressCodes(String provinceCode, String districtCode, String wardCode) {
        return provinceRepository.findByCode(provinceCode).isPresent()
                && districtRepository.findByCode(districtCode).isPresent()
                && wardRepository.findByCode(wardCode).isPresent();
    }

    @Override
    public LocalDate getEffectiveDate(String entityCode, String entityType) {
        return switch (entityType.toUpperCase()) {
            case "PROVINCE" -> provinceRepository.findByCode(entityCode)
                    .map(Province::getEffectiveFrom)
                    .orElse(null);
            case "DISTRICT" -> districtRepository.findByCode(entityCode)
                    .map(District::getEffectiveFrom)
                    .orElse(null);
            case "WARD" -> wardRepository.findByCode(entityCode)
                    .map(Ward::getEffectiveFrom)
                    .orElse(null);
            default -> null;
        };
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    private ProvinceResponse mapToProvinceResponse(Province province) {
        return ProvinceResponse.builder()
                .provinceId(province.getProvinceId())
                .name(province.getName())
                .code(province.getCode())
                .type(province.getType().name())
                .displayName(province.getDisplayName())
                .isActive(province.getIsActive())
                .isMerged(province.getIsMerged())
                .isParentProvince(province.getParentProvince() == null)
                .build();
    }

    private ProvinceResponse mapToProvinceResponseWithMergeInfo(Province province) {
        ProvinceResponse.ProvinceResponseBuilder builder = ProvinceResponse.builder()
                .provinceId(province.getProvinceId())
                .name(province.getName())
                .code(province.getCode())
                .type(province.getType().name())
                .displayName(province.getDisplayName())
                .isActive(province.getIsActive())
                .isMerged(province.getIsMerged())
                .isParentProvince(province.getParentProvince() == null);

        if (province.getIsMerged() && province.getParentProvince() != null) {
            builder.parentProvinceId(province.getParentProvince().getProvinceId())
                    .originalName(province.getOriginalName());
        }

        return builder.build();
    }

    private DistrictResponse mapToDistrictResponse(District district) {
        return DistrictResponse.builder()
                .districtId(district.getDistrictId())
                .name(district.getName())
                .code(district.getCode())
                .type(district.getType().name())
                .provinceId(district.getProvince().getProvinceId())
                .provinceName(district.getProvince().getName())
                .isActive(district.getIsActive())
                .build();
    }

    private WardResponse mapToWardResponse(Ward ward) {
        return WardResponse.builder()
                .wardId(ward.getWardId())
                .name(ward.getName())
                .code(ward.getCode())
                .type(ward.getType().name())
                .districtId(ward.getDistrict().getDistrictId())
                .districtName(ward.getDistrict().getName())
                .provinceId(ward.getDistrict().getProvince().getProvinceId())
                .provinceName(ward.getDistrict().getProvince().getName())
                .isActive(ward.getIsActive())
                .build();
    }

    private FullAddressResponse buildFullAddressResponse(Province province, District district, Ward ward) {
        return FullAddressResponse.builder()
                .province(FullAddressResponse.ProvinceInfo.builder()
                        .code(province.getCode())
                        .name(province.getName())
                        .type(province.getType().name())
                        .build())
                .district(FullAddressResponse.DistrictInfo.builder()
                        .code(district.getCode())
                        .name(district.getName())
                        .type(district.getType().name())
                        .build())
                .ward(FullAddressResponse.WardInfo.builder()
                        .code(ward.getCode())
                        .name(ward.getName())
                        .type(ward.getType().name())
                        .build())
                .fullAddressText(buildFullAddressString(province, district, ward))
                .build();
    }

    private String buildFullAddressString(Province province, District district, Ward ward) {
        return String.format("%s, %s, %s",
                ward.getName(),
                district.getName(),
                province.getDisplayName() != null ? province.getDisplayName() : province.getName());
    }

    private AddressSearchResponse.AddressMatch buildAddressMatch(Province province, District district, Ward ward, String query) {
        String fullAddress = buildFullAddressString(province, district, ward);
        double score = calculateMatchScore(fullAddress, query);

        return AddressSearchResponse.AddressMatch.builder()
                .provinceCode(province.getCode())
                .provinceName(province.getName())
                .districtCode(district.getCode())
                .districtName(district.getName())
                .wardCode(ward.getCode())
                .wardName(ward.getName())
                .fullAddress(fullAddress)
                .matchScore(score)
                .isActive(province.getIsActive() && district.getIsActive() && ward.getIsActive())
                .isMerged(province.getIsMerged())
                .build();
    }

    private boolean matchesQuery(String text, String query) {
        if (text == null || query == null) return false;
        return text.toLowerCase().contains(query.toLowerCase());
    }

    private double calculateMatchScore(String text, String query) {
        if (text == null || query == null) return 0.0;

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        // Exact match
        if (lowerText.equals(lowerQuery)) return 1.0;

        // Starts with query
        if (lowerText.startsWith(lowerQuery)) return 0.9;

        // Contains query
        if (lowerText.contains(lowerQuery)) {
            // Score based on position and length
            int index = lowerText.indexOf(lowerQuery);
            double positionScore = 1.0 - (index / (double) lowerText.length());
            double lengthScore = lowerQuery.length() / (double) lowerText.length();
            return 0.5 + (positionScore * 0.25) + (lengthScore * 0.25);
        }

        return 0.0;
    }

    private Province findMergedProvince(String code) {
        // Check if this code belongs to a merged province
        return provinceRepository.findByCode(code)
                .filter(p -> p.getIsMerged())
                .orElseThrow(() -> new AppException(DomainCode.PROVINCE_NOT_FOUND));
    }

    private Ward findMergedWard(String code) {
        // Placeholder for ward merge logic
        throw new AppException(DomainCode.WARD_NOT_FOUND);
    }

    private boolean checkWardMerged(Ward ward) {
        // Placeholder - extend Ward entity to support merge tracking
        return false;
    }
}
