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
import java.util.List;

/**
 * Response DTO for Ward in new administrative structure (after July 1, 2025)
 * Note: In new structure, wards are directly under provinces (no districts)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Ward information (new structure after July 1, 2025 - directly under province, no district)")
public class WardNewResponse {

    @Schema(description = "Ward ID", example = "1")
    Long wardId;

    @Schema(description = "Ward name", example = "Phường Điện Biên")
    String name;

    @Schema(description = "Ward code", example = "DBF")
    String code;

    @Schema(description = "Ward type", example = "WARD")
    String type;

    @Schema(description = "Province ID (direct parent, no district)", example = "1")
    Long provinceId;

    @Schema(description = "Province name", example = "Hà Nội")
    String provinceName;

    @Schema(description = "Whether the ward is active", example = "true")
    Boolean isActive;

    @Schema(description = "List of ward IDs that were merged into this ward")
    List<Long> mergedFromWardIds;

    @Schema(description = "Names of wards that were merged into this ward")
    List<String> mergedFromWardNames;

    @Schema(description = "Effective start date of this ward in new structure")
    LocalDate effectiveFrom;

    @Schema(description = "Old district name (before reorganization, for reference)")
    String formerDistrictName;
}
