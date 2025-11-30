package com.smartrent.controller;

import com.smartrent.dto.request.ListingDescriptionRequest;
import com.smartrent.dto.response.ListingDescriptionResponse;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.service.ai.ListingDescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ai")
@Tag(name = "AI", description = "AI related utilities (description generation, etc.)")
@RequiredArgsConstructor
public class AiController {

  private final ListingDescriptionService descriptionService;

  @PostMapping("/listing-description")
  @Operation(
      summary = "Generate listing description",
      description = "Generate a short, attractive listing description using AI. Provide property details in the request.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ListingDescriptionRequest.class),
              examples = @ExampleObject(
                  name = "Generate description",
                  value = "{ \"category\": \"CHO_THUE\", \"propertyType\": \"APARTMENT\", \"price\": 12000000, \"priceUnit\": \"MONTH\", \"addressText\": { \"newAddress\": \"123 Đường Láng, Đống Đa, Hà Nội\" }, \"area\": 78.5, \"bedrooms\": 2, \"bathrooms\": 1, \"direction\": \"Đông Nam\", \"furnishing\": \"SEMI_FURNISHED\", \"amenities\": [\"Điều hòa\", \"Tủ lạnh\", \"Máy giặt\"], \"waterPrice\": \"20000 VND/m3\", \"electricityPrice\": \"3500 VND/kWh\", \"internetPrice\": \"100000 VND/tháng\", \"serviceFee\": \"500000 VND/tháng\", \"tone\": \"friendly\", \"minWords\": 30, \"maxWords\": 50 }"
              )
          )
      ),
      responses = {
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "200",
              description = "Successful response",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ListingDescriptionResponse.class),
                  examples = @ExampleObject(
                      name = "Success",
                      value = "{ \"code\": \"999999\", \"message\": \"Operation completed successfully\", \"data\": { \"title\": \"Căn hộ 2PN view đẹp, đầy đủ nội thất\", \"description\": \"Căn hộ hiện đại với 2 phòng ngủ, 1 phòng tắm, diện tích 78.5m2 tại Đống Đa, Hà Nội. Đầy đủ nội thất cao cấp bao gồm điều hòa, tủ lạnh, máy giặt. Hướng Đông Nam thoáng mát...\" } }"
                  )
              )
          ),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "400",
              description = "Invalid request"
          ),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "500",
              description = "Internal server error"
          )
      }
  )
    public ApiResponse<ListingDescriptionResponse> generateDescription(@Valid @RequestBody ListingDescriptionRequest request) {
        ListingDescriptionResponse resp = descriptionService.generateDescription(request);

        return ApiResponse.<ListingDescriptionResponse>builder().data(resp).build();
    }
}
