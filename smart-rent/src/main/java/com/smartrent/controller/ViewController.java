package com.smartrent.controller;

import com.smartrent.dto.request.ViewTrackRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.ViewTrackResponse;
import com.smartrent.service.view.ViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

@RestController
@RequestMapping("/v1/views")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(
        name = "View Tracking",
        description = "Public API for tracking listing detail page views. No authentication required."
)
public class ViewController {

    ViewService viewService;

    @PostMapping
    @Operation(
            summary = "Track a listing detail page view",
            description = """
                    Track when a user opens a listing's detail page.

                    **Behavior:**
                    - Public endpoint, no authentication required
                    - Records the view with timestamp and IP address
                    - Deduped per IP address within a short time window so page
                      reloads don't inflate the count
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "View tracking request",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ViewTrackRequest.class),
                            examples = @ExampleObject(
                                    name = "Track View",
                                    value = """
                                            {
                                              "listingId": 123
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "View tracked successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Success",
                                              "data": {
                                                "listingId": 123,
                                                "recorded": true,
                                                "viewedAt": "2024-01-15T10:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Error",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "Listing not found",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<ViewTrackResponse> trackView(
            @Valid @RequestBody ViewTrackRequest request,
            HttpServletRequest httpRequest
    ) {
        String userId = currentUserIdOrNull();
        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.debug("Tracking view for listing {} from IP {}", request.getListingId(), ipAddress);

        ViewTrackResponse response = viewService.trackView(request, userId, ipAddress, userAgent);

        return ApiResponse.<ViewTrackResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    private String currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    /**
     * Extract IP address from HTTP request, handling proxy headers
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }
}
