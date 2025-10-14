package com.smartrent.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class VideoRequest {

    @NotBlank(message = "Video URL is required")
    String url;

    String title;

    String description;

    Integer durationSeconds;

    String mimeType;

    String thumbnailUrl;

    Integer sortOrder;
}
