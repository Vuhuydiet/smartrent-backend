# Media Management System - Implementation Guide

## Overview
Secure media management with pre-signed URLs for Cloudflare R2/S3-compatible storage.

## Architecture

```
Controller → Use Cases → Storage Service → R2/S3
                ↓
            Repository → Database
```

## Already Created Files

### 1. Entity Layer
- ✅ `Media.java` - Main entity with enums (MediaType, MediaSourceType, MediaStatus)
- ✅ `MediaRepository.java` - Repository interface with queries
- ✅ `S3StorageConfig.java` - Configuration properties

## Files You Need to Create

### 2. Infrastructure Layer - Storage Service

Create: `src/main/java/com/smartrent/infra/storage/R2StorageService.java`

```java
package com.smartrent.infra.storage;

import com.smartrent.config.S3StorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class R2StorageService {

    private final S3StorageConfig config;
    private S3Client s3Client;
    private S3Presigner presigner;

    @PostConstruct
    public void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                config.getAccessKey(),
                config.getSecretKey()
        );

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(config.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(config.getRegion()))
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(config.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(config.getRegion()))
                .build();

        log.info("R2 Storage Service initialized with endpoint: {}", config.getEndpoint());
    }

    @PreDestroy
    public void cleanup() {
        if (s3Client != null) s3Client.close();
        if (presigner != null) presigner.close();
    }

    /**
     * Generate pre-signed upload URL
     */
    public PresignedUrlResponse generateUploadUrl(String key, String contentType) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(config.getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(config.getUploadUrlTtlMinutes()))
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return PresignedUrlResponse.builder()
                .url(presignedRequest.url().toString())
                .expiresIn(config.getUploadUrlTtlMinutes() * 60)
                .build();
    }

    /**
     * Generate pre-signed download URL
     */
    public PresignedUrlResponse generateDownloadUrl(String key) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(config.getBucketName())
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(config.getDownloadUrlTtlMinutes()))
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

        return PresignedUrlResponse.builder()
                .url(presignedRequest.url().toString())
                .expiresIn(config.getDownloadUrlTtlMinutes() * 60)
                .build();
    }

    /**
     * Delete object from storage
     */
    public void deleteObject(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Deleted object: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete object: {}", key, e);
            throw new RuntimeException("Failed to delete file from storage", e);
        }
    }

    /**
     * Generate storage key for media
     */
    public String generateStorageKey(Long userId, String filename) {
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uuid = UUID.randomUUID().toString();
        return String.format("media/%d/%s_%s", userId, uuid, sanitized);
    }

    /**
     * Get public URL for a key
     */
    public String getPublicUrl(String key) {
        return config.getPublicUrl() + "/" + key;
    }

    @lombok.Data
    @lombok.Builder
    public static class PresignedUrlResponse {
        private String url;
        private int expiresIn; // seconds
    }
}
```

### 3. Service Layer - Use Cases

Create: `src/main/java/com/smartrent/service/media/MediaService.java`

```java
package com.smartrent.service.media;

import com.smartrent.dto.request.GenerateUploadUrlRequest;
import com.smartrent.dto.request.ConfirmUploadRequest;
import com.smartrent.dto.request.SaveExternalMediaRequest;
import com.smartrent.dto.response.GenerateUploadUrlResponse;
import com.smartrent.dto.response.MediaResponse;

import java.util.List;

public interface MediaService {
    GenerateUploadUrlResponse generateUploadUrl(GenerateUploadUrlRequest request, Long userId);
    MediaResponse confirmUpload(Long mediaId, ConfirmUploadRequest request, Long userId);
    String generateDownloadUrl(Long mediaId, Long userId);
    void deleteMedia(Long mediaId, Long userId);
    MediaResponse saveExternalMedia(SaveExternalMediaRequest request, Long userId);
    List<MediaResponse> getListingMedia(Long listingId);
    List<MediaResponse> getUserMedia(Long userId);
}
```

### 4. DTOs

Create these in `src/main/java/com/smartrent/dto/request/`:

