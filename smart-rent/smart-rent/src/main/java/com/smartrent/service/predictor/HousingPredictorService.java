package com.smartrent.service.predictor;

import com.smartrent.dto.request.HousingPredictorRequest;
import com.smartrent.dto.response.HousingPredictorResponse;

public interface HousingPredictorService {
    
    /**
     * Predict housing price using AI service
     * 
     * @param request housing prediction request with location and property details
     * @return predicted price range and location information
     */
    HousingPredictorResponse predictHousingPrice(HousingPredictorRequest request);
}