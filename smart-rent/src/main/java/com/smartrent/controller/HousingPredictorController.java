package com.smartrent.controller;

import com.smartrent.dto.request.HousingPredictorRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.HousingPredictorResponse;
import com.smartrent.service.predictor.HousingPredictorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/housing-predictor")
@RequiredArgsConstructor
@Tag(name = "Housing Price Predictor", description = "API for predicting real estate property prices using AI/ML models")
public class HousingPredictorController {

    private final HousingPredictorService housingPredictorService;

    @PostMapping("/predict")
    @Operation(
        summary = "Predict housing price",
        description = "Predicts the price range of a property based on location, type, size and other characteristics using AI models"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Price prediction successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Successful prediction",
                    value = """
                        {
                          "code": "999999",
                          "message": "Housing price prediction retrieved successfully",
                          "data": {
                            "price_range": {
                              "min": 9464324,
                              "max": 43084827
                            },
                            "location": "My Tho, Tien Giang",
                            "property_type": "House",
                            "currency": "VND"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Invalid request parameters",
            content = @Content(mediaType = "application/json")
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "AI service unavailable or internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<ApiResponse<HousingPredictorResponse>> predictHousingPrice(
            @Parameter(
                description = "Property details for price prediction",
                required = true,
                content = @Content(
                    examples = @ExampleObject(
                        name = "Sample request",
                        value = """
                            {
                              "city": "Ho Chi Minh",
                              "district": "District 1",
                              "ward": "Ben Nghe Ward",
                              "property_type": "Apartment",
                              "area": 80,
                              "latitude": 10.7769,
                              "longitude": 106.7009
                            }
                            """
                    )
                )
            )
            @Valid @RequestBody HousingPredictorRequest request) {
        
        log.info("Received housing price prediction request: {}", request);
        
        HousingPredictorResponse response = housingPredictorService.predictHousingPrice(request);
        
        return ResponseEntity.ok(ApiResponse.<HousingPredictorResponse>builder()
                .message("Housing price prediction retrieved successfully")
                .data(response)
                .build());
    }
}
