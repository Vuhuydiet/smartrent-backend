package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for legacy ward structure (before July 1, 2025)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LegacyWardResponse {
    Integer id;
    String name;
    String nameEn;
    String prefix;
    Integer provinceId;
    String provinceName;
    Integer districtId;
    String districtName;
}