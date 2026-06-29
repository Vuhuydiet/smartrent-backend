package com.smartrent.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.config.Constants;
import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.enums.ModerationStatus;
import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.infra.repository.entity.enums.VerificationStatus;
import com.smartrent.service.ai.AiListingVerificationService;
import com.smartrent.service.ai.AiModerationProcessorService;
import com.smartrent.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiModerationProcessorServiceImpl implements AiModerationProcessorService {

    private final ListingRepository listingRepository;
    private final ListingAiModerationRepository listingAiModerationRepository;
    private final AiListingVerificationService aiVerificationService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Processes a single listing in its own independent transaction.
     *
     * <p>Using {@code REQUIRES_NEW} ensures that:
     * <ul>
     *   <li>Each listing's result is committed immediately, even if another listing fails.</li>
     *   <li>A failure in one listing does not roll back the others in the parallel stream.</li>
     * </ul>
     *
     * <p>This method MUST live in a separate Spring bean (not the scheduler itself) so that
     * Spring's AOP proxy intercepts the call and applies the transaction correctly.
     * Calling a {@code @Transactional} method on {@code this} from within the same bean
     * bypasses the proxy and silently loses transaction isolation.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_BROWSE, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_DETAIL, key = "#listing.listingId"),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_STATS_CATEGORIES, allEntries = true)
    })
    public void processSingleListing(Listing listing, ListingAiModeration moderation) {
        try {
            log.info("Processing listing ID: {}", listing.getListingId());

            AiListingVerificationRequest request =
                    aiVerificationService.buildVerificationRequest(listing.getListingId());
            AiListingVerificationResponse response = aiVerificationService.verifyListing(request);

            moderation.setAiScore(response.getScore());
            try {
                // Persist the full AI response so admins can review image/video issues and violations.
                moderation.setAiReason(objectMapper.writeValueAsString(response));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize full AI response for listing ID: {}", listing.getListingId(), e);
            }

            String suggestedStatus = response.getSuggestedStatus();
            log.info("AI suggested status for listing ID {}: {}", listing.getListingId(), suggestedStatus);

            if ("APPROVED".equals(suggestedStatus)) {
                moderation.setVerificationStatus(VerificationStatus.VERIFIED);
                listing.setVerified(true);
                listing.setIsVerify(false); // clear the "pending review" flag so computeListingStatus() returns DISPLAYING
                listing.setModerationStatus(ModerationStatus.APPROVED);
            } else if ("REJECTED".equals(suggestedStatus)) {
                moderation.setVerificationStatus(VerificationStatus.REJECTED);
                listing.setVerified(false);
                listing.setIsVerify(false); // not pending review anymore, it's rejected
                listing.setModerationStatus(ModerationStatus.REJECTED);
            } else {
                // NEEDS_REVIEW or any unrecognized value → manual review queue
                moderation.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
                listing.setVerified(false);
                // Keep isVerify=true so owner can see it as IN_REVIEW
                listing.setModerationStatus(ModerationStatus.PENDING_REVIEW);
            }

            listingAiModerationRepository.save(moderation);
            listingRepository.save(listing);

            log.info("Finished processing listing ID: {}, verificationStatus: {}, moderationStatus: {}",
                    listing.getListingId(), moderation.getVerificationStatus(), listing.getModerationStatus());

            sendOwnerNotification(listing, suggestedStatus);

        } catch (Exception e) {
            log.error("Failed to verify listing ID: {}", listing.getListingId(), e);
            moderation.setRetryCount(moderation.getRetryCount() + 1);
            moderation.setVerificationStatus(VerificationStatus.PENDING); // revert for retry
            listingAiModerationRepository.save(moderation);
        }
    }

    private void sendOwnerNotification(Listing listing, String suggestedStatus) {
        if (listing.getUserId() == null) return;

        NotificationType notifType;
        String notifMessage;

        if ("APPROVED".equals(suggestedStatus)) {
            notifType = NotificationType.LISTING_APPROVED;
            notifMessage = "Tin đăng \"" + listing.getTitle() + "\" của bạn đã được hệ thống duyệt tự động và hiện đã hiển thị.";
        } else if ("REJECTED".equals(suggestedStatus)) {
            notifType = NotificationType.LISTING_REJECTED;
            notifMessage = "Tin đăng \"" + listing.getTitle() + "\" của bạn đã bị từ chối bởi hệ thống kiểm duyệt tự động.";
        } else {
            // NEEDS_REVIEW: listing vào hàng đợi kiểm duyệt thủ công, admin sẽ thông báo kết quả cuối
            return;
        }

        try {
            notificationService.sendNotification(
                    listing.getUserId(), RecipientType.USER,
                    notifType, "Cập nhật kiểm duyệt tin đăng", notifMessage,
                    listing.getListingId(), "LISTING");
        } catch (Exception e) {
            log.warn("Failed to send AI moderation notification for listing {}: {}", listing.getListingId(), e.getMessage());
        }
    }
}
