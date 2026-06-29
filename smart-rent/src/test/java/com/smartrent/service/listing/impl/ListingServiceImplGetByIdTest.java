package com.smartrent.service.listing.impl;

import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * A non-existent listing id must surface as 404 LISTING_NOT_FOUND, not a 500.
 * The AI's get_listing_detail tool has dedicated 404 handling that never fired
 * because the service threw a plain RuntimeException (→ catch-all 500).
 */
@ExtendWith(MockitoExtension.class)
class ListingServiceImplGetByIdTest {

    @Mock
    ListingRepository listingRepository;

    @InjectMocks
    ListingServiceImpl service;

    @Test
    void throwsListingNotFoundWhenMissing() {
        when(listingRepository.findByIdWithAmenities(123L)).thenReturn(Optional.empty());

        DomainException ex = assertThrows(DomainException.class,
                () -> service.getListingById(123L));
        assertEquals(DomainCode.LISTING_NOT_FOUND, ex.getDomainCode());
    }
}
