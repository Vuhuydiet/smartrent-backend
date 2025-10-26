package com.smartrent.mapper;

import com.smartrent.dto.response.MediaResponse;
import com.smartrent.infra.repository.entity.Media;

public interface MediaMapper {
    MediaResponse toResponse(Media media);
}