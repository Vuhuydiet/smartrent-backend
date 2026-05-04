package com.smartrent.controller;

import com.smartrent.dto.request.TakeDownListingRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.TakeDownResponse;
import com.smartrent.service.takedown.TakeDownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/take-downs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Take Down Management", description = "APIs for owners to take down (hide) their listings from public view")
public class TakeDownController {

    TakeDownService takeDownService;

    @PostMapping("/take-down")
    @Operation(
        summary = "Take down a listing",
        description = "Take down (hide) a currently displaying listing owned by the authenticated user. "
                + "The listing is marked as expired so it disappears from public search. "
                + "Owners can re-publish later via the standard renewal/post flow.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TakeDownListingRequest.class),
                examples = @ExampleObject(
                    name = "Take Down Listing",
                    value = """
                        {
                          "listingId": 123
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully took down listing",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TakeDownResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Listing is not in a state that can be taken down"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Listing does not belong to the authenticated user"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Listing not found"
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<TakeDownResponse> takeDownListing(@RequestBody @Valid TakeDownListingRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} taking down listing {}", userId, request.getListingId());
        TakeDownResponse response = takeDownService.takeDownListing(userId, request);
        return ApiResponse.<TakeDownResponse>builder()
                .data(response)
                .build();
    }
}
