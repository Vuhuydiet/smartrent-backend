package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Image/video quota for a VIP tier, optionally combined with the current usage
 * on a specific listing so the frontend can render "X / maxImages" counters and
 * disable the upload button when full.
 *
 * Returned by:
 *   GET /v1/vip-tiers/{tierCode}/media-limits        (limits only, no usage)
 *   GET /v1/listings/{listingId}/media-limits        (limits + currentImages/currentVideos)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "VIP tier media (image/video) limit and current usage")
public class VipTierMediaLimitResponse {

    @Schema(description = "VIP tier code", example = "SILVER",
            allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"})
    String tierCode;

    @Schema(description = "Maximum number of images allowed by this tier", example = "10")
    Integer maxImages;

    @Schema(description = "Maximum number of videos allowed by this tier", example = "2")
    Integer maxVideos;

    @Schema(description = "Current number of ACTIVE images on the listing. " +
            "Null when the response is for a tier only (no listingId).", example = "3")
    Integer currentImages;

    @Schema(description = "Current number of ACTIVE videos on the listing. " +
            "Null when the response is for a tier only (no listingId).", example = "0")
    Integer currentVideos;

    @Schema(description = "Remaining image slots (maxImages - currentImages). " +
            "Null when currentImages is null.", example = "7")
    Integer remainingImages;

    @Schema(description = "Remaining video slots (maxVideos - currentVideos). " +
            "Null when currentVideos is null.", example = "2")
    Integer remainingVideos;
}
