package com.smartrent.service.news.impl;

import com.smartrent.dto.request.NewsCreateRequest;
import com.smartrent.dto.request.NewsUpdateRequest;
import com.smartrent.dto.response.NewsDetailResponse;
import com.smartrent.dto.response.NewsListResponse;
import com.smartrent.dto.response.NewsResponse;
import com.smartrent.dto.response.NewsSummaryResponse;
import com.smartrent.enums.NewsCategory;
import com.smartrent.enums.NewsStatus;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.NewsRepository;
import com.smartrent.infra.repository.entity.News;
import com.smartrent.mapper.NewsMapper;
import com.smartrent.service.news.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    @Override
    @Transactional(readOnly = true)
    public NewsListResponse getPublishedNews(Integer page, Integer size, NewsCategory category, String tag, String keyword) {
        log.info("Getting published news - page: {}, size: {}, category: {}, tag: {}, keyword: {}",
                page, size, category, tag, keyword);

        // Validate pagination
        int pageNumber = (page != null && page > 0) ? page - 1 : 0;
        int pageSize = (size != null && size > 0) ? size : 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<News> newsPage;

        // Apply filters
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (category != null) {
                newsPage = newsRepository.searchPublishedNewsByCategory(
                        keyword.trim(), category, NewsStatus.PUBLISHED, pageable);
            } else {
                newsPage = newsRepository.searchPublishedNews(
                        keyword.trim(), NewsStatus.PUBLISHED, pageable);
            }
        } else if (tag != null && !tag.trim().isEmpty()) {
            newsPage = newsRepository.findByTagAndStatus(tag.trim(), NewsStatus.PUBLISHED, pageable);
        } else if (category != null) {
            newsPage = newsRepository.findByCategoryAndStatusOrderByPublishedAtDesc(
                    category, NewsStatus.PUBLISHED, pageable);
        } else {
            newsPage = newsRepository.findByStatusOrderByPublishedAtDesc(NewsStatus.PUBLISHED, pageable);
        }

        List<NewsSummaryResponse> newsList = newsPage.getContent().stream()
                .map(newsMapper::toSummaryResponse)
                .collect(Collectors.toList());

        return NewsListResponse.builder()
                .news(newsList)
                .totalItems(newsPage.getTotalElements())
                .currentPage(page != null ? page : 1)
                .pageSize(pageSize)
                .totalPages(newsPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public NewsDetailResponse getNewsBySlug(String slug) {
        log.info("Getting news by slug: {}", slug);

        News news = newsRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(DomainCode.NEWS_NOT_FOUND));

        // Check if news is published
        if (!news.isPublished()) {
            log.warn("Attempted to access unpublished news: {}", slug);
            throw new AppException(DomainCode.NEWS_NOT_PUBLISHED);
        }

        // Increment view count
        news.incrementViewCount();
        newsRepository.save(news);

        // Get related news
        List<NewsSummaryResponse> relatedNews = getRelatedNews(news);

        return newsMapper.toDetailResponse(news, relatedNews);
    }

    @Override
    @Transactional
    public NewsResponse createNews(NewsCreateRequest request, String adminId, String adminName) {
        log.info("Creating news - title: {}, category: {}, admin: {}", request.getTitle(), request.getCategory(), adminId);

        // Generate slug from title
        String slug = generateSlug(request.getTitle());

        // Check if slug already exists
        if (newsRepository.existsBySlug(slug)) {
            log.error("News slug already exists: {}", slug);
            throw new AppException(DomainCode.NEWS_SLUG_ALREADY_EXISTS);
        }

        News news = newsMapper.toEntity(request);
        news.setSlug(slug);
        news.setAuthorId(adminId);
        news.setAuthorName(adminName);
        news.setStatus(NewsStatus.DRAFT);
        news.setViewCount(0L);

        news = newsRepository.save(news);
        log.info("News created successfully with ID: {}", news.getNewsId());

        return newsMapper.toResponse(news);
    }

    @Override
    @Transactional
    public NewsResponse updateNews(Long newsId, NewsUpdateRequest request) {
        log.info("Updating news ID: {}", newsId);

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new AppException(DomainCode.NEWS_NOT_FOUND));

        // Update fields if provided
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            String newSlug = generateSlug(request.getTitle());
            if (!newSlug.equals(news.getSlug()) && newsRepository.existsBySlugAndNewsIdNot(newSlug, newsId)) {
                throw new AppException(DomainCode.NEWS_SLUG_ALREADY_EXISTS);
            }
            news.setTitle(request.getTitle());
            news.setSlug(newSlug);
        }

        if (request.getSummary() != null) {
            news.setSummary(request.getSummary());
        }
        if (request.getContent() != null) {
            news.setContent(request.getContent());
        }
        if (request.getCategory() != null) {
            news.setCategory(request.getCategory());
        }
        if (request.getTags() != null) {
            news.setTags(request.getTags());
        }
        if (request.getThumbnailUrl() != null) {
            news.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getMetaTitle() != null) {
            news.setMetaTitle(request.getMetaTitle());
        }
        if (request.getMetaDescription() != null) {
            news.setMetaDescription(request.getMetaDescription());
        }
        if (request.getMetaKeywords() != null) {
            news.setMetaKeywords(request.getMetaKeywords());
        }

        news = newsRepository.save(news);
        log.info("News updated successfully: {}", newsId);

        return newsMapper.toResponse(news);
    }

    @Override
    @Transactional
    public NewsResponse publishNews(Long newsId) {
        log.info("Publishing news ID: {}", newsId);

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new AppException(DomainCode.NEWS_NOT_FOUND));

        news.setStatus(NewsStatus.PUBLISHED);
        if (news.getPublishedAt() == null) {
            news.setPublishedAt(LocalDateTime.now());
        }

        news = newsRepository.save(news);
        log.info("News published successfully: {}", newsId);

        return newsMapper.toResponse(news);
    }

    @Override
    @Transactional
    public NewsResponse unpublishNews(Long newsId) {
        log.info("Unpublishing news ID: {}", newsId);

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new AppException(DomainCode.NEWS_NOT_FOUND));

        news.setStatus(NewsStatus.DRAFT);

        news = newsRepository.save(news);
        log.info("News unpublished successfully: {}", newsId);

        return newsMapper.toResponse(news);
    }

    @Override
    @Transactional
    public NewsResponse archiveNews(Long newsId) {
        log.info("Archiving news ID: {}", newsId);

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new AppException(DomainCode.NEWS_NOT_FOUND));

        news.setStatus(NewsStatus.ARCHIVED);

        news = newsRepository.save(news);
        log.info("News archived successfully: {}", newsId);

        return newsMapper.toResponse(news);
    }

    @Override
    @Transactional
    public void deleteNews(Long newsId) {
        log.info("Deleting news ID: {}", newsId);

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new AppException(DomainCode.NEWS_NOT_FOUND));

        newsRepository.delete(news);
        log.info("News deleted successfully: {}", newsId);
    }

    @Override
    @Transactional(readOnly = true)
    public NewsListResponse getAllNews(Integer page, Integer size, NewsStatus status) {
        log.info("Getting all news (admin) - page: {}, size: {}, status: {}", page, size, status);

        int pageNumber = (page != null && page > 0) ? page - 1 : 0;
        int pageSize = (size != null && size > 0) ? size : 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<News> newsPage;
        if (status != null) {
            newsPage = newsRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            newsPage = newsRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<NewsSummaryResponse> newsList = newsPage.getContent().stream()
                .map(newsMapper::toSummaryResponse)
                .collect(Collectors.toList());

        return NewsListResponse.builder()
                .news(newsList)
                .totalItems(newsPage.getTotalElements())
                .currentPage(page != null ? page : 1)
                .pageSize(pageSize)
                .totalPages(newsPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NewsResponse getNewsById(Long newsId) {
        log.info("Getting news by ID (admin): {}", newsId);

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new AppException(DomainCode.NEWS_NOT_FOUND));

        return newsMapper.toResponse(news);
    }

    // ========== Helper Methods ==========

    /**
     * Generate URL-friendly slug from title
     */
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    /**
     * Get related news based on category and tags
     */
    private List<NewsSummaryResponse> getRelatedNews(News news) {
        List<News> relatedNewsList = new ArrayList<>();
        Pageable limit = PageRequest.of(0, 5);

        // First try to find by category
        List<News> categoryRelated = newsRepository.findRelatedByCategory(
                news.getCategory(), news.getNewsId(), NewsStatus.PUBLISHED, limit);
        relatedNewsList.addAll(categoryRelated);

        // If we need more, try to find by tags
        if (relatedNewsList.size() < 5 && news.getTags() != null && !news.getTags().isEmpty()) {
            List<String> tags = Arrays.asList(news.getTags().split(","));
            if (!tags.isEmpty()) {
                String tag1 = tags.size() > 0 ? tags.get(0).trim() : "";
                String tag2 = tags.size() > 1 ? tags.get(1).trim() : "";
                String tag3 = tags.size() > 2 ? tags.get(2).trim() : "";

                List<News> tagRelated = newsRepository.findRelatedByTags(
                        tag1, tag2, tag3, news.getNewsId(), NewsStatus.PUBLISHED, limit);

                // Add unique items
                for (News tagNews : tagRelated) {
                    if (relatedNewsList.stream().noneMatch(n -> n.getNewsId().equals(tagNews.getNewsId()))) {
                        relatedNewsList.add(tagNews);
                        if (relatedNewsList.size() >= 5) break;
                    }
                }
            }
        }

        return relatedNewsList.stream()
                .limit(5)
                .map(newsMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }
}

