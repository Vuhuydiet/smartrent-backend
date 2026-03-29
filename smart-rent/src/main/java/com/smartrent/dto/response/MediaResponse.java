package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Media response for listing display")
public class MediaResponse {

    @Schema(description = "Unique identifier of the media", example = "456")
    private Long mediaId;

    @Schema(description = "Type of media", example = "IMAGE", allowableValues = {"IMAGE", "VIDEO"})
    private String mediaType;

    @Schema(description = "Current status of the media", example = "ACTIVE", allowableValues = {"PENDING", "ACTIVE", "DELETED"})
    private String status;

    @Schema(description = "Public URL to access the media", example = "https://pub-xxx.r2.dev/media/user-123/456-property-photo.jpg")
    private String url;

    @Schema(description = "Whether this media is the primary/main media for the listing", example = "true")
    private Boolean isPrimary;

    @Schema(description = "Display order for sorting media (lower numbers appear first)", example = "1")
    private Integer sortOrder;
}