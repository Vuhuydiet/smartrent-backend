package com.smartrent.mapper.impl;

import com.smartrent.dto.response.MediaResponse;
import com.smartrent.infra.repository.entity.Media;
import com.smartrent.mapper.MediaMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MediaMapperImpl implements MediaMapper {

    @Override
    public MediaResponse toResponse(Media media) {
        if (media == null) {
            return null;
        }

        return MediaResponse.builder()
                .mediaId(media.getMediaId())
                .mediaType(media.getMediaType() != null ? media.getMediaType().name() : null)
                .status(media.getStatus() != null ? media.getStatus().name() : null)
                .url(media.getUrl())
                .isPrimary(media.getIsPrimary())
                .sortOrder(media.getSortOrder())
                .build();
    }
}