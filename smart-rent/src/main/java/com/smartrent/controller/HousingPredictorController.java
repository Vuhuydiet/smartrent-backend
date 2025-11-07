package com.smartrent.controller;package com.smartrent.controller;



import com.smartrent.dto.request.HousingPredictorRequest;import com.smartrent.dto.request.HousingPredictorRequest;

import com.smartrent.dto.response.HousingPredictorResponse;import com.smartrent.dto.response.ApiResponse;

import com.smartrent.service.housing.HousingPredictorService;import com.smartrent.dto.response.HousingPredictorResponse;

import io.swagger.v3.oas.annotations.Operation;import com.smartrent.service.housing.HousingPredictorService;

import io.swagger.v3.oas.annotations.media.Content;import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.media.ExampleObject;import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.media.Schema;import io.swagger.v3.oas.annotations.media.Content;

import io.swagger.v3.oas.annotations.responses.ApiResponse;import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.responses.ApiResponses;import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.tags.Tag;import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController

@RequestMapping("/v1/housing-predictor")@RestController

@RequiredArgsConstructor@RequestMapping("/v1/housing-predictor")

@Slf4j@RequiredArgsConstructor

@Tag(name = "Housing Price Predictor", description = "APIs for predicting housing prices using AI service")@Slf4j

