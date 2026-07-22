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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    /**
     * The map draws one thumbnail per pin and a "N photos" badge, so shipping
     * every image URL for up to 500 cards is dead weight. Media must be trimmed
     * to the first IMAGE while imageCount keeps the real total for the badge.
     */
    @Test
    void mapBoundsTrimsMediaToTheThumbnailAndKeepsTheImageCount() {
        MapListingsResponse response = runMapBounds(ListingCardResponse.builder()
                .listingId(1L)
                .imageCount(6)
                .media(new ArrayList<>(List.of(
                        mediaCard("VIDEO", "https://cdn/clip.mp4"),
                        mediaCard("IMAGE", "https://cdn/first.jpg"),
                        mediaCard("IMAGE", "https://cdn/second.jpg"))))
                .build());

        List<ListingCardResponse.MediaCard> media = response.getListings().get(0).getMedia();
        assertEquals(1, media.size());
        // The first IMAGE, not the first entry -- a listing whose primary media
        // is a video must still come back with a usable thumbnail.
        assertEquals("https://cdn/first.jpg", media.get(0).getUrl());
        assertEquals(6, response.getListings().get(0).getImageCount());
    }

    /**
     * POST /v1/listings/map-bounds is public and unauthenticated, so the owner
     * contact fields must not ride along on up to 500 cards per request. The
     * name/avatar/broker badge the card actually renders stays.
     */
    @Test
    void mapBoundsStripsOwnerContactDetails() {
        MapListingsResponse response = runMapBounds(ListingCardResponse.builder()
                .listingId(1L)
                .user(ListingCardResponse.UserCard.builder()
                        .userId("u1")
                        .firstName("Minh")
                        .email("owner@example.com")
                        .contactPhoneNumber("0900000000")
                        .contactPhoneVerified(true)
                        .isBroker(true)
                        .build())
                .build());

        ListingCardResponse.UserCard user = response.getListings().get(0).getUser();
        assertNull(user.getEmail());
        assertNull(user.getContactPhoneNumber());
        assertEquals("Minh", user.getFirstName());
        assertEquals(Boolean.TRUE, user.getContactPhoneVerified());
        assertEquals(Boolean.TRUE, user.getIsBroker());
    }

    /** The description body is never rendered on the map. */
    @Test
    void mapBoundsDropsTheDescription() {
        MapListingsResponse response = runMapBounds(ListingCardResponse.builder()
                .listingId(1L)
                .description("A very long description body")
                .build());

        assertNull(response.getListings().get(0).getDescription());
    }

    private static ListingCardResponse.MediaCard mediaCard(String type, String url) {
        return ListingCardResponse.MediaCard.builder().mediaType(type).url(url).build();
    }

    /** Drives getListingsByMapBounds with a single stubbed card. */
    private MapListingsResponse runMapBounds(ListingCardResponse card) {
        Listing listing = Listing.builder().listingId(1L).build();

        when(listingQueryService.queryByMapBounds(
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(listing), Pageable.unpaged(), 1));
        when(listingRepository.findByIdsWithMediaAndAddress(anyCollection()))
                .thenReturn(List.of(listing));
        when(listingMapper.toCardResponse(any(), any(), any())).thenReturn(card);

        return service.getListingsByMapBounds(MapBoundsRequest.builder()
                .neLat(BigDecimal.valueOf(10.823))
                .neLng(BigDecimal.valueOf(106.701))
                .swLat(BigDecimal.valueOf(10.705))
                .swLng(BigDecimal.valueOf(106.590))
                .zoom(14)
                .limit(100)
                .build());
    }
}
