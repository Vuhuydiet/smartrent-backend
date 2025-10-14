package com.smartrent.dto.request.administrative;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for converting an address from old to new administrative structure
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to convert an address from old to new administrative structure")
public class AddressConversionRequest {

    @NotNull(message = "Old province ID is required")
    @Schema(description = "Province ID in old structure", example = "1", required = true)
    Long oldProvinceId;

    @NotNull(message = "Old district ID is required")
    @Schema(description = "District ID in old structure", example = "5", required = true)
    Long oldDistrictId;

    @NotNull(message = "Old ward ID is required")
    @Schema(description = "Ward ID in old structure", example = "15", required = true)
    Long oldWardId;

    @Schema(description = "Street number (optional)", example = "123")
    String streetNumber;

    @Schema(description = "Street name (optional)", example = "Nguyễn Trãi")
    String streetName;
}
