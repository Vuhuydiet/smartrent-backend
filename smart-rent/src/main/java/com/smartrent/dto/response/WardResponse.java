package com.smartrent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardResponse {
    private Long wardId;
    private String name;
    private String code;
    private String type;
    private Long districtId;
    private String districtName;
    private Long provinceId;
    private String provinceName;
    private boolean isActive;
}
