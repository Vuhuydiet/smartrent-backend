package com.smartrent.service.recommendation.impl;

import com.smartrent.dto.request.AIRecommendationRequest;
import com.smartrent.dto.response.AddressResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.RecentlyViewedItemResponse;
import com.smartrent.dto.response.RecommendationItemDto;
import com.smartrent.dto.response.RecommendationResponse;
import com.smartrent.infra.connector.SmartRentAiConnector;
import com.smartrent.infra.repository.AddressMappingRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.PhoneClickDetailRepository;
import com.smartrent.infra.repository.SavedListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.PhoneClickDetail;
import com.smartrent.infra.repository.entity.SavedListing;
import com.smartrent.infra.repository.entity.Address;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.recentlyviewed.RecentlyViewedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceImplTest {

    @Mock
    ListingRepository listingRepository;
    @Mock
    SavedListingRepository savedListingRepository;
    @Mock
    PhoneClickDetailRepository phoneClickDetailRepository;
    @Mock
    RecentlyViewedService recentlyViewedService;
    @Mock
    SmartRentAiConnector aiConnector;
    @Mock
    ListingService listingService;
    @Mock
    AddressMappingRepository addressMappingRepository;

    @Mock
    com.smartrent.service.recommendation.RecommendationExecutor recommendationExecutor;

    RecommendationServiceImpl recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationServiceImpl(
                listingRepository,
                savedListingRepository,
                phoneClickDetailRepository,
                recentlyViewedService,
                aiConnector,
                listingService,
                addressMappingRepository,
                recommendationExecutor
        );
        org.mockito.Mockito.lenient().when(recommendationExecutor.pool()).thenReturn(Runnable::run);
    }

    @Test
    void testRecommendationFlowCompilesAndRuns() {
        // Simple sanity test for setup
        assertNotNull(recommendationService);
    }

    @Test
    void testPersonalizedFeedColdStart() {
        // If there are no user interactions, it should fall back to cold start feed
        when(savedListingRepository.findByUserIdOrderByCreatedAtDesc("user1"))
                .thenReturn(Collections.emptyList());
        when(phoneClickDetailRepository.findListingIdsByUserId("user1"))
                .thenReturn(Collections.emptyList());
        when(recentlyViewedService.getRecentlyViewed("user1"))
                .thenReturn(Collections.emptyList());

        // For cold start
        Listing coldListing = new Listing();
        coldListing.setListingId(999L);
        when(listingRepository.findCandidatesForPersonalizedGlobal(anyList(), any()))
                .thenReturn(List.of(coldListing));

        ListingResponse coldResponse = new ListingResponse();
        coldResponse.setListingId(999L);
        when(listingService.getListingsByIds(anySet()))
                .thenReturn(List.of(coldResponse));

        RecommendationResponse response = recommendationService.getPersonalizedFeed("user1", 10);
        assertNotNull(response);
        assertTrue(response.getColdStart());
        assertEquals(1, response.getListings().size());
        assertEquals(999L, response.getListings().get(0).getListingId());
    }
}
