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

@Entity(name = "videos")
@Table(name = "videos",
        indexes = {
                @Index(name = "idx_listing_id", columnList = "listing_id"),
                @Index(name = "idx_listing_sort", columnList = "listing_id, sort_order")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    Listing listing;

    @Column(nullable = false, length = 500)
    String url;

    @Column(length = 200)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "duration_seconds")
    Integer durationSeconds;

    @Column(name = "file_size")
    Long fileSize;

    @Column(name = "mime_type", length = 50)
    String mimeType;

    @Column(name = "thumbnail_url", length = 500)
    String thumbnailUrl;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
