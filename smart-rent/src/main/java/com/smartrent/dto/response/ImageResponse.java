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
public class ImageResponse {
    Long id;
    String url;
    String altText;
    Integer sortOrder;
    Boolean isPrimary;
    Long fileSize;
    String mimeType;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
