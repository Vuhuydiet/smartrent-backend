package com.smartrent.service.housing;package com.smartrent.service.housing;



import com.smartrent.dto.request.HousingPredictorRequest;import com.smartrent.dto.request.HousingPredictorRequest;

import com.smartrent.dto.response.HousingPredictorResponse;import com.smartrent.dto.response.HousingPredictorResponse;



/**/**

 * Service interface for housing price prediction functionality. * Service interface for housing price prediction operations

 * This service acts as a proxy to the AI service for housing price predictions. */

 */public interface HousingPredictorService {

public interface HousingPredictorService {    

        /**

    /**     * Predict housing price based on property features

     * Predict housing price based on location, property type, and area.     * 

     *     * @param request the prediction request with property details

     * @param request the housing prediction request containing location and property details     * @return prediction response with estimated price and confidence

     * @return the housing prediction response with price range and location information     */

     * @throws com.smartrent.infra.exception.AppException if AI service is unavailable or returns invalid response    HousingPredictorResponse predictPrice(HousingPredictorRequest request);

     */    

    HousingPredictorResponse predictHousingPrice(HousingPredictorRequest request);    /**

}     * Check if the AI service is available and healthy
     * 
     * @return true if service is healthy, false otherwise
     */
    boolean checkHealth();
}