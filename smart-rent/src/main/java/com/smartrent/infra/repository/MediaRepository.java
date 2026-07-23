package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    /**
     * Find media by ID and user (for ownership validation)
     */
    Optional<Media> findByMediaIdAndUserId(Long mediaId, String userId);

    /**
     * Find media by ID and admin (for admin-owned upload confirmation)
     */
    Optional<Media> findByMediaIdAndAdminId(Long mediaId, String adminId);

    /**
     * Find all media for a listing
     */
    List<Media> findByListing_ListingIdAndStatusOrderBySortOrderAsc(
            Long listingId,
            Media.MediaStatus status
    );

    /**
     * Find all media for a user
     */
    List<Media> findByUserIdAndStatusOrderByCreatedAtDesc(
            String userId,
            Media.MediaStatus status
    );

    /**
     * Find primary media for a listing
     */
    Optional<Media> findByListing_ListingIdAndIsPrimaryTrueAndStatus(
            Long listingId,
            Media.MediaStatus status
    );

    /**
     * Find pending uploads that are not confirmed within a time window
     */
    @Query("SELECT m FROM media m WHERE m.status = 'PENDING' " +
           "AND m.uploadConfirmed = false " +
           "AND m.createdAt < :expiryTime")
    List<Media> findExpiredPendingUploads(@Param("expiryTime") LocalDateTime expiryTime);

    /**
     * Find orphan media (ACTIVE media without listing after expiry time).
     * Avatar uploads live under users/{userId}/avatar/... and stay listing-less forever, so
     * they must be excluded or the cron would delete every user's avatar 24h after upload.
     * Admin-uploaded media (adminId set) is excluded for the same reason: it backs news
     * thumbnails and content images, which the news row references by URL string rather than
     * a Media FK, so this listing-FK-based sweep can't tell they are in use and would delete
     * the R2 objects 24h after a post is published.
     */
    @Query("SELECT m FROM media m WHERE m.status = 'ACTIVE' " +
           "AND m.listing IS NULL " +
           "AND m.adminId IS NULL " +
           "AND m.sourceType = 'UPLOAD' " +
           "AND (m.storageKey IS NULL OR " +
           "     (m.storageKey NOT LIKE 'users/%/avatar/%' AND m.storageKey NOT LIKE 'users/%/broker/%')) " +
           "AND m.createdAt < :expiryTime")
    List<Media> findOrphanActiveMedia(@Param("expiryTime") LocalDateTime expiryTime);

    /**
     * Find media by storage key
     */
    Optional<Media> findByStorageKey(String storageKey);

    /**
     * Count media by listing
     */
    long countByListing_ListingIdAndStatus(Long listingId, Media.MediaStatus status);

    /**
     * Count media by user
     */
    long countByUserIdAndStatus(String userId, Media.MediaStatus status);

    /**
     * Find all images for a listing
     */
    List<Media> findByListing_ListingIdAndMediaTypeAndStatusOrderBySortOrderAsc(
            Long listingId,
            Media.MediaType mediaType,
            Media.MediaStatus status
    );

    /**
     * Delete all media for a listing (cascade)
     */
    @Query("UPDATE media m SET m.status = 'DELETED' WHERE m.listing.listingId = :listingId")
    void markListingMediaAsDeleted(@Param("listingId") Long listingId);

    /**
     * Check if user owns media
     */
    boolean existsByMediaIdAndUserId(Long mediaId, String userId);

    /**
     * Find media by status with pagination (for admin)
     */
    Page<Media> findByStatus(Media.MediaStatus status, Pageable pageable);

    /**
     * Batch-load active media for multiple listings (avoids N+1)
     */
    @Query("SELECT m FROM media m WHERE m.listing.listingId IN :listingIds AND m.status = 'ACTIVE' ORDER BY m.listing.listingId, m.sortOrder ASC")
    List<Media> findActiveMediaByListingIds(@Param("listingIds") Collection<Long> listingIds);
}
