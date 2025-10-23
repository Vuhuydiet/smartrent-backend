package com.smartrent.mapper.impl;

import com.smartrent.dto.response.*;
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
                .isActive(district.getIsActive() != null ? district.getIsActive() : false)
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
                .isActive(province.getIsActive() != null ? province.getIsActive() : false)
                .isMerged(province.getIsMerged() != null ? province.getIsMerged() : false)
                .isParentProvince(province.isParentProvince())
                .parentProvinceId(province.getParentProvince() != null ?
                                 province.getParentProvince().getProvinceId() : null)
                .originalName(province.getOriginalName())
                .build();
    }

    // ==================== NEW 2025 STRUCTURE MAPPINGS ====================

    @Override
    public NewProvinceResponse toNewProvinceResponse(Province province) {
        if (province == null) {
            return null;
        }

        return NewProvinceResponse.builder()
                .provinceId(province.getProvinceId())
                .code(province.getCode())
                .name(province.getName())
                .type(mapProvinceTypeToVietnamese(province.getType()))
                .build();
    }

    @Override
    public NewWardResponse toNewWardResponse(Ward ward) {
        if (ward == null) {
            return null;
        }

        return NewWardResponse.builder()
                .wardId(ward.getWardId())
                .code(ward.getCode())
                .name(ward.getName())
                .type(mapWardTypeToVietnamese(ward.getType()))
                .provinceCode(ward.getProvince() != null ? ward.getProvince().getCode() : null)
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

        return NewAddressSearchResponse.builder()
                .code(ward.getCode())
                .name(ward.getName())
                .type(mapWardTypeToVietnamese(ward.getType()))
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

        return NewAddressSearchResponse.builder()
                .code(province.getCode())
                .name(province.getName())
                .type(mapProvinceTypeToVietnamese(province.getType()))
                .provinceCode(province.getCode())
                .provinceName(province.getName())
                .fullAddress(province.getName())
                .build();
    }

    // Helper methods for Vietnamese type mapping
    private String mapProvinceTypeToVietnamese(Province.ProvinceType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case CITY -> "Thành phố";
            case PROVINCE -> "Tỉnh";
        };
    }

    private String mapWardTypeToVietnamese(Ward.WardType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case WARD -> "Phường";
            case COMMUNE -> "Xã";
            case TOWNSHIP -> "Thị trấn";
        };
    }
}
