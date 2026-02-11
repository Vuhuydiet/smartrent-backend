package com.smartrent.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveExternalMediaRequest {

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be|tiktok\\.com)/.*",
            message = "Only YouTube and TikTok URLs are supported")
    @Size(max = 1000, message = "URL must not exceed 1000 characters")
    private String url;

    private Long listingId; // Optional: associate with listing

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
}