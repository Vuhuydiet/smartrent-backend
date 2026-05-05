package com.smartrent.service.ai.impl;

import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.entity.Listing;
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
                .area(normalizedRequest.getArea() != null ? normalizedRequest.getArea().floatValue() : null)
                .address(normalizedRequest.getAddress())
                .propertyType(normalizedRequest.getPropertyType() != null ? normalizedRequest.getPropertyType().name() : null)
                .amenities(normalizedRequest.getAmenities())
                .images(normalizedRequest.getImages())
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
    public boolean isServiceAvailable() {
        // Simplified check as feign handles connection issues natively via exceptions
        return true;
    }

    /**
     * Normalize request by setting default values for optional fields
     * This handles cases where frontend doesn't send certain metadata fields
     */
    private AiListingVerificationRequest normalizeRequest(AiListingVerificationRequest request) {
        // Handle null metadata
        if (request.getMetadata() == null) {
            request.setMetadata(AiListingVerificationRequest.PropertyMetadata.builder()
                    .build());
        }

        // Handle null amenities list
        if (request.getAmenities() == null) {
            request.setAmenities(new ArrayList<>());
        }

        // Handle null images list
        if (request.getImages() == null) {
            request.setImages(new ArrayList<>());
        }

        // Handle null videos list
        if (request.getVideos() == null) {
            request.setVideos(new ArrayList<>());
        }

        log.debug("Normalized AI verification request - metadata: bedrooms={}, bathrooms={}", 
            request.getMetadata().getBedrooms(),
            request.getMetadata().getBathrooms());

        return request;
    }
}