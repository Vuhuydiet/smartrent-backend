package com.smartrent.service.listing;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.specification.ListingSpecification;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Service layer for common listing query operations
 * This is the SINGLE SOURCE OF TRUTH for all listing queries across different APIs
 *
 * Responsibilities:
 * - Execute listing queries with pagination
 * - Build sort specifications
 * - Build JPA specifications from filter requests
 *
 * Used by:
 * - Public search API (/search)
 * - Owner listings API (/my-listings)
 * - Admin listings API (/admin/list)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListingQueryService {

    ListingRepository listingRepository;

    /**
     * Execute listing query with pagination and sorting
     * This is the unified entry point for all listing queries
     *
     * @param filter Filter criteria containing all search parameters
     * @return Paginated listing results
     */
    public Page<Listing> executeQuery(ListingFilterRequest filter) {
        log.debug("Executing listing query - Category: {}, Province: {}/{}, Page: {}, Size: {}",
                filter.getCategoryId(), filter.getProvinceId(), filter.getProvinceCodes(),
                filter.getPage(), filter.getSize());

        if (isAdminPendingReviewQueueShape(filter)) {
            return queryAdminPendingReviewQueue(filter);
        }

        // Build JPA specification from filter request
        Specification<Listing> spec = buildSpecification(filter);

        // Create pageable with sorting
        Pageable pageable = buildPageable(filter);

        // Execute query
        Page<Listing> results = listingRepository.findAll(spec, pageable);

        log.debug("Query returned {} results out of {} total",
                results.getNumberOfElements(), results.getTotalElements());

        return results;
    }

    /**
     * Fast path for the admin "pending review" queue — see
     * {@link com.smartrent.infra.repository.ListingRepository#findAdminPendingReviewQueueIds}.
     * Only takes over when the filter is EXACTLY this shape: any other field set (a
     * category, a price range, a different sort, etc.) falls through to the generic
     * specification path unchanged, since the forced index/hardcoded WHERE only covers
     * this one combination.
     */
    private boolean isAdminPendingReviewQueueShape(ListingFilterRequest filter) {
        if (!Boolean.TRUE.equals(filter.getIsAdminRequest())) return false;
        if (!"PENDING_REVIEW".equals(filter.getModerationStatus())) return false;
        if (!"IN_REVIEW".equals(filter.getListingStatus())) return false;
        if (filter.getHasPendingOwnerAction() != null) return false;
        String sortBy = filter.getSortBy();
        if (sortBy != null && !sortBy.isEmpty() && !"NEWEST".equals(sortBy) && !"DEFAULT".equals(sortBy)) return false;

        return filter.getVerified() == null && filter.getIsVerify() == null
                && filter.getIsDraft() == null && filter.getExpired() == null
                && filter.getUserId() == null && filter.getCategoryId() == null
                && filter.getProvinceId() == null && isEmpty(filter.getProvinceCodes())
                && filter.getDistrictId() == null && filter.getWardId() == null
                && filter.getNewWardCode() == null && filter.getStreetId() == null
                && filter.getListingType() == null && filter.getVipType() == null
                && filter.getProductType() == null
                && filter.getPrice() == null && filter.getPriceUnit() == null
                && filter.getHasPriceReduction() == null && filter.getHasPriceIncrease() == null
                && filter.getPriceReductionPercent() == null && filter.getPriceChangedWithinDays() == null
                && filter.getArea() == null && filter.getBedrooms() == null && filter.getBathrooms() == null
                && filter.getBedroomsRange() == null && filter.getBathroomsRange() == null
                && filter.getFurnishing() == null && filter.getDirection() == null
                && filter.getRoomCapacity() == null
                && filter.getWaterPrice() == null && filter.getElectricityPrice() == null
                && filter.getInternetPrice() == null && filter.getServiceFee() == null
                && isEmpty(filter.getAmenityIds()) && filter.getHasMedia() == null
                && filter.getMinMediaCount() == null
                && filter.getKeyword() == null && filter.getTitle() == null && filter.getOwnerSearch() == null
                && filter.getOwnerPhoneVerified() == null && filter.getIsBroker() == null
                && filter.getPostedWithinDays() == null && filter.getUpdatedWithinDays() == null
                && filter.getPostDate() == null && filter.getExpiryDate() == null;
    }

    private static boolean isEmpty(java.util.Collection<?> c) {
        return c == null || c.isEmpty();
    }

    /**
     * Admin "pending review" queue via forced covering index — see
     * {@link com.smartrent.infra.repository.ListingRepository#findAdminPendingReviewQueueIds}
     * for why this bypasses the generic specification for this one filter shape.
     */
    private Page<Listing> queryAdminPendingReviewQueue(ListingFilterRequest filter) {
        int page = Math.max(filter.getPage() - 1, 0);
        int size = Math.min(Math.max(filter.getSize(), 1), 100);
        int offset = page * size;

        List<Long> ids = listingRepository.findAdminPendingReviewQueueIds(size, offset);
        long total = listingRepository.countAdminPendingReviewQueue();

        List<Listing> ordered;
        if (ids.isEmpty()) {
            ordered = Collections.emptyList();
        } else {
            Map<Long, Listing> byId = listingRepository.findAllById(ids).stream()
                    .collect(Collectors.toMap(Listing::getListingId, Function.identity(), (a, b) -> a));
            ordered = ids.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return new PageImpl<>(ordered, PageRequest.of(page, size), total);
    }

    /**
     * Build JPA specification from filter request
     * Delegates to ListingSpecification for the actual specification building
     *
     * @param filter Filter criteria
     * @return JPA specification
     */
    public Specification<Listing> buildSpecification(ListingFilterRequest filter) {
        return ListingSpecification.fromFilterRequest(filter);
    }

    /**
     * Build Pageable object with pagination and sorting
     *
     * @param filter Filter criteria containing page, size, sortBy, sortDirection
     * @return Pageable object for JPA query
     */
    private Pageable buildPageable(ListingFilterRequest filter) {
        Sort sort = buildSort(filter.getSortBy(), filter.getSortDirection());

        // Ensure valid page and size values
        // Convert from 1-based (frontend) to 0-based (Spring Data)
        int page = Math.max(filter.getPage() - 1, 0);
        int size = Math.min(Math.max(filter.getSize(), 1), 100);

        return PageRequest.of(page, size, sort);
    }

    /**
     * Build Sort object from sort field and direction
     * DEFAULT SORTING: All listings are always sorted by:
     * 1. vipTypeSortOrder ASC (DIAMOND=1, GOLD=2, SILVER=3, NORMAL=4)
     * 2. updatedAt DESC (newest first within each VIP tier)
     *
     * User can specify additional sorting criteria which will be applied AFTER the default sorting
     *
     * @param sortBy Sort field name (postDate, price, area, createdAt, updatedAt)
     * @param sortDirection Sort direction (ASC or DESC)
     * @return Sort object for JPA query
     */
    private Sort buildSort(String sortBy, String sortDirection) {
        // Default sorting: vipTypeSortOrder ASC, then updatedAt DESC
        Sort defaultSort = Sort.by(Sort.Direction.ASC, "vipTypeSortOrder")
                .and(Sort.by(Sort.Direction.DESC, "updatedAt"));

        // If user specifies custom sorting, apply it
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            // NEWEST/OLDEST replace default sort entirely (otherwise updatedAt conflicts)
            // PRICE_ASC/PRICE_DESC prepend price sort before default
            return switch (sortBy) {
                case "NEWEST" -> Sort.by(Sort.Direction.ASC, "vipTypeSortOrder")
                        .and(Sort.by(Sort.Direction.DESC, "updatedAt"));
                case "OLDEST" -> Sort.by(Sort.Direction.ASC, "vipTypeSortOrder")
                        .and(Sort.by(Sort.Direction.ASC, "updatedAt"));
                case "PRICE_ASC" -> Sort.by(Sort.Direction.ASC, "price")
                        .and(defaultSort);
                case "PRICE_DESC" -> Sort.by(Sort.Direction.DESC, "price")
                        .and(defaultSort);
                case "price" -> Sort.by(direction, "price").and(defaultSort);
                case "area" -> Sort.by(direction, "area").and(defaultSort);
                case "createdAt" -> Sort.by(direction, "createdAt").and(defaultSort);
                case "updatedAt" -> Sort.by(direction, "updatedAt");
                case "postDate" -> Sort.by(direction, "postDate").and(defaultSort);
                default -> defaultSort; // includes "DEFAULT"
            };
        }

        return defaultSort;
    }

    /**
     * Query listings within map bounds (bounding box)
     * Used for displaying listings on interactive maps
     *
     * @param neLat North-East latitude
     * @param neLng North-East longitude
     * @param swLat South-West latitude
     * @param swLng South-West longitude
     * @param limit Maximum number of results
     * @param verifiedOnly Only return verified listings
     * @param categoryId Optional category filter
     * @param vipType Optional VIP type filter
     * @return List of listings within bounds, sorted by default (VIP type then updatedAt)
     */
    public Page<Listing> queryByMapBounds(
            java.math.BigDecimal neLat,
            java.math.BigDecimal neLng,
            java.math.BigDecimal swLat,
            java.math.BigDecimal swLng,
            Integer limit,
            Boolean verifiedOnly,
            Long categoryId,
            String vipType) {

        log.debug("Querying listings by map bounds - NE: ({}, {}), SW: ({}, {}), limit: {}",
                neLat, neLng, swLat, swLng, limit);

        // Deterministic ordering is REQUIRED for the map: the query is capped by
        // `limit`, so without a stable ORDER BY the database is free to return a
        // different subset for the same bounding box on each call — the "points
        // flicker / disappear in the same area" bug. VIP tier first (DIAMOND wins
        // the cap), then newest, then listingId as a final tie-breaker. That order
        // is baked into findMapBoundsListingIds' native ORDER BY.
        int safeLimit = Math.min(Math.max(limit, 1), 500); // Max 500 for map queries

        // Force idx_listings_map_bounds. This bbox + ORDER BY + LIMIT shape makes
        // the optimizer prefer idx_listings_sort_order and filter row-by-row
        // (~1.5s at city zoom, worse when zoomed into a sparse area); the forced
        // geo index is a covering range scan (~70ms, consistent). The native query
        // reproduces ListingSpecification.withinMapBounds' filter exactly — the map
        // is always verified-only, so verifiedOnly/vipType aren't applied separately
        // (matching the previous behaviour, where vipType was disabled and
        // verifiedOnly resolved to verified=true either way).
        List<Long> ids = listingRepository.findMapBoundsListingIds(
                swLat, neLat, swLng, neLng, categoryId, safeLimit);
        long total = listingRepository.countMapBoundsListings(
                swLat, neLat, swLng, neLng, categoryId);

        // Hydrate the managed entities and restore the native order (IN loses it).
        // Left lazy on media/address here on purpose: the caller's
        // batchMapCardListings batch-fetches those in a single query.
        List<Listing> ordered;
        if (ids.isEmpty()) {
            ordered = Collections.emptyList();
        } else {
            Map<Long, Listing> byId = listingRepository.findAllById(ids).stream()
                    .collect(Collectors.toMap(Listing::getListingId, Function.identity(), (a, b) -> a));
            ordered = ids.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        log.debug("Map bounds query returned {} of {} listings", ordered.size(), total);
        return new PageImpl<>(ordered, PageRequest.of(0, safeLimit), total);
    }
}