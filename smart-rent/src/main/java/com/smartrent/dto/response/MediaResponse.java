package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Media response containing detailed information about uploaded or external media")
public class MediaResponse {

    @Schema(description = "Unique identifier of the media", example = "456")
    private Long mediaId;

    @Schema(description = "ID of the listing this media is associated with", example = "123", nullable = true)
    private Long listingId;

    @Schema(description = "UUID of the user who owns this media", example = "user-uuid-123")
    private String userId;

    @Schema(description = "Type of media", example = "IMAGE", allowableValues = {"IMAGE", "VIDEO"})
    private String mediaType;

    @Schema(description = "Source of the media", example = "UPLOAD", allowableValues = {"UPLOAD", "YOUTUBE", "TIKTOK"})
    private String sourceType;

    @Schema(description = "Current status of the media", example = "ACTIVE", allowableValues = {"PENDING", "ACTIVE", "DELETED"})
    private String status;

    @Schema(description = "Public URL to access the media (pre-signed URL for uploaded files, direct URL for external media)",
            example = "https://pub-xxx.r2.dev/media/user-123/456-property-photo.jpg")
    private String url;

    @Schema(description = "URL to the thumbnail image (mainly for videos)", example = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg", nullable = true)
    private String thumbnailUrl;

    @Schema(description = "Title or name of the media", example = "Living Room", nullable = true)
    private String title;

    @Schema(description = "Description of the media content", example = "Spacious living room with natural light", nullable = true)
    private String description;

    @Schema(description = "Alternative text for accessibility purposes", example = "Living room photo", nullable = true)
    private String altText;

    @Schema(description = "Whether this media is the primary/main media for the listing", example = "true")
    private Boolean isPrimary;

    @Schema(description = "Display order for sorting media (lower numbers appear first)", example = "1")
    private Integer sortOrder;

    @Schema(description = "File size in bytes (null for external media)", example = "2048576", nullable = true)
    private Long fileSize;

    @Schema(description = "MIME type of the file (null for external media)", example = "image/jpeg", nullable = true)
    private String mimeType;

    @Schema(description = "Original filename when uploaded (null for external media)", example = "property-photo.jpg", nullable = true)
    private String originalFilename;

    @Schema(description = "Video duration in seconds (null for images and external media)", example = "120", nullable = true)
    private Integer durationSeconds;

    @Schema(description = "Whether the upload has been confirmed (for pre-signed upload flow)", example = "true")
    private Boolean uploadConfirmed;

    @Schema(description = "Timestamp when the media was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the media was last updated", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
}
