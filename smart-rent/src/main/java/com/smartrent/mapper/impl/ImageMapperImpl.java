package com.smartrent.mapper.impl;

import com.smartrent.dto.request.ImageRequest;
import com.smartrent.dto.response.ImageResponse;
import com.smartrent.infra.repository.entity.Image;
import com.smartrent.mapper.ImageMapper;
import org.springframework.stereotype.Component;

@Component
public class ImageMapperImpl implements ImageMapper {

    @Override
    public Image toEntity(ImageRequest request) {
        if (request == null) {
            return null;
        }

        return Image.builder()
                .url(request.getUrl())
                .altText(request.getAltText())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .mimeType(request.getMimeType())
                .build();
    }

    @Override
    public ImageResponse toResponse(Image entity) {
        if (entity == null) {
            return null;
        }

        return ImageResponse.builder()
                .id(entity.getId())
                .url(entity.getUrl())
                .altText(entity.getAltText())
                .sortOrder(entity.getSortOrder())
                .isPrimary(entity.getIsPrimary())
                .fileSize(entity.getFileSize())
                .mimeType(entity.getMimeType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
