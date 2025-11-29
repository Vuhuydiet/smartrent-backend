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
        int page = Math.max(filter.getPage(), 0);
        int size = Math.min(Math.max(filter.getSize(), 1), 100);

        return PageRequest.of(page, size, sort);
    }

    /**
     * Build Sort object from sort field and direction
     *
     * @param sortBy Sort field name (postDate, price, area, createdAt, updatedAt)
     * @param sortDirection Sort direction (ASC or DESC)
     * @return Sort object for JPA query
     */
    private Sort buildSort(String sortBy, String sortDirection) {
        String sortField = sortBy != null ? sortBy : "postDate";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // Map sortBy field to entity field
        return switch (sortField) {
            case "price" -> Sort.by(direction, "price");
            case "area" -> Sort.by(direction, "area");
            case "createdAt" -> Sort.by(direction, "createdAt");
            case "updatedAt" -> Sort.by(direction, "updatedAt");
            default -> Sort.by(direction, "postDate");
        };
    }
}