package com.smartrent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceResponse {
    private Long provinceId;
    private String name;
    private String code;
    private String type;
    private String displayName;
    private boolean isActive;
    private boolean isMerged;
    private boolean isParentProvince;

    // For merged provinces
    private Long parentProvinceId;
    private String originalName;
}
