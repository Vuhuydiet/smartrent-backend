package com.smartrent.service.listing;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.ListingSectionsRequest;
import com.smartrent.dto.response.ListingCardListResponse;
import com.smartrent.dto.response.ListingSectionsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fans a single {@code POST /v1/listings/search/sections} request out into one
 * {@code searchListings} call per VIP tier, collapsing what used to be four
 * separate homepage round-trips into one.
 *
 * <p><b>Why delegate to {@link ListingService} (the bean) rather than inline the
 * query:</b> {@code searchListings} is {@code @Cacheable}, and Spring's cache
 * advice lives on the proxy. Calling it through the injected interface reference
 * goes through that proxy, so every tier still hits the listing-search cache and
 * runs the <i>exact</i> same specification + sort — the promoted ("đẩy tin")
 * ordering is preserved bit-for-bit. (Inlining, or a self-invocation inside
 * {@code ListingServiceImpl}, would silently bypass the cache.)
 *
 * <p>Each tier is isolated in its own try/catch so one failing tier degrades to
 * an empty carousel instead of blanking the whole homepage — mirroring the old
 * setup where each tier was an independent request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingSectionsService {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final ListingService listingService;

    public ListingSectionsResponse searchSections(ListingSectionsRequest request) {
        List<ListingSectionsRequest.Section> sections =
                request != null && request.getSections() != null
                        ? request.getSections()
                        : Collections.emptyList();

        Boolean verified = request != null ? request.getVerified() : null;
        int page = request != null && request.getPage() != null && request.getPage() > 0
                ? request.getPage()
                : 1;
        int defaultSize = clampSize(request != null ? request.getSize() : null);

        List<ListingSectionsResponse.SectionResult> results = new ArrayList<>(sections.size());
        for (ListingSectionsRequest.Section section : sections) {
            if (section == null || section.getVipType() == null || section.getVipType().isBlank()) {
                continue;
            }
            int size = section.getSize() != null && section.getSize() > 0
                    ? clampSize(section.getSize())
                    : defaultSize;

            // Built to be identical to the per-tier POST /search call it replaces.
            ListingFilterRequest filter = ListingFilterRequest.builder()
                    .vipType(section.getVipType())
                    .verified(verified)
                    .page(page)
                    .size(size)
                    .sortBy(section.getSortBy())
                    .build();

            ListingCardListResponse result;
            try {
                result = listingService.searchListings(filter);
            } catch (Exception e) {
                log.warn("Section search failed (vipType={}, sortBy={}): {}",
                        section.getVipType(), section.getSortBy(), e.getMessage());
                result = emptyResult(page, size);
            }

            results.add(ListingSectionsResponse.SectionResult.builder()
                    .vipType(section.getVipType())
                    .sortBy(section.getSortBy())
                    .result(result)
                    .build());
        }

        return ListingSectionsResponse.builder().sections(results).build();
    }

    private static int clampSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private static ListingCardListResponse emptyResult(int page, int size) {
        return ListingCardListResponse.builder()
                .listings(Collections.emptyList())
                .totalCount(0L)
                .currentPage(page)
                .pageSize(size)
                .totalPages(0)
                .build();
    }
}
