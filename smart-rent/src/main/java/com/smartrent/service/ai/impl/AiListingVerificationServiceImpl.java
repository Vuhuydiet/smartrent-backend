package com.smartrent.service.ai.impl;

import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.request.DuplicateCheckRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.dto.response.DuplicateCheckResponse;
import com.smartrent.dto.response.StoredAiModerationResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.entity.Address;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.mapper.AiListingMapper;
import com.smartrent.service.ai.AiListingVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import org.springframework.transaction.annotation.Transactional;
import com.smartrent.infra.repository.ListingRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiListingVerificationServiceImpl implements AiListingVerificationService {

    private final com.smartrent.infra.connector.SmartRentAiConnector smartRentAiConnector;
    private final AiListingMapper aiListingMapper;
    private final ListingRepository listingRepository;
    private final ListingAiModerationRepository listingAiModerationRepository;
    private final ObjectMapper objectMapper;


    @Override
    public AiListingVerificationResponse verifyListing(AiListingVerificationRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            // Normalize request - set default values for optional fields
            AiListingVerificationRequest normalizedRequest = normalizeRequest(request);
            
            log.info("Sending listing verification request via FeignClient for listing: {}", 
                normalizedRequest.getTitle());
            
            // Map request to FeignClient DTO
            com.smartrent.dto.request.ListingVerificationRequest feignRequest = 
                com.smartrent.dto.request.ListingVerificationRequest.builder()
                .title(normalizedRequest.getTitle())
                .description(normalizedRequest.getDescription())
                .price(normalizedRequest.getPrice())
                .priceUnit(normalizedRequest.getPriceUnit())
                .listingType(normalizedRequest.getListingType())
                .area(normalizedRequest.getArea() != null ? normalizedRequest.getArea().floatValue() : null)
                .address(normalizedRequest.getAddress())
                .propertyType(normalizedRequest.getPropertyType() != null ? normalizedRequest.getPropertyType().name() : null)
                .amenities(normalizedRequest.getAmenities())
                .images(normalizedRequest.getImages())
                .direction(normalizedRequest.getDirection())
                .furnishing(normalizedRequest.getFurnishing())
                .roomCapacity(normalizedRequest.getRoomCapacity())
                .waterPrice(normalizedRequest.getWaterPrice())
                .electricityPrice(normalizedRequest.getElectricityPrice())
                .internetPrice(normalizedRequest.getInternetPrice())
                .serviceFee(normalizedRequest.getServiceFee())
                .build();
                
            // Set metadata if present
            if (normalizedRequest.getMetadata() != null) {
                feignRequest.setMetadata(com.smartrent.dto.request.ListingVerificationRequest.MetadataDto.builder()
                    .bedrooms(normalizedRequest.getMetadata().getBedrooms())
                    .bathrooms(normalizedRequest.getMetadata().getBathrooms())
                    .floor(normalizedRequest.getMetadata().getFloor() != null ? normalizedRequest.getMetadata().getFloor().toString() : null)
                    .build());
            }

            // Set videos if present
            if (normalizedRequest.getVideos() != null) {
                feignRequest.setVideos(normalizedRequest.getVideos().stream()
                    .map(v -> com.smartrent.dto.request.ListingVerificationRequest.VideoDto.builder()
                        .url(v.getUrl())
                        .thumbnailUrl(v.getThumbnailUrl())
                        .duration(v.getDurationSeconds() != null ? v.getDurationSeconds().floatValue() : null)
                        .build())
                    .toList());
            }

            log.debug("Making HTTP request to AI service...");
            com.smartrent.dto.response.ListingVerificationResponse feignResponse = 
                smartRentAiConnector.verifyListing(feignRequest);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("AI service responded in {}ms", processingTime);
            
            log.info("Processing AI verification response...");

            if (feignResponse != null) {
                log.info("AI verification completed successfully with score: {}, confidence: {}", 
                    feignResponse.getScore(), feignResponse.getConfidence());

                AiListingVerificationResponse responseBody = AiListingVerificationResponse.builder()
                    .isValid(feignResponse.isValid())
                    .score(feignResponse.getScore())
                    .confidence(feignResponse.getConfidence())
                    .suggestedStatus(feignResponse.getSuggestedStatus())
                    .modelUsed(feignResponse.getModelUsed())
                    .verificationTimestamp(feignResponse.getVerificationTimestamp())
                    .processingTimeSeconds(feignResponse.getProcessingTimeSeconds())
                    .violationCodes(feignResponse.getViolationCodes())
                    .build();

                // Map reason
                if (feignResponse.getReason() != null) {
                    responseBody.setReason(AiListingVerificationResponse.StructuredReason.builder()
                        .blurrinessIssue(feignResponse.getReason().isBlurrinessIssue())
                        .missingFields(feignResponse.getReason().getMissingFields())
                        .inconsistentInfo(feignResponse.getReason().isInconsistentInfo())
                        .watermarkOrPhone(feignResponse.getReason().isWatermarkOrPhone())
                        .stockPhoto(feignResponse.getReason().isStockPhoto())
                        .details(feignResponse.getReason().getDetails())
                        .build());
                }

                // Map image_validation
                if (feignResponse.getImageValidation() != null) {
                    var iv = feignResponse.getImageValidation();
                    responseBody.setImageValidation(AiListingVerificationResponse.ImageValidation.builder()
                        .isValid(iv.isValid())
                        .totalImages(iv.getTotalImages())
                        .validImages(iv.getValidImages())
                        .issues(iv.getIssues())
                        .qualityScore(iv.getQualityScore())
                        .build());
                }

                // Map video_validation
                if (feignResponse.getVideoValidation() != null) {
                    var vv = feignResponse.getVideoValidation();
                    responseBody.setVideoValidation(AiListingVerificationResponse.VideoValidation.builder()
                        .isValid(vv.isValid())
                        .totalVideos(vv.getTotalVideos())
                        .validVideos(vv.getValidVideos())
                        .issues(vv.getIssues())
                        .qualityScore(vv.getQualityScore())
                        .build());
                }

                // Map content_validation
                if (feignResponse.getContentValidation() != null) {
                    var cv = feignResponse.getContentValidation();
                    responseBody.setContentValidation(AiListingVerificationResponse.ContentValidation.builder()
                        .isRentalRelated(cv.getIsRentalRelated())
                        .categoryMatch(cv.getCategoryMatch())
                        .contentScore(cv.getContentScore())
                        .issues(cv.getIssues())
                        .build());
                }

                // Map completeness_validation
                if (feignResponse.getCompletenessValidation() != null) {
                    var cmv = feignResponse.getCompletenessValidation();
                    responseBody.setCompletenessValidation(AiListingVerificationResponse.CompletenessValidation.builder()
                        .isComplete(cmv.getIsComplete())
                        .completenessScore(cmv.getCompletenessScore())
                        .missingFields(cmv.getMissingFields())
                        .qualityIssues(cmv.getQualityIssues())
                        .build());
                }

                // Map violations
                if (feignResponse.getViolations() != null) {
                    responseBody.setViolations(feignResponse.getViolations().stream()
                        .map(v -> AiListingVerificationResponse.Violation.builder()
                            .category(v.getCategory())
                            .severity(v.getSeverity())
                            .message(v.getMessage())
                            .field(v.getField())
                            .build())
                        .toList());
                }

                // Map suggestions
                if (feignResponse.getSuggestions() != null) {
                    responseBody.setSuggestions(feignResponse.getSuggestions().stream()
                        .map(s -> AiListingVerificationResponse.Suggestion.builder()
                            .category(s.getCategory())
                            .message(s.getMessage())
                            .field(s.getField())
                            .priority(s.getPriority())
                            .build())
                        .toList());
                }

                if (!feignResponse.isValid()) {
                    log.warn("Listing is invalid. Issues: {}", feignResponse.getSuggestions());
                }

                return responseBody;
            } else {
                log.error("AI service returned null response");
                throw new AppException(DomainCode.AI_SERVICE_INVALID_RESPONSE);
            }
            
        } catch (feign.FeignException e) {
            // The AI service now reports a failed analysis as a real HTTP error
            // (503, body {"error": "<error_code>", "message": ..., ...}) instead of
            // a fake 200 with fabricated scores. Log the body here — it carries the
            // classified error_code (LLM_QUOTA_EXCEEDED, LLM_AUTH, ...) — so a
            // failure is diagnosable from server logs even though the admin only
            // sees a generic "AI verification failed" in the UI.
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("AI service returned an error after {}ms (status={}): {}",
                    processingTime, e.status(), e.contentUTF8());
            throw new AppException(e.status() == 503
                    ? DomainCode.AI_SERVICE_UNAVAILABLE
                    : DomainCode.AI_SERVICE_ERROR);
        } catch (ResourceAccessException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("AI service timeout or connection error after {}ms: {}", processingTime, e.getMessage());
            throw new AppException(DomainCode.AI_SERVICE_TIMEOUT);
        } catch (RestClientException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Failed to communicate with AI verification service after {}ms: {}", processingTime, e.getMessage(), e);
            throw new AppException(DomainCode.AI_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during AI verification after {}ms: {}", processingTime, e.getMessage(), e);
            throw new AppException(DomainCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    public AiListingVerificationResponse verifyListing(Listing listing) {
        if (listing == null) {
            throw new IllegalArgumentException("Listing cannot be null");
        }
        
        log.info("Converting listing {} to verification request", listing.getListingId());
        
        AiListingVerificationRequest request = aiListingMapper.toVerificationRequest(listing);
        if (request == null) {
            throw new AppException(DomainCode.AI_SERVICE_ERROR);
        }
        
        return verifyListing(request);
    }

    @Override
    @Transactional(readOnly = true)
    public AiListingVerificationRequest buildVerificationRequest(Long listingId) {
        Listing listing = listingRepository.findByIdWithMedia(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found with id: " + listingId));
        
        log.info("Converting listing {} to verification request inside transaction", listingId);
        
        AiListingVerificationRequest request = aiListingMapper.toVerificationRequest(listing);
        if (request == null) {
            throw new AppException(DomainCode.AI_SERVICE_ERROR);
        }
        
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public DuplicateCheckResponse checkDuplicateById(Long listingId) {
        AiListingVerificationRequest request = buildVerificationRequest(listingId);
        Listing listing = listingRepository.findByIdWithAmenities(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found with id: " + listingId));

        // Same mapping the auto-moderation cronjob uses (runDuplicateCheck): reuse
        // the fields already gathered for verification and enrich with structured
        // location + product type so the AI can retrieve candidates.
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
    }

    @Override
    @Transactional(readOnly = true)
    public StoredAiModerationResponse getStoredModerationResult(Long listingId) {
        ListingAiModeration moderation =
            listingAiModerationRepository.findById(listingId).orElse(null);
        if (moderation == null
                || moderation.getAiReason() == null
                || moderation.getAiReason().isBlank()) {
            return null;
        }
        try {
            // ai_reason is {verification, duplicateCheck}; verification re-serializes
            // snake_case and duplicateCheck camelCase — matching the FE types.
            StoredAiModerationResponse stored = objectMapper.readValue(
                moderation.getAiReason(), StoredAiModerationResponse.class);
            stored.setAiScore(moderation.getAiScore());
            stored.setVerificationStatus(moderation.getVerificationStatus() != null
                ? moderation.getVerificationStatus().name() : null);
            stored.setAnalyzedAt(moderation.getUpdatedAt());
            return stored;
        } catch (Exception e) {
            log.warn("Failed to parse stored aiReason for listing {}: {}",
                listingId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isServiceAvailable() {
        // Simplified check as feign handles connection issues natively via exceptions
        return true;
    }

    /**
     * Normalize request by setting default values for optional fields
     * This handles cases where frontend doesn't send certain metadata fields
     */
    private static final int AI_TITLE_MAX_LEN       = 500;
    private static final int AI_DESCRIPTION_MAX_LEN = 50000;
    private static final int AI_ADDRESS_MAX_LEN      = 500;
    private static final int AI_IMAGES_MAX_COUNT     = 20;
    private static final int AI_VIDEOS_MAX_COUNT     = 5;

    private AiListingVerificationRequest normalizeRequest(AiListingVerificationRequest request) {
        if (request.getMetadata() == null) {
            request.setMetadata(AiListingVerificationRequest.PropertyMetadata.builder().build());
        }
        if (request.getAmenities() == null) {
            request.setAmenities(new ArrayList<>());
        }
        if (request.getImages() == null) {
            request.setImages(new ArrayList<>());
        }
        if (request.getVideos() == null) {
            request.setVideos(new ArrayList<>());
        }

        if (request.getTitle() != null && request.getTitle().length() > AI_TITLE_MAX_LEN) {
            request.setTitle(request.getTitle().substring(0, AI_TITLE_MAX_LEN));
        }
        if (request.getDescription() != null && request.getDescription().length() > AI_DESCRIPTION_MAX_LEN) {
            request.setDescription(request.getDescription().substring(0, AI_DESCRIPTION_MAX_LEN));
        }
        if (request.getAddress() != null && request.getAddress().length() > AI_ADDRESS_MAX_LEN) {
            request.setAddress(request.getAddress().substring(0, AI_ADDRESS_MAX_LEN));
        }
        if (request.getImages().size() > AI_IMAGES_MAX_COUNT) {
            request.setImages(request.getImages().subList(0, AI_IMAGES_MAX_COUNT));
        }
        if (request.getVideos().size() > AI_VIDEOS_MAX_COUNT) {
            request.setVideos(request.getVideos().subList(0, AI_VIDEOS_MAX_COUNT));
        }

        log.debug("Normalized AI verification request - metadata: bedrooms={}, bathrooms={}",
            request.getMetadata().getBedrooms(),
            request.getMetadata().getBathrooms());

        return request;
    }
}