package com.smartrent.dto.response;

import com.smartrent.infra.repository.entity.AddressMetadata;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Address response DTO
 * Contains both formatted address strings and structured metadata
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressResponse {

    Long addressId;

    /**
     * Formatted address for old structure (63 provinces, 3-tier)
     */
    String fullAddress;

    /**
     * Formatted address for new structure (34 provinces, 2-tier)
     */
    String fullNewAddress;

    /**
     * Display address - returns appropriate format based on availability
     */
    String displayAddress;

    BigDecimal latitude;
    BigDecimal longitude;

    /**
     * Address type (OLD or NEW)
     */
    AddressMetadata.AddressType addressType;

    /**
     * Metadata for querying - old structure
     */
    Integer provinceId;
    String provinceName;
    Integer districtId;
    String districtName;
    Integer wardId;
    String wardName;

    /**
     * Metadata for querying - new structure
     */
    String newProvinceCode;
    String newProvinceName;
    String newWardCode;
    String newWardName;

    /**
     * Common metadata
     */
    Integer streetId;
    String streetName;
    Integer projectId;
    String projectName;
    String streetNumber;
}

