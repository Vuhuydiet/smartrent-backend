package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Search response for address queries
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressSearchResponse {

    String query;
    Integer totalResults;
    List<AddressMatch> matches;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AddressMatch {
        String provinceCode;
        String provinceName;
        String districtCode;
        String districtName;
        String wardCode;
        String wardName;
        String fullAddress;
        Double matchScore; // Relevance score 0-1
        Boolean isActive;
        Boolean isMerged;
    }
}
