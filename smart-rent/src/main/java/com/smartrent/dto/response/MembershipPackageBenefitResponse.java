package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Membership package benefit details")
public class MembershipPackageBenefitResponse {

    @Schema(description = "Benefit ID", example = "1")
    Long benefitId;

    @Schema(
        description = "Benefit type: POST_SILVER, POST_GOLD, POST_DIAMOND, BOOST, AUTO_APPROVE, BADGE",
        example = "POST_SILVER",
        allowableValues = {"POST_SILVER", "POST_GOLD", "POST_DIAMOND", "BOOST", "AUTO_APPROVE", "BADGE"}
    )
    String benefitType;

    @Schema(description = "Display name for the benefit", example = "Silver Posts")
    String benefitNameDisplay;

    @Schema(description = "Quantity granted per month", example = "5")
    Integer quantityPerMonth;

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt;
}

