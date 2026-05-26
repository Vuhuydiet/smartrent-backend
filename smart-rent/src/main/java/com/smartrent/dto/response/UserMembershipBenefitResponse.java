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
        description = "Benefit type: POST_SILVER, POST_GOLD, POST_DIAMOND, PUSH",
        example = "POST_SILVER",
        allowableValues = {"POST_SILVER", "POST_GOLD", "POST_DIAMOND", "PUSH"}
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

    @Schema(description = "VIP tier code this benefit will create a listing under (NORMAL/SILVER/GOLD/DIAMOND). " +
            "Null when benefitType is non-post (e.g. PUSH).", example = "SILVER")
    String vipTierCode;

    @Schema(description = "Maximum number of images for a listing created with this benefit", example = "10")
    Integer maxImages;

    @Schema(description = "Maximum number of videos for a listing created with this benefit", example = "2")
    Integer maxVideos;

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt;
}
