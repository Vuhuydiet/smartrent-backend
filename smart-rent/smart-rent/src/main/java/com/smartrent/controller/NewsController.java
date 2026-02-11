package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.NewsDetailResponse;
import com.smartrent.dto.response.NewsListResponse;
import com.smartrent.dto.response.NewsSummaryResponse;
import com.smartrent.enums.NewsCategory;

import java.util.List;
import com.smartrent.service.news.NewsService;
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

/**
 * Public News and Blog API Controller
 * Provides endpoints for end users to view published news and blog posts
 */
@RestController
@RequestMapping("/v1/news")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "News & Blog (Public)", description = "Public APIs for news and blog posts")
@Slf4j
public class NewsController {

    NewsService newsService;

    @GetMapping
    @Operation(
            summary = "Get published news list",
            description = "Retrieve a paginated list of published news and blog posts with optional filtering by category, tag, or keyword search"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved news list",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NewsListResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": {
                                                "news": [
                                                  {
                                                    "newsId": 1,
                                                    "title": "Top 10 Tips for Finding the Perfect Rental",
                                                    "slug": "top-10-tips-for-finding-the-perfect-rental",
                                                    "summary": "Discover the best strategies for finding your ideal rental property",
                                                    "category": "GUIDE",
                                                    "tags": ["rental", "tips", "guide"],
                                                    "thumbnailUrl": "https://example.com/image.jpg",
                                                    "publishedAt": "2024-01-15T10:30:00",
                                                    "authorName": "Admin User",
                                                    "viewCount": 1250,
                                                    "createdAt": "2024-01-15T09:00:00"
                                                  }
                                                ],
                                                "totalItems": 150,
                                                "currentPage": 1,
                                                "pageSize": 20,
                                                "totalPages": 8
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<NewsListResponse> getPublishedNews(
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,

            @Parameter(description = "Filter by category")
            @RequestParam(required = false) NewsCategory category,

            @Parameter(description = "Filter by tag", example = "rental")
            @RequestParam(required = false) String tag,

            @Parameter(description = "Search keyword (searches in title and summary)", example = "rental tips")
            @RequestParam(required = false) String keyword
    ) {
        log.info("GET /v1/news - page: {}, size: {}, category: {}, tag: {}, keyword: {}",
                page, size, category, tag, keyword);

        NewsListResponse response = newsService.getPublishedNews(page, size, category, tag, keyword);

        return ApiResponse.<NewsListResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/newest")
    @Operation(
            summary = "Get newest published news",
            description = "Retrieve the N newest published news articles, sorted by publication date (newest first). " +
                    "This endpoint is ideal for displaying 'Latest News' sections on the homepage or sidebar."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved newest news",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": [
                                                {
                                                  "newsId": 5,
                                                  "title": "New Rental Market Trends for 2024",
                                                  "slug": "new-rental-market-trends-for-2024",
                                                  "summary": "Discover the latest trends shaping the rental market this year",
                                                  "category": "NEWS",
                                                  "tags": ["market", "trends", "2024"],
                                                  "thumbnailUrl": "https://example.com/image5.jpg",
                                                  "publishedAt": "2024-01-20T14:00:00",
                                                  "authorName": "Admin User",
                                                  "viewCount": 320,
                                                  "createdAt": "2024-01-20T12:00:00"
                                                },
                                                {
                                                  "newsId": 4,
                                                  "title": "How to Negotiate Your Rent",
                                                  "slug": "how-to-negotiate-your-rent",
                                                  "summary": "Expert tips on negotiating better rental terms",
                                                  "category": "BLOG",
                                                  "tags": ["negotiation", "tips", "rent"],
                                                  "thumbnailUrl": "https://example.com/image4.jpg",
                                                  "publishedAt": "2024-01-18T10:30:00",
                                                  "authorName": "Admin User",
                                                  "viewCount": 580,
                                                  "createdAt": "2024-01-18T09:00:00"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid limit parameter",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Limit",
                                    value = """
                                            {
                                              "code": "15004",
                                              "message": "Invalid limit: limit must be at least 1",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<List<NewsSummaryResponse>> getNewestNews(
            @Parameter(description = "Number of news articles to return (1-50, default: 10)", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        log.info("GET /v1/news/newest - limit: {}", limit);

        List<NewsSummaryResponse> response = newsService.getNewestNews(limit);

        return ApiResponse.<List<NewsSummaryResponse>>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{slug}")
    @Operation(
            summary = "Get news detail by slug",
            description = "Retrieve full details of a published news/blog post by its URL-friendly slug. " +
                    "This endpoint also increments the view count and returns related posts."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved news detail",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NewsDetailResponse.class)
                    )
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "News is not published",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Published",
                                    value = """
                                            {
                                              "code": "15003",
                                              "message": "News is not published",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<NewsDetailResponse> getNewsBySlug(
            @Parameter(description = "URL-friendly slug", example = "top-10-tips-for-finding-the-perfect-rental")
            @PathVariable String slug
    ) {
        log.info("GET /v1/news/{} - Getting news detail", slug);

        NewsDetailResponse response = newsService.getNewsBySlug(slug);

        return ApiResponse.<NewsDetailResponse>builder()
                .data(response)
                .build();
    }
}

