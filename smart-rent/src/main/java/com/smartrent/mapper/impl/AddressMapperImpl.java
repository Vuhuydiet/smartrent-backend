package com.smartrent.mapper.impl;

import com.smartrent.dto.response.*;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.infra.repository.*;
import com.smartrent.mapper.AddressMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

/**
 * Implementation of AddressMapper
 * Handles mapping for both legacy and new address structures
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressMapperImpl implements AddressMapper {

    LegacyProvinceRepository legacyProvinceRepository;
    LegacyDistrictRepository legacyDistrictRepository;
    LegacyWardRepository legacyWardRepository;
    ProvinceRepository provinceRepository;
    WardRepository wardRepository;
    ProjectRepository projectRepository;
    StreetRepository streetRepository;

    // ==================== ADDRESS ENTITY MAPPING ====================

    @Override
    public AddressResponse toResponse(Address address) {
        if (address == null) {
            return null;
        }

        AddressResponse.AddressResponseBuilder builder = AddressResponse.builder()
                .addressId(address.getAddressId())
                .fullAddress(address.getDisplayAddress())
                .fullNewAddress(address.getFullNewAddress())
                .addressType(address.getAddressType() != null ? address.getAddressType().name() : null)
                .latitude(address.getLatitude())
                .longitude(address.getLongitude());

        if (address.isNewStructure() && address.hasNewComponents()) {
            // New structure (34 provinces, 2-tier): province → ward (no district)
            if (address.getNewProvinceCode() != null) {
                builder.provinceCode(address.getNewProvinceCode());
                provinceRepository.findByCode(address.getNewProvinceCode())
                        .ifPresent(province -> builder.provinceName(province.getName()));
            }
            if (address.getNewWardCode() != null) {
                builder.wardCode(address.getNewWardCode());
                wardRepository.findByCode(address.getNewWardCode())
                        .ifPresent(ward -> builder.wardName(ward.getName()));
            }
            if (address.getNewStreet() != null) {
                builder.street(address.getNewStreet());
            }
        } else {
            // Legacy structure (63 provinces, 3-tier): province → district → ward
            if (address.getLegacyProvinceId() != null) {
                builder.provinceCode(String.valueOf(address.getLegacyProvinceId()));
                legacyProvinceRepository.findById(address.getLegacyProvinceId())
                        .ifPresent(province -> builder.provinceName(province.getName()));
            }
            if (address.getLegacyDistrictId() != null) {
                builder.districtCode(String.valueOf(address.getLegacyDistrictId()));
                legacyDistrictRepository.findById(address.getLegacyDistrictId())
                        .ifPresent(district -> builder.districtName(district.getName()));
            }
            if (address.getLegacyWardId() != null) {
                builder.wardCode(String.valueOf(address.getLegacyWardId()));
                legacyWardRepository.findById(address.getLegacyWardId())
                        .ifPresent(ward -> builder.wardName(ward.getName()));
            }
            if (address.getLegacyStreet() != null) {
                builder.street(address.getLegacyStreet());
            }
        }

        return builder.build();
    }

    // ==================== LEGACY STRUCTURE MAPPINGS ====================

    @Override
    public LegacyProvinceResponse toLegacyProvinceResponse(LegacyProvince province) {
        if (province == null) {
            return null;
        }

        return LegacyProvinceResponse.builder()
                .id(province.getId())
                .name(province.getName())
                .shortName(province.getShortName())
                .code(province.getCode())
                .key(province.getKey())
                .build();
    }

    @Override
    public LegacyDistrictResponse toLegacyDistrictResponse(District district) {
        if (district == null) {
            return null;
        }

        return LegacyDistrictResponse.builder()
                .id(district.getId())
                .name(district.getName())
                .shortName(district.getShortName())
                .code(district.getCode())
                .type(district.getType())
                .provinceCode(district.getProvinceCode())
                .provinceName(district.getProvinceName())
                .build();
    }

    @Override
    public LegacyWardResponse toLegacyWardResponse(LegacyWard ward) {
        if (ward == null) {
            return null;
        }

        return LegacyWardResponse.builder()
                .id(ward.getId())
                .name(ward.getName())
                .shortName(ward.getShortName())
                .code(ward.getCode())
                .type(ward.getType())
                .provinceCode(ward.getProvinceCode())
                .provinceName(ward.getProvinceName())
                .districtCode(ward.getDistrictCode())
                .districtName(ward.getDistrictName())
                .build();
    }

    // ==================== NEW STRUCTURE MAPPINGS ====================

    @Override
    public NewProvinceResponse toNewProvinceResponse(Province province) {
        if (province == null) {
            return null;
        }

        return NewProvinceResponse.builder()
                .id(province.getCode())
                .name(province.getName())
                .shortName(province.getShortName())
                .key(province.getKey())
                .latitude(province.getLatitude())
                .longitude(province.getLongitude())
                .alias(province.getAlias())
                .build();
    }

    @Override
    public NewWardResponse toNewWardResponse(Ward ward) {
        if (ward == null) {
            return null;
        }

        return NewWardResponse.builder()
                .code(ward.getCode())
                .name(ward.getName())
                .shortName(ward.getShortName())
                .type(ward.getType())
                .key(ward.getKey())
                .latitude(ward.getLatitude())
                .longitude(ward.getLongitude())
                .provinceCode(ward.getProvinceCode())
                .provinceName(ward.getProvinceName())
                .alias(ward.getAlias())
                .build();
    }

    @Override
    public NewFullAddressResponse toNewFullAddressResponse(Province province, Ward ward) {
        return NewFullAddressResponse.builder()
                .province(toNewProvinceResponse(province))
                .ward(ward != null ? toNewWardResponse(ward) : null)
                .build();
    }

    @Override
    public NewAddressSearchResponse toNewAddressSearchResponse(Ward ward) {
        if (ward == null) {
            return null;
        }

        String fullAddress = ward.getName() + ", " + ward.getProvinceName();

        return NewAddressSearchResponse.builder()
                .code(ward.getCode())
                .name(ward.getName())
                .type(ward.getType())
                .provinceCode(ward.getProvinceCode())
                .provinceName(ward.getProvinceName())
                .fullAddress(fullAddress)
                .build();
    }

    @Override
    public NewAddressSearchResponse toNewAddressSearchResponseFromProvince(Province province) {
        if (province == null) {
            return null;
        }

        return NewAddressSearchResponse.builder()
                .code(province.getCode())
                .name(province.getName())
                .type("Province")
                .provinceCode(province.getCode())
                .provinceName(province.getName())
                .fullAddress(province.getName())
                .build();
    }
}