package com.smartrent.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * DTO representing a unit of Vietnamese administrative hierarchy.
 * This DTO can represent Province, District, or Ward level administrative units.
 *
 * Structure supports legacy 3-tier hierarchy: Province → District → Ward
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressUnitDTO {

    /**
     * Unique identifier of the administrative unit
     */
    Long id;

    /**
     * Name of the administrative unit
     */
    String name;

    /**
     * Code of the administrative unit (e.g., "01" for Hà Nội)
     */
    String code;

    /**
     * Type of administrative unit (e.g., PROVINCE, CITY, DISTRICT, WARD, COMMUNE, TOWNSHIP)
     */
    String type;

    /**
     * Level in the hierarchy: PROVINCE, DISTRICT, or WARD
     */
    String level;

    /**
     * Whether this administrative unit is currently active
     */
    boolean isActive;

    /**
     * Parent province ID (for districts and wards)
     */
    Long provinceId;

    /**
     * Parent province name (for districts and wards)
     */
    String provinceName;

    /**
     * Parent district ID (for wards only in legacy structure)
     */
    Long districtId;

    /**
     * Parent district name (for wards only in legacy structure)
     */
    String districtName;

    /**
     * Full address text representing the complete hierarchy
     * Examples:
     * - Province level: "Thành phố Hà Nội"
     * - District level: "Quận Ba Đình, Thành phố Hà Nội"
     * - Ward level: "Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội"
     */
    String fullAddressText;

    /**
     * Whether this province is merged (for province level only)
     */
    Boolean isMerged;

    /**
     * Original name before any administrative changes (for merged entities)
     */
    String originalName;

    /**
     * Factory method to create from ProvinceResponse
     */
    public static AddressUnitDTO fromProvince(ProvinceResponse province) {
        return AddressUnitDTO.builder()
                .id(province.getProvinceId())
                .name(province.getName())
                .code(province.getCode())
                .type(province.getType())
                .level("PROVINCE")
                .isActive(province.isActive())
                .fullAddressText(province.getName())
                .isMerged(province.isMerged())
                .originalName(province.getOriginalName())
                .build();
    }

    /**
     * Factory method to create from DistrictResponse
     */
    public static AddressUnitDTO fromDistrict(DistrictResponse district) {
        String fullAddress = district.getName();
        if (district.getProvinceName() != null) {
            fullAddress += ", " + district.getProvinceName();
        }

        return AddressUnitDTO.builder()
                .id(district.getDistrictId())
                .name(district.getName())
                .code(district.getCode())
                .type(district.getType())
                .level("DISTRICT")
                .isActive(district.isActive())
                .provinceId(district.getProvinceId())
                .provinceName(district.getProvinceName())
                .fullAddressText(fullAddress)
                .build();
    }

    /**
     * Factory method to create from WardResponse
     */
    public static AddressUnitDTO fromWard(WardResponse ward) {
        String fullAddress = ward.getName();
        if (ward.getDistrictName() != null) {
            fullAddress += ", " + ward.getDistrictName();
        }
        if (ward.getProvinceName() != null) {
            fullAddress += ", " + ward.getProvinceName();
        }

        return AddressUnitDTO.builder()
                .id(ward.getWardId())
                .name(ward.getName())
                .code(ward.getCode())
                .type(ward.getType())
                .level("WARD")
                .isActive(ward.isActive())
                .provinceId(ward.getProvinceId())
                .provinceName(ward.getProvinceName())
                .districtId(ward.getDistrictId())
                .districtName(ward.getDistrictName())
                .fullAddressText(fullAddress)
                .build();
    }
}