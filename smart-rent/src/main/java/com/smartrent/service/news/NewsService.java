package com.smartrent.service.news;

import com.smartrent.dto.request.NewsCreateRequest;
import com.smartrent.dto.request.NewsUpdateRequest;
import com.smartrent.dto.response.NewsDetailResponse;
import com.smartrent.dto.response.NewsListResponse;
import com.smartrent.dto.response.NewsResponse;
import com.smartrent.enums.NewsCategory;
import com.smartrent.enums.NewsStatus;

/**
 * Service interface for News and Blog operations
 */
public interface NewsService {

    // ========== Public APIs (for end users) ==========

    /**
     * Get paginated list of published news
     * @param page Page number (1-based)
     * @param size Page size
     * @param category Optional category filter
     * @param tag Optional tag filter
     * @param keyword Optional search keyword
     * @return Paginated news list
     */
    NewsListResponse getPublishedNews(Integer page, Integer size, NewsCategory category, String tag, String keyword);

    /**
     * Get news detail by slug
     * @param slug URL-friendly slug
     * @return News detail with related posts
     */
    NewsDetailResponse getNewsBySlug(String slug);

    // ========== Admin APIs (for content management) ==========

    /**
     * Create a new news/blog post (admin only)
     * @param request News creation request
     * @param adminId Admin ID who creates the news
     * @param adminName Admin display name
     * @return Created news response
     */
    NewsResponse createNews(NewsCreateRequest request, String adminId, String adminName);

    /**
     * Update an existing news/blog post (admin only)
     * @param newsId News ID
     * @param request News update request
     * @return Updated news response
     */
    NewsResponse updateNews(Long newsId, NewsUpdateRequest request);

    /**
     * Publish a news/blog post (admin only)
     * @param newsId News ID
     * @return Published news response
     */
    NewsResponse publishNews(Long newsId);

    /**
     * Unpublish a news/blog post (admin only)
     * @param newsId News ID
     * @return Unpublished news response
     */
    NewsResponse unpublishNews(Long newsId);

    /**
     * Archive a news/blog post (admin only)
     * @param newsId News ID
     * @return Archived news response
     */
    NewsResponse archiveNews(Long newsId);

    /**
     * Delete a news/blog post (admin only)
     * @param newsId News ID
     */
    void deleteNews(Long newsId);

    /**
     * Get all news (admin view) with pagination
     * @param page Page number (1-based)
     * @param size Page size
     * @param status Optional status filter
     * @return Paginated news list
     */
    NewsListResponse getAllNews(Integer page, Integer size, NewsStatus status);

    /**
     * Get news by ID (admin view)
     * @param newsId News ID
     * @return News response
     */
    NewsResponse getNewsById(Long newsId);
}

