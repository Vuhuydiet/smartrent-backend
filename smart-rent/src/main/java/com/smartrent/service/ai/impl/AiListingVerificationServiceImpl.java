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
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiListingVerificationServiceImpl implements AiListingVerificationService {

    private final RestTemplate restTemplate;
    private final AiListingMapper aiListingMapper;

    @Value("${smartrent.ai.verification.url:http://localhost:8000/ai/verify-listing}")
    private String aiVerificationUrl;

    @Override
    public AiListingVerificationResponse verifyListing(AiListingVerificationRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            // Normalize request - set default values for optional fields
            AiListingVerificationRequest normalizedRequest = normalizeRequest(request);
            
            log.info("Sending listing verification request to AI service: {} for listing: {}", 
                aiVerificationUrl, normalizedRequest.getTitle());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            HttpEntity<AiListingVerificationRequest> entity = new HttpEntity<>(normalizedRequest, headers);
            
            log.debug("Making HTTP request to AI service...");
            ResponseEntity<AiListingVerificationResponse> response = restTemplate.exchange(
                aiVerificationUrl,
                HttpMethod.POST,
                entity,
                AiListingVerificationResponse.class
            );
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("AI service responded in {}ms with status: {}", processingTime, response.getStatusCode());
            
            log.info("Processing AI verification response...");

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AiListingVerificationResponse responseBody = response.getBody();
                log.info("AI verification completed successfully with score: {}, confidence: {}", 
                    responseBody.getScore(), responseBody.getConfidence());

                // Debug video validation inconsistency
                if (responseBody.getVideoValidation() != null) {
                    var videoVal = responseBody.getVideoValidation();
                    log.debug("Video validation: is_valid={}, total_videos={}, valid_videos={}, issues={}",
                        videoVal.getIsValid(), videoVal.getTotalVideos(), videoVal.getValidVideos(), videoVal.getIssues());
                    
                    if (Boolean.FALSE.equals(videoVal.getIsValid()) && 
                        videoVal.getValidVideos() != null && videoVal.getTotalVideos() != null &&
                        videoVal.getValidVideos().equals(videoVal.getTotalVideos()) && 
                        (videoVal.getIssues() == null || videoVal.getIssues().isEmpty())) {
                        log.warn("INCONSISTENCY DETECTED: video_validation.is_valid=false but all videos are valid with no issues");
                    }
                }

                if (Boolean.FALSE.equals(responseBody.getIsValid())) {
                    log.warn("Listing is invalid. Issues: {}", responseBody.getSuggestions());
                }

                return responseBody;
            } else {
                log.error("AI service returned non-2xx status: {}", response.getStatusCode());
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
    public boolean isServiceAvailable() {
        try {
            // Simple health check - attempt to reach the service with a basic request
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String healthUrl = aiVerificationUrl.replace("/verify-listing", "/health");
            
            ResponseEntity<String> response = restTemplate.exchange(
                healthUrl,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            boolean isAvailable = response.getStatusCode().is2xxSuccessful();
            log.debug("AI service health check result: {}", isAvailable ? "Available" : "Unavailable");
            return isAvailable;
            
        } catch (Exception e) {
            log.debug("AI service health check failed: {}", e.getMessage());
            return false;
        }
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