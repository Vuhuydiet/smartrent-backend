package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.AddressCreationRequest;
import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.ListingDraftRepository;
import com.smartrent.infra.repository.entity.ListingDraft;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.service.transaction.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    TransactionService transactionService;

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
    void publishesNewAddressDraftWithItsOwnStreet() {
        // A pure new-address draft only fills new_street; street stays null. Reading the
        // legacy column here published the listing with no street at all, and the draft
        // screen hid it by rendering new_street correctly.
        Long draftId = 4L;
        String userId = "user-1";
        ListingDraft draft = ListingDraft.builder()
                .addressType("NEW")
                .provinceCode("79")
                .wardCode("26734")
                .newStreet("Nguyễn Huệ")
                .build();
        when(listingDraftRepository.findByDraftIdAndUserId(draftId, userId))
                .thenReturn(Optional.of(draft));
        doReturn(ListingCreationResponse.builder().listingId(1L).build())
                .when(service).createListing(any());

        ListingCreationRequest request = validPublishRequest();
        request.setAddress(null); // client sends no address -> merge must fall back to the draft

        service.publishDraft(draftId, request, userId);

        ArgumentCaptor<ListingCreationRequest> captor = ArgumentCaptor.forClass(ListingCreationRequest.class);
        verify(service).createListing(captor.capture());
        assertEquals("Nguyễn Huệ", captor.getValue().getAddress().getNewAddress().getStreet());
    }

    @Test
    void emptyMediaIdsInPublishBodyDoesNotWipeTheDraftsMedia() {
        // "mediaIds": [] used to count as authoritative, so the listing published with no
        // photos and the draft holding them was deleted immediately after.
        Long draftId = 5L;
        String userId = "user-1";
        ListingDraft draft = ListingDraft.builder().mediaIds("11,22,33").build();
        when(listingDraftRepository.findByDraftIdAndUserId(draftId, userId))
                .thenReturn(Optional.of(draft));
        doReturn(ListingCreationResponse.builder().listingId(1L).build())
                .when(service).createListing(any());

        ListingCreationRequest request = validPublishRequest();
        request.setMediaIds(Set.of());

        service.publishDraft(draftId, request, userId);

        ArgumentCaptor<ListingCreationRequest> captor = ArgumentCaptor.forClass(ListingCreationRequest.class);
        verify(service).createListing(captor.capture());
        assertEquals(Set.of(11L, 22L, 33L), captor.getValue().getMediaIds());
    }

    @Test
    void blocksRepublishWhileTheFirstPublishIsStillAwaitingPayment() {
        // Publishing twice is how a user ends up charged with nothing created: the second
        // publish stamps the media with a new listing id, and the first payment's callback
        // can then never link it — a failure the IPN handler swallows.
        Long draftId = 6L;
        String userId = "user-1";
        ListingDraft draft = ListingDraft.builder().pendingTransactionId("tx-pending").build();
        when(listingDraftRepository.findByDraftIdAndUserId(draftId, userId))
                .thenReturn(Optional.of(draft));
        Transaction pending = new Transaction();
        pending.setStatus(TransactionStatus.PENDING);
        when(transactionService.getTransaction("tx-pending")).thenReturn(pending);

        AppException thrown = assertThrows(AppException.class,
                () -> service.publishDraft(draftId, validPublishRequest(), userId));

        assertEquals(DomainCode.DRAFT_PUBLISH_ALREADY_PENDING, thrown.getErrorCode());
        verify(service, never()).createListing(any());
        verify(listingDraftRepository, never()).delete(any());
    }

    @Test
    void allowsRepublishOnceTheEarlierPaymentHasFailed() {
        // The draft is kept precisely so an abandoned payment doesn't lose the user's
        // work — a dead transaction must not lock them out of it forever.
        Long draftId = 7L;
        String userId = "user-1";
        ListingDraft draft = ListingDraft.builder().pendingTransactionId("tx-failed").build();
        when(listingDraftRepository.findByDraftIdAndUserId(draftId, userId))
                .thenReturn(Optional.of(draft));
        Transaction failed = new Transaction();
        failed.setStatus(TransactionStatus.FAILED);
        when(transactionService.getTransaction("tx-failed")).thenReturn(failed);
        doReturn(ListingCreationResponse.builder().listingId(1L).build())
                .when(service).createListing(any());

        service.publishDraft(draftId, validPublishRequest(), userId);

        verify(service).createListing(any());
        assertNull(draft.getPendingTransactionId());
    }

    @Test
    void recordsThePendingTransactionOnTheDraftWhenPaymentIsRequired() {
        Long draftId = 8L;
        String userId = "user-1";
        ListingDraft draft = ListingDraft.builder().build();
        when(listingDraftRepository.findByDraftIdAndUserId(draftId, userId))
                .thenReturn(Optional.of(draft));
        doReturn(ListingCreationResponse.builder().paymentRequired(true).transactionId("tx-new").build())
                .when(service).createListing(any());

        service.publishDraft(draftId, validPublishRequest(), userId);

        // Without this the next publish has nothing to check against.
        assertEquals("tx-new", draft.getPendingTransactionId());
        verify(listingDraftRepository).save(draft);
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
