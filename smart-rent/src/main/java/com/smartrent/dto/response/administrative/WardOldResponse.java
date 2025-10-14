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
 * Response DTO for Ward in old administrative structure (before July 1, 2025)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Ward information (old structure before July 1, 2025)")
public class WardOldResponse {

    @Schema(description = "Ward ID", example = "1")
    Long wardId;

    @Schema(description = "Ward name", example = "Phường Điện Biên")
    String name;

    @Schema(description = "Ward code", example = "DBF")
    String code;

    @Schema(description = "Ward type", example = "WARD")
    String type;

    @Schema(description = "District ID", example = "1")
    Long districtId;

    @Schema(description = "District name", example = "Quận Ba Đình")
    String districtName;

    @Schema(description = "Province ID", example = "1")
    Long provinceId;

    @Schema(description = "Province name", example = "Hà Nội")
    String provinceName;

    @Schema(description = "Whether the ward is active", example = "true")
    Boolean isActive;

    @Schema(description = "Effective start date")
    LocalDate effectiveFrom;

    @Schema(description = "Effective end date (null if still active)")
    LocalDate effectiveTo;

    @Schema(description = "Whether this ward was merged into another")
    Boolean isMerged;

    @Schema(description = "Original name before merger (if applicable)")
    String originalName;
}
