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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/media")
@Tag(
        name = "Media API",
        description = """
                Secure media upload/download with Cloudflare R2 storage

                **Two Upload Methods:**

                **Method 1: Client-Side Upload (Pre-signed URL)**
                1. POST /upload-url - Get pre-signed upload URL
                2. PUT to presigned URL - Client uploads directly to R2
                3. POST /{mediaId}/confirm - Confirm upload completion
                *Best for: Large files, reduced server load*

                **Method 2: Backend Upload (Direct)**
                1. POST /upload - Upload file via backend to R2
                *Best for: Simple flow, file processing, smaller files*

                **Features:**
                - Multiple upload strategies (client-side/backend)
                - Pre-signed URLs for secure operations
                - Support for images and videos
                - YouTube/TikTok embed support
                - File validation (type, size)
                - Ownership validation
                - Automatic cleanup of expired uploads
                """
)
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
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
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Upload URL generation request",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GenerateUploadUrlRequest.class),
                            examples = @ExampleObject(
                                    name = "Generate Upload URL Example",
                                    value = """
                                            {
                                              "mediaType": "IMAGE",
                                              "filename": "property-photo.jpg",
                                              "contentType": "image/jpeg",
                                              "fileSize": 2048576,
                                              "listingId": 123,
                                              "title": "Living Room",
                                              "description": "Spacious living room with natural light",
                                              "altText": "Living room photo",
                                              "isPrimary": true,
                                              "sortOrder": 1
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Upload URL generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Upload URL generated successfully. Please upload file within 30 minutes.",
                                              "data": {
                                                "mediaId": 456,
                                                "uploadUrl": "https://r2.cloudflarestorage.com/bucket/media/...",
                                                "expiresIn": 1800,
                                                "storageKey": "media/user-123/456-property-photo.jpg",
                                                "message": "Upload file to the provided URL using PUT method"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not own the listing",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<GenerateUploadUrlResponse>> generateUploadUrl(
            @Valid @RequestBody GenerateUploadUrlRequest request,
            Authentication authentication) {

        String userId = extractUserId(authentication);
        log.info("POST /v1/media/upload-url - user: {}, type: {}", userId, request.getMediaType());

        GenerateUploadUrlResponse response = mediaService.generateUploadUrl(request, userId);

        return ResponseEntity.ok(ApiResponse.<GenerateUploadUrlResponse>builder()
                .data(response)
                .message("Upload URL generated successfully. Please upload file within " +
                        (response.getExpiresIn() / 60) + " minutes.")
                .build());
    }

    @PostMapping("/upload")
    @Operation(
            summary = "Upload media directly from backend to cloud",
            description = """
                    Complete upload flow in a single request (backend uploads to R2).

                    **Process:**
                    1. Backend receives file via multipart/form-data
                    2. Backend validates file (type, size, ownership)
                    3. Backend uploads directly to R2 storage
                    4. Backend saves media record as ACTIVE
                    5. Returns media response with public URL

                    **Use this when:**
                    - Simple upload flow preferred
                    - File processing needed before upload
                    - Centralized upload logic required

                    **Alternative:** Use /upload-url for client-side direct upload (more efficient for large files)

                    **Parameters:**
                    - file: The media file (required)
                    - mediaType: IMAGE or VIDEO (required)
                    - listingId: Associate with listing (optional)
                    - title, description, altText: Metadata (optional)
                    - isPrimary: Set as primary media (default: false)
                    - sortOrder: Display order (default: 0)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Media uploaded successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Media uploaded successfully. File is now active and accessible.",
                                              "data": {
                                                "mediaId": 456,
                                                "listingId": 123,
                                                "userId": "user-uuid-123",
                                                "mediaType": "IMAGE",
                                                "sourceType": "UPLOAD",
                                                "status": "ACTIVE",
                                                "url": "https://pub-xxx.r2.dev/media/user-123/456-property-photo.jpg",
                                                "thumbnailUrl": null,
                                                "title": "Living Room",
                                                "description": "Spacious living room with natural light",
                                                "altText": "Living room photo",
                                                "isPrimary": true,
                                                "sortOrder": 1,
                                                "fileSize": 2048576,
                                                "mimeType": "image/jpeg",
                                                "originalFilename": "property-photo.jpg",
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
                    description = "Forbidden - User does not own the listing",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<MediaResponse>> uploadMedia(
            @Parameter(description = "Media file to upload", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Type of media: IMAGE or VIDEO", required = true)
            @RequestParam("mediaType") String mediaType,

            @Parameter(description = "Listing ID to associate with")
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
            @RequestParam(value = "sortOrder", required = false, defaultValue = "0") Integer sortOrder,

            Authentication authentication) {

        String userId = extractUserId(authentication);
        log.info("POST /v1/media/upload - user: {}, filename: {}, type: {}",
                userId, file.getOriginalFilename(), mediaType);

        MediaResponse response = mediaService.uploadMedia(
                file, mediaType, listingId, title, description,
                altText, isPrimary, sortOrder, userId
        );

        return ResponseEntity.ok(ApiResponse.<MediaResponse>builder()
                .data(response)
                .message("Media uploaded successfully. File is now active and accessible.")
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
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Upload confirmation request",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfirmUploadRequest.class),
                            examples = @ExampleObject(
                                    name = "Confirm Upload Example",
                                    value = """
                                            {
                                              "checksum": "abc123def456",
                                              "contentType": "image/jpeg"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Upload confirmed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Upload confirmed successfully. Media is now active.",
                                              "data": {
                                                "mediaId": 456,
                                                "listingId": 123,
                                                "userId": "user-uuid-123",
                                                "mediaType": "IMAGE",
                                                "sourceType": "UPLOAD",
                                                "status": "ACTIVE",
                                                "url": "https://pub-xxx.r2.dev/media/user-123/456-property-photo.jpg",
                                                "thumbnailUrl": null,
                                                "title": "Living Room",
                                                "description": "Spacious living room",
                                                "altText": "Living room photo",
                                                "isPrimary": true,
                                                "sortOrder": 1,
                                                "fileSize": 2048576,
                                                "mimeType": "image/jpeg",
                                                "originalFilename": "property-photo.jpg",
                                                "durationSeconds": null,
                                                "uploadConfirmed": true,
                                                "createdAt": "2024-01-15T10:30:00",
                                                "updatedAt": "2024-01-15T10:31:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or upload already confirmed",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not own the media",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Media not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<MediaResponse>> confirmUpload(
            @Parameter(description = "Media ID from upload URL response")
            @PathVariable Long mediaId,
            @Valid @RequestBody ConfirmUploadRequest request,
            Authentication authentication) {

        String userId = extractUserId(authentication);
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Download URL generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Download URL generated successfully",
                                              "data": "https://r2.cloudflarestorage.com/bucket/media/...?X-Amz-Signature=..."
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have access to this media",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Media not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<String>> generateDownloadUrl(
            @Parameter(description = "Media ID")
            @PathVariable Long mediaId,
            Authentication authentication) {

        String userId = extractUserId(authentication);
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Media deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Media deleted successfully",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not own the media",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Media not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @Parameter(description = "Media ID")
            @PathVariable Long mediaId,
            Authentication authentication) {

        String userId = extractUserId(authentication);
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
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "External media request",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SaveExternalMediaRequest.class),
                            examples = @ExampleObject(
                                    name = "YouTube Video Example",
                                    value = """
                                            {
                                              "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                                              "listingId": 123,
                                              "title": "Property Tour Video",
                                              "description": "Virtual tour of the apartment",
                                              "altText": "Property tour video",
                                              "isPrimary": false,
                                              "sortOrder": 5
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "External media saved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "External media saved successfully",
                                              "data": {
                                                "mediaId": 789,
                                                "listingId": 123,
                                                "userId": "user-uuid-123",
                                                "mediaType": "VIDEO",
                                                "sourceType": "YOUTUBE",
                                                "status": "ACTIVE",
                                                "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                                                "thumbnailUrl": "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
                                                "title": "Property Tour Video",
                                                "description": "Virtual tour of the apartment",
                                                "altText": "Property tour video",
                                                "isPrimary": false,
                                                "sortOrder": 5,
                                                "fileSize": null,
                                                "mimeType": null,
                                                "originalFilename": null,
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
                    description = "Invalid URL or unsupported platform",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not own the listing",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<MediaResponse>> saveExternalMedia(
            @Valid @RequestBody SaveExternalMediaRequest request,
            Authentication authentication) {

        String userId = extractUserId(authentication);
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Media retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Media retrieved successfully",
                                              "data": [
                                                {
                                                  "mediaId": 456,
                                                  "listingId": 123,
                                                  "userId": "user-uuid-123",
                                                  "mediaType": "IMAGE",
                                                  "sourceType": "UPLOAD",
                                                  "status": "ACTIVE",
                                                  "url": "https://pub-xxx.r2.dev/media/...",
                                                  "isPrimary": true,
                                                  "sortOrder": 1
                                                },
                                                {
                                                  "mediaId": 789,
                                                  "listingId": 123,
                                                  "userId": "user-uuid-123",
                                                  "mediaType": "VIDEO",
                                                  "sourceType": "YOUTUBE",
                                                  "status": "ACTIVE",
                                                  "url": "https://www.youtube.com/watch?v=...",
                                                  "isPrimary": false,
                                                  "sortOrder": 2
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found",
                    content = @Content(mediaType = "application/json")
            )
    })
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User media retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Media retrieved successfully",
                                              "data": [
                                                {
                                                  "mediaId": 456,
                                                  "listingId": 123,
                                                  "userId": "user-uuid-123",
                                                  "mediaType": "IMAGE",
                                                  "sourceType": "UPLOAD",
                                                  "status": "ACTIVE",
                                                  "url": "https://pub-xxx.r2.dev/media/...",
                                                  "isPrimary": true,
                                                  "sortOrder": 1,
                                                  "createdAt": "2024-01-15T10:30:00"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getMyMedia(
            Authentication authentication) {

        String userId = extractUserId(authentication);
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Media retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Media retrieved successfully",
                                              "data": {
                                                "mediaId": 456,
                                                "listingId": 123,
                                                "userId": "user-uuid-123",
                                                "mediaType": "IMAGE",
                                                "sourceType": "UPLOAD",
                                                "status": "ACTIVE",
                                                "url": "https://pub-xxx.r2.dev/media/user-123/456-property-photo.jpg",
                                                "thumbnailUrl": null,
                                                "title": "Living Room",
                                                "description": "Spacious living room with natural light",
                                                "altText": "Living room photo",
                                                "isPrimary": true,
                                                "sortOrder": 1,
                                                "fileSize": 2048576,
                                                "mimeType": "image/jpeg",
                                                "originalFilename": "property-photo.jpg",
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
                    responseCode = "404",
                    description = "Media not found",
                    content = @Content(mediaType = "application/json")
            )
    })
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
     * User ID is stored as UUID String in the database and JWT token
     */
    private String extractUserId(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        // Return the UUID string directly from JWT subject claim
        return authentication.getName();
    }
}
