package com.smartrent.mapper;

import com.smartrent.dto.request.NewsCreateRequest;
import com.smartrent.dto.response.NewsDetailResponse;
import com.smartrent.dto.response.NewsResponse;
import com.smartrent.dto.response.NewsSummaryResponse;
import com.smartrent.infra.repository.entity.News;

import java.util.List;

/**
 * Mapper interface for News entity
 */
public interface NewsMapper {

    /**
     * Map News entity to NewsResponse
     */
    NewsResponse toResponse(News news);

    /**
     * Map News entity to NewsSummaryResponse (without full content)
     */
    NewsSummaryResponse toSummaryResponse(News news);

    /**
     * Map News entity to NewsDetailResponse with related news
     */
    NewsDetailResponse toDetailResponse(News news, List<NewsSummaryResponse> relatedNews);

    /**
     * Map NewsCreateRequest to News entity
     */
    News toEntity(NewsCreateRequest request);
}

