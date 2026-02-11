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
@Schema(description = "User's membership benefit with quota information")
public class UserMembershipBenefitResponse {

    @Schema(description = "User benefit ID", example = "1")
    Long userBenefitId;

    @Schema(
        description = "Benefit type: POST_SILVER, POST_GOLD, POST_DIAMOND, PUSH, AUTO_APPROVE, BADGE",
        example = "POST_SILVER",
        allowableValues = {"POST_SILVER", "POST_GOLD", "POST_DIAMOND", "PUSH", "AUTO_APPROVE", "BADGE"}
    )
    String benefitType;

    @Schema(description = "Display name for the benefit", example = "Silver Posts")
    String benefitNameDisplay;

    @Schema(description = "When the benefit was granted")
    LocalDateTime grantedAt;

    @Schema(description = "When the benefit expires")
    LocalDateTime expiresAt;

    @Schema(description = "Total quantity granted", example = "15")
    Integer totalQuantity;

    @Schema(description = "Quantity already used", example = "3")
    Integer quantityUsed;

    @Schema(description = "Quantity remaining", example = "12")
    Integer quantityRemaining;

    @Schema(description = "Benefit status", example = "ACTIVE", allowableValues = {"ACTIVE", "EXPIRED", "CONSUMED"})
    String status;

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt;
}

