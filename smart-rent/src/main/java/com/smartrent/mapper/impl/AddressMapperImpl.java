package com.smartrent.mapper.impl;

import com.smartrent.controller.dto.response.*;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.mapper.AddressMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressMapperImpl implements AddressMapper {

    @Override
    public AddressResponse toResponse(Address address) {
        if (address == null) {
            return null;
        }

        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .streetNumber(address.getStreetNumber())
                .streetId(address.getStreet() != null ? address.getStreet().getStreetId() : null)
                .streetName(address.getStreet() != null ? address.getStreet().getName() : null)
                .wardId(address.getWard() != null ? address.getWard().getWardId() : null)
                .wardName(address.getWard() != null ? address.getWard().getName() : null)
                .districtId(address.getDistrict() != null ? address.getDistrict().getDistrictId() : null)
                .districtName(address.getDistrict() != null ? address.getDistrict().getName() : null)
                .provinceId(address.getProvince() != null ? address.getProvince().getProvinceId() : null)
                .provinceName(address.getProvince() != null ? address.getProvince().getDisplayName() : null)
                .fullAddress(address.getFullAddress())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isVerified(address.getIsVerified())
                .build();
    }

    @Override
    public StreetResponse toResponse(Street street) {
        if (street == null) {
            return null;
        }

        return StreetResponse.builder()
                .streetId(street.getStreetId())
                .name(street.getName())
                .wardId(street.getWard() != null ? street.getWard().getWardId() : null)
                .wardName(street.getWard() != null ? street.getWard().getName() : null)
                .districtId(street.getWard() != null && street.getWard().getDistrict() != null ?
                           street.getWard().getDistrict().getDistrictId() : null)
                .districtName(street.getWard() != null && street.getWard().getDistrict() != null ?
                             street.getWard().getDistrict().getName() : null)
                .provinceId(street.getWard() != null && street.getWard().getDistrict() != null &&
                           street.getWard().getDistrict().getProvince() != null ?
                           street.getWard().getDistrict().getProvince().getProvinceId() : null)
                .provinceName(street.getWard() != null && street.getWard().getDistrict() != null &&
                             street.getWard().getDistrict().getProvince() != null ?
                             street.getWard().getDistrict().getProvince().getDisplayName() : null)
                .isActive(street.getIsActive())
                .build();
    }

    @Override
    public WardResponse toResponse(Ward ward) {
        if (ward == null) {
            return null;
        }

        return WardResponse.builder()
                .wardId(ward.getWardId())
                .name(ward.getName())
                .code(ward.getCode())
                .type(ward.getType() != null ? ward.getType().name() : null)
                .districtId(ward.getDistrict() != null ? ward.getDistrict().getDistrictId() : null)
                .districtName(ward.getDistrict() != null ? ward.getDistrict().getName() : null)
                .provinceId(ward.getDistrict() != null && ward.getDistrict().getProvince() != null ?
                           ward.getDistrict().getProvince().getProvinceId() : null)
                .provinceName(ward.getDistrict() != null && ward.getDistrict().getProvince() != null ?
                             ward.getDistrict().getProvince().getDisplayName() : null)
                .isActive(ward.getIsActive())
                .build();
    }

    @Override
    public DistrictResponse toResponse(District district) {
        if (district == null) {
            return null;
        }

        return DistrictResponse.builder()
                .districtId(district.getDistrictId())
                .name(district.getName())
                .code(district.getCode())
                .type(district.getType() != null ? district.getType().name() : null)
                .provinceId(district.getProvince() != null ? district.getProvince().getProvinceId() : null)
                .provinceName(district.getProvince() != null ? district.getProvince().getDisplayName() : null)
                .isActive(district.getIsActive())
                .build();
    }

    @Override
    public ProvinceResponse toResponse(Province province) {
        if (province == null) {
            return null;
        }

        return ProvinceResponse.builder()
                .provinceId(province.getProvinceId())
                .name(province.getName())
                .code(province.getCode())
                .type(province.getType() != null ? province.getType().name() : null)
                .displayName(province.getDisplayName())
                .isActive(province.getIsActive())
                .isMerged(province.getIsMerged())
                .isParentProvince(province.isParentProvince())
                .parentProvinceId(province.getParentProvince() != null ?
                                 province.getParentProvince().getProvinceId() : null)
                .originalName(province.getOriginalName())
                .build();
    }
}
