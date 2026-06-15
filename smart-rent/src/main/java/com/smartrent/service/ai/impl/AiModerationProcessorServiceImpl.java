package com.smartrent.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.request.DuplicateCheckRequest;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.dto.response.DuplicateCheckResponse;
import com.smartrent.enums.ModerationStatus;
import com.smartrent.infra.connector.SmartRentAiConnector;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Address;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.infra.repository.entity.enums.VerificationStatus;
import com.smartrent.service.ai.AiListingVerificationService;
import com.smartrent.service.ai.AiModerationProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiModerationProcessorServiceImpl implements AiModerationProcessorService {

    private final ListingRepository listingRepository;
    private final ListingAiModerationRepository listingAiModerationRepository;
    private final AiListingVerificationService aiVerificationService;
    private final SmartRentAiConnector smartRentAiConnector;
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
    public void processSingleListing(Listing listing, ListingAiModeration moderation) {
        try {
            log.info("Processing listing ID: {}", listing.getListingId());

            AiListingVerificationRequest request =
                    aiVerificationService.buildVerificationRequest(listing.getListingId());
            AiListingVerificationResponse response = aiVerificationService.verifyListing(request);

            moderation.setAiScore(response.getScore());

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

            // Duplicate detection — flag-for-admin, never hard-block. Best-effort:
            // any failure or AI downtime leaves the verification decision untouched.
            DuplicateCheckResponse duplicateResult = runDuplicateCheck(listing, request);
            if (duplicateResult != null
                    && ("DUPLICATE".equals(duplicateResult.getDecision())
                        || "SUSPICIOUS".equals(duplicateResult.getDecision()))) {
                log.info("Listing ID {} flagged as {} (score={}) — routing to admin review.",
                        listing.getListingId(), duplicateResult.getDecision(), duplicateResult.getHighestScore());
                // Do not auto-approve a suspected duplicate: downgrade an APPROVE to manual review.
                if (moderation.getVerificationStatus() == VerificationStatus.VERIFIED) {
                    moderation.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
                    listing.setVerified(false);
                    listing.setIsVerify(true); // back to IN_REVIEW so admin can adjudicate
                    listing.setModerationStatus(ModerationStatus.PENDING_REVIEW);
                }
            }

            try {
                // Persist verification + duplicate result together so admins can review
                // image/video issues, violations, and any duplicate matches in one place.
                Map<String, Object> aiReason = new LinkedHashMap<>();
                aiReason.put("verification", response);
                aiReason.put("duplicateCheck", duplicateResult);
                moderation.setAiReason(objectMapper.writeValueAsString(aiReason));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize AI moderation result for listing ID: {}", listing.getListingId(), e);
            }

            listingAiModerationRepository.save(moderation);
            listingRepository.save(listing);

            log.info("Finished processing listing ID: {}, verificationStatus: {}, moderationStatus: {}",
                    listing.getListingId(), moderation.getVerificationStatus(), listing.getModerationStatus());

        } catch (Exception e) {
            log.error("Failed to verify listing ID: {}", listing.getListingId(), e);
            moderation.setRetryCount(moderation.getRetryCount() + 1);
            moderation.setVerificationStatus(VerificationStatus.PENDING); // revert for retry
            listingAiModerationRepository.save(moderation);
        }
    }

    /**
     * Calls the AI duplicate-detection endpoint for a listing. Reuses the fields
     * already gathered for verification and enriches them with structured
     * location (province/district) so the AI can retrieve candidates.
     *
     * <p>Best-effort by design: any failure (AI down, timeout, validation) is
     * swallowed and returns {@code null} (treated as PASS) so duplicate detection
     * can never block the moderation pipeline.
     */
    private DuplicateCheckResponse runDuplicateCheck(Listing listing, AiListingVerificationRequest request) {
        try {
            DuplicateCheckRequest.DuplicateCheckRequestBuilder builder = DuplicateCheckRequest.builder()
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
