package com.smartrent.controller;

import com.smartrent.dto.request.ConfirmUploadRequest;
import com.smartrent.dto.request.GenerateUploadUrlRequest;
import com.smartrent.dto.request.SaveExternalMediaRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.GenerateUploadUrlResponse;
import com.smartrent.dto.response.MediaResponse;
import com.smartrent.service.media.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/media")
@Tag(
        name = "Media API",
        description = """
                Secure media upload/download with pre-signed URLs (Cloudflare R2)

                **Upload Flow:**
                1. POST /upload-url - Get pre-signed upload URL
                2. PUT to presigned URL - Upload file directly to R2 (frontend)
                3. POST /{mediaId}/confirm - Confirm upload completion

                **Features:**
                - Pre-signed URLs for secure upload/download
                - Support for images and videos
                - YouTube/TikTok embed support
                - Ownership validation
                - Automatic cleanup of expired uploads
                """
)
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearer-jwt")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload-url")
    @Operation(
            summary = "Generate pre-signed upload URL (Step 1 of 3)",
            description = """
                    Generates a pre-signed URL for direct upload to R2 storage.

                    **Process:**
                    1. Backend validates request and generates secure upload URL
                    2. Frontend uploads file directly to R2 using the URL
                    3. Frontend calls /confirm endpoint to finalize

                    **Security:**
                    - URL expires in 30 minutes
                    - File size and type validation
                    - Ownership validation for listing association
                    """
    )
    public ResponseEntity<ApiResponse<GenerateUploadUrlResponse>> generateUploadUrl(
            @Valid @RequestBody GenerateUploadUrlRequest request,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        log.info("POST /v1/media/upload-url - user: {}, type: {}", userId, request.getMediaType());

        GenerateUploadUrlResponse response = mediaService.generateUploadUrl(request, userId);

        return ResponseEntity.ok(ApiResponse.<GenerateUploadUrlResponse>builder()
                .data(response)
                .message("Upload URL generated successfully. Please upload file within " +
                        (response.getExpiresIn() / 60) + " minutes.")
                .build());
    }

    @PostMapping("/{mediaId}/confirm")
    @Operation(
            summary = "Confirm upload completion (Step 3 of 3)",
            description = """
                    Confirms that file upload is complete and makes media active.

                    **Required after:**
                    - File successfully uploaded to pre-signed URL

                    **Result:**
                    - Media status changed from PENDING to ACTIVE
                    - Public URL generated
                    - Media becomes accessible
                    """
    )
    public ResponseEntity<ApiResponse<MediaResponse>> confirmUpload(
            @Parameter(description = "Media ID from upload URL response")
            @PathVariable Long mediaId,
            @Valid @RequestBody ConfirmUploadRequest request,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        log.info("POST /v1/media/{}/confirm - user: {}", mediaId, userId);

        MediaResponse response = mediaService.confirmUpload(mediaId, request, userId);

        return ResponseEntity.ok(ApiResponse.<MediaResponse>builder()
                .data(response)
                .message("Upload confirmed successfully. Media is now active.")
                .build());
    }

    @GetMapping("/{mediaId}/download-url")
    @Operation(
            summary = "Generate pre-signed download URL",
            description = """
                    Generates a secure download URL for uploaded media.

                    **Permissions:**
                    - Media owner can always download
                    - Public listing media can be downloaded by anyone

                    **URL lifetime:** 60 minutes

                    **Note:** For external media (YouTube/TikTok), returns the original URL.
                    """
    )
    public ResponseEntity<ApiResponse<String>> generateDownloadUrl(
            @Parameter(description = "Media ID")
            @PathVariable Long mediaId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        log.info("GET /v1/media/{}/download-url - user: {}", mediaId, userId);

        String downloadUrl = mediaService.generateDownloadUrl(mediaId, userId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .data(downloadUrl)
                .message("Download URL generated successfully")
                .build());
    }

    @DeleteMapping("/{mediaId}")
    @Operation(
            summary = "Delete media",
            description = """
                    Deletes media (soft delete with storage cleanup).

                    **Actions:**
                    - Media status set to DELETED
                    - File removed from R2 storage
                    - Database record retained for audit

                    **Permission:** Only media owner can delete
                    """
    )
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @Parameter(description = "Media ID")
            @PathVariable Long mediaId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        log.info("DELETE /v1/media/{} - user: {}", mediaId, userId);

        mediaService.deleteMedia(mediaId, userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Media deleted successfully")
                .build());
    }

    @PostMapping("/external")
    @Operation(
            summary = "Save external media (YouTube/TikTok)",
            description = """
                    Saves YouTube or TikTok video as external media.

                    **Supported platforms:**
                    - YouTube (youtube.com, youtu.be)
                    - TikTok (tiktok.com)

                    **Features:**
                    - Automatic platform detection
                    - Embed code generation
                    - No file upload required
                    """
    )
    public ResponseEntity<ApiResponse<MediaResponse>> saveExternalMedia(
            @Valid @RequestBody SaveExternalMediaRequest request,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        log.info("POST /v1/media/external - user: {}, URL: {}", userId, request.getUrl());

        MediaResponse response = mediaService.saveExternalMedia(request, userId);

        return ResponseEntity.ok(ApiResponse.<MediaResponse>builder()
                .data(response)
                .message("External media saved successfully")
                .build());
    }

    @GetMapping("/listing/{listingId}")
    @Operation(
            summary = "Get all media for a listing",
            description = """
                    Returns all active media for a specific listing, ordered by sort order.

                    **Returns:**
                    - Images and videos
                    - Both uploaded and external media
                    - Ordered by sortOrder field
                    """
    )
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getListingMedia(
            @Parameter(description = "Listing ID")
            @PathVariable Long listingId) {

        log.info("GET /v1/media/listing/{}", listingId);

        List<MediaResponse> media = mediaService.getListingMedia(listingId);

        return ResponseEntity.ok(ApiResponse.<List<MediaResponse>>builder()
                .data(media)
                .message("Media retrieved successfully")
                .build());
    }

    @GetMapping("/my-media")
    @Operation(
            summary = "Get all media for current user",
            description = """
                    Returns all active media owned by the authenticated user.

                    **Returns:**
                    - All user's media regardless of listing
                    - Ordered by creation date (newest first)
                    """
    )
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getMyMedia(
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        log.info("GET /v1/media/my-media - user: {}", userId);

        List<MediaResponse> media = mediaService.getUserMedia(userId);

        return ResponseEntity.ok(ApiResponse.<List<MediaResponse>>builder()
                .data(media)
                .message("Media retrieved successfully")
                .build());
    }

    @GetMapping("/{mediaId}")
    @Operation(
            summary = "Get media by ID",
            description = "Returns detailed information about a specific media item."
    )
    public ResponseEntity<ApiResponse<MediaResponse>> getMediaById(
            @Parameter(description = "Media ID")
            @PathVariable Long mediaId) {

        log.info("GET /v1/media/{}", mediaId);

        MediaResponse media = mediaService.getMediaById(mediaId);

        return ResponseEntity.ok(ApiResponse.<MediaResponse>builder()
                .data(media)
                .message("Media retrieved successfully")
                .build());
    }

    /**
     * Extract user ID from authentication
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid user ID format in authentication", e);
        }
    }
}
