package com.smartrent.controller;

import com.smartrent.dto.request.NewsCreateRequest;
import com.smartrent.dto.request.NewsUpdateRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.NewsListResponse;
import com.smartrent.dto.response.NewsResponse;
import com.smartrent.enums.NewsStatus;
import com.smartrent.service.news.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Admin News and Blog API Controller
 * Provides endpoints for administrators to manage news and blog posts
 */
@RestController
@RequestMapping("/v1/admin/news")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin - News & Blog", description = "Admin APIs for managing news and blog posts")
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
public class AdminNewsController {

    NewsService newsService;

    @PostMapping
    @Operation(
            summary = "Create a new news/blog post (Admin)",
            description = "Creates a new news or blog post. The post is created in DRAFT status by default. " +
                    "Admin ID and name are automatically extracted from the JWT token."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "News created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NewsResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "News slug already exists",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Slug Conflict",
                                    value = """
                                            {
                                              "code": "15002",
                                              "message": "News slug already exists",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<NewsResponse> createNews(@Valid @RequestBody NewsCreateRequest request) {
        // Extract admin ID and name from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();
        
        // For admin name, we'll use the adminId as fallback
        // In a real scenario, you might want to fetch the admin details from the database
        String adminName = adminId; // TODO: Fetch actual admin name if needed

        log.info("POST /v1/admin/news - Creating news by admin: {}", adminId);

        NewsResponse response = newsService.createNews(request, adminId, adminName);

        return ApiResponse.<NewsResponse>builder()
                .data(response)
                .message("News created successfully")
                .build();
    }

    @PutMapping("/{newsId}")
    @Operation(
            summary = "Update a news/blog post (Admin)",
            description = "Updates an existing news or blog post. All fields are optional for partial updates."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "News updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "News not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                            {
                                              "code": "15001",
                                              "message": "News not found",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<NewsResponse> updateNews(
            @Parameter(description = "News ID", example = "1")
            @PathVariable Long newsId,
            @Valid @RequestBody NewsUpdateRequest request
    ) {
        log.info("PUT /v1/admin/news/{} - Updating news", newsId);

        NewsResponse response = newsService.updateNews(newsId, request);

        return ApiResponse.<NewsResponse>builder()
                .data(response)
                .message("News updated successfully")
                .build();
    }

    @PostMapping("/{newsId}/publish")
    @Operation(
            summary = "Publish a news/blog post (Admin)",
            description = "Changes the status of a news post to PUBLISHED and sets the published date if not already set."
    )
    public ApiResponse<NewsResponse> publishNews(
            @Parameter(description = "News ID", example = "1")
            @PathVariable Long newsId
    ) {
        log.info("POST /v1/admin/news/{}/publish - Publishing news", newsId);

        NewsResponse response = newsService.publishNews(newsId);

        return ApiResponse.<NewsResponse>builder()
                .data(response)
                .message("News published successfully")
                .build();
    }

    @PostMapping("/{newsId}/unpublish")
    @Operation(
            summary = "Unpublish a news/blog post (Admin)",
            description = "Changes the status of a news post back to DRAFT."
    )
    public ApiResponse<NewsResponse> unpublishNews(
            @Parameter(description = "News ID", example = "1")
            @PathVariable Long newsId
    ) {
        log.info("POST /v1/admin/news/{}/unpublish - Unpublishing news", newsId);

        NewsResponse response = newsService.unpublishNews(newsId);

        return ApiResponse.<NewsResponse>builder()
                .data(response)
                .message("News unpublished successfully")
                .build();
    }

    @PostMapping("/{newsId}/archive")
    @Operation(
            summary = "Archive a news/blog post (Admin)",
            description = "Changes the status of a news post to ARCHIVED. Archived posts are not visible to end users."
    )
    public ApiResponse<NewsResponse> archiveNews(
            @Parameter(description = "News ID", example = "1")
            @PathVariable Long newsId
    ) {
        log.info("POST /v1/admin/news/{}/archive - Archiving news", newsId);

        NewsResponse response = newsService.archiveNews(newsId);

        return ApiResponse.<NewsResponse>builder()
                .data(response)
                .message("News archived successfully")
                .build();
    }

    @DeleteMapping("/{newsId}")
    @Operation(
            summary = "Delete a news/blog post (Admin)",
            description = "Permanently deletes a news or blog post from the database."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "News deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "News not found"
            )
    })
    public ApiResponse<Void> deleteNews(
            @Parameter(description = "News ID", example = "1")
            @PathVariable Long newsId
    ) {
        log.info("DELETE /v1/admin/news/{} - Deleting news", newsId);

        newsService.deleteNews(newsId);

        return ApiResponse.<Void>builder()
                .message("News deleted successfully")
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Get all news (Admin)",
            description = "Retrieves a paginated list of all news posts regardless of status. " +
                    "Optionally filter by status (DRAFT, PUBLISHED, ARCHIVED)."
    )
    public ApiResponse<NewsListResponse> getAllNews(
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,

            @Parameter(description = "Filter by status")
            @RequestParam(required = false) NewsStatus status
    ) {
        log.info("GET /v1/admin/news - page: {}, size: {}, status: {}", page, size, status);

        NewsListResponse response = newsService.getAllNews(page, size, status);

        return ApiResponse.<NewsListResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{newsId}")
    @Operation(
            summary = "Get news by ID (Admin)",
            description = "Retrieves full details of a news post by its ID. " +
                    "Unlike the public endpoint, this works for all statuses including DRAFT and ARCHIVED."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "News retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "News not found"
            )
    })
    public ApiResponse<NewsResponse> getNewsById(
            @Parameter(description = "News ID", example = "1")
            @PathVariable Long newsId
    ) {
        log.info("GET /v1/admin/news/{} - Getting news by ID", newsId);

        NewsResponse response = newsService.getNewsById(newsId);

        return ApiResponse.<NewsResponse>builder()
                .data(response)
                .build();
    }
}

