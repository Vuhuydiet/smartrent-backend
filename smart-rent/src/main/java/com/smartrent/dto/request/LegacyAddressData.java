package com.smartrent.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Legacy address data (63 provinces, 3-tier structure)
 * Used in nested address creation request
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LegacyAddressData {

    /**
     * Province ID for old structure
     * Example: 1 for Hà Nội, 79 for TP.HCM
     */
    Integer provinceId;

    /**
     * District ID for old structure
     * Example: 5 for Quận Ba Đình
     */
    Integer districtId;

    /**
     * Ward ID for old structure
     * Example: 20 for Phường Điện Biên
     */
    Integer wardId;

    /**
     * Street name or street ID
     * Example: "Nguyễn Trãi" or could reference a street table
     */
    String street;

    /**
     * Check if all required fields are present
     */
    public boolean isValid() {
        return provinceId != null && districtId != null && wardId != null;
    }
}
