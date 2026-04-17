package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.SavedListing;
import com.smartrent.infra.repository.entity.SavedListingId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SavedListingRepository extends JpaRepository<SavedListing, SavedListingId> {
    
    List<SavedListing> findByIdUserId(String userId);
    
    List<SavedListing> findByIdListingId(Long listingId);
    
    boolean existsByIdUserIdAndIdListingId(String userId, Long listingId);
    
    void deleteByIdUserIdAndIdListingId(String userId, Long listingId);
    
    @Query("SELECT sl FROM saved_listings sl WHERE sl.id.userId = :userId ORDER BY sl.createdAt DESC")
    List<SavedListing> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    @Query(value = "SELECT sl FROM saved_listings sl WHERE sl.id.userId = :userId ORDER BY sl.createdAt DESC",
           countQuery = "SELECT COUNT(sl) FROM saved_listings sl WHERE sl.id.userId = :userId")
    org.springframework.data.domain.Page<SavedListing> findByUserIdWithDetails(
            @Param("userId") String userId,
            org.springframework.data.domain.Pageable pageable);

    long countByIdUserId(String userId);

    // ─── Owner Analytics: Saved Listings Trend ───

    @Query(value = "SELECT DATE(sl.created_at) AS save_date, COUNT(*) AS save_count " +
            "FROM saved_listings sl " +
            "WHERE sl.listing_id = :listingId " +
            "GROUP BY DATE(sl.created_at) ORDER BY save_date ASC", nativeQuery = true)
    List<Object[]> countSavesGroupedByDate(@Param("listingId") Long listingId);

    @Query(value = "SELECT sl.listing_id, COUNT(*) AS save_count " +
            "FROM saved_listings sl " +
            "JOIN listings l ON sl.listing_id = l.listing_id " +
            "WHERE l.user_id = :ownerId " +
            "GROUP BY sl.listing_id ORDER BY save_count DESC", nativeQuery = true)
    List<Object[]> countSavesPerListingForOwner(@Param("ownerId") String ownerId);

    @Query(value = "SELECT sl.listing_id, COUNT(*) AS save_count " +
            "FROM saved_listings sl " +
            "JOIN listings l ON sl.listing_id = l.listing_id " +
            "WHERE l.user_id = :ownerId " +
            "AND l.is_draft = false " +
            "AND l.is_shadow = false " +
            "AND l.expired = false " +
            "AND l.verified = true " +
            "GROUP BY sl.listing_id " +
            "ORDER BY save_count DESC, sl.listing_id DESC", nativeQuery = true)
    List<Object[]> findTopSavedListingIdsForOwner(
            @Param("ownerId") String ownerId,
            Pageable pageable);

    long countByIdListingId(Long listingId);

    @Query(value = "SELECT DATE(sl.created_at) AS save_date, COUNT(*) AS save_count " +
            "FROM saved_listings sl " +
            "WHERE sl.listing_id = :listingId AND sl.created_at >= :since " +
            "GROUP BY DATE(sl.created_at) ORDER BY save_date ASC", nativeQuery = true)
    List<Object[]> countSavesGroupedByDateSince(@Param("listingId") Long listingId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT sl.listing_id, COUNT(*) AS save_count " +
            "FROM saved_listings sl " +
            "JOIN listings l ON sl.listing_id = l.listing_id " +
            "WHERE l.user_id = :ownerId " +
            "GROUP BY sl.listing_id ORDER BY save_count DESC",
            countQuery = "SELECT COUNT(DISTINCT sl.listing_id) " +
            "FROM saved_listings sl JOIN listings l ON sl.listing_id = l.listing_id " +
            "WHERE l.user_id = :ownerId",
            nativeQuery = true)
    Page<Object[]> countSavesPerListingForOwnerPaged(@Param("ownerId") String ownerId, Pageable pageable);

    @Query(value = "SELECT sl.listing_id, COUNT(*) AS save_count " +
            "FROM saved_listings sl " +
            "JOIN listings l ON sl.listing_id = l.listing_id " +
            "WHERE l.user_id = :ownerId AND LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "GROUP BY sl.listing_id ORDER BY save_count DESC",
            countQuery = "SELECT COUNT(DISTINCT sl.listing_id) " +
            "FROM saved_listings sl JOIN listings l ON sl.listing_id = l.listing_id " +
            "WHERE l.user_id = :ownerId AND LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))",
            nativeQuery = true)
    Page<Object[]> countSavesPerListingForOwnerPagedWithSearch(
            @Param("ownerId") String ownerId, @Param("keyword") String keyword, Pageable pageable);

    // ─── Recommendation Signal Queries ───

    /**
     * Get recent saved listings from ALL users (for CF global interactions).
     * Ordered by createdAt desc so we get the freshest signal.
     */
    @Query("SELECT sl FROM saved_listings sl ORDER BY sl.createdAt DESC")
    List<SavedListing> findRecentGlobalSavedListings(Pageable pageable);
}
