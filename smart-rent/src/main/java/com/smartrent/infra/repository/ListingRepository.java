package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Listing;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
    List<Listing> findByListingIdIn(Collection<Long> listingIds);

    List<Listing> findByUserId(String userId);

    Optional<Listing> findByParentListingId(Long parentListingId);

    default Optional<Listing> findShadowListingByMainListingId(Long mainListingId) {
        return findByParentListingId(mainListingId);
    }

    /**
     * Find listing by transaction ID for idempotency check
     * @param transactionId Transaction ID
     * @return Optional listing
     */
    Optional<Listing> findByTransactionId(String transactionId);


    @Query("SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.address LEFT JOIN FETCH l.amenities WHERE l.listingId = :id")
    Optional<Listing> findByIdWithAmenities(@Param("id") Long id);

    @Query("SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.media WHERE l.listingId = :id")
    Optional<Listing> findByIdWithMedia(@Param("id") Long id);

    @Query("SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.address LEFT JOIN FETCH l.amenities WHERE l.listingId IN :ids")
    List<Listing> findByIdsWithAmenities(@Param("ids") Collection<Long> ids);

    @Query("SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.media WHERE l.listingId IN :ids")
    List<Listing> findByIdsWithMedia(@Param("ids") Collection<Long> ids);

    /**
     * Find listings by ward ID for location-based pricing
     */
    @Query("""
        SELECT l FROM listings l
        JOIN l.address a
        WHERE a.legacyWardId = :wardId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
        ORDER BY l.price ASC
    """)
    List<Listing> findByWardIdAndProductTypeAndPriceUnit(
        @Param("wardId") Integer wardId,
        @Param("productType") Listing.ProductType productType,
        @Param("priceUnit") Listing.PriceUnit priceUnit,
        Pageable pageable
    );

    /**
     * Find listings by district ID for location-based pricing
     */
    @Query("""
        SELECT l FROM listings l
        JOIN l.address a
        WHERE a.legacyDistrictId = :districtId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
        ORDER BY l.price ASC
    """)
    List<Listing> findByDistrictIdAndProductTypeAndPriceUnit(
        @Param("districtId") Integer districtId,
        @Param("productType") Listing.ProductType productType,
        @Param("priceUnit") Listing.PriceUnit priceUnit,
        Pageable pageable
    );

    /**
     * Find listings by province ID for location-based pricing
     */
    @Query("""
        SELECT l FROM listings l
        JOIN l.address a
        WHERE a.legacyProvinceId = :provinceId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
        ORDER BY l.price ASC
    """)
    List<Listing> findByProvinceIdAndProductTypeAndPriceUnit(
        @Param("provinceId") Integer provinceId,
        @Param("productType") Listing.ProductType productType,
        @Param("priceUnit") Listing.PriceUnit priceUnit,
        Pageable pageable
    );

    /**
     * Get pricing statistics for ward
     */
    @Query("""
        SELECT
            COUNT(l),
            AVG(l.price),
            MIN(l.price),
            MAX(l.price),
            AVG(l.area),
            AVG(CASE WHEN l.area > 0 THEN l.price / l.area ELSE 0 END)
        FROM listings l
        JOIN l.address a
        WHERE a.legacyWardId = :wardId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
    """)
    Object[] getPricingStatisticsByWard(
        @Param("wardId") Integer wardId,
        @Param("productType") Listing.ProductType productType,
        @Param("priceUnit") Listing.PriceUnit priceUnit
    );

    /**
     * Get pricing statistics for district
     */
    @Query("""
        SELECT
            COUNT(l),
            AVG(l.price),
            MIN(l.price),
            MAX(l.price),
            AVG(l.area),
            AVG(CASE WHEN l.area > 0 THEN l.price / l.area ELSE 0 END)
        FROM listings l
        JOIN l.address a
        WHERE a.legacyDistrictId = :districtId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
    """)
    Object[] getPricingStatisticsByDistrict(
        @Param("districtId") Integer districtId,
        @Param("productType") Listing.ProductType productType,
        @Param("priceUnit") Listing.PriceUnit priceUnit
    );

    /**
     * Get pricing statistics for province
     */
    @Query("""
        SELECT
            COUNT(l),
            AVG(l.price),
            MIN(l.price),
            MAX(l.price),
            AVG(l.area),
            AVG(CASE WHEN l.area > 0 THEN l.price / l.area ELSE 0 END)
        FROM listings l
        JOIN l.address a
        WHERE a.legacyProvinceId = :provinceId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
    """)
    Object[] getPricingStatisticsByProvince(
        @Param("provinceId") Integer provinceId,
        @Param("productType") Listing.ProductType productType,
        @Param("priceUnit") Listing.PriceUnit priceUnit
    );

    /**
     * Get admin statistics in a single query instead of 10 separate COUNT queries.
     * Returns: [pendingVerification, verified, expired, rejected, drafts, shadows, normalListings, silverListings, goldListings, diamondListings]
     */
    @Query("""
        SELECT
            SUM(CASE WHEN l.isVerify = true AND l.verified = false THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.verified = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.expired = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.verified = false AND l.isVerify = false THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.isDraft = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.isShadow = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType = 'NORMAL' THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType = 'SILVER' THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType = 'GOLD' THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType = 'DIAMOND' THEN 1 ELSE 0 END)
        FROM listings l
    """)
    List<Object[]> getAdminStatistics();

    /**
     * Get listing statistics grouped by province (old structure)
     * Returns: provinceId, totalCount, verifiedCount, vipCount
     */
    @Query("""
        SELECT
            a.legacyProvinceId,
            COUNT(DISTINCT l.listingId),
            SUM(CASE WHEN l.verified = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType IN ('SILVER', 'GOLD', 'DIAMOND') THEN 1 ELSE 0 END)
        FROM listings l
        JOIN l.address a
        WHERE a.legacyProvinceId IN :provinceIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.expired = false
        GROUP BY a.legacyProvinceId
    """)
    List<Object[]> getListingStatsByProvinceIds(@Param("provinceIds") List<Integer> provinceIds);

    /**
     * Get listing statistics grouped by province (old structure), excluding listings already mapped to new structure.
     * Adding AND a.newProvinceCode IS NULL prevents double-counting listings that have both old and new address codes.
     * Returns: provinceId, totalCount, verifiedCount, vipCount
     */
    @Query("""
        SELECT
            a.legacyProvinceId,
            COUNT(DISTINCT l.listingId),
            SUM(CASE WHEN l.verified = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType IN ('SILVER', 'GOLD', 'DIAMOND') THEN 1 ELSE 0 END)
        FROM listings l
        JOIN l.address a
        WHERE a.legacyProvinceId IN :provinceIds
        AND a.newProvinceCode IS NULL
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.expired = false
        GROUP BY a.legacyProvinceId
    """)
    List<Object[]> getListingStatsByProvinceIdsWithoutNewCode(@Param("provinceIds") List<Integer> provinceIds);

    /**
     * Get listing statistics grouped by province code (new structure)
     * Returns: provinceCode, totalCount, verifiedCount, vipCount
     */
    @Query("""
        SELECT
            a.newProvinceCode,
            COUNT(DISTINCT l.listingId),
            SUM(CASE WHEN l.verified = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType IN ('SILVER', 'GOLD', 'DIAMOND') THEN 1 ELSE 0 END)
        FROM listings l
        JOIN l.address a
        WHERE a.newProvinceCode IN :provinceCodes
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.expired = false
        GROUP BY a.newProvinceCode
    """)
    List<Object[]> getListingStatsByProvinceCodes(@Param("provinceCodes") List<String> provinceCodes);

    /**
     * Get listing statistics grouped by category
     * Returns: categoryId, totalCount, verifiedCount, vipCount
     */
    @Query("""
        SELECT
            l.categoryId,
            COUNT(l.listingId),
            SUM(CASE WHEN l.verified = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType IN ('SILVER', 'GOLD', 'DIAMOND') THEN 1 ELSE 0 END)
        FROM listings l
        WHERE l.categoryId IN :categoryIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.expired = false
        GROUP BY l.categoryId
    """)
    List<Object[]> getListingStatsByCategoryIds(@Param("categoryIds") List<Long> categoryIds);

    /**
     * Get owner statistics in a single query instead of 9 separate COUNT queries.
     * Returns: [drafts, pendingVerification, rejected, active, expired, normalListings, silverListings, goldListings, diamondListings]
     */
    @Query("""
        SELECT
            SUM(CASE WHEN l.isDraft = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.isVerify = true AND l.verified = false AND l.isDraft = false THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.verified = false AND l.isVerify = false AND l.isDraft = false THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.verified = true AND l.expired = false AND l.isDraft = false THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.expired = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType = 'NORMAL' THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType = 'SILVER' THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType = 'GOLD' THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType = 'DIAMOND' THEN 1 ELSE 0 END)
        FROM listings l
        WHERE l.userId = :userId
    """)
    List<Object[]> getOwnerStatistics(@Param("userId") String userId);

    @Query(value = """
        SELECT l FROM listings l JOIN FETCH l.address
        WHERE l.titleNorm LIKE CONCAT(:prefix, '%')
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
    """,
    countQuery = """
        SELECT COUNT(l) FROM listings l
        WHERE l.titleNorm LIKE CONCAT(:prefix, '%')
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
    """)
    Page<Listing> findAutocomplete(@Param("prefix") String prefix, Pageable pageable);

    /**
     * Public listing browse: fetch with address eagerly joined, filtered to public-visible listings only.
     * Uses a separate countQuery to avoid Hibernate in-memory pagination with JOIN FETCH.
     */
    @Query(value = """
        SELECT l FROM listings l JOIN FETCH l.address
        WHERE l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
    """,
    countQuery = """
        SELECT COUNT(l) FROM listings l
        WHERE l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
    """)
    Page<Listing> findPublicListings(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE listings l SET l.expired = true " +
           "WHERE l.expiryDate IS NOT NULL AND l.expiryDate < :now AND l.expired = false")
    int markExpiredListings(@Param("now") LocalDateTime now);

    @Query(value = "SELECT DATE(l.created_at) AS label, COUNT(*) AS cnt " +
            "FROM listings l WHERE l.created_at BETWEEN :start AND :end " +
            "AND l.is_draft = false AND l.is_shadow = false " +
            "GROUP BY DATE(l.created_at) ORDER BY label ASC", nativeQuery = true)
    List<Object[]> countNewListingsByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT DATE_FORMAT(l.created_at, '%Y-%m') AS label, COUNT(*) AS cnt " +
            "FROM listings l WHERE l.created_at BETWEEN :start AND :end " +
            "AND l.is_draft = false AND l.is_shadow = false " +
            "GROUP BY DATE_FORMAT(l.created_at, '%Y-%m') ORDER BY label ASC", nativeQuery = true)
    List<Object[]> countNewListingsByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ─── Recommendation Queries ───

    /**
     * Fetch a single listing with its address eagerly loaded (used by recommendation engine).
     */
    @Query("SELECT l FROM listings l LEFT JOIN FETCH l.address WHERE l.listingId = :id")
    Optional<Listing> findByIdWithAddress(@Param("id") Long id);

    /**
     * Fetch listings by IDs with address eagerly loaded (for recommendation feature extraction).
     */
    @Query("SELECT l FROM listings l LEFT JOIN FETCH l.address WHERE l.listingId IN :ids")
    List<Listing> findWithAddressByListingIds(@Param("ids") Collection<Long> ids);

    /**
     * Candidate pool for "similar listings": same province (legacyProvinceId or newProvinceCode),
     * same productType and listingType, excluding the target listing itself.
     * Active, verified, non-draft, non-expired listings only.
     */
    @Query("""
        SELECT l FROM listings l LEFT JOIN FETCH l.address a
        WHERE (
            (:provinceId IS NOT NULL AND a.legacyProvinceId = :provinceId)
            OR (:provinceCode IS NOT NULL AND a.newProvinceCode = :provinceCode)
        )
        AND l.productType = :productType
        AND l.listingType = :listingType
        AND l.listingId <> :excludeId
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForSimilar(
        @Param("provinceId") Integer provinceId,
        @Param("provinceCode") String provinceCode,
        @Param("productType") Listing.ProductType productType,
        @Param("listingType") Listing.ListingType listingType,
        @Param("excludeId") Long excludeId,
        Pageable pageable
    );

    /**
     * Global fallback candidate pool for "similar listings" (no province filter).
     */
    @Query("""
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.productType = :productType
        AND l.listingType = :listingType
        AND l.listingId <> :excludeId
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForSimilarGlobal(
        @Param("productType") Listing.ProductType productType,
        @Param("listingType") Listing.ListingType listingType,
        @Param("excludeId") Long excludeId,
        Pageable pageable
    );

    /**
     * Candidate pool for personalized feed: preferred province (legacyProvinceId or newProvinceCode),
     * excluding already-interacted listings.
     */
    @Query("""
        SELECT l FROM listings l LEFT JOIN FETCH l.address a
        WHERE (
            (:provinceId IS NOT NULL AND a.legacyProvinceId = :provinceId)
            OR (:provinceCode IS NOT NULL AND a.newProvinceCode = :provinceCode)
        )
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForPersonalized(
        @Param("provinceId") Integer provinceId,
        @Param("provinceCode") String provinceCode,
        @Param("excludedIds") java.util.List<Long> excludedIds,
        Pageable pageable
    );

    /**
     * Global candidate pool for personalized feed (no province filter).
     * Used as Stage-2 top-up or full fallback.
     */
    @Query("""
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForPersonalizedGlobal(
        @Param("excludedIds") java.util.List<Long> excludedIds,
        Pageable pageable
    );
}

