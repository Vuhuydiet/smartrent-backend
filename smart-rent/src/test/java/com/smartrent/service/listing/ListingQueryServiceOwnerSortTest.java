package com.smartrent.service.listing;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * getMyListings (isOwnerRequest=true) must sort strictly by date, ignoring
 * vipTypeSortOrder — a seller looking at only their own listings should not see
 * them regrouped by package tier when sorting by "Newest"/"Oldest". Public/admin
 * requests (isOwnerRequest=false) keep the existing VIP-first ordering.
 */
@ExtendWith(MockitoExtension.class)
class ListingQueryServiceOwnerSortTest {

    @Mock
    ListingRepository listingRepository;

    @InjectMocks
    ListingQueryService service;

    private static ListingFilterRequest.ListingFilterRequestBuilder baseFilter() {
        return ListingFilterRequest.builder().page(1).size(10);
    }

    private Sort captureSort(ListingFilterRequest filter) {
        when(listingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.executeQuery(filter);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(listingRepository)
                .findAll(any(Specification.class), pageableCaptor.capture());
        return pageableCaptor.getValue().getSort();
    }

    @Test
    void ownerRequest_newest_sortsByUpdatedAtOnly_noVipTierGrouping() {
        ListingFilterRequest filter = baseFilter()
                .isOwnerRequest(true)
                .sortBy("NEWEST")
                .build();

        Sort sort = captureSort(filter);

        assertEquals(Sort.by(Sort.Direction.DESC, "updatedAt"), sort);
    }

    @Test
    void ownerRequest_oldest_sortsByUpdatedAtOnly_noVipTierGrouping() {
        ListingFilterRequest filter = baseFilter()
                .isOwnerRequest(true)
                .sortBy("OLDEST")
                .build();

        Sort sort = captureSort(filter);

        assertEquals(Sort.by(Sort.Direction.ASC, "updatedAt"), sort);
    }

    @Test
    void ownerRequest_noSortBy_defaultsToUpdatedAtDesc_noVipTierGrouping() {
        ListingFilterRequest filter = baseFilter()
                .isOwnerRequest(true)
                .build();

        Sort sort = captureSort(filter);

        assertEquals(Sort.by(Sort.Direction.DESC, "updatedAt"), sort);
    }

    @Test
    void publicRequest_newest_stillGroupsByVipTierFirst() {
        ListingFilterRequest filter = baseFilter()
                .sortBy("NEWEST")
                .build();

        Sort sort = captureSort(filter);

        Sort expected = Sort.by(Sort.Direction.ASC, "vipTypeSortOrder")
                .and(Sort.by(Sort.Direction.DESC, "updatedAt"));
        assertEquals(expected, sort);
    }
}