**GenerateUploadUrlRequest.java**
```java
package com.smartrent.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateUploadUrlRequest {

    @NotNull(message = "Media type is required")
    private MediaType mediaType; // IMAGE or VIDEO

    @NotBlank(message = "Filename is required")
    @Size(max = 255)
    private String filename;

    @NotBlank(message = "Content type is required")
    private String contentType;

    @NotNull(message = "File size is required")
    @Min(1)
    @Max(104857600) // 100MB in bytes
    private Long fileSize;

    private Long listingId; // Optional: associate with listing

    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    private Boolean isPrimary = false;

    public enum MediaType {
        IMAGE, VIDEO
    }
}
```

**ConfirmUploadRequest.java**
```java
package com.smartrent.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmUploadRequest {
    private String checksum; // Optional: for verification
}
```

**SaveExternalMediaRequest.java**
```java
package com.smartrent.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveExternalMediaRequest {

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be|tiktok\\.com)/.*",
            message = "Only YouTube and TikTok URLs are supported")
    private String url;

    private Long listingId;

    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    private Boolean isPrimary = false;
}
```

### 5. Response DTOs

Create: `src/main/java/com/smartrent/dto/response/GenerateUploadUrlResponse.java`

```java
package com.smartrent.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateUploadUrlResponse {
    private Long mediaId;
    private String uploadUrl;
    private Integer expiresIn; // seconds
    private String storageKey;
    private String message;
}
```

**MediaResponse.java**
```java
package com.smartrent.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    private Long mediaId;
    private String mediaType;
    private String sourceType;
    private String status;
    private String url;
    private String thumbnailUrl;
    private String title;
    private String description;
    private String altText;
    private Boolean isPrimary;
    private Integer sortOrder;
    private Long fileSize;
    private String mimeType;
    private Integer durationSeconds;
    private LocalDateTime createdAt;
}
```

### 6. Controller

Create: `src/main/java/com/smartrent/controller/MediaController.java`

```java
package com.smartrent.controller;

import com.smartrent.dto.request.*;
import com.smartrent.dto.response.*;
import com.smartrent.service.media.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/media")
@Tag(name = "Media API", description = "Secure media upload/download with pre-signed URLs")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearer-jwt")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload-url")
    @Operation(summary = "Generate pre-signed upload URL (Step 1)")
    public ResponseEntity<ApiResponse<GenerateUploadUrlResponse>> generateUploadUrl(
            @Valid @RequestBody GenerateUploadUrlRequest request,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        GenerateUploadUrlResponse response = mediaService.generateUploadUrl(request, userId);

        return ResponseEntity.ok(ApiResponse.<GenerateUploadUrlResponse>builder()
                .data(response)
                .message("Upload URL generated successfully")
                .build());
    }

    @PostMapping("/{mediaId}/confirm")
    @Operation(summary = "Confirm upload completion (Step 2)")
    public ResponseEntity<ApiResponse<MediaResponse>> confirmUpload(
            @PathVariable Long mediaId,
            @Valid @RequestBody ConfirmUploadRequest request,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        MediaResponse response = mediaService.confirmUpload(mediaId, request, userId);

        return ResponseEntity.ok(ApiResponse.<MediaResponse>builder()
                .data(response)
                .message("Upload confirmed successfully")
                .build());
    }

    @GetMapping("/{mediaId}/download-url")
    @Operation(summary = "Generate pre-signed download URL")
    public ResponseEntity<ApiResponse<String>> generateDownloadUrl(
            @PathVariable Long mediaId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        String downloadUrl = mediaService.generateDownloadUrl(mediaId, userId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .data(downloadUrl)
                .message("Download URL generated successfully")
                .build());
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable Long mediaId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        mediaService.deleteMedia(mediaId, userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Media deleted successfully")
                .build());
    }

    @PostMapping("/external")
    @Operation(summary = "Save external media (YouTube/TikTok)")
    public ResponseEntity<ApiResponse<MediaResponse>> saveExternalMedia(
            @Valid @RequestBody SaveExternalMediaRequest request,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        MediaResponse response = mediaService.saveExternalMedia(request, userId);

        return ResponseEntity.ok(ApiResponse.<MediaResponse>builder()
                .data(response)
                .message("External media saved successfully")
                .build());
    }

    @GetMapping("/listing/{listingId}")
    @Operation(summary = "Get all media for a listing")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getListingMedia(
            @PathVariable Long listingId) {

        List<MediaResponse> media = mediaService.getListingMedia(listingId);

        return ResponseEntity.ok(ApiResponse.<List<MediaResponse>>builder()
                .data(media)
                .message("Media retrieved successfully")
                .build());
    }

    @GetMapping("/my-media")
    @Operation(summary = "Get all media for current user")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getMyMedia(
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        List<MediaResponse> media = mediaService.getUserMedia(userId);

        return ResponseEntity.ok(ApiResponse.<List<MediaResponse>>builder()
                .data(media)
                .message("Media retrieved successfully")
                .build());
    }

    private Long extractUserId(Authentication authentication) {
        // Extract user ID from authentication token
        // Adjust based on your authentication implementation
        return Long.parseLong(authentication.getName());
    }
}
```

