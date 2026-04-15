package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Listing;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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


    @Query("SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.amenities WHERE l.listingId = :id")
    Optional<Listing> findByIdWithAmenities(@Param("id") Long id);

    @Query("SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.media WHERE l.listingId = :id")
    Optional<Listing> findByIdWithMedia(@Param("id") Long id);

    @Query("SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.amenities WHERE l.listingId IN :ids")
    List<Listing> findByIdsWithAmenities(@Param("ids") Collection<Long> ids);

    @Query("SELECT DISTINCT l FROM listings l LEFT JOIN FETCH l.media WHERE l.listingId IN :ids")
    List<Listing> findByIdsWithMedia(@Param("ids") Collection<Long> ids);

    /**
     * Find listings by ward ID for location-based pricing
     */
    @Query("""
        SELECT l FROM listings l
        JOIN l.address a
        JOIN AddressMetadata am ON am.address.addressId = a.addressId
        WHERE am.wardId = :wardId
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
        JOIN AddressMetadata am ON am.address.addressId = a.addressId
        WHERE am.districtId = :districtId
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
        JOIN AddressMetadata am ON am.address.addressId = a.addressId
        WHERE am.provinceId = :provinceId
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
        JOIN AddressMetadata am ON am.address.addressId = a.addressId
        WHERE am.wardId = :wardId
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
        JOIN AddressMetadata am ON am.address.addressId = a.addressId
        WHERE am.districtId = :districtId
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
        JOIN AddressMetadata am ON am.address.addressId = a.addressId
        WHERE am.provinceId = :provinceId
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
     * Get listing statistics grouped by province (old structure)
     * Returns: provinceId, totalCount, verifiedCount, vipCount
     */
    @Query("""
        SELECT
            am.provinceId,
            COUNT(l.listingId),
            SUM(CASE WHEN l.verified = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType IN ('SILVER', 'GOLD', 'DIAMOND') THEN 1 ELSE 0 END)
        FROM listings l
        JOIN l.address a
        JOIN AddressMetadata am ON am.address.addressId = a.addressId
        WHERE am.provinceId IN :provinceIds
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.expired = false
        GROUP BY am.provinceId
    """)
    List<Object[]> getListingStatsByProvinceIds(@Param("provinceIds") List<Integer> provinceIds);

    /**
     * Get listing statistics grouped by province code (new structure)
     * Returns: provinceCode, totalCount, verifiedCount, vipCount
     */
    @Query("""
        SELECT
            am.newProvinceCode,
            COUNT(l.listingId),
            SUM(CASE WHEN l.verified = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN l.vipType IN ('SILVER', 'GOLD', 'DIAMOND') THEN 1 ELSE 0 END)
        FROM listings l
        JOIN l.address a
        JOIN AddressMetadata am ON am.address.addressId = a.addressId
        WHERE am.newProvinceCode IN :provinceCodes
        AND l.isDraft = false
        AND l.isShadow = false
        AND l.expired = false
        GROUP BY am.newProvinceCode
    """)
    List<Object[]> getListingStatsByProvinceCodes(@Param("provinceCodes") List<String> provinceCodes);

    /**
     * Find listings that need AI verification / moderation.
     *
     * Criteria (initial version):
     * - Not draft and not shadow
     * - Not expired
     * - Not manually verified
     * - Marked as not auto-verified yet (isVerify = false)
     */
    @Query("""
        SELECT l FROM listings l
        WHERE l.isDraft = false
          AND l.isShadow = false
          AND l.expired = false
          AND (l.verified = false OR l.verified IS NULL)
          AND (l.isVerify = false OR l.isVerify IS NULL)
    """)
    List<Listing> findListingsNeedingAiVerification();

    @Query("""
        SELECT l FROM listings l
        WHERE l.productType = :productType
        AND l.listingType = :listingType
        AND l.expired = false
        AND l.verified = true
        AND l.isDraft = false AND l.isShadow = false
        AND l.listingId != :excludeId
        ORDER BY l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForSimilarGlobal(
        @Param("productType") Listing.ProductType productType,
        @Param("listingType") Listing.ListingType listingType,
        @Param("excludeId") Long excludeId,
        Pageable pageable
    );

    @Query("SELECT l FROM listings l LEFT JOIN FETCH l.address WHERE l.listingId = :id")
    Optional<Listing> findByIdWithAddress(@Param("id") Long id);

    @Query("""
        SELECT l FROM listings l
        INNER JOIN FETCH l.address a
        WHERE ((:provinceId IS NOT NULL AND a.legacyProvinceId = :provinceId) OR (:provinceCode IS NOT NULL AND a.newProvinceCode = :provinceCode))
        AND l.productType = :productType
        AND l.listingType = :listingType
        AND l.expired = false
        AND l.verified = true
        AND l.isDraft = false AND l.isShadow = false
        AND l.listingId != :excludeId
        ORDER BY 
            CASE l.vipType 
                WHEN 'DIAMOND' THEN 4 
                WHEN 'GOLD' THEN 3 
                WHEN 'SILVER' THEN 2 
                ELSE 1 
            END DESC,
            l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForSimilar(
        @Param("provinceId") Integer provinceId,
        @Param("provinceCode") String provinceCode,
        @Param("productType") Listing.ProductType productType,
        @Param("listingType") Listing.ListingType listingType,
        @Param("excludeId") Long excludeId,
        Pageable pageable
    );

    @Query("""
        SELECT l FROM listings l
        JOIN FETCH l.address a
        WHERE l.expired = false
        AND l.verified = true
        AND l.isDraft = false AND l.isShadow = false
        AND l.listingId NOT IN :excludeIds
        ORDER BY 
            CASE l.vipType 
                WHEN 'DIAMOND' THEN 4 
                WHEN 'GOLD' THEN 3 
                WHEN 'SILVER' THEN 2 
                ELSE 1 
            END DESC,
            l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForPersonalizedGlobal(
        @Param("excludeIds") List<Long> excludeIds,
        Pageable pageable
    );

    /**
     * Candidates for Personalized Feed
     */
    @Query("""
        SELECT l FROM listings l
        JOIN FETCH l.address a
        WHERE ((:provinceId IS NOT NULL AND a.legacyProvinceId = :provinceId) OR (:provinceCode IS NOT NULL AND a.newProvinceCode = :provinceCode))
        AND l.expired = false
        AND l.verified = true
        AND l.isDraft = false AND l.isShadow = false
        AND l.listingId NOT IN :excludeIds
        ORDER BY 
            CASE l.vipType 
                WHEN 'DIAMOND' THEN 4 
                WHEN 'GOLD' THEN 3 
                WHEN 'SILVER' THEN 2 
                ELSE 1 
            END DESC,
            l.pushedAt DESC NULLS LAST, l.postDate DESC
    """)
    List<Listing> findCandidatesForPersonalized(
        @Param("provinceId") Integer provinceId,
        @Param("provinceCode") String provinceCode,
        @Param("excludeIds") List<Long> excludeIds,
        Pageable pageable
    );
}