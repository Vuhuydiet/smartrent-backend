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

    @Query("""
        SELECT l FROM listings l
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
}
