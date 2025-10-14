package com.smartrent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresignedUrlRequest {

    @NotBlank(message = "Filename is required")
    String filename;

    @NotBlank(message = "Content type is required")
    @Pattern(
        regexp = "image/(jpeg|png|webp)|video/(mp4|quicktime)",
        message = "Invalid content type. Allowed: image/jpeg, image/png, image/webp, video/mp4, video/quicktime"
    )
    String contentType;

    @Positive(message = "File size must be positive")
    Long fileSize; // in bytes

    // Optional: metadata for the upload
    String altText;
    Boolean isPrimary;
}
