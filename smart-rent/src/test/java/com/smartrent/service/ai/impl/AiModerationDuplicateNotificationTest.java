package com.smartrent.service.ai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.dto.response.DuplicateCheckResponse;
import com.smartrent.enums.NotificationType;
import com.smartrent.infra.connector.SmartRentAiConnector;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.service.ai.AiListingVerificationService;
import com.smartrent.service.ai.AiModerationExecutor;
import com.smartrent.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A duplicate hit fans out a notification to EVERY admin. Before this, every
 * hit qualified: DUPLICATE and SUSPICIOUS alike, at any similarity score, and
 * again on every re-analysis of the same listing. Draining a backlog through
 * the backstop sweep would bury the admin inbox in flags nobody can action in
 * bulk — so the alert is gated, while the full result stays on the moderation
 * row for the review dialog either way.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiModerationDuplicateNotificationTest {

    private static final long LISTING_ID = 716923L;

    @Mock
    ListingAiModerationRepository listingAiModerationRepository;

    @Mock
    AiListingVerificationService aiVerificationService;

    @Mock
    NotificationService notificationService;

    @Mock
    SmartRentAiConnector smartRentAiConnector;

    @Mock
    AiModerationExecutor aiModerationExecutor;

    ObjectMapper objectMapper = new ObjectMapper();

    AiModerationProcessorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiModerationProcessorServiceImpl(
                listingAiModerationRepository,
                aiVerificationService,
                notificationService,
                smartRentAiConnector,
                objectMapper,
                aiModerationExecutor);
        // @Value defaults are not applied when the bean is built by hand.
        ReflectionTestUtils.setField(service, "notifySuspicious", false);
        ReflectionTestUtils.setField(service, "notifyMinScore", 0.85);

        when(aiModerationExecutor.taskPool()).thenReturn(Runnable::run);
        when(aiVerificationService.buildVerificationRequest(anyLong()))
                .thenReturn(AiListingVerificationRequest.builder().title("Cho thuê phòng").build());
        when(aiVerificationService.verifyListing(any(AiListingVerificationRequest.class)))
                .thenReturn(AiListingVerificationResponse.builder().score(0.9).build());
    }

    private void run(String decision, double score, String previousAiReason) {
        when(smartRentAiConnector.checkDuplicate(any())).thenReturn(
                DuplicateCheckResponse.builder().decision(decision).highestScore(score).build());

        Listing listing = new Listing();
        listing.setListingId(LISTING_ID);
        listing.setTitle("Cho thuê phòng");

        service.processSingleListing(listing, ListingAiModeration.builder()
                .listingId(LISTING_ID)
                .retryCount(0)
                .aiReason(previousAiReason)
                .build());
    }

    private void verifyNotified(int times) {
        verify(notificationService, times(times)).sendToAllAdmins(
                eq(NotificationType.LISTING_DUPLICATE_DETECTED),
                anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void confidentDuplicateStillNotifies() {
        run("DUPLICATE", 0.95, null);
        verifyNotified(1);
    }

    @Test
    void suspiciousIsSilentByDefault() {
        run("SUSPICIOUS", 0.99, null);
        verifyNotified(0);
    }

    @Test
    void suspiciousNotifiesWhenTheFlagIsTurnedOn() {
        ReflectionTestUtils.setField(service, "notifySuspicious", true);
        run("SUSPICIOUS", 0.99, null);
        verifyNotified(1);
    }

    @Test
    void duplicateBelowTheScoreThresholdIsSilent() {
        run("DUPLICATE", 0.70, null);
        verifyNotified(0);
    }

    @Test
    void aListingAlreadyReportedIsNotReportedAgain() {
        // The state a re-verify / retry / sweep re-run starts from.
        run("DUPLICATE", 0.95, "{\"duplicateCheck\":{\"decision\":\"DUPLICATE\"}}");
        verifyNotified(0);
    }

    @Test
    void aListingThatOnlyPassedBeforeIsReportedOnTheFirstDuplicateHit() {
        run("DUPLICATE", 0.95, "{\"duplicateCheck\":{\"decision\":\"PASS\"}}");
        verifyNotified(1);
    }

    @Test
    void unparseableOldReasonErrsTowardsNotifying() {
        run("DUPLICATE", 0.95, "not json at all");
        verifyNotified(1);
    }

    @Test
    void passNeverNotifies() {
        run("PASS", 0.99, null);
        verifyNotified(0);
    }

    @Test
    void theResultIsStoredEvenWhenTheAlertIsSuppressed() {
        // Suppressing the notification must not suppress the evidence — the
        // review dialog still needs to show why the listing was flagged.
        run("SUSPICIOUS", 0.99, null);
        verifyNotified(0);
        verify(listingAiModerationRepository).save(org.mockito.ArgumentMatchers.argThat(
                saved -> saved.getAiReason() != null && saved.getAiReason().contains("SUSPICIOUS")));
    }
}
