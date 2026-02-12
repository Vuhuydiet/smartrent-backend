package com.smartrent.service.listing;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.specification.ListingSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
                filter.getCategoryId(), filter.getProvinceId(), filter.getProvinceCode(),
                filter.getPage(), filter.getSize());

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

        // Build specification for map bounds query
        Specification<Listing> spec = ListingSpecification.withinMapBounds(
                neLat, neLng, swLat, swLng, verifiedOnly, categoryId, vipType);

        // Create pageable with limit (no default sorting)
        int safeLimit = Math.min(Math.max(limit, 1), 500); // Max 500 for map queries
        Pageable pageable = PageRequest.of(0, safeLimit);

        // Execute query
        Page<Listing> results = listingRepository.findAll(spec, pageable);

        log.debug("Map bounds query returned {} results", results.getNumberOfElements());

        return results;
    }
}