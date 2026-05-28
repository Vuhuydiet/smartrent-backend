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

    /**
     * Find listings that need AI verification.
     *
     * <p>Exclusion rules:
     * <ul>
     *   <li>{@code lam IS NULL} → new listing, never processed → include</li>
     *   <li>{@code lam.verificationStatus = PENDING} AND retryCount < 3 → needs processing → include</li>
     *   <li>{@code l.moderationStatus IN (APPROVED, REJECTED, SUSPENDED, REVISION_REQUIRED)} → already resolved → exclude</li>
     *   <li>{@code l.verified = true} → already approved → exclude</li>
     *   <li>{@code lam.manualOverride = true} → human took over → exclude</li>
     * </ul>
     */
    @Query("""
        SELECT l FROM listings l
        LEFT JOIN listing_ai_moderation lam ON lam.listingId = l.listingId
        WHERE (lam IS NULL OR (lam.verificationStatus = 'PENDING' AND lam.retryCount < 3))
        AND (lam IS NULL OR lam.manualOverride = false)
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.postDate IS NOT NULL
        AND (l.verified IS NULL OR l.verified = false)
        AND (l.moderationStatus IS NULL
             OR l.moderationStatus = com.smartrent.enums.ModerationStatus.PENDING_REVIEW
             OR l.moderationStatus = com.smartrent.enums.ModerationStatus.RESUBMITTED)
        ORDER BY l.createdAt DESC
    """)
    Page<Listing> findListingsNeedingAiVerification(Pageable pageable);


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
     * Get admin statistics in a single query instead of 11 separate COUNT queries.
     * Returns: [pendingVerification, verified, expired, rejected, drafts, shadows,
     *          normalListings, silverListings, goldListings, diamondListings, totalListings]
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
            SUM(CASE WHEN l.vipType = 'DIAMOND' THEN 1 ELSE 0 END),
            COUNT(l)
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
        AND (:verifiedOnly = false OR l.verified = true)
        GROUP BY a.legacyProvinceId
    """)
    List<Object[]> getListingStatsByProvinceIdsWithoutNewCode(
        @Param("provinceIds") List<Integer> provinceIds,
        @Param("verifiedOnly") boolean verifiedOnly);

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
        AND (:verifiedOnly = false OR l.verified = true)
        GROUP BY a.newProvinceCode
    """)
    List<Object[]> getListingStatsByProvinceCodes(
        @Param("provinceCodes") List<String> provinceCodes,
        @Param("verifiedOnly") boolean verifiedOnly);

    /**
     * Get listing statistics grouped by category.
     * Returns: categoryId, totalCount, verifiedCount, vipCount.
     *
     * <p>The count is filtered down to moderation_status = APPROVED to match
     * what the public search endpoint actually surfaces after PR #247 changed
     * the search gate from {@code verified=true} to
     * {@code moderation_status='APPROVED'}. Without this predicate the
     * homepage cards would advertise "38,094 tin đăng" but the listing page
     * underneath would only show the ~30k APPROVED rows — a confusing
     * mismatch. The predicate is also load-bearing for performance: it
     * lets the planner use {@code idx_listings_cat_mod_filter} (V80) instead
     * of falling back to {@code idx_is_shadow}, cutting the homepage stats
     * query from ~1.9 s to ~80 ms on the 100k-row dataset.
     */
    @Query("""
        SELECT
            l.categoryId,
            COUNT(l.listingId),
            SUM(CASE WHEN l.verified = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType IN ('SILVER', 'GOLD', 'DIAMOND') THEN 1 ELSE 0 END)
        FROM listings l
        WHERE l.categoryId IN :categoryIds
        AND l.moderationStatus = com.smartrent.enums.ModerationStatus.APPROVED
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.expired = false
        AND (:verifiedOnly = false OR l.verified = true)
        GROUP BY l.categoryId
    """)
    List<Object[]> getListingStatsByCategoryIds(
        @Param("categoryIds") List<Long> categoryIds,
        @Param("verifiedOnly") boolean verifiedOnly);

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

    /**
     * Find live, public listings whose expiryDate falls inside [start, end].
     * Used by the expiring-soon notification scheduler to build the daily
     * per-owner summary across the full [now, now+7d] window.
     */
    @Query("""
        SELECT l FROM listings l
        WHERE l.expiryDate IS NOT NULL
        AND l.expiryDate BETWEEN :start AND :end
        AND l.expired = false
        AND l.verified = true
        AND l.isDraft = false
        AND l.isShadow = false
    """)
    List<Listing> findExpiringBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);

    /**
     * Public, live listings posted by any of the given user IDs — feeds the
     * "from users I follow" tab. Excludes drafts, shadow rows, and unverified
     * listings so the feed mirrors what those users actually have publicly visible.
     */
    @Query("""
        SELECT l FROM listings l
        LEFT JOIN FETCH l.address
        WHERE l.userId IN :userIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.expired = false
        AND l.verified = true
    """)
    Page<Listing> findPublicListingsByUserIdIn(
        @Param("userIds") Collection<String> userIds,
        Pageable pageable);

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
     * Proximity-based candidate pool (same district) for similar listings multi-channel retrieval.
     */
    @Query("""
        SELECT l FROM listings l JOIN FETCH l.address a
        WHERE (a.newProvinceCode = :provinceCode OR (a.legacyProvinceId = :provinceId AND :provinceId IS NOT NULL))
        AND a.legacyDistrictId = :districtId
        AND l.productType = :productType
        AND l.listingType = :listingType
        AND l.listingId <> :excludeId
        AND l.isDraft = false AND l.isShadow = false AND l.verified = true AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findSimilarProximityCandidates(
        @Param("provinceCode") String provinceCode,
        @Param("provinceId") Integer provinceId,
        @Param("districtId") Integer districtId,
        @Param("productType") Listing.ProductType productType,
        @Param("listingType") Listing.ListingType listingType,
        @Param("excludeId") Long excludeId,
        Pageable pageable
    );

    /**
     * Price-based candidate pool (matching price range) for similar listings multi-channel retrieval.
     */
    @Query("""
        SELECT l FROM listings l JOIN FETCH l.address a
        WHERE (a.newProvinceCode = :provinceCode OR (a.legacyProvinceId = :provinceId AND :provinceId IS NOT NULL))
        AND l.price BETWEEN :minPrice AND :maxPrice
        AND l.productType = :productType
        AND l.listingType = :listingType
        AND l.listingId <> :excludeId
        AND l.isDraft = false AND l.isShadow = false AND l.verified = true AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findSimilarPriceCandidates(
        @Param("provinceCode") String provinceCode,
        @Param("provinceId") Integer provinceId,
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        @Param("productType") Listing.ProductType productType,
        @Param("listingType") Listing.ListingType listingType,
        @Param("excludeId") Long excludeId,
        Pageable pageable
    );

    /**
     * Candidate pool for "similar listings" by legacy province ID.
     */
    @Query("""
        SELECT l FROM listings l LEFT JOIN FETCH l.address a
        WHERE a.legacyProvinceId = :provinceId
        AND l.productType = :productType
        AND l.listingType = :listingType
        AND l.listingId <> :excludeId
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForSimilarByLegacyProvince(
        @Param("provinceId") Integer provinceId,
        @Param("productType") Listing.ProductType productType,
        @Param("listingType") Listing.ListingType listingType,
        @Param("excludeId") Long excludeId,
        Pageable pageable
    );

    /**
     * Candidate pool for "similar listings" by new province code.
     */
    @Query("""
        SELECT l FROM listings l LEFT JOIN FETCH l.address a
        WHERE a.newProvinceCode = :provinceCode
        AND l.productType = :productType
        AND l.listingType = :listingType
        AND l.listingId <> :excludeId
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForSimilarByNewProvince(
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
     * Budget-based candidate pool for personalized feed multi-channel retrieval.
     */
    @Query("""
        SELECT DISTINCT l FROM listings l JOIN FETCH l.address a
        WHERE (a.newProvinceCode = :provinceCode OR (a.legacyProvinceId = :provinceId AND :provinceId IS NOT NULL))
        AND l.price BETWEEN :minPrice AND :maxPrice
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false AND l.isShadow = false AND l.verified = true AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findPersonalizedPriceCandidates(
        @Param("provinceCode") String provinceCode,
        @Param("provinceId") Integer provinceId,
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        @Param("excludedIds") java.util.List<Long> excludedIds,
        Pageable pageable
    );

    /**
     * Candidate pool for personalized feed by legacy province ID.
     */
    @Query("""
        SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.address a
        WHERE a.legacyProvinceId = :provinceId
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForPersonalizedByLegacyProvince(
        @Param("provinceId") Integer provinceId,
        @Param("excludedIds") java.util.List<Long> excludedIds,
        Pageable pageable
    );

    /**
     * Candidate pool for personalized feed by new province code.
     */
    @Query("""
        SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.address a
        WHERE a.newProvinceCode = :provinceCode
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForPersonalizedByNewProvince(
        @Param("provinceCode") String provinceCode,
        @Param("excludedIds") java.util.List<Long> excludedIds,
        Pageable pageable
    );

    /**
     * Candidate pool for personalized feed: Same Legacy Ward
     */
    @Query("""
        SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.address a
        WHERE a.legacyWardId = :wardId
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesByLegacyWard(
        @Param("wardId") Integer wardId,
        @Param("excludedIds") java.util.List<Long> excludedIds,
        Pageable pageable
    );

    /**
     * Candidate pool for personalized feed: Same New Ward
     */
    @Query("""
        SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.address a
        WHERE a.newWardCode = :wardCode
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesByNewWard(
        @Param("wardCode") String wardCode,
        @Param("excludedIds") java.util.List<Long> excludedIds,
        Pageable pageable
    );

    /**
     * Candidate pool for personalized feed: Same District
     */
    @Query("""
        SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.address a
        WHERE a.legacyDistrictId = :districtId
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesByDistrict(
        @Param("districtId") Integer districtId,
        @Param("excludedIds") java.util.List<Long> excludedIds,
        Pageable pageable
    );

    /**
     * Global candidate pool for personalized feed (no province filter).
     * Used as Stage-2 top-up or full fallback.
     */
    @Query("""
        SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.address
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

    /**
     * Get the pushed_at timestamps of recently pushed listings for push limiting feature.
     * Use Pageable to limit the results.
     */
    @Query("SELECT l.pushedAt FROM listings l WHERE l.pushedAt >= :since AND l.isDraft = false AND l.isShadow = false AND l.expired = false ORDER BY l.pushedAt DESC")
    List<LocalDateTime> findRecentPushedTimesSince(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Full-text title suggestion candidates for the search-suggestions endpoint.
     * <p>
     * Uses a native query (projected columns only) for maximum performance —
     * no full entity hydration, no JOIN FETCH overhead.
     * <p>
     * Public visibility is enforced at the query layer:
     * excludes drafts, shadow listings, unverified, and expired listings.
     *
     * @param fulltextQuery MySQL boolean-mode full-text query generated from normalized user input
     * @param provinceId Legacy province ID integer; {@code null} means no province filter
     * @param categoryId Category ID; {@code null} means no category filter
     * @param limit      Maximum rows to return (already clamped by the service layer)
     * @return Raw tuples: [listing_id, title, full_address, full_newaddress, legacy_province_id, new_province_code]
     */
    @Query(nativeQuery = true, value = """
        SELECT   l.listing_id,
                 l.title,
                 a.full_address,
                 a.full_newaddress,
                 a.legacy_province_id,
                 a.new_province_code,
                 MATCH(l.title_norm) AGAINST(:fulltextQuery IN BOOLEAN MODE) AS relevance
        FROM     listings l
        JOIN     addresses a ON l.address_id = a.address_id
        WHERE    MATCH(l.title_norm) AGAINST(:fulltextQuery IN BOOLEAN MODE) > 0
          AND    (:provinceId IS NULL OR a.legacy_province_id = :provinceId)
          AND    (:categoryId IS NULL OR l.category_id        = :categoryId)
          AND    l.is_draft  = false
          AND    l.is_shadow = false
          AND    l.verified  = true
          AND    l.expired   = false
        ORDER BY relevance DESC,
                 l.vip_type_sort_order ASC,
                 l.pushed_at          DESC
        LIMIT    :lim
        """)
    List<Object[]> findTitleSuggestions(
        @Param("fulltextQuery") String  fulltextQuery,
        @Param("provinceId")    Integer provinceId,
        @Param("categoryId")    Long    categoryId,
        @Param("lim")           int     limit
    );

    @Query(nativeQuery = true, value = """
        SELECT   l.listing_id,
                 l.title,
                 a.full_address,
                 a.full_newaddress
        FROM     listings l
        JOIN     addresses a ON l.address_id = a.address_id
        WHERE    l.phonetic_title LIKE CONCAT('%', :phoneticQuery, '%')
          AND    l.phonetic_title IS NOT NULL
          AND    (:provinceId IS NULL OR a.legacy_province_id = :provinceId)
          AND    (:categoryId IS NULL OR l.category_id        = :categoryId)
          AND    l.is_draft  = false
          AND    l.is_shadow = false
          AND    l.verified  = true
          AND    l.expired   = false
        ORDER BY l.vip_type_sort_order ASC,
                 l.pushed_at          DESC,
                 l.post_date          DESC
        LIMIT    :lim
        """)
    List<Object[]> findPhoneticTitleSuggestions(
        @Param("phoneticQuery") String  phoneticQuery,
        @Param("provinceId")    Integer provinceId,
        @Param("categoryId")    Long    categoryId,
        @Param("lim")           int     limit
    );
}
