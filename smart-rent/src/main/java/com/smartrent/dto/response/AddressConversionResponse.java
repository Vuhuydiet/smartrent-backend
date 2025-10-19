package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * Response for address conversion from old to new administrative structure
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressConversionResponse {

    // Original address
    OldAddress oldAddress;

    // Converted address
    NewAddress newAddress;

    // Conversion metadata
    ConversionInfo conversionInfo;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OldAddress {
        String provinceCode;
        String provinceName;
        String districtCode;
        String districtName;
        String wardCode;
        String wardName;
        String fullAddress;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class NewAddress {
        String provinceCode;
        String provinceName;
        String districtCode;
        String districtName;
        String wardCode;
        String wardName;
        String fullAddress;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ConversionInfo {
        Boolean wasConverted;
        String conversionType; // NONE, PROVINCE_MERGE, WARD_MERGE, DISTRICT_MERGE
        LocalDate effectiveDate;
        String notes;
    }
}
