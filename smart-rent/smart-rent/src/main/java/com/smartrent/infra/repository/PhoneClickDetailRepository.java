package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.PhoneClickDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhoneClickDetailRepository extends JpaRepository<PhoneClickDetail, Long> {

    /**
     * Find all phone clicks for a specific listing
     */
    List<PhoneClickDetail> findByListing_ListingIdOrderByClickedAtDesc(Long listingId);

    /**
     * Find all phone clicks by a specific user
     */
    List<PhoneClickDetail> findByUser_UserIdOrderByClickedAtDesc(String userId);

    /**
     * Check if a user has already clicked on a listing's phone number
     */
    boolean existsByListing_ListingIdAndUser_UserId(Long listingId, String userId);

    /**
     * Find the most recent phone click for a user on a specific listing
     */
    Optional<PhoneClickDetail> findFirstByListing_ListingIdAndUser_UserIdOrderByClickedAtDesc(Long listingId, String userId);

    /**
     * Count total phone clicks for a listing
     */
    long countByListing_ListingId(Long listingId);

    /**
     * Count unique users who clicked on a listing's phone number
     */
    @Query("SELECT COUNT(DISTINCT pc.user.userId) FROM phone_clicks pc WHERE pc.listing.listingId = :listingId")
    long countDistinctUsersByListingId(@Param("listingId") Long listingId);

    /**
     * Get phone clicks for a listing within a date range
     */
    @Query("SELECT pc FROM phone_clicks pc WHERE pc.listing.listingId = :listingId " +
           "AND pc.clickedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY pc.clickedAt DESC")
    List<PhoneClickDetail> findByListingIdAndDateRange(
            @Param("listingId") Long listingId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get all listings a user has clicked phone numbers on
     */
    @Query("SELECT DISTINCT pc.listing.listingId FROM phone_clicks pc WHERE pc.user.userId = :userId")
    List<Long> findListingIdsByUserId(@Param("userId") String userId);

    /**
     * Get phone clicks for all listings owned by a specific user (renter)
     */
    @Query("SELECT pc FROM phone_clicks pc WHERE pc.listing.userId = :ownerId ORDER BY pc.clickedAt DESC")
    List<PhoneClickDetail> findByListingOwnerIdOrderByClickedAtDesc(@Param("ownerId") String ownerId);

    /**
     * Get unique users who clicked on phone numbers for listings owned by a specific user
     */
    @Query("SELECT DISTINCT pc FROM phone_clicks pc " +
           "WHERE pc.listing.listingId = :listingId " +
           "ORDER BY pc.clickedAt DESC")
    List<PhoneClickDetail> findDistinctUsersByListingId(@Param("listingId") Long listingId);

    /**
     * Get unique users who clicked on a specific listing's phone number (paginated)
     */
    @Query("SELECT DISTINCT pc FROM phone_clicks pc " +
           "WHERE pc.listing.listingId = :listingId " +
           "ORDER BY pc.clickedAt DESC")
    Page<PhoneClickDetail> findDistinctUsersByListingId(@Param("listingId") Long listingId, Pageable pageable);

    /**
     * Find all phone clicks by a specific user (paginated)
     */
    Page<PhoneClickDetail> findByUser_UserIdOrderByClickedAtDesc(String userId, Pageable pageable);

    /**
     * Get phone clicks for all listings owned by a specific user (renter) (paginated)
     */
    @Query("SELECT pc FROM phone_clicks pc WHERE pc.listing.userId = :ownerId ORDER BY pc.clickedAt DESC")
    Page<PhoneClickDetail> findByListingOwnerIdOrderByClickedAtDesc(@Param("ownerId") String ownerId, Pageable pageable);

    /**
     * Search phone clicks for listings owned by a specific user by listing title (paginated)
     */
    @Query("SELECT pc FROM phone_clicks pc " +
           "WHERE pc.listing.userId = :ownerId " +
           "AND LOWER(pc.listing.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%')) " +
           "ORDER BY pc.clickedAt DESC")
    Page<PhoneClickDetail> searchByListingOwnerIdAndTitle(
            @Param("ownerId") String ownerId,
            @Param("titleKeyword") String titleKeyword,
            Pageable pageable);

    /**
     * Get distinct user IDs who clicked on a specific listing's phone number (paginated)
     */
    @Query("SELECT DISTINCT pc.user.userId FROM phone_clicks pc " +
           "WHERE pc.listing.listingId = :listingId")
    Page<String> findDistinctUserIdsByListingId(@Param("listingId") Long listingId, Pageable pageable);

    /**
     * Get all phone clicks for a specific listing by a specific user
     */
    @Query("SELECT pc FROM phone_clicks pc " +
           "WHERE pc.listing.listingId = :listingId AND pc.user.userId = :userId " +
           "ORDER BY pc.clickedAt DESC")
    List<PhoneClickDetail> findByListingIdAndUserId(
            @Param("listingId") Long listingId,
            @Param("userId") String userId);

    /**
     * Count distinct users who clicked on a specific listing
     */
    @Query("SELECT COUNT(DISTINCT pc.user.userId) FROM phone_clicks pc " +
           "WHERE pc.listing.listingId = :listingId")
    long countDistinctUsersForListing(@Param("listingId") Long listingId);

    /**
     * Get distinct user IDs who clicked on any listing owned by a specific user (paginated)
     */
    @Query("SELECT DISTINCT pc.user.userId FROM phone_clicks pc " +
           "WHERE pc.listing.userId = :ownerId")
    Page<String> findDistinctUserIdsByListingOwnerId(@Param("ownerId") String ownerId, Pageable pageable);

    /**
     * Get all phone clicks for listings owned by a specific user, filtered by clicking user
     */
    @Query("SELECT pc FROM phone_clicks pc " +
           "WHERE pc.listing.userId = :ownerId AND pc.user.userId = :clickingUserId " +
           "ORDER BY pc.clickedAt DESC")
    List<PhoneClickDetail> findByListingOwnerIdAndClickingUserId(
            @Param("ownerId") String ownerId,
            @Param("clickingUserId") String clickingUserId);
}

