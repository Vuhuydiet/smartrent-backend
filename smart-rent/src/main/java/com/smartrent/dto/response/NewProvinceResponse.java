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

    String code;
    String name;

    @JsonProperty("name_en")
    String nameEn;

    @JsonProperty("full_name")
    String fullName;

    @JsonProperty("full_name_en")
    String fullNameEn;

    @JsonProperty("code_name")
    String codeName;

    @JsonProperty("administrative_unit_type")
    String administrativeUnitType;
}