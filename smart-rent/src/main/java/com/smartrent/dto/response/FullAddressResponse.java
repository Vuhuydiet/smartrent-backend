package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Full address response with complete administrative hierarchy
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FullAddressResponse {

    ProvinceInfo province;
    DistrictInfo district;
    WardInfo ward;
    String fullAddressText;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ProvinceInfo {
        String code;
        String name;
        String type;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DistrictInfo {
        String code;
        String name;
        String type;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WardInfo {
        String code;
        String name;
        String type;
    }
}
