package com.smartrent.mapper.impl;

import com.smartrent.dto.request.VideoRequest;
import com.smartrent.dto.response.VideoResponse;
import com.smartrent.infra.repository.entity.Video;
import com.smartrent.mapper.VideoMapper;
import org.springframework.stereotype.Component;

@Component
public class VideoMapperImpl implements VideoMapper {

    @Override
    public Video toEntity(VideoRequest request) {
        if (request == null) {
            return null;
        }

        return Video.builder()
                .url(request.getUrl())
                .title(request.getTitle())
                .description(request.getDescription())
                .durationSeconds(request.getDurationSeconds())
                .mimeType(request.getMimeType())
                .thumbnailUrl(request.getThumbnailUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
    }

    @Override
    public VideoResponse toResponse(Video entity) {
        if (entity == null) {
            return null;
        }

        return VideoResponse.builder()
                .id(entity.getId())
                .url(entity.getUrl())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .durationSeconds(entity.getDurationSeconds())
                .fileSize(entity.getFileSize())
                .mimeType(entity.getMimeType())
                .thumbnailUrl(entity.getThumbnailUrl())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
