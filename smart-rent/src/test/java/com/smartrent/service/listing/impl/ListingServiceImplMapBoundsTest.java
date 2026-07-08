package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.MapBoundsRequest;
import com.smartrent.dto.response.ListingCardResponse;
import com.smartrent.dto.response.MapListingsResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.service.listing.ListingQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GET /v1/listings/map-bounds was slow because it mapped results through
 * batchMapListings(), which LEFT JOIN FETCHes listing_amenities (a
 * @ManyToMany) even though the map only ever renders card-level fields
 * (title, price, area, first-image thumbnail). That join multiplies the
 * result set by amenity count per listing for data the map never displays.
 * Locks in the fix: the endpoint must use the card path and must never
 * touch the amenities batch-load.
 *
 * Also locks in the N+1 fix: the card path must hydrate media AND address in a
 * single query (findByIdsWithMediaAndAddress). The map-bounds query joins
 * addresses only for its WHERE/sort, so without this the per-card
 * getAddress() would lazy-load one row at a time -- an N+1 of up to 200
 * queries. The media-only findByIdsWithMedia must no longer be used here.
 */
@ExtendWith(MockitoExtension.class)
class ListingServiceImplMapBoundsTest {

    @Mock
    ListingQueryService listingQueryService;

    @Mock
    ListingRepository listingRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ListingMapper listingMapper;

    @InjectMocks
    ListingServiceImpl service;

    @Test
    void mapBoundsUsesCardResponseAndSkipsAmenitiesFetch() {
        Listing listing = Listing.builder()
                .listingId(1L)
                .build();

        when(listingQueryService.queryByMapBounds(
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(listing), Pageable.unpaged(), 1));
        when(listingRepository.findByIdsWithMediaAndAddress(anyCollection()))
                .thenReturn(List.of(listing));
        when(listingMapper.toCardResponse(any(), any(), any()))
                .thenReturn(ListingCardResponse.builder().listingId(1L).build());

        MapBoundsRequest request = MapBoundsRequest.builder()
                .neLat(BigDecimal.valueOf(10.823))
                .neLng(BigDecimal.valueOf(106.701))
                .swLat(BigDecimal.valueOf(10.705))
                .swLng(BigDecimal.valueOf(106.590))
                .zoom(14)
                .limit(100)
                .build();

        MapListingsResponse response = service.getListingsByMapBounds(request);

        assertEquals(1, response.getListings().size());
        assertEquals(1L, response.getListings().get(0).getListingId());
        verify(listingRepository, never()).findByIdsWithAmenities(anyCollection());
        // Media + address hydrated together; the media-only path (which left
        // address lazy and caused the N+1) must not be used on the map path.
        verify(listingRepository).findByIdsWithMediaAndAddress(anyCollection());
        verify(listingRepository, never()).findByIdsWithMedia(anyCollection());
    }
}
