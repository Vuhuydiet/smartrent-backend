package com.smartrent.controller;

import com.smartrent.dto.request.SearchSuggestionClickRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.service.discovery.SearchSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Click-tracking endpoint for the search suggestions feature.
 *
 * <p>Frontend should call {@code POST /v1/listings/search-suggestions/click}
 * whenever the user selects a suggestion from the dropdown returned by
 * {@code GET /v1/listings/search-suggestions}.  The call is fire-and-forget
 * from the UI perspective — the response body is always an empty {@code data} field.
 *
 * <p>The recorded events are later aggregated by
 * {@link com.smartrent.infra.repository.SearchSuggestionClickRepository#findPopularQueryTexts}
 * to drive the POPULAR_QUERY suggestion source.
 */
@RestController
@RequestMapping("/v1/listings/search-suggestions")
@Tag(
    name = "Listings - Search & Discovery",
    description = "Click telemetry for the search suggestions feature. " +
                  "See GET /v1/listings/search-suggestions for the main suggestions endpoint."
)
@RequiredArgsConstructor
public class SearchSuggestionClickController {

    private final SearchSuggestionService searchSuggestionService;

    @PostMapping("/click")
    @io.swagger.v3.oas.annotations.Operation(
        summary = "[PUBLIC API] Record a suggestion click event",
        description = """
            **PUBLIC API — no authentication required.**

            Records that the user clicked/selected a suggestion from the search-suggestions result
            list.  Use the `impressionId` returned by `GET /v1/listings/search-suggestions` to link
            the click back to the original result set.

            **Important**: This endpoint is fire-and-forget from the client's perspective.
            A successful `200` response means the event was accepted, not necessarily persisted
            (transient errors are silently swallowed server-side).
            """,
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "TITLE click example",
                    value = """
                        {
                          "impressionId": 9876,
                          "type": "TITLE",
                          "text": "Căn hộ 2PN Quận 1 full nội thất",
                          "listingId": 12345,
                          "rank": 0
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Click accepted (fire-and-forget; data field is null)"
            )
        }
    )
    public com.smartrent.dto.response.ApiResponse<Void> recordClick(
            @Valid @org.springframework.web.bind.annotation.RequestBody SearchSuggestionClickRequest request) {

        searchSuggestionService.recordClick(
                request.getImpressionId(),
                request.getType(),
                request.getText(),
                request.getListingId(),
                request.getRank());

        return com.smartrent.dto.response.ApiResponse.<Void>builder().build();
    }
}
