package com.smartrent.service.housing.impl;package com.smartrent.service.housing.impl;



import com.smartrent.dto.request.HousingPredictorRequest;import com.smartrent.dto.request.HousingPredictorRequest;

import com.smartrent.dto.response.HousingPredictorResponse;import com.smartrent.dto.response.HousingPredictorResponse;

import com.smartrent.infra.connector.SmartRentAiConnector;import com.smartrent.infra.connector.SmartRentAiConnector;

import com.smartrent.infra.exception.AppException;import com.smartrent.infra.exception.AppException;

import com.smartrent.infra.exception.model.DomainCode;import com.smartrent.infra.exception.model.DomainCode;

import com.smartrent.service.housing.HousingPredictorService;import com.smartrent.service.housing.HousingPredictorService;

import lombok.RequiredArgsConstructor;import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.stereotype.Service;

@Service

@RequiredArgsConstructor/**

@Slf4j * Implementation of HousingPredictorService that calls external AI API using Feign Client

public class HousingPredictorServiceImpl implements HousingPredictorService { */

@Service

    private final SmartRentAiConnector aiConnector;@ConditionalOnProperty(value = "application.housing-predictor.mock.enabled", havingValue = "false", matchIfMissing = false)

@RequiredArgsConstructor

    @Override@Slf4j

    public HousingPredictorResponse predictHousingPrice(HousingPredictorRequest request) {public class HousingPredictorServiceImpl implements HousingPredictorService {

        log.info("Making AI service call for housing price prediction: city={}, district={}, ward={}, propertyType={}, area={}", 

            request.getCity(), request.getDistrict(), request.getWard(), request.getPropertyType(), request.getArea());    private final SmartRentAiConnector aiConnector;

        

        try {    @Override

            HousingPredictorResponse response = aiConnector.predictHousingPrice(request);    public HousingPredictorResponse predictPrice(HousingPredictorRequest request) {

                    try {

            if (response != null) {            log.info("Calling AI service for housing price prediction via Feign client");

                // Calculate predicted price as average of min/max if not provided            

                if (response.getPredictedPrice() == null && response.getPriceRange() != null) {            HousingPredictorResponse response = aiConnector.predictHousingPrice(request);

                    var priceRange = response.getPriceRange();            

                    if (priceRange.getMinPrice() != null && priceRange.getMaxPrice() != null) {            if (response != null) {

                        double predictedPrice = (priceRange.getMinPrice() + priceRange.getMaxPrice()) / 2;                log.info("AI service prediction completed successfully");

                        response.setPredictedPrice(predictedPrice);                return response;

                        log.info("Calculated predicted price as average: {}", predictedPrice);            } else {

                    }                log.error("AI service returned null response");

                }                throw new AppException(DomainCode.AI_SERVICE_ERROR);

                            }

                log.info("AI service prediction completed successfully for {}, {}",             

                    request.getCity(), request.getDistrict());        } catch (Exception e) {

                return response;            log.error("Failed to call AI service: {}", e.getMessage(), e);

            } else {            throw new AppException(DomainCode.AI_SERVICE_ERROR);

                log.error("AI service returned null response");        }

                throw new AppException(DomainCode.AI_SERVICE_ERROR);    }

            }

                @Override

        } catch (Exception e) {    public boolean checkHealth() {

            log.error("Error calling AI service for housing prediction", e);        try {

            throw new AppException(DomainCode.AI_SERVICE_ERROR);            log.info("Checking AI service health via housing price prediction test");

        }            // Simple health check by attempting a minimal prediction

    }            HousingPredictorRequest testRequest = HousingPredictorRequest.builder()

}                .latitude(21.0285)
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