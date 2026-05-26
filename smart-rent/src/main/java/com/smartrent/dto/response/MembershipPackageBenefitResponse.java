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
        description = "Benefit type: POST_SILVER, POST_GOLD, POST_DIAMOND, PUSH",
        example = "POST_SILVER",
        allowableValues = {"POST_SILVER", "POST_GOLD", "POST_DIAMOND", "PUSH"}
    )
    String benefitType;

    @Schema(description = "Display name for the benefit", example = "Silver Posts")
    String benefitNameDisplay;

    @Schema(description = "Quantity granted per month", example = "5")
    Integer quantityPerMonth;

    @Schema(description = "VIP tier code this benefit produces a listing for (NORMAL/SILVER/GOLD/DIAMOND). " +
            "Null when benefitType is non-post (e.g. PUSH).", example = "SILVER")
    String vipTierCode;

    @Schema(description = "Maximum number of images a listing posted with this benefit can have. " +
            "Use this to render the image upload limit in the listing-create UI.", example = "10")
    Integer maxImages;

    @Schema(description = "Maximum number of videos a listing posted with this benefit can have. " +
            "Use this to render the video upload limit in the listing-create UI.", example = "2")
    Integer maxVideos;

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt;
}
