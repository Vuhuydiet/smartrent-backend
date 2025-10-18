package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Listing;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByListingIdIn(Collection<Long> listingIds);

    List<Listing> findByUserId(String userId);

    Optional<Listing> findShadowListingByMainListingId(Long mainListingId);



    @Query("SELECT l FROM listings l LEFT JOIN FETCH l.amenities WHERE l.listingId = :id")
    Optional<Listing> findByIdWithAmenities(@Param("id") Long id);

    @Query("SELECT l FROM listings l LEFT JOIN FETCH l.amenities WHERE l.listingId IN :ids")
    List<Listing> findByIdsWithAmenities(@Param("ids") Collection<Long> ids);

    /**
     * Find listings by ward ID for location-based pricing
     */
    @Query("""
        SELECT l FROM listings l
        JOIN addresses a ON l.addressId = a.addressId
        WHERE a.ward.wardId = :wardId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
        ORDER BY l.price ASC
    """)
    List<Listing> findByWardIdAndProductTypeAndPriceUnit(
        @Param("wardId") Long wardId,
        @Param("productType") Listing.ProductType productType,
        @Param("priceUnit") Listing.PriceUnit priceUnit,
        Pageable pageable
    );

    /**
     * Find listings by district ID for location-based pricing
     */
    @Query("""
        SELECT l FROM listings l
        JOIN addresses a ON l.addressId = a.addressId
        WHERE a.district.districtId = :districtId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
        ORDER BY l.price ASC
    """)
    List<Listing> findByDistrictIdAndProductTypeAndPriceUnit(
        @Param("districtId") Long districtId,
        @Param("productType") Listing.ProductType productType,
        @Param("priceUnit") Listing.PriceUnit priceUnit,
        Pageable pageable
    );

    /**
     * Find listings by province ID for location-based pricing
     */
    @Query("""
        SELECT l FROM listings l
        JOIN addresses a ON l.addressId = a.addressId
        WHERE a.province.provinceId = :provinceId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
        ORDER BY l.price ASC
    """)
    List<Listing> findByProvinceIdAndProductTypeAndPriceUnit(
        @Param("provinceId") Long provinceId,
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
        JOIN addresses a ON l.addressId = a.addressId
        WHERE a.ward.wardId = :wardId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
    """)
    Object[] getPricingStatisticsByWard(
        @Param("wardId") Long wardId,
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
        JOIN addresses a ON l.addressId = a.addressId
        WHERE a.district.districtId = :districtId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
    """)
    Object[] getPricingStatisticsByDistrict(
        @Param("districtId") Long districtId,
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
        JOIN addresses a ON l.addressId = a.addressId
        WHERE a.province.provinceId = :provinceId
        AND l.expired = false
        AND l.productType = :productType
        AND l.priceUnit = :priceUnit
    """)
    Object[] getPricingStatisticsByProvince(
        @Param("provinceId") Long provinceId,
        @Param("productType") Listing.ProductType productType,
        @Param("priceUnit") Listing.PriceUnit priceUnit
    );
}