public class HousingPredictorController {@Tag(

    name = "Housing Price Predictor",

    private final HousingPredictorService housingPredictorService;    description = """

        AI-powered housing price prediction service that provides accurate property valuations 

    @Operation(        based on location, property characteristics, and market data.

        summary = "Predict housing price",        

        description = "Predict housing price based on location, property type, and area using AI service"        **Key Features:**

    )        - **Smart Price Prediction**: AI-driven property price estimation using machine learning models

    @ApiResponses(value = {        - **Location Intelligence**: GPS coordinates and address-based pricing analysis  

        @ApiResponse(        - **Property Type Support**: Apartments, Houses, Offices, and Room pricing

            responseCode = "200",        - **Market Insights**: Confidence scores and price range analysis

            description = "Successfully predicted housing price",        - **Real-time Processing**: Fast API responses with cached market data

            content = @Content(        

                mediaType = "application/json",        **Authentication Required**: All endpoints require valid JWT token with USER or ADMIN role.

                schema = @Schema(implementation = HousingPredictorResponse.class),        """

                examples = @ExampleObject()

                    name = "Successful prediction",public class HousingPredictorController {

                    value = """

                        {    private final HousingPredictorService housingPredictorService;

                          "price_range": {

                            "min": 9464324,    @PostMapping("/predict")

                            "max": 43084827    @Operation(

                          },        summary = "Predict Property Price",

                          "location": "My Tho, Tien Giang",        description = """

                          "property_type": "House",            **Estimate property value using AI-powered prediction model**

                          "currency": "VND",            

                          "predicted_price": 26274575.5            This endpoint analyzes property characteristics including location coordinates, 

                        }            property type, area size, and regional data to provide accurate price predictions.

                        """            

                )            **Input Requirements:**

            )            - Valid GPS coordinates (latitude/longitude) within Vietnam

        ),            - Property type: Apartment, House, Office, or Room

        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),            - Area in square meters (positive number)

        @ApiResponse(responseCode = "500", description = "Internal server error"),            - Complete address information (city, district, ward)

        @ApiResponse(responseCode = "503", description = "AI service unavailable")            

    })            **AI Model Output:**

    @PostMapping("/predict")            - Predicted price in VND millions

    public ResponseEntity<HousingPredictorResponse> predictHousingPrice(            - Price range (min/max estimates) 

        @Valid @RequestBody HousingPredictorRequest request            - Confidence score (0.0 - 1.0)

    ) {            - Market metadata and calculation time

        log.info("Received housing price prediction request for {}, {} - {} sqm",             

            request.getCity(), request.getDistrict(), request.getArea());            **Business Use Cases:**

                    - Real estate listing price suggestions

        HousingPredictorResponse response = housingPredictorService.predictHousingPrice(request);            - Property investment analysis  

                    - Market valuation reports

        log.info("Housing price prediction completed for {}, {}",             - Rental price benchmarking

            request.getCity(), request.getDistrict());            """,

                tags = {"Property Valuation"}

        return ResponseEntity.ok(response);    )

    }    @ApiResponses(value = {

        @io.swagger.v3.oas.annotations.responses.ApiResponse(

    @Operation(            responseCode = "200",

        summary = "Health check endpoint",            description = "Price prediction completed successfully",

        description = "Check if the housing predictor service is operational"            content = @Content(

    )                mediaType = "application/json",

    @ApiResponses(value = {                schema = @Schema(implementation = HousingPredictorResponse.class)

        @ApiResponse(responseCode = "200", description = "Service is operational")            )

    })        ),

    @GetMapping("/health")        @io.swagger.v3.oas.annotations.responses.ApiResponse(

    public ResponseEntity<String> healthCheck() {            responseCode = "400",

        return ResponseEntity.ok("Housing Predictor Service is operational");            description = "Invalid request parameters - check property type, coordinates, or area values"

    }        ),

}        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Authentication required - valid JWT token needed"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions - USER or ADMIN role required"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "AI service unavailable or internal server error"
        )
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<HousingPredictorResponse> predictHousingPrice(
            @Parameter(
                description = """
                    **Property Information for Price Prediction**
                    
                    Complete property details required for accurate AI-based price estimation:
                    
                    - **latitude/longitude**: GPS coordinates within Vietnam (required)
                    - **propertyType**: Must be one of: Apartment, House, Office, Room (required) 
                    - **city**: Vietnamese city name (e.g., "Hanoi", "Ho Chi Minh City") (required)
                    - **district**: District/Quan name within the city (required)
                    - **ward**: Ward/Phuong name within the district (required)  
                    - **area**: Property area in square meters, must be positive (required)
                    
                    **Example coordinates:**
                    - Hanoi CBD: 21.0285, 105.8542
                    - HCMC District 1: 10.7769, 106.6981
                    - Da Nang Center: 16.0471, 108.2068
                    """,
                required = true,
                schema = @Schema(implementation = HousingPredictorRequest.class)
            )
            @Valid @RequestBody HousingPredictorRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        
        String userName = authentication != null ? authentication.getName() : "anonymous";
        log.info("Housing price prediction requested by user: {}", userName);
        
        HousingPredictorResponse response = housingPredictorService.predictPrice(request);
        
        return ApiResponse.<HousingPredictorResponse>builder()
            .code("200")
            .message("Price prediction successful")
            .data(response)
            .build();
    }

    @GetMapping("/health")
    @Operation(
        summary = "AI Service Health Check",
        description = """
            **Monitor AI prediction service availability**
            
            Performs a lightweight health check on the external AI service to verify:
            - Service connectivity and response time
            - AI model availability and functionality  
            - System readiness for price predictions
            
            **Use Cases:**
            - System monitoring and alerting
            - Load balancer health checks
            - Service dependency verification
            - Troubleshooting connection issues
            
            **Note:** This endpoint is public and does not require authentication.
            """,
        tags = {"System Health"}
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "AI service is healthy and ready to process predictions"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "503", 
            description = "AI service is unavailable - check service status or network connectivity"
        )
    })
    public ApiResponse<String> checkHealth() {
        boolean isHealthy = housingPredictorService.checkHealth();
        
        if (isHealthy) {
        return ApiResponse.<String>builder()
            .code("200")
            .message("AI service is healthy")
            .data("OK")
            .build();
        } else {
        return ApiResponse.<String>builder()
            .code("503")
            .message("AI service is unavailable")
            .data("UNAVAILABLE")
            .build();
        }
    }
}