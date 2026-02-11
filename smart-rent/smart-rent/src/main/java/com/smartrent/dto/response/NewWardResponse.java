package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Response DTO for wards in new province structure (after 1/7/2025)
 * In the new structure, wards are directly under provinces without districts.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewWardResponse {

    String code;
    String name;

    @JsonProperty("short_name")
    String shortName;

    String type;

    String key;

    BigDecimal latitude;

    BigDecimal longitude;

    @JsonProperty("province_code")
    String provinceCode;

    @JsonProperty("province_name")
    String provinceName;

    String alias;
}