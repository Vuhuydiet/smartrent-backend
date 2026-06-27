package com.smartrent.infra.repository.specification;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.MyListingsFilterRequest;
import com.smartrent.infra.repository.entity.Address;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

            // Determine if we need location filters
            boolean hasLocationFilter = filter.getProvinceId() != null
                    || (filter.getProvinceCodes() != null && !filter.getProvinceCodes().isEmpty())
                    || filter.getDistrictId() != null || filter.getWardId() != null
                    || filter.getNewWardCode() != null || filter.getStreetId() != null;

            // Eagerly fetch address to avoid N+1 lazy loads (skip for count queries)
            // When location filters are used, the INNER JOIN below handles the fetch
            if (query.getResultType() != Long.class && query.getResultType() != long.class && !hasLocationFilter) {
                root.fetch("address", JoinType.LEFT);
            }

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
                // Public search gates on moderation outcome, not the admin "verified" badge.
                // verified=true still surfaces a "Tin đã xác minh" badge on the FE, but pending
                // listings that passed moderation (APPROVED) are visible too — matching how
                // batdongsan.com / chotot.vn treat the verified badge as a trust signal, not
                // a visibility gate.
                predicates.add(criteriaBuilder.equal(
                    root.get("moderationStatus"),
                    com.smartrent.enums.ModerationStatus.APPROVED));
            }

            // Verification pending filter
            if (filter.getIsVerify() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isVerify"), filter.getIsVerify()));
            }

            // Expired filter
            if (filter.getExpired() != null) {
                predicates.add(criteriaBuilder.equal(root.get("expired"), filter.getExpired()));
            } else if (Boolean.TRUE.equals(filter.getExcludeExpired())) {
                LocalDateTime now = LocalDateTime.now();
                predicates.add(criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("expired"), false),
                    criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("expiryDate")),
                        criteriaBuilder.greaterThan(root.get("expiryDate"), now)
                    )
                ));
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

            // Moderation Status filter (admin-only, direct moderationStatus column)
            if (filter.getModerationStatus() != null && !filter.getModerationStatus().isEmpty()) {
                try {
                    com.smartrent.enums.ModerationStatus moderationStatus =
                        com.smartrent.enums.ModerationStatus.valueOf(filter.getModerationStatus());
                    if (moderationStatus == com.smartrent.enums.ModerationStatus.PENDING_REVIEW) {
                        // PENDING_REVIEW is the canonical default for a not-yet-moderated
                        // listing. Legacy/older rows (and any that slipped through a
                        // creation path that forgot to stamp it) have moderation_status
                        // NULL while still being IN_REVIEW. Treat NULL as PENDING_REVIEW
                        // so those listings stay visible on the seller's IN_REVIEW tab
                        // and in the admin review queue instead of disappearing.
                        predicates.add(criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("moderationStatus"), moderationStatus),
                            criteriaBuilder.isNull(root.get("moderationStatus"))
                        ));
                    } else {
                        predicates.add(criteriaBuilder.equal(root.get("moderationStatus"), moderationStatus));
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid moderation status value, skip filter
                }
            }

            // ============ LOCATION FILTERS ============
            // Category filter
            if (filter.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), filter.getCategoryId()));
            }

            // Province/District/Ward filter - supports both old and new address structures
            // Queries addresses table directly (no AddressMetadata dependency)
            if (filter.getProvinceId() != null || (filter.getProvinceCodes() != null && !filter.getProvinceCodes().isEmpty()) ||
                filter.getDistrictId() != null || filter.getWardId() != null ||
                filter.getNewWardCode() != null || filter.getStreetId() != null) {

                // Join address (already fetched above for non-count queries)
                Join<Listing, Address> addressJoin = root.join("address", JoinType.INNER);

                // Province filters — combine old provinceId and new provinceCode as OR
                // so listings from either address structure are included
                {
                    List<Predicate> provinceOrParts = new ArrayList<>();

                    // Old structure: provinceId (Integer)
                    if (filter.getProvinceId() != null) {
                        try {
                            Integer provinceIdInt = Integer.parseInt(filter.getProvinceId());
                            provinceOrParts.add(criteriaBuilder.equal(
                                addressJoin.get("legacyProvinceId"), provinceIdInt));
                        } catch (NumberFormatException ignored) {}

                        // Also match new-structure listings via resolved new province codes
                        // (populated by service when FE sends old provinceId in LEGACY mode)
                        if (filter.getResolvedNewProvinceCodes() != null
                                && !filter.getResolvedNewProvinceCodes().isEmpty()) {
                            provinceOrParts.add(addressJoin.get("newProvinceCode")
                                    .in(filter.getResolvedNewProvinceCodes()));
                        }
                    }

                    // New structure: provinceCodes list + resolved legacy IDs
                    if (filter.getProvinceCodes() != null && !filter.getProvinceCodes().isEmpty()) {
                        List<String> normalizedCodes = new ArrayList<>();
                        for (String c : filter.getProvinceCodes()) {
                            String stripped = c.replaceFirst("^0+(?!$)", "");
                            normalizedCodes.add(stripped);
                            try {
                                normalizedCodes.add(String.format("%02d", Integer.parseInt(stripped)));
                            } catch (NumberFormatException ignored) {}
                        }
                        provinceOrParts.add(addressJoin.get("newProvinceCode").in(normalizedCodes));

                        // Also match old-structure listings via resolved legacy province IDs
                        if (filter.getResolvedLegacyProvinceIds() != null
                                && !filter.getResolvedLegacyProvinceIds().isEmpty()) {
                            provinceOrParts.add(addressJoin.get("legacyProvinceId")
                                    .in(filter.getResolvedLegacyProvinceIds()));
                        }
                    }

                    if (!provinceOrParts.isEmpty()) {
                        predicates.add(criteriaBuilder.or(
                                provinceOrParts.toArray(new Predicate[0])));
                    }
                }

                // District filter — OR'd with resolved new ward codes so that
                // listings created under the 2-tier NEW structure (with NULL
                // legacy_district_id) are still matched when FE picks a district
                // in LEGACY mode. The new ward codes were resolved by the
                // service layer from this districtId.
                if (filter.getDistrictId() != null) {
                    List<Predicate> districtOrParts = new ArrayList<>();
                    districtOrParts.add(criteriaBuilder.equal(
                        addressJoin.get("legacyDistrictId"),
                        filter.getDistrictId()
                    ));
                    if (filter.getResolvedNewWardCodesForDistrict() != null
                            && !filter.getResolvedNewWardCodesForDistrict().isEmpty()) {
                        districtOrParts.add(addressJoin.get("newWardCode")
                                .in(filter.getResolvedNewWardCodesForDistrict()));
                    }
                    predicates.add(criteriaBuilder.or(
                            districtOrParts.toArray(new Predicate[0])));
                }

                // Ward filters — combine new-structure newWardCode with old-structure
                // legacyWardId as OR so listings from either address structure are included
                {
                    List<Predicate> wardOrParts = new ArrayList<>();

                    // Old structure: explicit wardId (String → Integer)
                    if (filter.getWardId() != null) {
                        try {
                            Integer wardIdInt = Integer.parseInt(filter.getWardId());
                            wardOrParts.add(criteriaBuilder.equal(
                                addressJoin.get("legacyWardId"), wardIdInt));
                        } catch (NumberFormatException ignored) {}

                        // Also match new-structure listings via resolved new ward codes
                        // (populated by service when FE sends old wardId in LEGACY mode)
                        if (filter.getResolvedNewWardCodes() != null
                                && !filter.getResolvedNewWardCodes().isEmpty()) {
                            wardOrParts.add(addressJoin.get("newWardCode")
                                    .in(filter.getResolvedNewWardCodes()));
                        }
                    }

                    if (filter.getNewWardCode() != null) {
                        // New-structure listings: direct match on new_ward_code
                        wardOrParts.add(criteriaBuilder.equal(
                            addressJoin.get("newWardCode"), filter.getNewWardCode()));

                        // Old-structure listings: match via address_mapping-resolved legacy ward IDs
                        if (filter.getResolvedLegacyWardIds() != null
                                && !filter.getResolvedLegacyWardIds().isEmpty()) {
                            wardOrParts.add(addressJoin.get("legacyWardId")
                                    .in(filter.getResolvedLegacyWardIds()));
                        }
                    }

                    if (!wardOrParts.isEmpty()) {
                        predicates.add(criteriaBuilder.or(
                                wardOrParts.toArray(new Predicate[0])));
                    }
                }

                // Street filter (uses legacy_street on addresses table)
                // Note: streetId was from address_metadata, not available on addresses directly
                // Skip streetId filter as it has no corresponding column in addresses
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

            // ============ ADMIN DATE-RANGE FILTERS ============
            // Format: YYYY-MM-DD..YYYY-MM-DD ; either side may be omitted for open-ended range.
            DateRangeBounds postRange = parseDateRange(filter.getPostDate());
            if (postRange != null) {
                if (postRange.from() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("postDate"), postRange.from()));
                }
                if (postRange.to() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("postDate"), postRange.to()));
                }
            }

            DateRangeBounds expiryRange = parseDateRange(filter.getExpiryDate());
            if (expiryRange != null) {
                if (expiryRange.from() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("expiryDate"), expiryRange.from()));
                }
                if (expiryRange.to() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("expiryDate"), expiryRange.to()));
                }
            }

            // ============ PRICING FILTERS ============
            // Basic price range filter
            BigDecimalRangeBounds priceRange = parseBigDecimalRange(filter.getPrice());
            if (priceRange != null) {
                if (priceRange.from() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), priceRange.from()));
                }
                if (priceRange.to() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), priceRange.to()));
                }
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
            BigDecimalRangeBounds priceReductionRange = parseBigDecimalRange(filter.getPriceReductionPercent());
            if (priceReductionRange != null) {
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
                if (priceReductionRange.from() != null) {
                    // Use ABS because changePercentage for DECREASE is stored as negative value
                    pricePercentPredicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        criteriaBuilder.abs(priceHistoryRoot.get("changePercentage")),
                        priceReductionRange.from()
                    ));
                }
                if (priceReductionRange.to() != null) {
                    pricePercentPredicates.add(criteriaBuilder.lessThanOrEqualTo(
                        criteriaBuilder.abs(priceHistoryRoot.get("changePercentage")),
                        priceReductionRange.to()
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
                && priceReductionRange == null) {

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
                                ),
                                criteriaBuilder.notEqual(
                                    priceHistoryRoot.get("changeType"),
                                    PricingHistory.PriceChangeType.ADJUSTED
                                )
                        ));

                predicates.add(criteriaBuilder.exists(priceChangeSubquery));
            }

            // Area range filter
            FloatRangeBounds areaRange = parseFloatRange(filter.getArea());
            if (areaRange != null) {
                if (areaRange.from() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("area"), areaRange.from()));
                }
                if (areaRange.to() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("area"), areaRange.to()));
                }
            }

            // Bedrooms filter - exact or range
            if (filter.getBedrooms() != null) {
                predicates.add(criteriaBuilder.equal(root.get("bedrooms"), filter.getBedrooms()));
            } else {
                IntRangeBounds bedroomsRange = parseIntRange(filter.getBedroomsRange());
                if (bedroomsRange != null) {
                    if (bedroomsRange.from() != null) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("bedrooms"), bedroomsRange.from()));
                    }
                    if (bedroomsRange.to() != null) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("bedrooms"), bedroomsRange.to()));
                    }
                }
            }

            // Bathrooms filter - exact or range
            if (filter.getBathrooms() != null) {
                predicates.add(criteriaBuilder.equal(root.get("bathrooms"), filter.getBathrooms()));
            } else {
                IntRangeBounds bathroomsRange = parseIntRange(filter.getBathroomsRange());
                if (bathroomsRange != null) {
                    if (bathroomsRange.from() != null) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("bathrooms"), bathroomsRange.from()));
                    }
                    if (bathroomsRange.to() != null) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("bathrooms"), bathroomsRange.to()));
                    }
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
            IntRangeBounds roomCapacityRange = parseIntRange(filter.getRoomCapacity());
            if (roomCapacityRange != null) {
                if (roomCapacityRange.from() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("roomCapacity"), roomCapacityRange.from()));
                }
                if (roomCapacityRange.to() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("roomCapacity"), roomCapacityRange.to()));
                }
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

            // ============ KEYWORD SEARCH (FULLTEXT) ============
            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                String normalized = TextNormalizer.normalize(filter.getKeyword());
                if (normalized != null && !normalized.isEmpty()) {
                    // Use MySQL FULLTEXT MATCH...AGAINST in BOOLEAN MODE for indexed search
                    // Each word gets a '+' prefix for AND semantics, '*' suffix for prefix matching
                    String[] words = normalized.split("\\s+");
                    StringBuilder ftQuery = new StringBuilder();
                    for (String word : words) {
                        if (!word.isEmpty()) {
                            ftQuery.append("+").append(word).append("* ");
                        }
                    }
                    String fulltextQuery = ftQuery.toString().trim();
                    if (!fulltextQuery.isEmpty()) {
                        predicates.add(criteriaBuilder.greaterThan(
                            criteriaBuilder.function("match_against",
                                Double.class,
                                root.get("searchText"),
                                criteriaBuilder.literal(fulltextQuery)),
                            0.0));
                    }
                }
            }

            // ============ ADMIN: TITLE-ONLY SEARCH ============
            if (filter.getTitle() != null && !filter.getTitle().trim().isEmpty()) {
                String titleNeedle = "%" + filter.getTitle().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")), titleNeedle));
            }

            // ============ ADMIN: OWNER NAME / PHONE SEARCH ============
            // Matches owner firstName, lastName, contactPhoneNumber, or phoneNumber (case-insensitive contains).
            if (filter.getOwnerSearch() != null && !filter.getOwnerSearch().trim().isEmpty()) {
                String ownerNeedle = "%" + filter.getOwnerSearch().trim().toLowerCase() + "%";
                Subquery<String> ownerSubquery = query.subquery(String.class);
                var ownerRoot = ownerSubquery.from(User.class);
                ownerSubquery.select(ownerRoot.get("userId"))
                        .where(criteriaBuilder.and(
                                criteriaBuilder.equal(ownerRoot.get("userId"), root.get("userId")),
                                criteriaBuilder.or(
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(ownerRoot.get("firstName")), ownerNeedle),
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(ownerRoot.get("lastName")), ownerNeedle),
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(ownerRoot.get("contactPhoneNumber")), ownerNeedle),
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(ownerRoot.get("phoneNumber")), ownerNeedle)
                                )
                        ));
                predicates.add(root.get("userId").in(ownerSubquery));
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

            // Broker owner filter
            // isBroker=true: only listings from users with is_broker=true AND brokerVerificationStatus=APPROVED
            // isBroker=false: only listings from non-broker users
            // isBroker=null: no filter (all users)
            if (filter.getIsBroker() != null) {
                Subquery<String> brokerSubquery = query.subquery(String.class);
                var brokerUserRoot = brokerSubquery.from(User.class);

                if (Boolean.TRUE.equals(filter.getIsBroker())) {
                    brokerSubquery.select(brokerUserRoot.get("userId"))
                            .where(criteriaBuilder.and(
                                    criteriaBuilder.equal(brokerUserRoot.get("userId"), root.get("userId")),
                                    criteriaBuilder.equal(brokerUserRoot.get("isBroker"), true),
                                    criteriaBuilder.equal(
                                            brokerUserRoot.get("brokerVerificationStatus"),
                                            com.smartrent.enums.BrokerVerificationStatus.APPROVED)
                            ));
                    predicates.add(root.get("userId").in(brokerSubquery));
                } else {
                    // isBroker=false: exclude listings whose owner is an approved broker
                    Subquery<String> approvedBrokerSubquery = query.subquery(String.class);
                    var approvedBrokerRoot = approvedBrokerSubquery.from(User.class);
                    approvedBrokerSubquery.select(approvedBrokerRoot.get("userId"))
                            .where(criteriaBuilder.and(
                                    criteriaBuilder.equal(approvedBrokerRoot.get("isBroker"), true),
                                    criteriaBuilder.equal(
                                            approvedBrokerRoot.get("brokerVerificationStatus"),
                                            com.smartrent.enums.BrokerVerificationStatus.APPROVED)
                            ));
                    predicates.add(criteriaBuilder.not(root.get("userId").in(approvedBrokerSubquery)));
                }
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
                // Includes both PENDING_REVIEW and RESUBMITTED listings
                // Use moderationStatus filter for granular control
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

            case RESUBMITTED ->
                // moderationStatus = RESUBMITTED
                criteriaBuilder.equal(root.get("moderationStatus"),
                    com.smartrent.enums.ModerationStatus.RESUBMITTED);
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

            // Exclude expired listings (flag AND date check)
            LocalDateTime mapNow = LocalDateTime.now();
            predicates.add(criteriaBuilder.and(
                criteriaBuilder.equal(root.get("expired"), false),
                criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("expiryDate")),
                    criteriaBuilder.greaterThan(root.get("expiryDate"), mapNow)
                )
            ));

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

    /**
     * Build specification for natural language search parsed by AI.
     *
     * <p>Backwards-compatible overload: amenities are still matched by the
     * legacy {@code LOWER(name) LIKE} fallback. Prefer
     * {@link #matchesCriteria(com.smartrent.dto.request.AiParsedCriteriaDto, java.util.Set, java.util.Collection)}
     * with resolver-provided ids so amenities filter by canonical id (the LIKE
     * never matched, because the DB stores "Điều hòa" while the parser emits
     * "máy lạnh").
     */
    public static Specification<Listing> matchesCriteria(com.smartrent.dto.request.AiParsedCriteriaDto criteria) {
        return matchesCriteria(
                criteria,
                Collections.emptySet(),
                criteria != null ? criteria.getAmenities() : null);
    }

    /**
     * Build specification for natural language search parsed by AI, with
     * amenities already resolved to canonical ids by
     * {@link com.smartrent.service.discovery.AmenityResolver}.
     *
     * @param amenityIds          canonical amenity ids; matched with ALL
     *                            semantics (listing must have every one) via an
     *                            EXISTS subquery — same as the structured
     *                            {@code fromFilterRequest} path.
     * @param unresolvedAmenities phrases the resolver could not map; kept on the
     *                            legacy {@code LOWER(name) LIKE} OR-match so
     *                            recall never regresses for unknown amenities.
     */
    public static Specification<Listing> matchesCriteria(
            com.smartrent.dto.request.AiParsedCriteriaDto criteria,
            Set<Long> amenityIds,
            Collection<String> unresolvedAmenities) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only verified and active listings
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.distinct(true);
            }
            predicates.add(criteriaBuilder.equal(root.get("verified"), true));
            predicates.add(criteriaBuilder.equal(root.get("expired"), false));
            predicates.add(criteriaBuilder.equal(root.get("isDraft"), false));
            predicates.add(criteriaBuilder.equal(root.get("isShadow"), false));

            if (criteria.getPropertyType() != null) {
                try {
                    Listing.ProductType productType = Listing.ProductType.valueOf(criteria.getPropertyType().toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("productType"), productType));
                } catch (IllegalArgumentException e) {
                    // Ignore if not exactly matching ProductType
                }
            }

            if (criteria.getListingType() != null) {
                try {
                    Listing.ListingType listingType = Listing.ListingType.valueOf(criteria.getListingType().toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("listingType"), listingType));
                } catch (IllegalArgumentException e) {
                    // Ignore if not exactly matching ListingType
                }
            }

            if (criteria.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), criteria.getMaxPrice()));
            }
            if (criteria.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), criteria.getMinPrice()));
            }

            // Area (m²) — Listing.area is Float.
            if (criteria.getMinArea() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("area"), criteria.getMinArea()));
            }
            if (criteria.getMaxArea() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("area"), criteria.getMaxArea()));
            }

            // Bedrooms — treated as a minimum ("2 phòng ngủ" ⇒ at least 2) so
            // an exact-match never starves the result set.
            if (criteria.getBedrooms() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("bedrooms"), criteria.getBedrooms()));
            }

            // Location filters using text pattern matching, since AI returns a
            // raw string (e.g. "quận 1", "Bình Thạnh", "TP.HCM"). Address has NO
            // per-component name columns — only the full address strings — so we
            // match each term against fullAddress (legacy structure) OR
            // fullNewAddress (post-2025 structure). Matching the non-existent
            // districtName/provinceName/wardName attributes previously made
            // Hibernate throw while building the query, 500-ing every
            // location-filtered search.
            if (criteria.getDistrict() != null || criteria.getProvince() != null || criteria.getWard() != null) {
                Join<Listing, Address> addressJoin = root.join("address", JoinType.INNER);

                for (String locationTerm : new String[]{
                        criteria.getDistrict(), criteria.getProvince(), criteria.getWard()}) {
                    if (locationTerm != null && !locationTerm.isBlank()) {
                        String pattern = "%" + locationTerm.toLowerCase() + "%";
                        predicates.add(criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(addressJoin.get("fullAddress")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(addressJoin.get("fullNewAddress")), pattern)
                        ));
                    }
                }
            }

            // Resolved amenities → ALL semantics via per-id EXISTS subquery
            // (mirrors the structured fromFilterRequest path so NL search and
            // filter chips behave identically).
            if (amenityIds != null && !amenityIds.isEmpty()) {
                for (Long amenityId : amenityIds) {
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
            }

            // Unresolved amenity phrases → legacy LOWER(name) LIKE OR-match so
            // an amenity the resolver doesn't know still narrows the results.
            if (unresolvedAmenities != null && !unresolvedAmenities.isEmpty()) {
                Join<Listing, Amenity> amenityJoin = root.join("amenities", JoinType.LEFT);
                List<Predicate> amenityPredicates = new ArrayList<>();
                for (String amenity : unresolvedAmenities) {
                    if (amenity == null || amenity.isBlank()) continue;
                    String raw = amenity.toLowerCase();
                    String normalized = TextNormalizer.normalize(amenity);
                    amenityPredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(amenityJoin.get("name")),
                            "%" + raw + "%"));
                    if (normalized != null && !normalized.equals(raw)) {
                        amenityPredicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(amenityJoin.get("name")),
                                "%" + normalized + "%"));
                    }
                }
                if (!amenityPredicates.isEmpty()) {
                    predicates.add(criteriaBuilder.or(amenityPredicates.toArray(new Predicate[0])));
                }
            }

            // Keyword fallback: FULLTEXT or Typo Tolerance
            if (criteria.getKeyword() != null && !criteria.getKeyword().isEmpty()) {
                String normalized = TextNormalizer.normalize(criteria.getKeyword());
                if (normalized != null && !normalized.isEmpty()) {
                    String[] words = normalized.split("\\s+");
                    StringBuilder ftQuery = new StringBuilder();
                    for (String word : words) {
                        if (!word.isEmpty()) {
                            ftQuery.append("+").append(word).append("* ");
                        }
                    }
                    String fulltextQuery = ftQuery.toString().trim();
                    if (!fulltextQuery.isEmpty()) {
                        predicates.add(criteriaBuilder.greaterThan(
                            criteriaBuilder.function("match_against",
                                Double.class,
                                root.get("searchText"),
                                criteriaBuilder.literal(fulltextQuery)),
                            0.0));
                    }
                }
            } else if (criteria.getPhoneticKeyword() != null && !criteria.getPhoneticKeyword().isEmpty()) {
                 // Typo tolerance: if no keyword matched, use phonetic title
                 predicates.add(criteriaBuilder.like(root.get("phoneticTitle"), "%" + criteria.getPhoneticKeyword() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Split a `from..to` range string. Returns the two raw sides (trimmed) or
     * {@code null} if the input is blank, has no `..` separator, or both sides are empty.
     * Caller is responsible for parsing each side into the target type.
     */
    static String[] splitRange(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        int idx = s.indexOf("..");
        if (idx < 0) return null;
        String fromStr = s.substring(0, idx).trim();
        String toStr   = s.substring(idx + 2).trim();
        if (fromStr.isEmpty() && toStr.isEmpty()) return null;
        return new String[] { fromStr, toStr };
    }

    /**
     * Parse a date-range filter of the form {@code YYYY-MM-DD..YYYY-MM-DD}.
     * Either side may be omitted (e.g. {@code "2026-03-01.."} or {@code "..2026-03-31"}).
     * Returns {@code null} if the input is unusable.
     * Malformed dates throw {@link java.time.format.DateTimeParseException}.
     */
    static DateRangeBounds parseDateRange(String raw) {
        String[] parts = splitRange(raw);
        if (parts == null) return null;
        LocalDateTime from = parts[0].isEmpty() ? null : java.time.LocalDate.parse(parts[0]).atStartOfDay();
        LocalDateTime to   = parts[1].isEmpty() ? null : java.time.LocalDate.parse(parts[1]).atTime(java.time.LocalTime.MAX);
        if (from == null && to == null) return null;
        return new DateRangeBounds(from, to);
    }

    /** Parse a `from..to` range into BigDecimal bounds. */
    static BigDecimalRangeBounds parseBigDecimalRange(String raw) {
        String[] parts = splitRange(raw);
        if (parts == null) return null;
        BigDecimal from = parts[0].isEmpty() ? null : new BigDecimal(parts[0]);
        BigDecimal to   = parts[1].isEmpty() ? null : new BigDecimal(parts[1]);
        if (from == null && to == null) return null;
        return new BigDecimalRangeBounds(from, to);
    }

    /** Parse a `from..to` range into Float bounds. */
    static FloatRangeBounds parseFloatRange(String raw) {
        String[] parts = splitRange(raw);
        if (parts == null) return null;
        Float from = parts[0].isEmpty() ? null : Float.valueOf(parts[0]);
        Float to   = parts[1].isEmpty() ? null : Float.valueOf(parts[1]);
        if (from == null && to == null) return null;
        return new FloatRangeBounds(from, to);
    }

    /** Parse a `from..to` range into Integer bounds. */
    static IntRangeBounds parseIntRange(String raw) {
        String[] parts = splitRange(raw);
        if (parts == null) return null;
        Integer from = parts[0].isEmpty() ? null : Integer.valueOf(parts[0]);
        Integer to   = parts[1].isEmpty() ? null : Integer.valueOf(parts[1]);
        if (from == null && to == null) return null;
        return new IntRangeBounds(from, to);
    }

    record DateRangeBounds(LocalDateTime from, LocalDateTime to) {}
    record BigDecimalRangeBounds(BigDecimal from, BigDecimal to) {}
    record FloatRangeBounds(Float from, Float to) {}
    record IntRangeBounds(Integer from, Integer to) {}
}
