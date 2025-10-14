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
 * Response DTO for Province in new administrative structure (after July 1, 2025)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Province information (new structure after July 1, 2025)")
public class ProvinceNewResponse {

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

    @Schema(description = "List of province IDs that were merged into this province")
    List<Long> mergedFromProvinceIds;

    @Schema(description = "Names of provinces that were merged into this province")
    List<String> mergedFromProvinceNames;

    @Schema(description = "Effective start date of this province in new structure")
    LocalDate effectiveFrom;

    @Schema(description = "Number of wards directly under this province (no districts)")
    Integer wardCount;
}
