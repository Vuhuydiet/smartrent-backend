-- V54: Create news and blog table
-- This migration creates the news table for storing news articles, blog posts, market trends, and guides

CREATE TABLE IF NOT EXISTS news (
    news_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(300) NOT NULL UNIQUE,
    summary TEXT,
    content LONGTEXT NOT NULL,
    category ENUM('NEWS', 'BLOG') NOT NULL,
    tags TEXT COMMENT 'Comma-separated tags',
    thumbnail_url VARCHAR(500),
    status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME,
    author_id VARCHAR(36) COMMENT 'Admin ID who created the news',
    author_name VARCHAR(100) COMMENT 'Display name of the author',
    view_count BIGINT NOT NULL DEFAULT 0,
    meta_title VARCHAR(255) COMMENT 'SEO meta title',
    meta_description VARCHAR(500) COMMENT 'SEO meta description',
    meta_keywords VARCHAR(500) COMMENT 'SEO keywords',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_news_slug (slug),
    INDEX idx_news_category (category),
    INDEX idx_news_status (status),
    INDEX idx_news_published_at (published_at),
    INDEX idx_news_category_status (category, status),
    INDEX idx_news_status_published (status, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='News and blog posts table';

-- Add foreign key constraint to admins table (optional, allows NULL for system-generated content)
-- Note: We don't enforce FK constraint to allow flexibility for deleted admins
-- ALTER TABLE news ADD CONSTRAINT fk_news_author FOREIGN KEY (author_id) REFERENCES admins(admin_id) ON DELETE SET NULL;

