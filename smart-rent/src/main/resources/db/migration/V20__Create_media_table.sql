-- Create media table for secure file management with pre-signed URLs
CREATE TABLE IF NOT EXISTS media (
    media_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT,
    user_id VARCHAR(36) NOT NULL,
    media_type VARCHAR(20) NOT NULL COMMENT 'IMAGE or VIDEO',
    source_type VARCHAR(20) NOT NULL COMMENT 'UPLOAD, YOUTUBE, TIKTOK, or EXTERNAL',
    status VARCHAR(20) NOT NULL COMMENT 'PENDING, ACTIVE, ARCHIVED, or DELETED',
    storage_key VARCHAR(500) COMMENT 'S3/R2 object key for uploaded files',
    url VARCHAR(1000) COMMENT 'Public CDN URL or external URL',
    original_filename VARCHAR(255) COMMENT 'Original filename from upload',
    mime_type VARCHAR(100) COMMENT 'MIME type (e.g., image/jpeg, video/mp4)',
    file_size BIGINT COMMENT 'File size in bytes',
    title VARCHAR(255) COMMENT 'Media title',
    description TEXT COMMENT 'Media description',
    alt_text VARCHAR(255) COMMENT 'Alt text for accessibility',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Is this the primary media for listing',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'Display order',
    duration_seconds INT COMMENT 'Video duration in seconds',
    thumbnail_url VARCHAR(500) COMMENT 'Thumbnail URL for videos',
    embed_code TEXT COMMENT 'Embed code for YouTube/TikTok',
    upload_confirmed BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Has upload been confirmed',
    confirmed_at TIMESTAMP NULL COMMENT 'When upload was confirmed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes for performance
    INDEX idx_listing_id (listing_id),
    INDEX idx_user_id (user_id),
    INDEX idx_media_type (media_type),
    INDEX idx_status (status),
    INDEX idx_listing_sort (listing_id, sort_order),
    INDEX idx_storage_key (storage_key),
    INDEX idx_source_type (source_type),
    INDEX idx_created_at (created_at),

    -- Foreign key constraints
    CONSTRAINT fk_media_listing
        FOREIGN KEY (listing_id)
        REFERENCES listings(listing_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_media_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    -- Check constraints
    CONSTRAINT chk_media_type
        CHECK (media_type IN ('IMAGE', 'VIDEO')),

    CONSTRAINT chk_source_type
        CHECK (source_type IN ('UPLOAD', 'YOUTUBE', 'TIKTOK', 'EXTERNAL')),

    CONSTRAINT chk_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'ARCHIVED', 'DELETED')),

    CONSTRAINT chk_file_size
        CHECK (file_size IS NULL OR file_size > 0)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Media management with secure pre-signed URL support';
