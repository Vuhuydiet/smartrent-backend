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
 * Response DTO for province merger history
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Province merger history information")
public class ProvinceMergeHistoryResponse {

    @Schema(description = "Province ID", example = "1")
    Long provinceId;

    @Schema(description = "Current province name", example = "Hà Nội")
    String currentName;

    @Schema(description = "Province code", example = "HN")
    String code;

    @Schema(description = "Whether this province was merged into another", example = "false")
    Boolean isMerged;

    @Schema(description = "Date when this province was merged (if applicable)")
    LocalDate mergedDate;

    @Schema(description = "Original name before merger (if applicable)", example = "Hà Tây")
    String originalName;

    @Schema(description = "Parent province ID (if this province was merged into another)")
    Long parentProvinceId;

    @Schema(description = "Parent province name (if this province was merged into another)", example = "Hà Nội")
    String parentProvinceName;

    @Schema(description = "List of provinces that were merged into this province")
    List<MergedProvinceInfo> mergedProvinces;

    @Schema(description = "Total number of provinces merged into this province", example = "1")
    Integer totalMergedProvinces;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Information about a merged province")
    public static class MergedProvinceInfo {

        @Schema(description = "Merged province ID", example = "65")
        Long provinceId;

        @Schema(description = "Original name of merged province", example = "Hà Tây")
        String originalName;

        @Schema(description = "Current name (should match parent)", example = "Hà Nội")
        String currentName;

        @Schema(description = "Date when merger occurred", example = "2008-05-29")
        LocalDate mergedDate;

        @Schema(description = "Province code", example = "HT")
        String code;
    }
}
