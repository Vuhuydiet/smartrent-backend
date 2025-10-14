package com.smartrent.dto.response.administrative;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Response DTO for batch address conversion
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Result of batch address conversion")
public class BatchConversionResponse {

    @Schema(description = "List of conversion results")
    List<AddressConversionResponse> conversions;

    @Schema(description = "Total number of addresses requested for conversion", example = "50")
    Integer totalRequested;

    @Schema(description = "Number of successful conversions", example = "48")
    Integer successfulConversions;

    @Schema(description = "Number of failed conversions", example = "2")
    Integer failedConversions;

    @Schema(description = "List of error messages for failed conversions")
    List<String> errors;

    @Schema(description = "Overall success rate percentage", example = "96.0")
    Double successRate;
}
