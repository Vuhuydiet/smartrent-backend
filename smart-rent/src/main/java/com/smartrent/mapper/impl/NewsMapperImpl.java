package com.smartrent.mapper.impl;

import com.smartrent.dto.request.NewsCreateRequest;
import com.smartrent.dto.response.NewsDetailResponse;
import com.smartrent.dto.response.NewsResponse;
import com.smartrent.dto.response.NewsSummaryResponse;
import com.smartrent.infra.repository.entity.News;
import com.smartrent.mapper.NewsMapper;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class NewsMapperImpl implements NewsMapper {

    @Override
    public NewsResponse toResponse(News news) {
        if (news == null) {
            return null;
        }

        return NewsResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .slug(news.getSlug())
                .summary(news.getSummary())
                .content(news.getContent())
                .category(news.getCategory())
                .tags(parseTags(news.getTags()))
                .thumbnailUrl(news.getThumbnailUrl())
                .status(news.getStatus())
                .publishedAt(news.getPublishedAt())
                .authorId(news.getAuthorId())
                .authorName(news.getAuthorName())
                .viewCount(news.getViewCount())
                .metaTitle(news.getMetaTitle())
                .metaDescription(news.getMetaDescription())
                .metaKeywords(news.getMetaKeywords())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .build();
    }

    @Override
    public NewsSummaryResponse toSummaryResponse(News news) {
        if (news == null) {
            return null;
        }

        return NewsSummaryResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .slug(news.getSlug())
                .summary(news.getSummary())
                .category(news.getCategory())
                .tags(parseTags(news.getTags()))
                .thumbnailUrl(news.getThumbnailUrl())
                .publishedAt(news.getPublishedAt())
                .authorName(news.getAuthorName())
                .viewCount(news.getViewCount())
                .createdAt(news.getCreatedAt())
                .build();
    }

    @Override
    public NewsDetailResponse toDetailResponse(News news, List<NewsSummaryResponse> relatedNews) {
        if (news == null) {
            return null;
        }

        return NewsDetailResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .slug(news.getSlug())
                .summary(news.getSummary())
                .content(news.getContent())
                .category(news.getCategory())
                .tags(parseTags(news.getTags()))
                .thumbnailUrl(news.getThumbnailUrl())
                .publishedAt(news.getPublishedAt())
                .authorName(news.getAuthorName())
                .viewCount(news.getViewCount())
                .metaTitle(news.getMetaTitle())
                .metaDescription(news.getMetaDescription())
                .metaKeywords(news.getMetaKeywords())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .relatedNews(relatedNews != null ? relatedNews : Collections.emptyList())
                .build();
    }

    @Override
    public News toEntity(NewsCreateRequest request) {
        if (request == null) {
            return null;
        }

        return News.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .content(request.getContent())
                .category(request.getCategory())
                .tags(request.getTags())
                .thumbnailUrl(request.getThumbnailUrl())
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .metaKeywords(request.getMetaKeywords())
                .build();
    }

    /**
     * Parse comma-separated tags string into list
     */
    private List<String> parseTags(String tagsString) {
        if (tagsString == null || tagsString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tagsString.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }
}

