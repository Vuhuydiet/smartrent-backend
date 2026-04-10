package com.smartrent.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateUploadUrlRequest {

    @NotNull(message = "Media type is required")
    private MediaType mediaType; // IMAGE or VIDEO

    @NotNull(message = "Purpose is required")
    private Purpose purpose; // LISTING or AVATAR

    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename must not exceed 255 characters")
    private String filename;

    @NotBlank(message = "Content type is required")
    @Pattern(regexp = "^(image/(jpeg|png|webp)|video/(mp4|webm|quicktime))$",
            message = "Invalid content type. Allowed: image/jpeg, image/png, image/webp, video/mp4, video/webm, video/quicktime")
    private String contentType;

    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be at least 1 byte")
    @Max(value = 104857600, message = "File size must not exceed 100MB")
    private Long fileSize;

    private Long listingId; // Required when purpose=LISTING, must be null otherwise

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    private String altText;

    @lombok.Builder.Default
    private Boolean isPrimary = false;

    @lombok.Builder.Default
    private Integer sortOrder = 0;

    public enum MediaType {
        IMAGE, VIDEO
    }

    public enum Purpose {
        LISTING, AVATAR
    }
}
