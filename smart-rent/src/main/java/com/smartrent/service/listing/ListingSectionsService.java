package com.smartrent.service.listing;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.ListingSectionsRequest;
import com.smartrent.dto.response.ListingCardListResponse;
import com.smartrent.dto.response.ListingSectionsResponse;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
 * ordering is preserved bit-for-bit.
 *
 * <p><b>Why parallel:</b> the four tiers are independent and each
 * {@code searchListings} is {@code @Transactional(readOnly = true)}, so it opens
 * its own session on whatever thread runs it (OSIV is off, so we rely on that
 * transaction — not a request-bound session). Running them on a dedicated pool
 * therefore reproduces the concurrency the old four-parallel-browser-calls had,
 * but with one round-trip: wall-clock ≈ the slowest single tier instead of the
 * sum of all four. Running them sequentially made a cold-cache homepage as slow
 * as 4× one query — the regression this fixes.
 *
 * <p>Each tier is isolated in its own try/catch so one failing tier degrades to
 * an empty carousel instead of blanking the whole homepage. {@code CallerRunsPolicy}
 * degrades gracefully to the request thread under saturation; daemon threads
 * never block JVM shutdown. Results are returned in request order regardless of
 * completion order.
 */
@Slf4j
@Service
public class ListingSectionsService {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final ListingService listingService;
    private final ThreadPoolExecutor pool;

    public ListingSectionsService(ListingService listingService) {
        this.listingService = listingService;
        this.pool = new ThreadPoolExecutor(
                4, 8,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(64),
                r -> {
                    Thread t = new Thread(r, "listing-section");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

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

        // Dispatch each tier onto the pool; collect in request order afterwards.
        List<CompletableFuture<ListingSectionsResponse.SectionResult>> futures = new ArrayList<>(sections.size());
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

            futures.add(CompletableFuture.supplyAsync(
                    () -> runSection(section, filter, page, size), pool));
        }

        List<ListingSectionsResponse.SectionResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        return ListingSectionsResponse.builder().sections(results).build();
    }

    private ListingSectionsResponse.SectionResult runSection(
            ListingSectionsRequest.Section section, ListingFilterRequest filter, int page, int size) {
        ListingCardListResponse result;
        try {
            result = listingService.searchListings(filter);
        } catch (Exception e) {
            log.warn("Section search failed (vipType={}, sortBy={}): {}",
                    section.getVipType(), section.getSortBy(), e.getMessage());
            result = emptyResult(page, size);
        }
        return ListingSectionsResponse.SectionResult.builder()
                .vipType(section.getVipType())
                .sortBy(section.getSortBy())
                .result(result)
                .build();
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

    @PreDestroy
    void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
