package com.smartrent.mapper.impl;

import com.smartrent.dto.response.*;
import com.smartrent.infra.repository.entity.*;
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

    // ==================== ADDRESS ENTITY MAPPING ====================

    @Override
    public AddressResponse toResponse(Address address) {
        if (address == null) {
            return null;
        }

        Street street = address.getStreet();
        LegacyWard ward = address.getWard();
        District district = address.getDistrict();
        LegacyProvince province = address.getProvince();

        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .streetNumber(address.getStreetNumber())
                .streetId(street != null ? street.getId() : null)
                .streetName(street != null ? street.getName() : null)
                .wardId(ward != null ? ward.getId() : null)
                .wardName(ward != null ? ward.getName() : null)
                .districtId(district != null ? district.getId() : null)
                .districtName(district != null ? district.getName() : null)
                .provinceId(province != null ? province.getId() : null)
                .provinceName(province != null ? province.getName() : null)
                .fullAddress(address.getFullAddress())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isVerified(address.getIsVerified())
                .build();
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
                .nameEn(province.getNameEn())
                .code(province.getCode())
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
                .nameEn(district.getNameEn())
                .prefix(district.getPrefix())
                .provinceId(district.getProvince() != null ? district.getProvince().getId() : null)
                .provinceName(district.getProvince() != null ? district.getProvince().getName() : null)
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
                .nameEn(ward.getNameEn())
                .prefix(ward.getPrefix())
                .provinceId(ward.getProvince() != null ? ward.getProvince().getId() : null)
                .provinceName(ward.getProvince() != null ? ward.getProvince().getName() : null)
                .districtId(ward.getDistrict() != null ? ward.getDistrict().getId() : null)
                .districtName(ward.getDistrict() != null ? ward.getDistrict().getName() : null)
                .build();
    }

    @Override
    public LegacyStreetResponse toLegacyStreetResponse(Street street) {
        if (street == null) {
            return null;
        }

        return LegacyStreetResponse.builder()
                .id(street.getId())
                .name(street.getName())
                .nameEn(street.getNameEn())
                .prefix(street.getPrefix())
                .provinceId(street.getProvince() != null ? street.getProvince().getId() : null)
                .provinceName(street.getProvince() != null ? street.getProvince().getName() : null)
                .districtId(street.getDistrict() != null ? street.getDistrict().getId() : null)
                .districtName(street.getDistrict() != null ? street.getDistrict().getName() : null)
                .wardId(street.getWard() != null ? street.getWard().getId() : null)
                .wardName(street.getWard() != null ? street.getWard().getName() : null)
                .build();
    }

    // ==================== NEW STRUCTURE MAPPINGS ====================

    @Override
    public NewProvinceResponse toNewProvinceResponse(Province province) {
        if (province == null) {
            return null;
        }

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

    @Override
    public NewWardResponse toNewWardResponse(Ward ward) {
        if (ward == null) {
            return null;
        }

        Province province = ward.getProvince();

        return NewWardResponse.builder()
                .code(ward.getCode())
                .name(ward.getName())
                .nameEn(ward.getNameEn())
                .fullName(ward.getFullName())
                .fullNameEn(ward.getFullNameEn())
                .codeName(ward.getCodeName())
                .provinceCode(province != null ? province.getCode() : null)
                .provinceName(province != null ? province.getName() : null)
                .administrativeUnitType(ward.getAdministrativeUnit() != null ?
                        ward.getAdministrativeUnit().getFullName() : null)
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

        Province province = ward.getProvince();
        String fullAddress = ward.getName();
        if (province != null) {
            fullAddress = ward.getName() + ", " + province.getName();
        }

        String type = ward.getAdministrativeUnit() != null ?
                ward.getAdministrativeUnit().getFullName() : "Ward";

        return NewAddressSearchResponse.builder()
                .code(ward.getCode())
                .name(ward.getName())
                .type(type)
                .provinceCode(province != null ? province.getCode() : null)
                .provinceName(province != null ? province.getName() : null)
                .fullAddress(fullAddress)
                .build();
    }

    @Override
    public NewAddressSearchResponse toNewAddressSearchResponseFromProvince(Province province) {
        if (province == null) {
            return null;
        }

        String type = province.getAdministrativeUnit() != null ?
                province.getAdministrativeUnit().getFullName() : "Province";

        return NewAddressSearchResponse.builder()
                .code(province.getCode())
                .name(province.getName())
                .type(type)
                .provinceCode(province.getCode())
                .provinceName(province.getName())
                .fullAddress(province.getName())
                .build();
    }
}