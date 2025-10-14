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
 * Response DTO for ward merger history
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Ward merger history information")
public class WardMergeHistoryResponse {

    @Schema(description = "Ward ID", example = "1")
    Long wardId;

    @Schema(description = "Current ward name", example = "Phường Điện Biên")
    String currentName;

    @Schema(description = "Ward code", example = "DBF")
    String code;

    @Schema(description = "Whether this ward was merged into another", example = "false")
    Boolean isMerged;

    @Schema(description = "Date when this ward was merged (if applicable)")
    LocalDate mergedDate;

    @Schema(description = "Original name before merger (if applicable)")
    String originalName;

    @Schema(description = "Parent ward ID (if this ward was merged into another)")
    Long parentWardId;

    @Schema(description = "Parent ward name (if this ward was merged into another)")
    String parentWardName;

    @Schema(description = "List of wards that were merged into this ward")
    List<MergedWardInfo> mergedWards;

    @Schema(description = "Total number of wards merged into this ward", example = "2")
    Integer totalMergedWards;

    @Schema(description = "Old district name (before reorganization)", example = "Quận Ba Đình")
    String oldDistrictName;

    @Schema(description = "New province name (after reorganization, direct parent)", example = "Hà Nội")
    String newProvinceName;

    @Schema(description = "Administrative structure version", example = "BOTH")
    String structureVersion;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Information about a merged ward")
    public static class MergedWardInfo {

        @Schema(description = "Merged ward ID", example = "102")
        Long wardId;

        @Schema(description = "Original name of merged ward", example = "Phường Kim Mã")
        String originalName;

        @Schema(description = "Current name (should match parent)", example = "Phường Điện Biên")
        String currentName;

        @Schema(description = "Date when merger occurred", example = "2025-07-01")
        LocalDate mergedDate;

        @Schema(description = "Ward code", example = "KMF")
        String code;

        @Schema(description = "Former district name", example = "Quận Ba Đình")
        String formerDistrictName;
    }
}
