package com.smartrent.mapper;

import com.smartrent.dto.request.ImageRequest;
import com.smartrent.dto.response.ImageResponse;
import com.smartrent.infra.repository.entity.Image;

public interface ImageMapper {
    Image toEntity(ImageRequest request);
    ImageResponse toResponse(Image entity);
}
