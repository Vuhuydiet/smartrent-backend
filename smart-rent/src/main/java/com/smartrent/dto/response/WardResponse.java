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
}
