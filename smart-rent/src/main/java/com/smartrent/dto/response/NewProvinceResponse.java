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
 * Response DTO for new province structure (34 provinces after 1/7/2025)
 * This represents the simplified administrative structure.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewProvinceResponse {

    String id;
    String name;

    @JsonProperty("short_name")
    String shortName;

    String key;

    BigDecimal latitude;

    BigDecimal longitude;

    String alias;
}