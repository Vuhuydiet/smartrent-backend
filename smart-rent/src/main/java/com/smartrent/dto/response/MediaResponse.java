package com.smartrent.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    private Long mediaId;
    private Long listingId;
    private String userId;
    private String mediaType;
    private String sourceType;
    private String status;
    private String url;
    private String thumbnailUrl;
    private String title;
    private String description;
    private String altText;
    private Boolean isPrimary;
    private Integer sortOrder;
    private Long fileSize;
    private String mimeType;
    private String originalFilename;
    private Integer durationSeconds;
    private Boolean uploadConfirmed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
