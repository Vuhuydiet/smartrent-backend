package com.smartrent.infra.connector;

import com.smartrent.dto.request.HousingPredictorRequest;
import com.smartrent.dto.response.HousingPredictorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "smartrent-ai", 
    url = "${application.housing-predictor.ai-service.base-url:http://localhost:8000}"
)
public interface SmartRentAiConnector {

    /**
     * Housing price prediction endpoint
     */
    @PostMapping(value = "/api/v1/house-pricing/get-price-range", consumes = MediaType.APPLICATION_JSON_VALUE)
    HousingPredictorResponse predictHousingPrice(@RequestBody HousingPredictorRequest request);
}