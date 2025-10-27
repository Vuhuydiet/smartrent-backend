package com.smartrent.infra.repository.entity;

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
 * Media entity supporting both uploaded files and external media (YouTube/TikTok)
 * Uses pre-signed URLs for secure upload/download with Cloudflare R2
 */
@Entity(name = "media")
@Table(name = "media",
        indexes = {
                @Index(name = "idx_listing_id", columnList = "listing_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_media_type", columnList = "media_type"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_listing_sort", columnList = "listing_id, sort_order"),
                @Index(name = "idx_storage_key", columnList = "storage_key")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long mediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    Listing listing;

    @Column(name = "user_id", nullable = false)
    String userId; // Owner ID of the media

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    MediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    MediaSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    MediaStatus status;

    // For UPLOAD type: S3 object key
    // For EXTERNAL type: null
    @Column(name = "storage_key", length = 500)
    String storageKey;

    // For UPLOAD type: public CDN URL (if confirmed)
    // For EXTERNAL type: original URL (YouTube, TikTok, etc.)
    @Column(length = 1000)
    String url;

    @Column(name = "original_filename", length = 255)
    String originalFilename;

    @Column(name = "mime_type", length = 100)
    String mimeType;

    @Column(name = "file_size")
    Long fileSize; // in bytes

    @Column(length = 255)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "alt_text", length = 255)
    String altText;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    Boolean isPrimary = false;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    Integer sortOrder = 0;

    // For videos
    @Column(name = "duration_seconds")
    Integer durationSeconds;

    @Column(name = "thumbnail_url", length = 500)
    String thumbnailUrl;

    // For external videos (YouTube/TikTok)
    @Column(name = "embed_code", columnDefinition = "TEXT")
    String embedCode;

    // Upload confirmation tracking
    @Column(name = "upload_confirmed", nullable = false)
    @Builder.Default
    Boolean uploadConfirmed = false;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public enum MediaType {
        IMAGE, VIDEO
    }

    public enum MediaSourceType {
        UPLOAD,     // Uploaded to R2/S3
        YOUTUBE,    // YouTube embed
        TIKTOK,     // TikTok embed
        EXTERNAL    // Other external URL
    }

    public enum MediaStatus {
        PENDING,    // Upload URL generated, awaiting upload
        ACTIVE,     // Confirmed and available
        ARCHIVED,   // Soft deleted
        DELETED     // Hard deleted (marked for cleanup)
    }

    // Helper methods
    public boolean isUploaded() {
        return sourceType == MediaSourceType.UPLOAD;
    }

    public boolean isExternal() {
        return sourceType != MediaSourceType.UPLOAD;
    }

    public boolean isActive() {
        return status == MediaStatus.ACTIVE;
    }

    public boolean isOwnedBy(String userId) {
        return this.userId != null && this.userId.equals(userId);
    }
}
