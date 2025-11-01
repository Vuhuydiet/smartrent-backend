package com.smartrent.service.housing;

import com.smartrent.dto.request.HousingPredictorRequest;
import com.smartrent.dto.response.HousingPredictorResponse;

/**
 * Service interface for housing price prediction operations
 */
public interface HousingPredictorService {
    
    /**
     * Predict housing price based on property features
     * 
     * @param request the prediction request with property details
     * @return prediction response with estimated price and confidence
     */
    HousingPredictorResponse predictPrice(HousingPredictorRequest request);
    
    /**
     * Check if the AI service is available and healthy
     * 
     * @return true if service is healthy, false otherwise
     */
    boolean checkHealth();
}