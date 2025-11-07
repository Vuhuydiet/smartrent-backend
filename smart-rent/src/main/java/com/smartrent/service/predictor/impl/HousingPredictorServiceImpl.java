package com.smartrent.service.predictor.impl;

import com.smartrent.infra.connector.SmartRentAiConnector;
import com.smartrent.dto.request.HousingPredictorRequest;
import com.smartrent.dto.response.HousingPredictorResponse;
import com.smartrent.service.predictor.HousingPredictorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HousingPredictorServiceImpl implements HousingPredictorService {

    SmartRentAiConnector smartRentAiConnector;

    @Override
    public HousingPredictorResponse predictHousingPrice(HousingPredictorRequest request) {
        log.info("Calling AI service for housing price prediction with request: {}", request);
        
        try {
            HousingPredictorResponse response = smartRentAiConnector.predictHousingPrice(request);
            log.info("Successfully received price prediction from AI service");
            return response;
        } catch (Exception e) {
            log.error("Error calling AI service for housing price prediction", e);
            throw e;
        }
    }
}