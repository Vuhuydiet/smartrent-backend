package com.smartrent.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoResponse {
    Long id;
    String url;
    String title;
    String description;
    Integer durationSeconds;
    Long fileSize;
    String mimeType;
    String thumbnailUrl;
    Integer sortOrder;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
