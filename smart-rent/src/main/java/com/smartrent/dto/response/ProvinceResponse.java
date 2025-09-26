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
public class ProvinceResponse {
    Long provinceId;
    String name;
    String code;
    String type;
    String displayName;
    boolean isActive;
    boolean isMerged;
    boolean isParentProvince;

    // For merged provinces
    Long parentProvinceId;
    String originalName;
}
