package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Full address response with complete administrative hierarchy (legacy structure)
 * 3-tier structure: Province → District → Ward
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FullAddressResponse {

    LegacyProvinceResponse province;
    LegacyDistrictResponse district;
    LegacyWardResponse ward;
    LegacyStreetResponse street;
}
