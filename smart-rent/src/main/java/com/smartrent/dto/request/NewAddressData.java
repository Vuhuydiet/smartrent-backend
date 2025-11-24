package com.smartrent.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * New address data (34 provinces, 2-tier structure)
 * Used in nested address creation request
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewAddressData {

    /**
     * Province code for new structure
     * Example: "01" for Hà Nội, "79" for TP.HCM
     */
    String provinceCode;

    /**
     * Ward code for new structure (includes district info)
     * Example: "00004" for Phường Điện Biên, Ba Đình, Hà Nội
     */
    String wardCode;

    /**
     * Street name or street ID
     * Example: "Nguyễn Trãi" or could reference a street table
     */
    String street;

    /**
     * Check if all required fields are present
     */
    public boolean isValid() {
        return provinceCode != null && !provinceCode.isEmpty() &&
               wardCode != null && !wardCode.isEmpty();
    }
}
