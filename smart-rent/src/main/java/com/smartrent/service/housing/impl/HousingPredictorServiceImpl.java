package com.smartrent.service.housing.impl;

import com.smartrent.dto.request.HousingPredictorRequest;
import com.smartrent.dto.response.HousingPredictorResponse;
import com.smartrent.infra.connector.SmartRentAiConnector;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.service.housing.HousingPredictorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementation of HousingPredictorService that calls external AI API using Feign Client
 */
@Service
@ConditionalOnProperty(value = "application.housing-predictor.mock.enabled", havingValue = "false", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class HousingPredictorServiceImpl implements HousingPredictorService {

    private final SmartRentAiConnector aiConnector;

    @Override
    public HousingPredictorResponse predictPrice(HousingPredictorRequest request) {
        try {
            log.info("Calling AI service for housing price prediction via Feign client");
            
            HousingPredictorResponse response = aiConnector.predictHousingPrice(request);
            
            if (response != null) {
                log.info("AI service prediction completed successfully");
                return response;
            } else {
                log.error("AI service returned null response");
                throw new AppException(DomainCode.AI_SERVICE_ERROR);
            }
            
        } catch (Exception e) {
            log.error("Failed to call AI service: {}", e.getMessage(), e);
            throw new AppException(DomainCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    public boolean checkHealth() {
        try {
            log.info("Checking AI service health via housing price prediction test");
            // Simple health check by attempting a minimal prediction
            HousingPredictorRequest testRequest = HousingPredictorRequest.builder()
                .latitude(21.0285)
                .longitude(105.8542)
                .propertyType("APARTMENT")
                .city("Hanoi")
                .district("Ba Dinh")
                .ward("Test Ward")
                .area(50.0f)
                .build();
            
            HousingPredictorResponse response = aiConnector.predictHousingPrice(testRequest);
            boolean isHealthy = response != null;
            
            log.info("AI service health check: {}", isHealthy ? "HEALTHY" : "UNHEALTHY");
            return isHealthy;
            
        } catch (Exception e) {
            log.warn("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }
}