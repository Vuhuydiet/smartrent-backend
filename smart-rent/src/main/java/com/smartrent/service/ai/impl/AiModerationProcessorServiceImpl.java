package com.smartrent.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.request.DuplicateCheckRequest;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.dto.response.DuplicateCheckResponse;
import com.smartrent.enums.NotificationType;
import com.smartrent.infra.connector.SmartRentAiConnector;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.entity.Address;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.infra.repository.entity.enums.VerificationStatus;
import com.smartrent.service.ai.AiListingVerificationService;
import com.smartrent.service.ai.AiModerationExecutor;
import com.smartrent.service.ai.AiModerationProcessorService;
import com.smartrent.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiModerationProcessorServiceImpl implements AiModerationProcessorService {

    private final ListingAiModerationRepository listingAiModerationRepository;
    private final AiListingVerificationService aiVerificationService;
    private final NotificationService notificationService;
    private final SmartRentAiConnector smartRentAiConnector;
    private final ObjectMapper objectMapper;
    private final AiModerationExecutor aiModerationExecutor;

    /**
     * Pre-computes the AI analysis for a single listing and STORES it, in its own
     * independent transaction.
     *
     * <p><b>Store-only by design.</b> This never changes the listing's own state —
     * it does not touch {@code verified}, {@code isVerify} or {@code moderationStatus},
     * and it never approves or rejects. Its only job is to have the AI result ready
     * on {@link ListingAiModeration} so the admin review dialog can show it the
     * instant it opens, instead of making the admin wait ~30s for a live run. The
     * final approve/reject decision stays entirely with the admin, via
     * {@code PUT /v1/admin/listings/{id}/status}.
     *
     * <p>{@code moderation.verificationStatus} is set from the AI's suggestion, but
     * purely as an <i>opinion/processed marker</i> on the moderation record — it is
     * what stops the listing from being re-analysed on the next tick, and what the
     * dialog displays as "AI suggests …". It is not the listing's status.
     *
     * <p>Using {@code REQUIRES_NEW} ensures that:
     * <ul>
     *   <li>Each listing's result is committed immediately, even if another listing fails.</li>
     *   <li>A failure in one listing does not roll back the others in the parallel batch.</li>
     * </ul>
     *
     * <p>This method MUST live in a separate Spring bean (not the scheduler itself) so that
     * Spring's AOP proxy intercepts the call and applies the transaction correctly.
     * Calling a {@code @Transactional} method on {@code this} from within the same bean
     * bypasses the proxy and silently loses transaction isolation.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleListing(Listing listing, ListingAiModeration moderation) {
        try {
            log.info("Pre-computing AI analysis for listing ID: {}", listing.getListingId());

            AiListingVerificationRequest request =
                    aiVerificationService.buildVerificationRequest(listing.getListingId());

            // Verify and duplicate-check are independent — run them concurrently.
            CompletableFuture<AiListingVerificationResponse> verifyFuture =
                    CompletableFuture.supplyAsync(
                            () -> aiVerificationService.verifyListing(request),
                            aiModerationExecutor.taskPool());
            CompletableFuture<DuplicateCheckResponse> duplicateFuture =
                    CompletableFuture.supplyAsync(
                            () -> runDuplicateCheck(listing, request),
                            aiModerationExecutor.taskPool());

            AiListingVerificationResponse response = verifyFuture.join();
            DuplicateCheckResponse duplicateResult = duplicateFuture.join();

            moderation.setAiScore(response.getScore());

            String suggestedStatus = response.getSuggestedStatus();
            log.info("AI suggested status for listing ID {}: {} (advisory only — not applied)",
                    listing.getListingId(), suggestedStatus);

            // Record the AI's opinion on the moderation row only. The listing itself is
            // left untouched — it stays in the admin's review queue either way.
            if ("APPROVED".equals(suggestedStatus)) {
                moderation.setVerificationStatus(VerificationStatus.VERIFIED);
            } else if ("REJECTED".equals(suggestedStatus)) {
                moderation.setVerificationStatus(VerificationStatus.REJECTED);
            } else {
                moderation.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
            }

            try {
                // Persist verification + duplicate result together so the review dialog can
                // show image/video issues, violations, and any duplicate matches in one place.
                Map<String, Object> aiReason = new LinkedHashMap<>();
                aiReason.put("verification", response);
                aiReason.put("duplicateCheck", duplicateResult);
                moderation.setAiReason(objectMapper.writeValueAsString(aiReason));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize AI moderation result for listing ID: {}", listing.getListingId(), e);
            }

            listingAiModerationRepository.save(moderation);

            log.info("Stored AI analysis for listing ID: {} (aiSuggestion: {}) — awaiting admin review",
                    listing.getListingId(), moderation.getVerificationStatus());

            // Duplicates are the one thing worth proactively alerting admins about, since
            // the listing would otherwise look unremarkable in the review queue.
            if (duplicateResult != null
                    && ("DUPLICATE".equals(duplicateResult.getDecision())
                        || "SUSPICIOUS".equals(duplicateResult.getDecision()))) {
                log.info("Listing ID {} flagged as {} (score={}) — notifying admins.",
                        listing.getListingId(), duplicateResult.getDecision(), duplicateResult.getHighestScore());
                sendAdminDuplicateNotification(listing, duplicateResult);
            }

        } catch (Exception e) {
            log.error("Failed to pre-compute AI analysis for listing ID: {}", listing.getListingId(), e);
            moderation.setRetryCount(moderation.getRetryCount() + 1);
            moderation.setVerificationStatus(VerificationStatus.PENDING); // revert for retry
            listingAiModerationRepository.save(moderation);
        }
    }

    private void sendAdminDuplicateNotification(Listing listing, DuplicateCheckResponse duplicateResult) {
        String message = "Tin đăng \"" + listing.getTitle() + "\" (ID: " + listing.getListingId()
                + ") bị phát hiện " + duplicateResult.getDecision()
                + " (score=" + String.format("%.2f", duplicateResult.getHighestScore()) + "). Cần xem xét thủ công.";
        try {
            notificationService.sendToAllAdmins(
                    NotificationType.LISTING_DUPLICATE_DETECTED,
                    "Phát hiện tin đăng trùng lặp",
                    message,
                    listing.getListingId(), "LISTING");
        } catch (Exception e) {
            log.warn("Failed to send duplicate detection admin notification for listing {}: {}", listing.getListingId(), e.getMessage());
        }
    }

    /**
     * Calls the AI duplicate-detection endpoint for a listing. Reuses the fields
     * already gathered for verification and enriches them with structured
     * location (province/district) so the AI can retrieve candidates.
     *
     * <p>Best-effort by design: any failure (AI down, timeout, validation) is
     * swallowed and returns {@code null} (treated as PASS) so duplicate detection
     * can never block the pre-computation pipeline.
     */
    private DuplicateCheckResponse runDuplicateCheck(Listing listing, AiListingVerificationRequest request) {
        try {
            DuplicateCheckRequest.DuplicateCheckRequestBuilder builder = DuplicateCheckRequest.builder()
                    .listingId(listing.getListingId())
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .price(request.getPrice() != null ? request.getPrice().doubleValue() : null)
                    .area(request.getArea())
                    .address(request.getAddress())
                    .productType(listing.getProductType() != null ? listing.getProductType().name() : null)
                    .imageUrls(request.getImages());

            Address addr = listing.getAddress();
            if (addr != null) {
                String provinceCode = addr.getNewProvinceCode() != null
                        ? addr.getNewProvinceCode()
                        : (addr.getLegacyProvinceId() != null ? String.valueOf(addr.getLegacyProvinceId()) : null);
                builder.provinceCode(provinceCode).districtId(addr.getLegacyDistrictId());
            }

            return smartRentAiConnector.checkDuplicate(builder.build());
        } catch (Exception e) {
            log.warn("Duplicate check failed for listing ID {} (treating as PASS): {}",
                    listing.getListingId(), e.getMessage());
            return null;
        }
    }
}
