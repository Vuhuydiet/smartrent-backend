package com.smartrent.service.listing;

import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * queryByMapBounds must go through the FORCE INDEX path (findMapBoundsListingIds +
 * countMapBoundsListings), NOT findAll(spec, pageable). With the map query's bbox
 * + ORDER BY + LIMIT shape the optimizer picks idx_listings_sort_order and filters
 * row-by-row (~1.5s at city zoom, worse when zoomed into a sparse area) instead of
 * the covering geo index idx_listings_map_bounds (~70ms). The wiring must also
 * preserve the native VIP-first order after hydration (IN loses it) and cap the
 * limit at 500.
 *
 * The native SQL itself can't run on the H2 test DB (FORCE INDEX / MySQL-only), so
 * the repository is mocked here; the SQL was verified against prod via EXPLAIN
 * ANALYZE (covering range scan on idx_listings_map_bounds, ~70ms).
 */
@ExtendWith(MockitoExtension.class)
class ListingQueryServiceMapBoundsTest {

    @Mock
    ListingRepository listingRepository;

    @InjectMocks
    ListingQueryService service;

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }

    @Test
    void queryByMapBounds_usesForcedIndex_preservesOrder_andTotal() {
        when(listingRepository.findMapBoundsListingIds(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(30L, 10L, 20L)); // VIP-first native order
        when(listingRepository.countMapBoundsListings(any(), any(), any(), any(), any()))
                .thenReturn(57L);
        when(listingRepository.findAllById(any())) // IN returns them out of order
                .thenReturn(List.of(
                        Listing.builder().listingId(10L).build(),
                        Listing.builder().listingId(30L).build(),
                        Listing.builder().listingId(20L).build()));

        // limit (=3) <= total (=57): the 3 pins are the capped page, and totalCount
        // still reports the full 57 (the FE "X tổng, phóng to" hint). Keeping limit
        // <= total avoids PageImpl re-deriving total as a "last page" — which in
        // production never triggers, since the id query returns min(total, limit).
        Page<Listing> page = service.queryByMapBounds(
                bd(10.823), bd(106.701), bd(10.705), bd(106.590),
                3, false, null, null);

        assertEquals(57L, page.getTotalElements(), "totalCount comes from the count query");
        assertEquals(List.of(30L, 10L, 20L),
                page.getContent().stream().map(Listing::getListingId).toList(),
                "native VIP-first order must survive hydration");
        // The Criteria path (which the optimizer mis-plans) must not be used.
        verify(listingRepository, never())
                .findAll(org.mockito.ArgumentMatchers.<Specification<Listing>>any(), any(Pageable.class));
    }

    @Test
    void queryByMapBounds_capsLimitAt500() {
        when(listingRepository.findMapBoundsListingIds(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(listingRepository.countMapBoundsListings(any(), any(), any(), any(), any()))
                .thenReturn(0L);

        service.queryByMapBounds(bd(10.9), bd(106.8), bd(10.7), bd(106.5),
                9999, false, null, null);

        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(listingRepository).findMapBoundsListingIds(
                any(), any(), any(), any(), any(), limitCaptor.capture());
        assertEquals(500, limitCaptor.getValue(), "map limit is capped at 500");
    }
}
