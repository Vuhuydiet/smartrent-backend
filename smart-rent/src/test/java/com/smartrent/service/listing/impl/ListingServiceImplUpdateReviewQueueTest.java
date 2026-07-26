package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.ListingRequest;
import com.smartrent.enums.ModerationStatus;
import com.smartrent.event.ListingSubmittedEvent;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.infra.repository.entity.enums.VerificationStatus;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.mapper.UserMapper;
import com.smartrent.service.pricing.PricingHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An owner edit must invalidate the stored AI verdict and put the listing back on the
 * AI queue — the verdict describes the pre-edit content, and the admin review dialog
 * would otherwise show it against content that has since changed.
 */
@ExtendWith(MockitoExtension.class)
class ListingServiceImplUpdateReviewQueueTest {

    private static final Long LISTING_ID = 42L;
    private static final String USER_ID = "user-1";

    @Mock
    ListingRepository listingRepository;

    @Mock
    ListingAiModerationRepository listingAiModerationRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    ListingMapper listingMapper;

    @Mock
    PricingHistoryService pricingHistoryService;

    /** Only reached while building the response — mocked so updateListing doesn't NPE. */
    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @InjectMocks
    ListingServiceImpl service;

    private Listing existingListing(ModerationStatus status) {
        return Listing.builder()
                .listingId(LISTING_ID)
                .userId(USER_ID)
                .title("Phòng trọ Cầu Giấy")
                .description("Phòng sạch sẽ, đầy đủ nội thất")
                .price(BigDecimal.valueOf(3_000_000))
                .priceUnit(Listing.PriceUnit.MONTH)
                .area(25f)
                .moderationStatus(status)
                .verified(status == ModerationStatus.APPROVED)
                .isVerify(status != ModerationStatus.APPROVED)
                .build();
    }

    private Listing whenUpdating(Listing existing) {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(existing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        return existing;
    }

    private ListingAiModeration storedVerdict() {
        ListingAiModeration moderation = ListingAiModeration.builder()
                .listingId(LISTING_ID)
                .verificationStatus(VerificationStatus.VERIFIED)
                .retryCount(2)
                .manualOverride(true)
                .build();
        when(listingAiModerationRepository.findById(LISTING_ID)).thenReturn(Optional.of(moderation));
        return moderation;
    }

    @Test
    void editingAListingAlreadyWaitingInTheQueueReQueuesIt() {
        Listing existing = whenUpdating(existingListing(ModerationStatus.PENDING_REVIEW));
        ListingAiModeration moderation = storedVerdict();

        service.updateListing(LISTING_ID, ListingRequest.builder()
                .title("Phòng trọ Cầu Giấy - giá mới").build(), USER_ID);

        // Analysis already stored for the old title: reset it, or the worker skips the
        // listing as already-computed and the admin reviews a stale verdict.
        assertEquals(VerificationStatus.PENDING, moderation.getVerificationStatus());
        assertEquals(0, moderation.getRetryCount());
        verify(listingAiModerationRepository).save(moderation);
        verify(eventPublisher).publishEvent(new ListingSubmittedEvent(LISTING_ID));
        // Still awaiting review — an edit does not move it out of the queue.
        assertEquals(ModerationStatus.PENDING_REVIEW, existing.getModerationStatus());
    }

    @Test
    void editingAResubmittedListingReQueuesIt() {
        Listing existing = whenUpdating(existingListing(ModerationStatus.RESUBMITTED));
        storedVerdict();

        service.updateListing(LISTING_ID, ListingRequest.builder()
                .description("Mô tả đã sửa lại").build(), USER_ID);

        verify(eventPublisher).publishEvent(new ListingSubmittedEvent(LISTING_ID));
        assertEquals(ModerationStatus.RESUBMITTED, existing.getModerationStatus());
    }

    @Test
    void editingAnApprovedListingSendsItBackToReviewAndReQueuesIt() {
        Listing existing = whenUpdating(existingListing(ModerationStatus.APPROVED));
        storedVerdict();

        service.updateListing(LISTING_ID, ListingRequest.builder()
                .price(BigDecimal.valueOf(4_000_000)).build(), USER_ID);

        assertEquals(ModerationStatus.PENDING_REVIEW, existing.getModerationStatus());
        assertEquals(Boolean.FALSE, existing.getVerified());
        verify(eventPublisher).publishEvent(new ListingSubmittedEvent(LISTING_ID));
    }

    @Test
    void editingARevisionRequiredListingResetsTheVerdictButDoesNotQueue() {
        Listing existing = whenUpdating(existingListing(ModerationStatus.REVISION_REQUIRED));
        ListingAiModeration moderation = storedVerdict();

        service.updateListing(LISTING_ID, ListingRequest.builder()
                .title("Đã sửa theo yêu cầu").build(), USER_ID);

        // The worker ignores REVISION_REQUIRED — the owner still has to resubmit. Resetting
        // here is what makes that resubmit analyse the edited content.
        assertEquals(VerificationStatus.PENDING, moderation.getVerificationStatus());
        verify(eventPublisher, never()).publishEvent(any(ListingSubmittedEvent.class));
    }

    @Test
    void anEditThatChangesNothingTheAiSeesDoesNotReQueue() {
        Listing existing = whenUpdating(existingListing(ModerationStatus.PENDING_REVIEW));

        // Same values re-sent plus a field the AI is never shown: no new AI spend.
        service.updateListing(LISTING_ID, ListingRequest.builder()
                .title("Phòng trọ Cầu Giấy")
                .price(BigDecimal.valueOf(3_000_000))
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build(), USER_ID);

        verify(listingAiModerationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(ListingSubmittedEvent.class));
        assertEquals(ModerationStatus.PENDING_REVIEW, existing.getModerationStatus());
    }
}
