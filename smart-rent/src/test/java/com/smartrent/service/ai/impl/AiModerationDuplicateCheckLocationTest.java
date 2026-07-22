package com.smartrent.service.ai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.request.DuplicateCheckRequest;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.dto.response.DuplicateCheckResponse;
import com.smartrent.infra.connector.SmartRentAiConnector;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.service.ai.AiListingVerificationService;
import com.smartrent.service.ai.AiModerationExecutor;
import com.smartrent.service.notification.NotificationService;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The reconciliation sweep loads listings with a query that does not fetch the
 * address association and then processes them on the {@code ai-mod-task} pool,
 * with no session bound to that thread. Reading {@code listing.getAddress()}
 * for the duplicate check therefore threw LazyInitializationException, which
 * runDuplicateCheck catches and logs as "treating as PASS" — duplicate
 * detection was silently off for every listing that came through the sweep.
 *
 * <p>Location now comes from the listing's own denormalized columns (V97), so
 * no session is needed. This test proves it by making the address proxy throw.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiModerationDuplicateCheckLocationTest {

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

    @InjectMocks
    AiModerationProcessorServiceImpl service;

    private AiModerationProcessorServiceImpl newService() {
        return new AiModerationProcessorServiceImpl(
                listingAiModerationRepository,
                aiVerificationService,
                notificationService,
                smartRentAiConnector,
                objectMapper,
                aiModerationExecutor);
    }

    @Test
    void duplicateCheckReadsLocationWithoutTouchingTheAddressProxy() {
        // A detached listing: the address proxy blows up exactly like it does on
        // the ai-mod-task thread, but the denormalized columns are populated.
        Listing listing = new Listing() {
            @Override
            public com.smartrent.infra.repository.entity.Address getAddress() {
                throw new LazyInitializationException(
                        "Could not initialize proxy [Address#508468] - no session");
            }
        };
        listing.setListingId(716923L);
        listing.setTitle("Cho thuê phòng");
        listing.setNewProvinceCode("79");
        listing.setLegacyDistrictId(760);

        when(aiModerationExecutor.taskPool()).thenReturn(Runnable::run);
        when(aiVerificationService.buildVerificationRequest(anyLong()))
                .thenReturn(AiListingVerificationRequest.builder().title("Cho thuê phòng").build());
        when(aiVerificationService.verifyListing(any(AiListingVerificationRequest.class)))
                .thenReturn(AiListingVerificationResponse.builder().score(0.9).build());
        when(smartRentAiConnector.checkDuplicate(any()))
                .thenReturn(DuplicateCheckResponse.builder().decision("UNIQUE").build());

        newService().processSingleListing(listing, ListingAiModeration.builder()
                .listingId(716923L)
                .retryCount(0)
                .build());

        ArgumentCaptor<DuplicateCheckRequest> sent =
                ArgumentCaptor.forClass(DuplicateCheckRequest.class);
        verify(smartRentAiConnector).checkDuplicate(sent.capture());
        assertEquals("79", sent.getValue().getProvinceCode());
        assertEquals(760, sent.getValue().getDistrictId());
    }

    @Test
    void provinceCodeFallsBackToTheLegacyIdWhenThereIsNoNewCode() {
        Listing listing = new Listing();
        listing.setLegacyProvinceId(79);

        assertEquals("79", listing.resolveProvinceCodeForAi());
    }

    @Test
    void provinceCodePrefersTheNewCode() {
        Listing listing = new Listing();
        listing.setNewProvinceCode("79");
        listing.setLegacyProvinceId(1);

        assertEquals("79", listing.resolveProvinceCodeForAi());
    }

    @Test
    void provinceCodeIsNullWhenTheListingHasNoLocationAtAll() {
        assertEquals(null, new Listing().resolveProvinceCodeForAi());
    }
}
