package com.smartrent.mapper;

import com.smartrent.dto.request.VideoRequest;
import com.smartrent.dto.response.VideoResponse;
import com.smartrent.infra.repository.entity.Video;

public interface VideoMapper {
    Video toEntity(VideoRequest request);
    VideoResponse toResponse(Video entity);
}
