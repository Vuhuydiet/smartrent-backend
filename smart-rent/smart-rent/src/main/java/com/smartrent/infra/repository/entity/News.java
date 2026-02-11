package com.smartrent.infra.repository.entity;

import com.smartrent.enums.NewsCategory;
import com.smartrent.enums.NewsStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * News and Blog entity
 * Stores news articles, blog posts, market trends, and guides
 */
@Entity(name = "news")
@Table(name = "news",
        indexes = {
                @Index(name = "idx_news_slug", columnList = "slug", unique = true),
                @Index(name = "idx_news_category", columnList = "category"),
                @Index(name = "idx_news_status", columnList = "status"),
                @Index(name = "idx_news_published_at", columnList = "published_at"),
                @Index(name = "idx_news_category_status", columnList = "category, status"),
                @Index(name = "idx_news_status_published", columnList = "status, published_at")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class News {

    @Id
    @Column(name = "news_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long newsId;

    @Column(name = "title", nullable = false, length = 255)
    String title;

    @Column(name = "slug", nullable = false, unique = true, length = 300)
    String slug;

    @Column(name = "summary", columnDefinition = "TEXT")
    String summary;

    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    NewsCategory category;

    @Column(name = "tags", columnDefinition = "TEXT")
    String tags; // Comma-separated tags

    @Column(name = "thumbnail_url", length = 500)
    String thumbnailUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    NewsStatus status = NewsStatus.DRAFT;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @Column(name = "author_id", length = 36)
    String authorId; // Admin ID who created the news

    @Column(name = "author_name", length = 100)
    String authorName; // Display name of the author

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    Long viewCount = 0L;

    @Column(name = "meta_title", length = 255)
    String metaTitle; // SEO meta title

    @Column(name = "meta_description", length = 500)
    String metaDescription; // SEO meta description

    @Column(name = "meta_keywords", length = 500)
    String metaKeywords; // SEO keywords

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    /**
     * Check if this news is published and visible to public
     */
    public boolean isPublished() {
        return status == NewsStatus.PUBLISHED && publishedAt != null && !publishedAt.isAfter(LocalDateTime.now());
    }

    /**
     * Increment view count
     */
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0L : this.viewCount) + 1;
    }
}

