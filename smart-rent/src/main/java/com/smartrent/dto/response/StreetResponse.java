package com.smartrent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreetResponse {
    private Long streetId;
    private String name;
    private Long wardId;
    private String wardName;
    private Long districtId;
    private String districtName;
    private Long provinceId;
    private String provinceName;
    private boolean isActive;
}
