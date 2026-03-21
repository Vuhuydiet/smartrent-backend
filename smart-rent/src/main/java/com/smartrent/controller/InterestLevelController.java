package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.InterestLevelResponse;
import com.smartrent.service.interestlevel.InterestLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/listings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Listing Interest Level", description = "Public API for customer social proof")
public class InterestLevelController {

    InterestLevelService interestLevelService;

    @GetMapping("/{listingId}/interest-level")
    @Operation(
            summary = "Get interest level for a listing",
            description = "Returns a qualitative interest level based on recent phone clicks. This is a public endpoint that does NOT expose raw click numbers."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Interest level retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "999999",
                                      "data": {
                                        "level": "HIGH",
                                        "label": "Nhiều người đã liên hệ tin đăng này gần đây"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found")
    })
    public ApiResponse<InterestLevelResponse> getInterestLevel(
            @Parameter(description = "Listing ID", example = "123")
            @PathVariable Long listingId
    ) {
        log.info("Getting interest level for listing {}", listingId);

        InterestLevelResponse response = interestLevelService.getInterestLevel(listingId);

        return ApiResponse.<InterestLevelResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }
}
