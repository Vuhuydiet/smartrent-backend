package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Quota status for a specific benefit type")
public class QuotaStatusResponse {

    @Schema(
        description = "Benefit type being checked",
        example = "POST_SILVER",
        allowableValues = {"POST_SILVER", "POST_GOLD", "POST_DIAMOND", "BOOST"}
    )
    String benefitType;

    @Schema(description = "Total quota available (remaining)", example = "12")
    Integer totalAvailable;

    @Schema(description = "Total quota already used", example = "3")
    Integer totalUsed;

    @Schema(description = "Total quota granted", example = "15")
    Integer totalGranted;

    @Schema(description = "Whether user has an active membership", example = "true")
    Boolean hasActiveMembership;

    @Schema(description = "Status message", example = "You have 12 Silver posts available")
    String message;
}

