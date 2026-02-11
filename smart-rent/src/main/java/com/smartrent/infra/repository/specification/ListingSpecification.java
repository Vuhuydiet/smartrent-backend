package com.smartrent.infra.repository.specification;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.MyListingsFilterRequest;
import com.smartrent.infra.repository.entity.Address;
import com.smartrent.infra.repository.entity.AddressMetadata;
import com.smartrent.infra.repository.entity.Amenity;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.Media;
import com.smartrent.infra.repository.entity.PricingHistory;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.util.TextNormalizer;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic Listing queries
 * Supports unified filtering for all listing search scenarios
 */
public class ListingSpecification {

    /**
     * Build unified specification from filter request
     * Handles both public search and my listings in a single specification
     */
    public static Specification<Listing> fromFilterRequest(ListingFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ============ USER & OWNERSHIP FILTERS ============
            // User ID filter - if provided, search listings of specific user (my listings)
            if (filter.getUserId() != null && !filter.getUserId().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), filter.getUserId()));
            }

            // Draft status filter
            if (filter.getIsDraft() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isDraft"), filter.getIsDraft()));
            } else if (filter.getUserId() == null && !Boolean.TRUE.equals(filter.getIsAdminRequest())) {
                // For public search (userId null AND not admin), exclude drafts by default
                // Admin requests can see drafts if they want to
                predicates.add(criteriaBuilder.equal(root.get("isDraft"), false));
            }

            // Verified filter
            if (filter.getVerified() != null) {
                predicates.add(criteriaBuilder.equal(root.get("verified"), filter.getVerified()));
            } else if (filter.getUserId() == null && !Boolean.TRUE.equals(filter.getIsAdminRequest())) {
                // For public search (userId null AND not admin), only show verified listings
                // This ensures unverified/pending/rejected listings are NOT visible to public users
                // Admin requests (isAdminRequest=true) can see ALL listings regardless of verification status
                predicates.add(criteriaBuilder.equal(root.get("verified"), true));
            }

            // Verification pending filter
            if (filter.getIsVerify() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isVerify"), filter.getIsVerify()));
            }

            // Expired filter
            if (filter.getExpired() != null) {
                predicates.add(criteriaBuilder.equal(root.get("expired"), filter.getExpired()));
            } else if (Boolean.TRUE.equals(filter.getExcludeExpired())) {
                // Exclude expired by default if not explicitly filtered
                predicates.add(criteriaBuilder.equal(root.get("expired"), false));
            }

            // Listing Status filter (owner-specific computed status)
            if (filter.getListingStatus() != null) {
                try {
                    com.smartrent.enums.ListingStatus requestedStatus =
                        com.smartrent.enums.ListingStatus.valueOf(filter.getListingStatus());

                    Predicate statusPredicate = buildStatusPredicate(root, criteriaBuilder, requestedStatus);
                    if (statusPredicate != null) {
                        predicates.add(statusPredicate);
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid status value, skip filter
                }
            }

            // ============ LOCATION FILTERS ============
            // Category filter
            if (filter.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), filter.getCategoryId()));
            }

            // Province/District/Ward filter - supports both old and new address structures
            if (filter.getProvinceId() != null || filter.getProvinceCode() != null ||
                filter.getDistrictId() != null || filter.getWardId() != null ||
                filter.getNewWardCode() != null || filter.getStreetId() != null) {

                Subquery<Long> subquery = query.subquery(Long.class);
                var metadataRoot = subquery.from(AddressMetadata.class);

                List<Predicate> locationPredicates = new ArrayList<>();
                locationPredicates.add(criteriaBuilder.equal(
                    root.get("address").get("addressId"),
                    metadataRoot.get("address").get("addressId")
                ));

                // Province filters
                if (filter.getProvinceId() != null) {
                    // provinceId is String in request but Integer in DB (old structure)
                    // Parse and handle as Integer for proper comparison
                    try {
                        Integer provinceIdInt = Integer.parseInt(filter.getProvinceId());
                        locationPredicates.add(criteriaBuilder.equal(
                            metadataRoot.get("provinceId"),
                            provinceIdInt
                        ));
                    } catch (NumberFormatException e) {
                        // Invalid provinceId format, skip this filter
                    }
                }
                if (filter.getProvinceCode() != null) {
                    // Normalize province code: remove leading zeros ("01" -> "1")
                    // Database stores codes without leading zeros (e.g., '1', '79', not '01', '79')
                    String normalizedCode = filter.getProvinceCode().replaceFirst("^0+(?!$)", "");
                    locationPredicates.add(criteriaBuilder.equal(
                        metadataRoot.get("newProvinceCode"),
                        normalizedCode
                    ));
                }

                // District filter (old structure)
                if (filter.getDistrictId() != null) {
                    locationPredicates.add(criteriaBuilder.equal(
                        metadataRoot.get("districtId"),
                        filter.getDistrictId()
                    ));
                }

                // Ward filters
                if (filter.getWardId() != null) {
                    // wardId is String in request but Integer in DB (old structure)
                    try {
                        Integer wardIdInt = Integer.parseInt(filter.getWardId());
                        locationPredicates.add(criteriaBuilder.equal(
                            metadataRoot.get("wardId"),
                            wardIdInt
                        ));
                    } catch (NumberFormatException e) {
                        // Invalid wardId format, skip this filter
                    }
                }
                if (filter.getNewWardCode() != null) {
                    locationPredicates.add(criteriaBuilder.equal(
                        metadataRoot.get("newWardCode"),
                        filter.getNewWardCode()
                    ));
                }

                // Street filter
                if (filter.getStreetId() != null) {
                    locationPredicates.add(criteriaBuilder.equal(
                        metadataRoot.get("streetId"),
                        filter.getStreetId()
                    ));
                }

                subquery.select(metadataRoot.get("address").get("addressId"))
                        .where(criteriaBuilder.and(locationPredicates.toArray(new Predicate[0])));

                predicates.add(criteriaBuilder.in(root.get("address").get("addressId")).value(subquery));
            }

            // Listing type filter
            if (filter.getListingType() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("listingType"),
                    Listing.ListingType.valueOf(filter.getListingType())
                ));
            }

            // VIP type filter
            if (filter.getVipType() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("vipType"),
                    Listing.VipType.valueOf(filter.getVipType())
                ));
            }

            // Product type filter
            if (filter.getProductType() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("productType"),
                    Listing.ProductType.valueOf(filter.getProductType())
                ));
            }

            // ============ PRICING FILTERS ============
            // Basic price range filter
            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }

            // Price unit filter
            if (filter.getPriceUnit() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("priceUnit"),
                    Listing.PriceUnit.valueOf(filter.getPriceUnit())
                ));
            }

            // Price reduction filter - listings with recent price drops
            if (Boolean.TRUE.equals(filter.getHasPriceReduction())) {
                Subquery<Long> priceReductionSubquery = query.subquery(Long.class);
                var priceHistoryRoot = priceReductionSubquery.from(PricingHistory.class);

                List<Predicate> priceReductionPredicates = new ArrayList<>();
                priceReductionPredicates.add(criteriaBuilder.equal(
                    priceHistoryRoot.get("listing").get("listingId"),
                    root.get("listingId")
                ));
                priceReductionPredicates.add(criteriaBuilder.equal(
                    priceHistoryRoot.get("isCurrent"),
                    true
                ));
                priceReductionPredicates.add(criteriaBuilder.equal(
                    priceHistoryRoot.get("changeType"),
                    PricingHistory.PriceChangeType.DECREASE
                ));

                // If priceChangedWithinDays is specified, filter by date
                if (filter.getPriceChangedWithinDays() != null && filter.getPriceChangedWithinDays() > 0) {
                    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(filter.getPriceChangedWithinDays());
                    priceReductionPredicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        priceHistoryRoot.get("changedAt"),
                        cutoffDate
                    ));
                }

                priceReductionSubquery.select(priceHistoryRoot.get("listing").get("listingId"))
                        .where(criteriaBuilder.and(priceReductionPredicates.toArray(new Predicate[0])));

                predicates.add(criteriaBuilder.exists(priceReductionSubquery));
            }

            // Price increase filter - listings with recent price increases
            if (Boolean.TRUE.equals(filter.getHasPriceIncrease())) {
                Subquery<Long> priceIncreaseSubquery = query.subquery(Long.class);
                var priceHistoryRoot = priceIncreaseSubquery.from(PricingHistory.class);

                List<Predicate> priceIncreasePredicates = new ArrayList<>();
                priceIncreasePredicates.add(criteriaBuilder.equal(
                    priceHistoryRoot.get("listing").get("listingId"),
                    root.get("listingId")
                ));
                priceIncreasePredicates.add(criteriaBuilder.equal(
                    priceHistoryRoot.get("isCurrent"),
                    true
                ));
                priceIncreasePredicates.add(criteriaBuilder.equal(
                    priceHistoryRoot.get("changeType"),
                    PricingHistory.PriceChangeType.INCREASE
                ));

                // If priceChangedWithinDays is specified, filter by date
                if (filter.getPriceChangedWithinDays() != null && filter.getPriceChangedWithinDays() > 0) {
                    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(filter.getPriceChangedWithinDays());
                    priceIncreasePredicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        priceHistoryRoot.get("changedAt"),
                        cutoffDate
                    ));
                }

                priceIncreaseSubquery.select(priceHistoryRoot.get("listing").get("listingId"))
                        .where(criteriaBuilder.and(priceIncreasePredicates.toArray(new Predicate[0])));

                predicates.add(criteriaBuilder.exists(priceIncreaseSubquery));
            }

            // Price reduction percentage filter - filter by discount percentage
            if (filter.getMinPriceReductionPercent() != null || filter.getMaxPriceReductionPercent() != null) {
                Subquery<Long> pricePercentSubquery = query.subquery(Long.class);
                var priceHistoryRoot = pricePercentSubquery.from(PricingHistory.class);

                List<Predicate> pricePercentPredicates = new ArrayList<>();
                pricePercentPredicates.add(criteriaBuilder.equal(
                    priceHistoryRoot.get("listing").get("listingId"),
                    root.get("listingId")
                ));
                pricePercentPredicates.add(criteriaBuilder.equal(
                    priceHistoryRoot.get("isCurrent"),
                    true
                ));
                pricePercentPredicates.add(criteriaBuilder.equal(
                    priceHistoryRoot.get("changeType"),
                    PricingHistory.PriceChangeType.DECREASE
                ));

                // Filter by percentage range
                if (filter.getMinPriceReductionPercent() != null) {
                    // Use ABS because changePercentage for DECREASE is stored as negative value
                    pricePercentPredicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        criteriaBuilder.abs(priceHistoryRoot.get("changePercentage")),
                        filter.getMinPriceReductionPercent()
                    ));
                }
                if (filter.getMaxPriceReductionPercent() != null) {
                    pricePercentPredicates.add(criteriaBuilder.lessThanOrEqualTo(
                        criteriaBuilder.abs(priceHistoryRoot.get("changePercentage")),
                        filter.getMaxPriceReductionPercent()
                    ));
                }

                // If priceChangedWithinDays is specified, filter by date
                if (filter.getPriceChangedWithinDays() != null && filter.getPriceChangedWithinDays() > 0) {
                    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(filter.getPriceChangedWithinDays());
                    pricePercentPredicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        priceHistoryRoot.get("changedAt"),
                        cutoffDate
                    ));
                }

                pricePercentSubquery.select(priceHistoryRoot.get("listing").get("listingId"))
                        .where(criteriaBuilder.and(pricePercentPredicates.toArray(new Predicate[0])));

                predicates.add(criteriaBuilder.exists(pricePercentSubquery));
            }

            // General price changed within days filter (any type of change)
            if (filter.getPriceChangedWithinDays() != null && filter.getPriceChangedWithinDays() > 0
                && filter.getHasPriceReduction() == null && filter.getHasPriceIncrease() == null
                && filter.getMinPriceReductionPercent() == null && filter.getMaxPriceReductionPercent() == null) {

                Subquery<Long> priceChangeSubquery = query.subquery(Long.class);
                var priceHistoryRoot = priceChangeSubquery.from(PricingHistory.class);

                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(filter.getPriceChangedWithinDays());

                priceChangeSubquery.select(priceHistoryRoot.get("listing").get("listingId"))
                        .where(criteriaBuilder.and(
                                criteriaBuilder.equal(
                                    priceHistoryRoot.get("listing").get("listingId"),
                                    root.get("listingId")
                                ),
                                criteriaBuilder.equal(
                                    priceHistoryRoot.get("isCurrent"),
                                    true
                                ),
                                criteriaBuilder.greaterThanOrEqualTo(
                                    priceHistoryRoot.get("changedAt"),
                                    cutoffDate
                                ),
                                criteriaBuilder.notEqual(
                                    priceHistoryRoot.get("changeType"),
                                    PricingHistory.PriceChangeType.INITIAL
                                )
                        ));

                predicates.add(criteriaBuilder.exists(priceChangeSubquery));
            }

            // Area range filter
            if (filter.getMinArea() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("area"), filter.getMinArea()));
            }
            if (filter.getMaxArea() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("area"), filter.getMaxArea()));
            }

            // Bedrooms filter - exact or range
            if (filter.getBedrooms() != null) {
                predicates.add(criteriaBuilder.equal(root.get("bedrooms"), filter.getBedrooms()));
            } else {
                if (filter.getMinBedrooms() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("bedrooms"), filter.getMinBedrooms()));
                }
                if (filter.getMaxBedrooms() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("bedrooms"), filter.getMaxBedrooms()));
                }
            }

            // Bathrooms filter - exact or range
            if (filter.getBathrooms() != null) {
                predicates.add(criteriaBuilder.equal(root.get("bathrooms"), filter.getBathrooms()));
            } else {
                if (filter.getMinBathrooms() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("bathrooms"), filter.getMinBathrooms()));
                }
                if (filter.getMaxBathrooms() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("bathrooms"), filter.getMaxBathrooms()));
                }
            }

            // ============ PROPERTY SPECS FILTERS ============
            // Furnishing filter
            if (filter.getFurnishing() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("furnishing"),
                    Listing.Furnishing.valueOf(filter.getFurnishing())
                ));
            }

            // Direction filter
            if (filter.getDirection() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("direction"),
                    Listing.Direction.valueOf(filter.getDirection())
                ));
            }

            // Room capacity range
            if (filter.getMinRoomCapacity() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("roomCapacity"), filter.getMinRoomCapacity()));
            }
            if (filter.getMaxRoomCapacity() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("roomCapacity"), filter.getMaxRoomCapacity()));
            }

            // ============ AMENITIES & MEDIA FILTERS ============
            // Amenities filter
            if (filter.getAmenityIds() != null && !filter.getAmenityIds().isEmpty()) {
                if ("ALL".equalsIgnoreCase(filter.getAmenityMatchMode())) {
                    // Must have ALL specified amenities
                    for (Long amenityId : filter.getAmenityIds()) {
                        Subquery<Long> amenitySubquery = query.subquery(Long.class);
                        var amenityRoot = amenitySubquery.from(Listing.class);
                        Join<Listing, Amenity> amenityJoin = amenityRoot.join("amenities");

                        amenitySubquery.select(amenityRoot.get("listingId"))
                                .where(criteriaBuilder.and(
                                        criteriaBuilder.equal(amenityRoot.get("listingId"), root.get("listingId")),
                                        criteriaBuilder.equal(amenityJoin.get("amenityId"), amenityId)
                                ));

                        predicates.add(criteriaBuilder.exists(amenitySubquery));
                    }
                } else {
                    // Must have ANY of the specified amenities (at least one)
                    Subquery<Long> amenitySubquery = query.subquery(Long.class);
                    var amenityRoot = amenitySubquery.from(Listing.class);
                    Join<Listing, Amenity> amenityJoin = amenityRoot.join("amenities");

                    amenitySubquery.select(amenityRoot.get("listingId"))
                            .where(criteriaBuilder.and(
                                    criteriaBuilder.equal(amenityRoot.get("listingId"), root.get("listingId")),
                                    amenityJoin.get("amenityId").in(filter.getAmenityIds())
                            ));

                    predicates.add(criteriaBuilder.exists(amenitySubquery));
                }
            }

            // Has media filter
            if (Boolean.TRUE.equals(filter.getHasMedia())) {
                Subquery<Long> mediaSubquery = query.subquery(Long.class);
                var mediaRoot = mediaSubquery.from(Media.class);

                mediaSubquery.select(mediaRoot.get("listing").get("listingId"))
                        .where(criteriaBuilder.and(
                                criteriaBuilder.equal(mediaRoot.get("listing").get("listingId"), root.get("listingId")),
                                criteriaBuilder.equal(mediaRoot.get("status"), Media.MediaStatus.ACTIVE)
                        ));

                predicates.add(criteriaBuilder.exists(mediaSubquery));
            }

            // Min media count filter
            // Note: This is a simplified implementation. For production, consider using native query or JOIN FETCH with HAVING clause
            if (filter.getMinMediaCount() != null && filter.getMinMediaCount() > 0) {
                // Use exists with count in subquery WHERE clause
                Subquery<Long> mediaCountSubquery = query.subquery(Long.class);
                var mediaRoot = mediaCountSubquery.from(Media.class);

                mediaCountSubquery.select(criteriaBuilder.count(mediaRoot.get("mediaId")))
                        .where(criteriaBuilder.and(
                                criteriaBuilder.equal(mediaRoot.get("listing").get("listingId"), root.get("listingId")),
                                criteriaBuilder.equal(mediaRoot.get("status"), Media.MediaStatus.ACTIVE)
                        ))
                        .groupBy(mediaRoot.get("listing").get("listingId"))
                        .having(criteriaBuilder.greaterThanOrEqualTo(
                                criteriaBuilder.count(mediaRoot.get("mediaId")),
                                filter.getMinMediaCount().longValue()
                        ));

                predicates.add(root.get("listingId").in(
                        mediaCountSubquery.getSelection()
                ));
            }

            // ============ KEYWORD SEARCH ============
            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                String normalized = TextNormalizer.normalize(filter.getKeyword());
                if (normalized != null && !normalized.isEmpty()) {
                    String searchPattern = "%" + normalized + "%";
                    predicates.add(criteriaBuilder.like(root.get("searchText"), searchPattern));
                }
            }

            // ============ CONTACT FILTERS ============
            // Owner phone verified filter
            if (Boolean.TRUE.equals(filter.getOwnerPhoneVerified())) {
                Subquery<String> userSubquery = query.subquery(String.class);
                var userRoot = userSubquery.from(User.class);

                userSubquery.select(userRoot.get("userId"))
                        .where(criteriaBuilder.and(
                                criteriaBuilder.equal(userRoot.get("userId"), root.get("userId")),
                                criteriaBuilder.equal(userRoot.get("contactPhoneVerified"), true),
                                criteriaBuilder.isNotNull(userRoot.get("contactPhoneNumber"))
                        ));

                predicates.add(root.get("userId").in(userSubquery));
            }

            // ============ TIME FILTERS ============
            // Posted within days filter
            if (filter.getPostedWithinDays() != null && filter.getPostedWithinDays() > 0) {
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(filter.getPostedWithinDays());
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("postDate"), cutoffDate));
            }

            // Updated within days filter
            if (filter.getUpdatedWithinDays() != null && filter.getUpdatedWithinDays() > 0) {
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(filter.getUpdatedWithinDays());
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), cutoffDate));
            }

            // ============ ALWAYS EXCLUDE SHADOW LISTINGS ============
            predicates.add(criteriaBuilder.equal(root.get("isShadow"), false));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Build specification for "My Listings" with user ID and filters
     */
    public static Specification<Listing> fromMyListingsFilter(String userId, MyListingsFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // User ID filter (required)
            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

            // Verified filter
            if (filter.getVerified() != null) {
                predicates.add(criteriaBuilder.equal(root.get("verified"), filter.getVerified()));
            }

            // Is verify filter
            if (filter.getIsVerify() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isVerify"), filter.getIsVerify()));
            }

            // Expired filter
            if (filter.getExpired() != null) {
                predicates.add(criteriaBuilder.equal(root.get("expired"), filter.getExpired()));
            }

            // Draft filter
            if (filter.getIsDraft() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isDraft"), filter.getIsDraft()));
            }

            // VIP type filter
            if (filter.getVipType() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("vipType"),
                    Listing.VipType.valueOf(filter.getVipType())
                ));
            }

            // Listing type filter
            if (filter.getListingType() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("listingType"),
                    Listing.ListingType.valueOf(filter.getListingType())
                ));
            }

            // Exclude shadow listings
            predicates.add(criteriaBuilder.equal(root.get("isShadow"), false));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Build predicate for filtering by listing status
     * Translates computed status logic into JPA criteria predicates
     */
    private static Predicate buildStatusPredicate(
            jakarta.persistence.criteria.Root<Listing> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            com.smartrent.enums.ListingStatus status) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysFromNow = now.plusDays(7);

        return switch (status) {
            case EXPIRED ->
                // expired = true OR expiryDate < now
                criteriaBuilder.or(
                    criteriaBuilder.isTrue(root.get("expired")),
                    criteriaBuilder.lessThan(root.get("expiryDate"), now)
                );

            case EXPIRING_SOON ->
                // verified = true AND expiryDate between now and 7 days from now
                criteriaBuilder.and(
                    criteriaBuilder.isTrue(root.get("verified")),
                    criteriaBuilder.between(root.get("expiryDate"), now, sevenDaysFromNow),
                    criteriaBuilder.or(
                        criteriaBuilder.isFalse(root.get("expired")),
                        criteriaBuilder.isNull(root.get("expired"))
                    )
                );

            case DISPLAYING ->
                // verified = true AND NOT expired AND expiryDate > now
                criteriaBuilder.and(
                    criteriaBuilder.isTrue(root.get("verified")),
                    criteriaBuilder.or(
                        criteriaBuilder.isFalse(root.get("expired")),
                        criteriaBuilder.isNull(root.get("expired"))
                    ),
                    criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("expiryDate")),
                        criteriaBuilder.greaterThan(root.get("expiryDate"), now)
                    )
                );

            case IN_REVIEW ->
                // isVerify = true AND verified = false
                criteriaBuilder.and(
                    criteriaBuilder.isTrue(root.get("isVerify")),
                    criteriaBuilder.or(
                        criteriaBuilder.isFalse(root.get("verified")),
                        criteriaBuilder.isNull(root.get("verified"))
                    )
                );

            case PENDING_PAYMENT ->
                // transactionId is not null AND verified = false AND isVerify = false
                criteriaBuilder.and(
                    criteriaBuilder.isNotNull(root.get("transactionId")),
                    criteriaBuilder.or(
                        criteriaBuilder.isFalse(root.get("verified")),
                        criteriaBuilder.isNull(root.get("verified"))
                    ),
                    criteriaBuilder.or(
                        criteriaBuilder.isFalse(root.get("isVerify")),
                        criteriaBuilder.isNull(root.get("isVerify"))
                    )
                );

            case REJECTED ->
                // verified = false AND isVerify = false AND isDraft = false AND postDate is not null
                criteriaBuilder.and(
                    criteriaBuilder.or(
                        criteriaBuilder.isFalse(root.get("verified")),
                        criteriaBuilder.isNull(root.get("verified"))
                    ),
                    criteriaBuilder.or(
                        criteriaBuilder.isFalse(root.get("isVerify")),
                        criteriaBuilder.isNull(root.get("isVerify"))
                    ),
                    criteriaBuilder.or(
                        criteriaBuilder.isFalse(root.get("isDraft")),
                        criteriaBuilder.isNull(root.get("isDraft"))
                    ),
                    criteriaBuilder.isNotNull(root.get("postDate"))
                );

            case VERIFIED ->
                // verified = true
                criteriaBuilder.isTrue(root.get("verified"));
        };
    }

    /**
     * Build specification for querying listings within map bounds (bounding box)
     * Used for displaying listings on interactive maps
     *
     * @param neLat North-East latitude (top-right corner)
     * @param neLng North-East longitude (top-right corner)
     * @param swLat South-West latitude (bottom-left corner)
     * @param swLng South-West longitude (bottom-left corner)
     * @param verifiedOnly Only return verified listings
     * @param categoryId Optional category filter
     * @param vipType Optional VIP type filter (NORMAL, SILVER, GOLD, DIAMOND)
     * @return Specification for map bounds query
     */
    public static Specification<Listing> withinMapBounds(
            BigDecimal neLat,
            BigDecimal neLng,
            BigDecimal swLat,
            BigDecimal swLng,
            Boolean verifiedOnly,
            Long categoryId,
            String vipType) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join with Address table to access latitude and longitude
            Join<Listing, Address> addressJoin = root.join("address", JoinType.INNER);

            // Bounding box filter: latitude and longitude must be within bounds
            // Latitude: swLat <= latitude <= neLat
            // Longitude: swLng <= longitude <= neLng
            predicates.add(criteriaBuilder.between(
                addressJoin.get("latitude"),
                swLat.doubleValue(),
                neLat.doubleValue()
            ));

            predicates.add(criteriaBuilder.between(
                addressJoin.get("longitude"),
                swLng.doubleValue(),
                neLng.doubleValue()
            ));

            // Exclude drafts - only show published listings on map
            predicates.add(criteriaBuilder.equal(root.get("isDraft"), false));

            // Verified filter
            if (Boolean.TRUE.equals(verifiedOnly)) {
                predicates.add(criteriaBuilder.equal(root.get("verified"), true));
            } else {
                // By default, only show verified listings on public map
                predicates.add(criteriaBuilder.equal(root.get("verified"), true));
            }

            // Exclude expired listings
            predicates.add(criteriaBuilder.equal(root.get("expired"), false));

            // Optional category filter
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), categoryId));
            }

//            // Optional VIP type filter
//            if (vipType != null && !vipType.isEmpty()) {
//                try {
//                    Listing.VipType vipTypeEnum = Listing.VipType.valueOf(vipType.toUpperCase());
//                    predicates.add(criteriaBuilder.equal(root.get("vipType"), vipTypeEnum));
//                } catch (IllegalArgumentException e) {
//                    // Invalid vip type, skip filter
//                }
//            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
