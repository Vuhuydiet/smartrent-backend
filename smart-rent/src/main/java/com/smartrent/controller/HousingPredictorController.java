package com.smartrent.controller;

import com.smartrent.dto.request.HousingPredictorRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.HousingPredictorResponse;
import com.smartrent.service.housing.HousingPredictorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/housing-predictor")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Housing Price Predictor",
    description = """
        AI-powered housing price prediction service that provides accurate property valuations 
        based on location, property characteristics, and market data.
        
        **Key Features:**
        - **Smart Price Prediction**: AI-driven property price estimation using machine learning models
        - **Location Intelligence**: GPS coordinates and address-based pricing analysis  
        - **Property Type Support**: Apartments, Houses, Offices, and Room pricing
        - **Market Insights**: Confidence scores and price range analysis
        - **Real-time Processing**: Fast API responses with cached market data
        
        **Authentication Required**: All endpoints require valid JWT token with USER or ADMIN role.
        """
)
public class HousingPredictorController {

    private final HousingPredictorService housingPredictorService;

    @PostMapping("/predict")
    @Operation(
        summary = "Predict Property Price",
        description = """
            **Estimate property value using AI-powered prediction model**
            
            This endpoint analyzes property characteristics including location coordinates, 
            property type, area size, and regional data to provide accurate price predictions.
            
            **Input Requirements:**
            - Valid GPS coordinates (latitude/longitude) within Vietnam
            - Property type: Apartment, House, Office, or Room
            - Area in square meters (positive number)
            - Complete address information (city, district, ward)
            
            **AI Model Output:**
            - Predicted price in VND millions
            - Price range (min/max estimates) 
            - Confidence score (0.0 - 1.0)
            - Market metadata and calculation time
            
            **Business Use Cases:**
            - Real estate listing price suggestions
            - Property investment analysis  
            - Market valuation reports
            - Rental price benchmarking
            """,
        tags = {"Property Valuation"}
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Price prediction completed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = HousingPredictorResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters - check property type, coordinates, or area values"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
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