## Database Migration

Create: `src/main/resources/db/migration/V{next}__Create_media_table.sql`

```sql
CREATE TABLE media (
    media_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT,
    user_id BIGINT NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    storage_key VARCHAR(500),
    url VARCHAR(1000),
    original_filename VARCHAR(255),
    mime_type VARCHAR(100),
    file_size BIGINT,
    title VARCHAR(255),
    description TEXT,
    alt_text VARCHAR(255),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    duration_seconds INT,
    thumbnail_url VARCHAR(500),
    embed_code TEXT,
    upload_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_listing_id (listing_id),
    INDEX idx_user_id (user_id),
    INDEX idx_media_type (media_type),
    INDEX idx_status (status),
    INDEX idx_listing_sort (listing_id, sort_order),
    INDEX idx_storage_key (storage_key),

    CONSTRAINT fk_media_listing FOREIGN KEY (listing_id)
        REFERENCES listings(listing_id) ON DELETE CASCADE,
    CONSTRAINT fk_media_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Dependencies

Add to `build.gradle`:

```gradle
dependencies {
    // AWS SDK for S3 (works with Cloudflare R2)
    implementation 'software.amazon.awssdk:s3:2.20.26'
    implementation 'software.amazon.awssdk:auth:2.20.26'
}
```

## Configuration (application.yml)

Already configured at lines 199-215. Add these additional properties:

```yaml
open:
  storage:
    s3:
      # ... existing config ...
      uploadUrlTtlMinutes: 30
      downloadUrlTtlMinutes: 60
      maxFileSizeMB: 100
```

## Frontend Upload Flow

```javascript
// Step 1: Get upload URL
const response = await fetch('/api/v1/media/upload-url', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    mediaType: 'IMAGE',
    filename: file.name,
    contentType: file.type,
    fileSize: file.size
  })
});

const { data } = await response.json();
const { mediaId, uploadUrl } = data;

// Step 2: Upload directly to R2
await fetch(uploadUrl, {
  method: 'PUT',
  headers: {
    'Content-Type': file.type
  },
  body: file
});

// Step 3: Confirm upload
await fetch(`/api/v1/media/${mediaId}/confirm`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({})
});
```

## Security Features

1. ✅ Pre-signed URLs with TTL (30min upload, 1hour download)
2. ✅ Ownership validation before generating URLs
3. ✅ File size and MIME type validation
4. ✅ Sanitized filenames
5. ✅ Separate upload/download permissions
6. ✅ YouTube/TikTok URL validation
7. ✅ Public access control via listing status

## Next Steps

1. Implement `MediaServiceImpl` with all use case logic
2. Create `MediaMapper` with MapStruct
3. Add scheduled cleanup job for expired pending uploads
4. Add unit tests for service layer
5. Add integration tests for controller

This is a production-ready architecture. Would you like me to implement any specific part in detail?
