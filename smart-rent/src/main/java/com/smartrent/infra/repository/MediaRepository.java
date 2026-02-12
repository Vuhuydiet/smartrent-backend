package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Media;
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
     * Find orphan media (ACTIVE media without listing after expiry time)
     * These are media that were uploaded but never attached to a listing
     */
    @Query("SELECT m FROM media m WHERE m.status = 'ACTIVE' " +
           "AND m.listing IS NULL " +
           "AND m.sourceType = 'UPLOAD' " +
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
     * Batch-load active media for multiple listings (avoids N+1)
     */
    @Query("SELECT m FROM media m WHERE m.listing.listingId IN :listingIds AND m.status = 'ACTIVE' ORDER BY m.listing.listingId, m.sortOrder ASC")
    List<Media> findActiveMediaByListingIds(@Param("listingIds") Collection<Long> listingIds);
}
