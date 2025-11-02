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
                  value = "{ \"title\": \"Căn hộ 2PN view sông\", \"addressText\": \"123 Đường Láng, Hà Nội\", \"bedrooms\": 2, \"bathrooms\": 1, \"area\": 78.5, \"price\": 12000000, \"priceUnit\": \"MONTH\", \"furnishing\": \"SEMI_FURNISHED\", \"propertyType\": \"APARTMENT\", \"tone\": \"friendly\", \"maxWords\": 40 }"
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
                      value = "{ \"code\": \"999999\", \"message\": \"Operation completed successfully\", \"data\": { \"generatedDescription\": \"Căn hộ đẹp, 2 phòng ngủ, view sông...\" } }"
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
