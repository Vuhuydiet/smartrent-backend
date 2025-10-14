package com.smartrent.dto.response.administrative;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * Response DTO for District in old administrative structure (before July 1, 2025)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "District information (old structure before July 1, 2025)")
public class DistrictOldResponse {

    @Schema(description = "District ID", example = "1")
    Long districtId;

    @Schema(description = "District name", example = "Quận Ba Đình")
    String name;

    @Schema(description = "District code", example = "BAD")
    String code;

    @Schema(description = "District type", example = "DISTRICT")
    String type;

    @Schema(description = "Province ID", example = "1")
    Long provinceId;

    @Schema(description = "Province name", example = "Hà Nội")
    String provinceName;

    @Schema(description = "Whether the district is active", example = "true")
    Boolean isActive;

    @Schema(description = "Effective start date")
    LocalDate effectiveFrom;

    @Schema(description = "Effective end date (null if still active)")
    LocalDate effectiveTo;
}
