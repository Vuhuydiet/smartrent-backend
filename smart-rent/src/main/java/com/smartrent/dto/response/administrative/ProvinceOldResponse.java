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
 * Response DTO for Province in old administrative structure (before July 1, 2025)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Province information (old structure before July 1, 2025)")
public class ProvinceOldResponse {

    @Schema(description = "Province ID", example = "1")
    Long provinceId;

    @Schema(description = "Province name", example = "Hà Nội")
    String name;

    @Schema(description = "Province code", example = "HN")
    String code;

    @Schema(description = "Province type", example = "CITY")
    String type;

    @Schema(description = "Whether the province is active", example = "true")
    Boolean isActive;

    @Schema(description = "Effective start date")
    LocalDate effectiveFrom;

    @Schema(description = "Effective end date (null if still active)")
    LocalDate effectiveTo;

    @Schema(description = "Whether this province was merged into another")
    Boolean isMerged;

    @Schema(description = "Original name before merger (if applicable)")
    String originalName;
}
