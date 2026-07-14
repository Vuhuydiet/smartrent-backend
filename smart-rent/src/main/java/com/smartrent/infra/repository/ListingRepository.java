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
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing>, ListingRepositoryCustom {
    List<Listing> findByListingIdIn(Collection<Long> listingIds);

    /**
     * Lightweight {@code [listingId, title]} projection for a batch of ids —
     * used by owner analytics to resolve titles for a page of aggregate rows in
     * ONE query instead of a {@code findById} per row (N+1). Avoids loading the
     * full Listing entity (notably the longtext description).
     */
    @Query("SELECT l.listingId, l.title FROM listings l WHERE l.listingId IN :listingIds")
    List<Object[]> findIdAndTitleByListingIdIn(@Param("listingIds") Collection<Long> listingIds);

    /**
     * Fetch listings by id restricted to those currently publicly displayed
     * (same visibility filter as {@link #findPublicListings}). Used so that
     * features like "recently viewed" never surface listings that have since
     * been hidden, expired, unverified, drafted or shadowed.
     */
    @Query("""
        SELECT l FROM listings l
        WHERE l.listingId IN :listingIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
    """)
    List<Listing> findDisplayingByListingIdIn(@Param("listingIds") Collection<Long> listingIds);

    /**
     * Homepage "by category" counts in ONE grouped query. Replaces the previous
     * per-category {@code count(ListingSpecification)} loop (one COUNT per
     * category → N sequential round-trips; ~20s for 5 categories on a cold
     * cache). The WHERE clause MUST mirror the public-visibility predicates that
     * {@link com.smartrent.infra.repository.specification.ListingSpecification#fromFilterRequest}
     * applies for a category card filter (categoryId + excludeExpired, no auth):
     * isDraft=false, isShadow=false, moderationStatus=APPROVED, and not-expired
     * (expired=false AND (expiryDate IS NULL OR expiryDate &gt; now)) — so the
     * homepage number equals the /properties total for that card. Categories
     * with no matching listing are absent from the result (caller defaults them
     * to 0). Served by idx_listings_public_cursor_category (leads with
     * category_id).
     */
    @Query("""
        SELECT new com.smartrent.infra.repository.CategoryListingCount(l.categoryId, COUNT(l))
        FROM listings l
        WHERE l.categoryId IN :categoryIds
          AND l.isDraft = false
          AND l.isShadow = false
          AND l.moderationStatus = :approved
          AND l.expired = false
          AND (l.expiryDate IS NULL OR l.expiryDate > :now)
        GROUP BY l.categoryId
    """)
    List<CategoryListingCount> countPublicListingsByCategory(
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("approved") com.smartrent.enums.ModerationStatus approved,
            @Param("now") LocalDateTime now);

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
     * Map-card hydration: media AND address in a single query, without the
     * amenities join (cards never render amenities). The map-bounds query joins
     * addresses only for its WHERE/sort, so the address association is left lazy
     * on the returned entities — mapping each card would then lazy-load address
     * one listing at a time (an N+1 of up to 200 queries that dominated the
     * map-bounds latency). Fetching it here collapses that into one query.
     */
    @Query("SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.media LEFT JOIN FETCH l.address WHERE l.listingId IN :ids")
    List<Listing> findByIdsWithMediaAndAddress(@Param("ids") Collection<Long> ids);

    /**
     * Map-bounds pin IDs — ordered (VIP-first, then newest, then id) and capped,
     * with the geo index FORCED. The bbox + visibility filter mirrors
     * {@link com.smartrent.infra.repository.specification.ListingSpecification#withinMapBounds}
     * exactly (is_draft/is_shadow/verified/moderation_status/expired + bbox + expiry
     * + optional category).
     *
     * <p>FORCE INDEX is load-bearing: with this ORDER BY + LIMIT shape the optimizer
     * otherwise prefers idx_listings_sort_order and filters row-by-row (measured
     * ~1.5s at city zoom on the small-buffer-pool prod DB, and far worse when zoomed
     * into a sparse area where it scans most of the sort index before hitting the
     * cap). idx_listings_map_bounds is a covering, geo-bounded range scan (~70ms,
     * consistent). A histogram on latitude/longitude does NOT flip the planner — the
     * ORDER BY+LIMIT bias wins — so the hint is required. Returns IDs only; the
     * caller hydrates the managed entities (media+address) by id.
     */
    @Query(value = """
            SELECT l.listing_id
            FROM listings l FORCE INDEX (idx_listings_map_bounds)
            WHERE l.is_draft = 0
              AND l.is_shadow = 0
              AND l.verified = 1
              AND l.moderation_status = 'APPROVED'
              AND l.expired = 0
              AND l.latitude BETWEEN :swLat AND :neLat
              AND l.longitude BETWEEN :swLng AND :neLng
              AND (l.expiry_date IS NULL OR l.expiry_date > NOW())
              AND (:categoryId IS NULL OR l.category_id = :categoryId)
            ORDER BY l.vip_type_sort_order ASC, l.updated_at DESC, l.listing_id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findMapBoundsListingIds(
            @Param("swLat") BigDecimal swLat, @Param("neLat") BigDecimal neLat,
            @Param("swLng") BigDecimal swLng, @Param("neLng") BigDecimal neLng,
            @Param("categoryId") Long categoryId, @Param("limit") int limit);

    /**
     * Total matching pins in the bounds (drives the FE "X tổng, phóng to" hint).
     * Same filter + forced geo index as {@link #findMapBoundsListingIds}; no ORDER BY
     * so it's a plain covering range-count.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM listings l FORCE INDEX (idx_listings_map_bounds)
            WHERE l.is_draft = 0
              AND l.is_shadow = 0
              AND l.verified = 1
              AND l.moderation_status = 'APPROVED'
              AND l.expired = 0
              AND l.latitude BETWEEN :swLat AND :neLat
              AND l.longitude BETWEEN :swLng AND :neLng
              AND (l.expiry_date IS NULL OR l.expiry_date > NOW())
              AND (:categoryId IS NULL OR l.category_id = :categoryId)
            """, nativeQuery = true)
    long countMapBoundsListings(
            @Param("swLat") BigDecimal swLat, @Param("neLat") BigDecimal neLat,
            @Param("swLng") BigDecimal swLng, @Param("neLng") BigDecimal neLng,
            @Param("categoryId") Long categoryId);

    /**
     * Homepage VIP-tier carousel: the latest {@code N} verified, non-draft,
     * non-shadow listings of one tier. Returns a plain {@code List} (not a
     * {@code Page}) so Spring Data does NOT run a COUNT(*) — the carousel only
     * needs the top N, and counting the whole tier (huge for NORMAL) was the
     * main cost of reusing the paginated search. {@code Pageable} supplies only
     * the LIMIT. Backed by idx_listings_public_vip_tier (no filesort).
     *
     * <p>address is to-one, so JOIN FETCH + LIMIT is safe (no in-memory paging).
     *
     * <p>ORDER BY is just {@code updatedAt DESC}: vipType is pinned to a single
     * value, so vip_type_sort_order is constant across the whole result set and
     * adding it to the sort is redundant. Dropping it lets idx_listings_public_vip_tier
     * — which ends in a plain (ascending) updated_at — satisfy the order with a
     * backward index scan on EVERY MySQL version (no dependency on a descending
     * index, no filesort), which is the difference between a 10-row read and
     * sorting the entire (huge) NORMAL tier.
     *
     * <p>PUSH ("đẩy tin") is preserved: pushing bumps updated_at (PushServiceImpl
     * saves the row, updatedAt is @UpdateTimestamp; it also stamps pushed_at/
     * post_date), so a pushed listing rises to the top of {@code updatedAt DESC}
     * exactly as on the old search path. This is the same effective order the
     * homepage search produced (its vipTypeSortOrder key is constant per tier).
     */
    @Query("SELECT l FROM listings l LEFT JOIN FETCH l.address " +
           "WHERE l.vipType = :vipType AND l.verified = true " +
           // Defense-in-depth: verified is the leading equality idx_listings_public_vip_tier
           // (vip_type, verified, is_draft, is_shadow, updated_at) seeks on — keep it so the
           // index scan + backward-order LIMIT still avoids a filesort on the huge NORMAL tier.
           // moderationStatus is the canonical visibility gate (see ListingSpecification);
           // added on top so this query can't drift from it even if verified/moderationStatus
           // ever fall out of sync on a row.
           "AND l.moderationStatus = com.smartrent.enums.ModerationStatus.APPROVED " +
           "AND l.isDraft = false AND l.isShadow = false " +
           "AND l.expired = false " +
           "AND (l.expiryDate IS NULL OR l.expiryDate > CURRENT_TIMESTAMP) " +
           "ORDER BY l.updatedAt DESC")
    List<Listing> findHomepageTier(@Param("vipType") Listing.VipType vipType, Pageable pageable);

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
     * Admin "pending review" queue fast path — POST /v1/listings/admin/list with
     * {@code moderationStatus=PENDING_REVIEW, listingStatus=IN_REVIEW}, the FE's
     * default admin-dashboard tab. Backed by idx_listings_admin_review_queue (V102).
     *
     * <p>FORCE INDEX is load-bearing, same technique as {@link #findMapBoundsListingIds}:
     * with this WHERE + ORDER BY + LIMIT shape the cost-based optimizer instead picks
     * idx_listings_public_cursor_default (built for a different query), which requires
     * a full-row fetch of every is_shadow/moderation_status match (~15k rows measured)
     * before it can even apply the is_verify/verified/expired filter — ~9s. The forced
     * covering index evaluates every predicate except the final row assembly via index
     * condition pushdown. Mirrors the WHERE in ListingSpecification's
     * buildStatusPredicate(IN_REVIEW) + the moderationStatus=PENDING_REVIEW branch —
     * keep both in sync; see {@link com.smartrent.service.listing.ListingQueryService}
     * for the eligibility check that gates this fast path.
     */
    @Query(value = """
            SELECT l.listing_id
            FROM listings l FORCE INDEX (idx_listings_admin_review_queue)
            WHERE l.is_shadow = 0
              AND l.is_verify = 1
              AND l.verified = 0
              AND l.expired = 0
              AND (l.moderation_status = 'PENDING_REVIEW' OR l.moderation_status IS NULL)
              AND (l.expiry_date IS NULL OR l.expiry_date > NOW())
            ORDER BY l.vip_type_sort_order ASC, l.updated_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Long> findAdminPendingReviewQueueIds(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * Total matching rows for {@link #findAdminPendingReviewQueueIds}. Already a
     * covering index scan without forcing (MySQL picks idx_listings_admin_review_queue
     * on its own for the plain COUNT — measured ~22ms) but FORCE INDEX is added anyway
     * so this stays fast even if the optimizer's choice drifts as the table grows.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM listings l FORCE INDEX (idx_listings_admin_review_queue)
            WHERE l.is_shadow = 0
              AND l.is_verify = 1
              AND l.verified = 0
              AND l.expired = 0
              AND (l.moderation_status = 'PENDING_REVIEW' OR l.moderation_status IS NULL)
              AND (l.expiry_date IS NULL OR l.expiry_date > NOW())
            """, nativeQuery = true)
    long countAdminPendingReviewQueue();

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

    long countByCreatedAtBeforeAndIsDraftFalseAndIsShadowFalse(LocalDateTime dateTime);

    @Query(value = "SELECT l.listing_type AS label, COUNT(*) AS cnt FROM listings l " +
            "WHERE l.created_at BETWEEN :start AND :end AND l.is_draft = false AND l.is_shadow = false " +
            "GROUP BY l.listing_type ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countNewListingsByType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT l.product_type AS label, COUNT(*) AS cnt FROM listings l " +
            "WHERE l.created_at BETWEEN :start AND :end AND l.is_draft = false AND l.is_shadow = false " +
            "GROUP BY l.product_type ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countNewListingsByProductType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT CASE WHEN l.verified = true THEN 'VERIFIED' ELSE 'UNVERIFIED' END AS label, COUNT(*) AS cnt " +
            "FROM listings l WHERE l.created_at BETWEEN :start AND :end AND l.is_draft = false AND l.is_shadow = false " +
            "GROUP BY l.verified", nativeQuery = true)
    List<Object[]> countNewListingsByVerification(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.legacyDistrictId = :districtId
        AND (l.newProvinceCode = :provinceCode OR (l.legacyProvinceId = :provinceId AND :provinceId IS NOT NULL))
        AND l.productType = :productType
        AND l.listingType = :listingType
        AND l.listingId <> :excludeId
        AND l.isDraft = false AND l.isShadow = false AND l.verified = true AND l.expired = false
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE (l.newProvinceCode = :provinceCode OR (l.legacyProvinceId = :provinceId AND :provinceId IS NOT NULL))
        AND l.price BETWEEN :minPrice AND :maxPrice
        AND l.productType = :productType
        AND l.listingType = :listingType
        AND l.listingId <> :excludeId
        AND l.isDraft = false AND l.isShadow = false AND l.verified = true AND l.expired = false
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.legacyProvinceId = :provinceId
        AND l.productType = :productType
        AND l.listingType = :listingType
        AND l.listingId <> :excludeId
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.newProvinceCode = :provinceCode
        AND l.productType = :productType
        AND l.listingType = :listingType
        AND l.listingId <> :excludeId
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE (l.newProvinceCode = :provinceCode OR (l.legacyProvinceId = :provinceId AND :provinceId IS NOT NULL))
        AND l.price BETWEEN :minPrice AND :maxPrice
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false AND l.isShadow = false AND l.verified = true AND l.expired = false
        AND (l.expiryDate IS NULL OR l.expiryDate > CURRENT_TIMESTAMP)
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.legacyProvinceId = :provinceId
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        AND (l.expiryDate IS NULL OR l.expiryDate > CURRENT_TIMESTAMP)
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.newProvinceCode = :provinceCode
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        AND (l.expiryDate IS NULL OR l.expiryDate > CURRENT_TIMESTAMP)
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.legacyWardId = :wardId
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        AND (l.expiryDate IS NULL OR l.expiryDate > CURRENT_TIMESTAMP)
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.newWardCode = :wardCode
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        AND (l.expiryDate IS NULL OR l.expiryDate > CURRENT_TIMESTAMP)
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.legacyDistrictId = :districtId
        AND l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        AND (l.expiryDate IS NULL OR l.expiryDate > CURRENT_TIMESTAMP)
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
        SELECT l FROM listings l LEFT JOIN FETCH l.address
        WHERE l.listingId NOT IN :excludedIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.verified = true
        AND l.expired = false
        AND (l.expiryDate IS NULL OR l.expiryDate > CURRENT_TIMESTAMP)
        ORDER BY l.pushedAt DESC, l.postDate DESC
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
