package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.AddressCreationRequest;
import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.infra.repository.ListingDraftRepository;
import com.smartrent.infra.repository.entity.ListingDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceImplPublishDraftTest {

    @Mock
    ListingDraftRepository listingDraftRepository;

    @InjectMocks
    ListingServiceImpl injected;

    ListingServiceImpl service;

    @BeforeEach
    void setUp() {
        // Spy so we can stub createListing() and isolate publishDraft's draft-lifecycle logic.
        service = spy(injected);
    }

    private ListingCreationRequest validPublishRequest() {
        return ListingCreationRequest.builder()
                .title("Nice room")
                .description("A description")
                .listingType("RENT")
                .productType("APARTMENT")
                .price(BigDecimal.valueOf(1_000_000))
                .priceUnit("VND")
                .categoryId(1L)
                .vipType("NORMAL")
                .address(AddressCreationRequest.builder().build())
                .build();
    }

    @Test
    void keepsDraftWhenPaymentRequired() {
        Long draftId = 1L;
        String userId = "user-1";
        when(listingDraftRepository.findByDraftIdAndUserId(draftId, userId))
                .thenReturn(Optional.of(ListingDraft.builder().build()));
        doReturn(ListingCreationResponse.builder().paymentRequired(true).transactionId("tx-1").build())
                .when(service).createListing(any());

        service.publishDraft(draftId, validPublishRequest(), userId);

        // Payment not yet completed -> draft must survive so a failed payment doesn't lose it.
        verify(listingDraftRepository, never()).delete(any());
    }

    @Test
    void propagatesDraftIdIntoCreateListingRequest() {
        Long draftId = 7L;
        String userId = "user-1";
        when(listingDraftRepository.findByDraftIdAndUserId(draftId, userId))
                .thenReturn(Optional.of(ListingDraft.builder().build()));
        doReturn(ListingCreationResponse.builder().paymentRequired(true).build())
                .when(service).createListing(any());

        service.publishDraft(draftId, validPublishRequest(), userId);

        ArgumentCaptor<ListingCreationRequest> captor = ArgumentCaptor.forClass(ListingCreationRequest.class);
        verify(service).createListing(captor.capture());
        // draftId must ride along so the post-payment callback can delete the draft.
        assertEquals(draftId, captor.getValue().getSourceDraftId());
    }

    @Test
    void deletesDraftWhenListingCreatedImmediately() {
        Long draftId = 2L;
        String userId = "user-1";
        ListingDraft draft = ListingDraft.builder().build();
        when(listingDraftRepository.findByDraftIdAndUserId(draftId, userId))
                .thenReturn(Optional.of(draft));
        doReturn(ListingCreationResponse.builder().paymentRequired(false).listingId(99L).build())
                .when(service).createListing(any());

        service.publishDraft(draftId, validPublishRequest(), userId);

        // Quota path: listing already exists, no pending payment -> draft is safe to remove now.
        verify(listingDraftRepository).delete(draft);
    }

    @Test
    void deletesDraftWhenListingWentStraightToReview() {
        // What the quota path actually returns: toCreationResponse() only fills
        // listingId + status, so paymentRequired comes back null rather than false.
        // The listing is live in the moderation queue ("đang chờ duyệt") and the
        // draft has to go with it — a null here must not read as "payment pending".
        Long draftId = 3L;
        String userId = "user-1";
        ListingDraft draft = ListingDraft.builder().build();
        when(listingDraftRepository.findByDraftIdAndUserId(draftId, userId))
                .thenReturn(Optional.of(draft));
        doReturn(ListingCreationResponse.builder().listingId(99L).status("CREATED").build())
                .when(service).createListing(any());

        service.publishDraft(draftId, validPublishRequest(), userId);

        verify(listingDraftRepository).delete(draft);
    }
}
