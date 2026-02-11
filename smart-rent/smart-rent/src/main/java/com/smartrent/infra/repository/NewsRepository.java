package com.smartrent.infra.repository;

import com.smartrent.enums.NewsCategory;
import com.smartrent.enums.NewsStatus;
import com.smartrent.infra.repository.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    /**
     * Find news by slug (for detail page)
     */
    Optional<News> findBySlug(String slug);

    /**
     * Check if slug exists (for uniqueness validation)
     */
    boolean existsBySlug(String slug);

    /**
     * Check if slug exists excluding a specific news ID (for update validation)
     */
    boolean existsBySlugAndNewsIdNot(String slug, Long newsId);

    /**
     * Find published news by category with pagination
     */
    Page<News> findByCategoryAndStatusOrderByPublishedAtDesc(
            NewsCategory category,
            NewsStatus status,
            Pageable pageable
    );

    /**
     * Find published news ordered by published date
     */
    Page<News> findByStatusOrderByPublishedAtDesc(NewsStatus status, Pageable pageable);

    /**
     * Find all news (admin view) with pagination
     */
    Page<News> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find news by status (admin view)
     */
    Page<News> findByStatusOrderByCreatedAtDesc(NewsStatus status, Pageable pageable);

    /**
     * Full-text search in title and summary for published news
     */
    @Query("SELECT n FROM news n WHERE n.status = :status " +
           "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(n.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.publishedAt DESC")
    Page<News> searchPublishedNews(
            @Param("keyword") String keyword,
            @Param("status") NewsStatus status,
            Pageable pageable
    );

    /**
     * Search by category and keyword
     */
    @Query("SELECT n FROM news n WHERE n.status = :status " +
           "AND n.category = :category " +
           "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(n.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.publishedAt DESC")
    Page<News> searchPublishedNewsByCategory(
            @Param("keyword") String keyword,
            @Param("category") NewsCategory category,
            @Param("status") NewsStatus status,
            Pageable pageable
    );

    /**
     * Search by tag
     */
    @Query("SELECT n FROM news n WHERE n.status = :status " +
           "AND LOWER(n.tags) LIKE LOWER(CONCAT('%', :tag, '%')) " +
           "ORDER BY n.publishedAt DESC")
    Page<News> findByTagAndStatus(
            @Param("tag") String tag,
            @Param("status") NewsStatus status,
            Pageable pageable
    );

    /**
     * Find related news by category (excluding current news)
     */
    @Query("SELECT n FROM news n WHERE n.status = :status " +
           "AND n.category = :category " +
           "AND n.newsId != :excludeId " +
           "ORDER BY n.publishedAt DESC")
    List<News> findRelatedByCategory(
            @Param("category") NewsCategory category,
            @Param("excludeId") Long excludeId,
            @Param("status") NewsStatus status,
            Pageable pageable
    );

    /**
     * Find related news by tags (excluding current news)
     */
    @Query("SELECT n FROM news n WHERE n.status = :status " +
           "AND n.newsId != :excludeId " +
           "AND (LOWER(n.tags) LIKE LOWER(CONCAT('%', :tag1, '%')) " +
           "OR LOWER(n.tags) LIKE LOWER(CONCAT('%', :tag2, '%')) " +
           "OR LOWER(n.tags) LIKE LOWER(CONCAT('%', :tag3, '%'))) " +
           "ORDER BY n.publishedAt DESC")
    List<News> findRelatedByTags(
            @Param("tag1") String tag1,
            @Param("tag2") String tag2,
            @Param("tag3") String tag3,
            @Param("excludeId") Long excludeId,
            @Param("status") NewsStatus status,
            Pageable pageable
    );
}

