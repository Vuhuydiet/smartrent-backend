package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.MediaResponse;
import com.smartrent.service.media.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/media")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin - Media", description = "Admin APIs for managing media uploads")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_UA', 'ROLE_SPA')")
@Slf4j
public class AdminMediaController {

    MediaService mediaService;

    @PostMapping("/upload")
    @Operation(
            summary = "Upload media (Admin)",
            description = """
                    Admin uploads a media file directly to cloud storage.
                    No user ownership check is performed.
                    The admin's ID is stored as the media owner.
                    Optionally associate with a listing (no ownership required).
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Media uploaded successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Upload Success",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Media uploaded successfully by admin.",
                                              "data": {
                                                "mediaId": 456,
                                                "listingId": 123,
                                                "userId": "admin-uuid-123",
                                                "mediaType": "IMAGE",
                                                "sourceType": "UPLOAD",
                                                "status": "ACTIVE",
                                                "url": "https://pub-xxx.r2.dev/media/admin-uuid-123/456-photo.jpg",
                                                "thumbnailUrl": null,
                                                "title": "Banner Image",
                                                "description": "Homepage banner",
                                                "altText": "Banner photo",
                                                "isPrimary": true,
                                                "sortOrder": 1,
                                                "fileSize": 2048576,
                                                "mimeType": "image/jpeg",
                                                "originalFilename": "banner.jpg",
                                                "durationSeconds": null,
                                                "uploadConfirmed": true,
                                                "createdAt": "2024-01-15T10:30:00",
                                                "updatedAt": "2024-01-15T10:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid file type or size",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Requires admin role (SA, UA, SPA)",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<MediaResponse>> uploadMedia(
            @Parameter(description = "Media file to upload", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Type of media: IMAGE or VIDEO", required = true)
            @RequestParam("mediaType") String mediaType,

            @Parameter(description = "Listing ID to associate with (optional, no ownership check)")
            @RequestParam(value = "listingId", required = false) Long listingId,

            @Parameter(description = "Media title")
            @RequestParam(value = "title", required = false) String title,

            @Parameter(description = "Media description")
            @RequestParam(value = "description", required = false) String description,

            @Parameter(description = "Alt text for accessibility")
            @RequestParam(value = "altText", required = false) String altText,

            @Parameter(description = "Set as primary media")
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary,

            @Parameter(description = "Display order")
            @RequestParam(value = "sortOrder", required = false, defaultValue = "0") Integer sortOrder) {

        String adminId = extractAdminId();
        log.info("POST /v1/admin/media/upload - admin: {}, filename: {}, type: {}",
                adminId, file.getOriginalFilename(), mediaType);

        MediaResponse response = mediaService.adminUploadMedia(
                file, mediaType, listingId, title, description,
                altText, isPrimary, sortOrder, adminId
        );

        return ResponseEntity.ok(ApiResponse.<MediaResponse>builder()
                .data(response)
                .message("Media uploaded successfully by admin.")
                .build());
    }

    @DeleteMapping("/{mediaId}")
    @Operation(
            summary = "Delete any media (Admin)",
            description = "Admin can delete any media regardless of ownership."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Media deleted successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Media not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @Parameter(description = "Media ID") @PathVariable Long mediaId) {

        String adminId = extractAdminId();
        log.info("DELETE /v1/admin/media/{} - admin: {}", mediaId, adminId);

        mediaService.adminDeleteMedia(mediaId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Media deleted successfully by admin.")
                .build());
    }

    @GetMapping
    @Operation(
            summary = "List all media (Admin)",
            description = "Admin can list all media with optional status filter and pagination."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Media list retrieved successfully",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getAllMedia(
            @Parameter(description = "Filter by status: PENDING, ACTIVE, ARCHIVED, DELETED")
            @RequestParam(value = "status", required = false) String status,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {

        String adminId = extractAdminId();
        log.info("GET /v1/admin/media - admin: {}, status: {}, page: {}, size: {}", adminId, status, page, size);

        List<MediaResponse> media = mediaService.adminGetAllMedia(status, page, size);

        return ResponseEntity.ok(ApiResponse.<List<MediaResponse>>builder()
                .data(media)
                .message("Media retrieved successfully")
                .build());
    }

    @GetMapping("/{mediaId}")
    @Operation(
            summary = "Get media by ID (Admin)",
            description = "Admin can view any media details regardless of ownership."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Media retrieved successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Media not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<MediaResponse>> getMediaById(
            @Parameter(description = "Media ID") @PathVariable Long mediaId) {

        log.info("GET /v1/admin/media/{}", mediaId);

        MediaResponse media = mediaService.getMediaById(mediaId);

        return ResponseEntity.ok(ApiResponse.<MediaResponse>builder()
                .data(media)
                .message("Media retrieved successfully")
                .build());
    }

    private String extractAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Admin is not authenticated");
        }
        return authentication.getName();
    }
}