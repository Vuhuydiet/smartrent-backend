package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for address search results in new structure (after 1/7/2025)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewAddressSearchResponse {

    String code;
    String name;
    String type;

    @JsonProperty("province_code")
    String provinceCode;

    @JsonProperty("province_name")
    String provinceName;

    // Full address text for display
    @JsonProperty("full_address")
    String fullAddress;
}