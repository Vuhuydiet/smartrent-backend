package com.smartrent.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.infra.connector.SmartRentAiConnector;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.infra.repository.entity.enums.VerificationStatus;
import com.smartrent.service.ai.impl.AiModerationProcessorServiceImpl;
import com.smartrent.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The analysis runs for ~30s off the request thread, which is plenty of time for the
 * owner to edit the listing underneath it. The edit resets the moderation row to
 * PENDING and re-queues it, so the in-flight verdict must not be written back.
 */
@ExtendWith(MockitoExtension.class)
class AiModerationProcessorServiceImplTest {

    private static final Long LISTING_ID = 7L;

    @Mock
    ListingAiModerationRepository listingAiModerationRepository;

    @Mock
    AiListingVerificationService aiVerificationService;

    @Mock
    NotificationService notificationService;

    @Mock
    SmartRentAiConnector smartRentAiConnector;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    AiModerationExecutor aiModerationExecutor;

    @InjectMocks
    AiModerationProcessorServiceImpl service;

    Listing listing;
    ListingAiModeration claimed;

    @BeforeEach
    void setUp() {
        listing = Listing.builder().listingId(LISTING_ID).title("Phòng trọ").build();
        // What AiAnalysisWorker hands over: the row it just claimed, now detached.
        claimed = ListingAiModeration.builder()
                .listingId(LISTING_ID)
                .verificationStatus(VerificationStatus.IN_PROGRESS)
                .retryCount(0)
                .manualOverride(false)
                .build();

        // Run the verify/duplicate fan-out on the calling thread.
        when(aiModerationExecutor.taskPool()).thenReturn(Runnable::run);
        when(aiVerificationService.buildVerificationRequest(LISTING_ID))
                .thenReturn(AiListingVerificationRequest.builder().title("Phòng trọ").build());
        when(aiVerificationService.verifyListing(any(AiListingVerificationRequest.class)))
                .thenReturn(AiListingVerificationResponse.builder()
                        .score(0.9)
                        .suggestedStatus("APPROVED")
                        .build());
    }

    @Test
    void storesTheVerdictWhenTheListingWasNotTouched() {
        when(listingAiModerationRepository.findById(LISTING_ID))
                .thenReturn(Optional.of(claimed));

        service.processSingleListing(listing, claimed);

        verify(listingAiModerationRepository).save(claimed);
    }

    @Test
    void discardsTheVerdictWhenTheListingWasEditedMidAnalysis() {
        // The owner's edit committed while the AI was running: row is back to PENDING.
        when(listingAiModerationRepository.findById(LISTING_ID))
                .thenReturn(Optional.of(ListingAiModeration.builder()
                        .listingId(LISTING_ID)
                        .verificationStatus(VerificationStatus.PENDING)
                        .retryCount(0)
                        .manualOverride(false)
                        .build()));

        service.processSingleListing(listing, claimed);

        // Writing it would both show the admin a verdict for content that no longer
        // exists and make the re-queued delivery skip as "already computed".
        verify(listingAiModerationRepository, never()).save(any());
    }
}
