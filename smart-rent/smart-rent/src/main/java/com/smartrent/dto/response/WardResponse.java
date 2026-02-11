package com.smartrent.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WardResponse {
    Long wardId;
    String name;
    String code;
    String type;
    Long districtId;
    String districtName;
    Long provinceId;
    String provinceName;
    boolean isActive;

    // External API integration fields
    String externalId;           // ID from external API (tinhthanhpho.com)
    String districtExternalId;   // External district ID
    String fullName;             // Full administrative name
    String codeName;             // Code name for URL-friendly format
}
