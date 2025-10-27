package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for legacy project structure
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LegacyProjectResponse {
    Integer id;
    String name;
    String nameEn;
    Integer provinceId;
    String provinceName;
    Integer districtId;
    String districtName;
    Double latitude;
    Double longitude;
}
