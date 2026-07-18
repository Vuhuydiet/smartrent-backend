package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.SavedListingRequest;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.SavedListingResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.SavedListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.SavedListing;
import com.smartrent.infra.repository.entity.SavedListingId;
import com.smartrent.mapper.SavedListingMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Saved listings have no visibility filter of their own (see
 * SavedListingRepository), so an expired/unpublished listing used to keep
 * showing full data in "Tin đã lưu" even though opening it 404s. These tests
 * lock in the two fixes: the list-fetch drops (and deletes) stale rows, and
 * saveListing() enforces the per-user cap instead of growing unbounded.
 */
@ExtendWith(MockitoExtension.class)
class SavedListingServiceImplTest {

    private static final String USER_ID = "user-1";

    @Mock
    SavedListingRepository savedListingRepository;

    @Mock
    SavedListingMapper savedListingMapper;

    @InjectMocks
    SavedListingServiceImpl service;

    @BeforeEach
    void setUpAuthentication() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveListingThrowsWhenAtLimit() {
        SavedListingRequest request = SavedListingRequest.builder().listingId(999L).build();
        when(savedListingRepository.existsByIdUserIdAndIdListingId(USER_ID, 999L)).thenReturn(false);
        when(savedListingRepository.countByIdUserId(USER_ID)).thenReturn(50L);

        DomainException ex = assertThrows(DomainException.class, () -> service.saveListing(request));

        assertEquals(DomainCode.SAVED_LISTING_LIMIT_EXCEEDED, ex.getDomainCode());
        verify(savedListingRepository, never()).save(any());
    }

    @Test
    void saveListingSucceedsWhenUnderLimit() {
        SavedListingRequest request = SavedListingRequest.builder().listingId(999L).build();
        SavedListing entity = SavedListing.builder().id(new SavedListingId(USER_ID, 999L)).build();
        when(savedListingRepository.existsByIdUserIdAndIdListingId(USER_ID, 999L)).thenReturn(false);
        when(savedListingRepository.countByIdUserId(USER_ID)).thenReturn(49L);
        when(savedListingMapper.toEntity(request, USER_ID)).thenReturn(entity);
        when(savedListingRepository.save(entity)).thenReturn(entity);
        when(savedListingMapper.toResponse(entity)).thenReturn(SavedListingResponse.builder().build());

        service.saveListing(request);

        verify(savedListingRepository, times(1)).save(entity);
    }

    @Test
    void getMySavedListingsDropsAndDeletesStaleEntries() {
        SavedListing visible = SavedListing.builder()
                .id(new SavedListingId(USER_ID, 1L))
                .listing(Listing.builder().listingId(1L).verified(true).expired(false).build())
                .build();
        SavedListing expired = SavedListing.builder()
                .id(new SavedListingId(USER_ID, 2L))
                .listing(Listing.builder().listingId(2L).verified(true).expired(true).build())
                .build();
        when(savedListingRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(visible, expired));
        lenient().when(savedListingMapper.toResponseWithListing(visible))
                .thenReturn(SavedListingResponse.builder().listingId(1L).build());

        List<SavedListingResponse> result = service.getMySavedListings();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getListingId());
        verify(savedListingRepository, times(1)).deleteByIdUserIdAndIdListingId(USER_ID, 2L);
        verify(savedListingRepository, never()).deleteByIdUserIdAndIdListingId(USER_ID, 1L);
    }

    @Test
    void getMySavedListingsPagedComputesTotalsFromVisibleOnly() {
        SavedListing visible = SavedListing.builder()
                .id(new SavedListingId(USER_ID, 1L))
                .listing(Listing.builder().listingId(1L).verified(true).expired(false).build())
                .build();
        SavedListing expired = SavedListing.builder()
                .id(new SavedListingId(USER_ID, 2L))
                .listing(Listing.builder().listingId(2L).verified(true).expired(true).build())
                .build();
        when(savedListingRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(visible, expired));
        lenient().when(savedListingMapper.toResponseWithListing(visible))
                .thenReturn(SavedListingResponse.builder().listingId(1L).build());

        PageResponse<SavedListingResponse> result = service.getMySavedListings(1, 10);

        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getData().size());
        verify(savedListingRepository, times(1)).deleteByIdUserIdAndIdListingId(eq(USER_ID), eq(2L));
    }
}
