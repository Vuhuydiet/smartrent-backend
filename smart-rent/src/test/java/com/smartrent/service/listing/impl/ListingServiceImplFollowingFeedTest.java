package com.smartrent.service.listing.impl;

import com.smartrent.dto.response.ListingCardListResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.UserFollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GET /v1/listings/my-following-feed used to sort purely by postDate/createdAt,
 * so a DIAMOND-tier post from a followed user could sit behind an older NORMAL
 * post. Locks in that the feed reuses the same vipTypeSortOrder-first ordering
 * as the main search endpoint (see ListingQueryService#buildSort): DIAMOND(1)
 * before GOLD(2) before SILVER(3) before NORMAL(4), newest first within a tier.
 */
@ExtendWith(MockitoExtension.class)
class ListingServiceImplFollowingFeedTest {

    @Mock
    UserFollowRepository userFollowRepository;

    @Mock
    ListingRepository listingRepository;

    @InjectMocks
    ListingServiceImpl service;

    @Test
    void sortsByVipTierBeforeRecency() {
        when(userFollowRepository.findFollowingIdsByFollowerId("viewer-1"))
                .thenReturn(List.of("followed-1"));
        when(listingRepository.findPublicListingsByUserIdIn(anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.unpaged(), 0));

        ListingCardListResponse response =
                service.getListingsFromFollowedUsers("viewer-1", null, 1, 12);

        assertEquals(0, response.getTotalCount());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(listingRepository).findPublicListingsByUserIdIn(anyCollection(), pageableCaptor.capture());

        Iterator<Sort.Order> orders = pageableCaptor.getValue().getSort().iterator();

        Sort.Order first = orders.next();
        assertEquals("vipTypeSortOrder", first.getProperty());
        assertEquals(Sort.Direction.ASC, first.getDirection());

        Sort.Order second = orders.next();
        assertEquals("postDate", second.getProperty());
        assertEquals(Sort.Direction.DESC, second.getDirection());
    }
}
