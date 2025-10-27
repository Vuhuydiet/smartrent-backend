package com.smartrent.service.media;

import com.smartrent.dto.request.ConfirmUploadRequest;
import com.smartrent.dto.request.GenerateUploadUrlRequest;
import com.smartrent.dto.request.SaveExternalMediaRequest;
import com.smartrent.dto.response.GenerateUploadUrlResponse;
import com.smartrent.dto.response.MediaResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.MediaRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.Media;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.infra.storage.R2StorageService;
import com.smartrent.mapper.MediaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final R2StorageService storageService;
    private final MediaMapper mediaMapper;

    @Override
    @Transactional
    public GenerateUploadUrlResponse generateUploadUrl(GenerateUploadUrlRequest request, Long userId) {
        String userIdStr = String.valueOf(userId);
        log.info("Generating upload URL for user: {}, type: {}", userIdStr, request.getMediaType());

        // Validate user exists
        User user = userRepository.findById(userIdStr)
                .orElseThrow(() -> new AppException(DomainCode.USER_NOT_FOUND, "User not found"));

        // Validate content type
        boolean isImage = request.getMediaType() == GenerateUploadUrlRequest.MediaType.IMAGE;
        if (!storageService.isValidContentType(request.getContentType(), isImage)) {
            throw new AppException(DomainCode.INVALID_FILE_TYPE,
                    String.format("Invalid content type: %s", request.getContentType()));
        }

        // Validate file size
        if (!storageService.isValidFileSize(request.getFileSize())) {
            throw new AppException(DomainCode.INVALID_FILE_TYPE,
                    "File size exceeds maximum allowed");
        }

        // Validate listing if provided
        Listing listing = null;
        if (request.getListingId() != null) {
            listing = listingRepository.findById(request.getListingId())
                    .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND, "Listing not found"));

            // Validate user owns the listing
            if (!listing.getUserId().equals(userIdStr)) {
                throw new AppException(DomainCode.UNAUTHORIZED,
                        "You don't have permission to add media to this listing");
            }
        }

        // Generate storage key
        String storageKey = storageService.generateStorageKey(userId, request.getFilename());

        // Create media entity in PENDING status
        Media media = Media.builder()
                .userId(userIdStr)
                .listing(listing)
                .mediaType(request.getMediaType() == GenerateUploadUrlRequest.MediaType.IMAGE ?
                        Media.MediaType.IMAGE : Media.MediaType.VIDEO)
                .sourceType(Media.MediaSourceType.UPLOAD)
                .status(Media.MediaStatus.PENDING)
                .storageKey(storageKey)
                .originalFilename(request.getFilename())
                .mimeType(request.getContentType())
                .fileSize(request.getFileSize())
                .title(request.getTitle())
                .description(request.getDescription())
                .altText(request.getAltText())
                .isPrimary(request.getIsPrimary() != null && request.getIsPrimary())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .uploadConfirmed(false)
                .build();

        media = mediaRepository.save(media);

        // Generate pre-signed upload URL
        R2StorageService.PresignedUrlResponse presignedUrl =
                storageService.generateUploadUrl(storageKey, request.getContentType());

        log.info("Upload URL generated successfully for media ID: {}", media.getMediaId());

        return GenerateUploadUrlResponse.builder()
                .mediaId(media.getMediaId())
                .uploadUrl(presignedUrl.getUrl())
                .expiresIn(presignedUrl.getExpiresIn())
                .storageKey(storageKey)
                .message("Upload URL generated. Please upload the file to the provided URL within "
                        + (presignedUrl.getExpiresIn() / 60) + " minutes.")
                .build();
    }

    @Override
    @Transactional
    public MediaResponse uploadMedia(
            MultipartFile file,
            String mediaType,
            Long listingId,
            String title,
            String description,
            String altText,
            Boolean isPrimary,
            Integer sortOrder,
            Long userId) {

        String userIdStr = String.valueOf(userId);
        log.info("Starting direct upload for user: {}, filename: {}, type: {}",
                userIdStr, file.getOriginalFilename(), mediaType);

        // Validate user exists
        User user = userRepository.findById(userIdStr)
                .orElseThrow(() -> new AppException(DomainCode.USER_NOT_FOUND, "User not found"));

        // Validate file is not empty
        if (file.isEmpty()) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR, "File is empty");
        }

        // Validate file size
        if (!storageService.isValidFileSize(file.getSize())) {
            throw new AppException(DomainCode.INVALID_FILE_TYPE,
                    String.format("File size exceeds maximum allowed: %d MB", file.getSize() / (1024 * 1024)));
        }

        // Determine if image or video
        boolean isImage = "IMAGE".equalsIgnoreCase(mediaType);
        Media.MediaType mediaTypeEnum = isImage ? Media.MediaType.IMAGE : Media.MediaType.VIDEO;

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !storageService.isValidContentType(contentType, isImage)) {
            throw new AppException(DomainCode.INVALID_FILE_TYPE,
                    String.format("Invalid content type: %s for media type: %s", contentType, mediaType));
        }

        // Validate listing if provided
        Listing listing = null;
        if (listingId != null) {
            listing = listingRepository.findById(listingId)
                    .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND, "Listing not found"));

            // Validate user owns the listing
            if (!listing.getUserId().equals(userIdStr)) {
                throw new AppException(DomainCode.UNAUTHORIZED,
                        "You don't have permission to add media to this listing");
            }
        }

        // Generate storage key
        String storageKey = storageService.generateStorageKey(userId, file.getOriginalFilename());

        try {
            // Upload file directly to R2/S3
            log.info("Uploading file to cloud storage: {}", storageKey);
            storageService.uploadFile(
                    storageKey,
                    file.getInputStream(),
                    contentType,
                    file.getSize()
            );
            log.info("File uploaded successfully to cloud storage: {}", storageKey);

            // Generate public URL
            String publicUrl = storageService.getPublicUrl(storageKey);

            // Create media entity in ACTIVE status (already uploaded)
            Media media = Media.builder()
                    .userId(userIdStr)
                    .listing(listing)
                    .mediaType(mediaTypeEnum)
                    .sourceType(Media.MediaSourceType.UPLOAD)
                    .status(Media.MediaStatus.ACTIVE)
                    .storageKey(storageKey)
                    .url(publicUrl)
                    .originalFilename(file.getOriginalFilename())
                    .mimeType(contentType)
                    .fileSize(file.getSize())
                    .title(title)
                    .description(description)
                    .altText(altText)
                    .isPrimary(isPrimary != null && isPrimary)
                    .sortOrder(sortOrder != null ? sortOrder : 0)
                    .uploadConfirmed(true)
                    .confirmedAt(LocalDateTime.now())
                    .build();

            media = mediaRepository.save(media);

            log.info("Media saved successfully with ID: {}", media.getMediaId());

            return mediaMapper.toResponse(media);

        } catch (IOException e) {
            log.error("Failed to read file content: {}", file.getOriginalFilename(), e);
            throw new AppException(DomainCode.FILE_READ_ERROR,
                    e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to upload file to cloud storage: {}", storageKey, e);
            throw new AppException(DomainCode.FILE_UPLOAD_ERROR,
                    e.getMessage());
        }
    }

    @Override
    @Transactional
    public MediaResponse confirmUpload(Long mediaId, ConfirmUploadRequest request, Long userId) {
        String userIdStr = String.valueOf(userId);
        log.info("Confirming upload for media ID: {}, user: {}", mediaId, userIdStr);

        // Find media and validate ownership
        Media media = mediaRepository.findByMediaIdAndUserId(mediaId, userIdStr)
                .orElseThrow(() -> new AppException(DomainCode.UNAUTHORIZED,
                        "Media not found or you don't have permission"));

        // Validate status
        if (media.getStatus() != Media.MediaStatus.PENDING) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                    "Media is not in pending status");
        }

        // Already confirmed
        if (media.getUploadConfirmed()) {
            log.warn("Upload already confirmed for media ID: {}", mediaId);
            return mediaMapper.toResponse(media);
        }

        // Update media status
        media.setStatus(Media.MediaStatus.ACTIVE);
        media.setUploadConfirmed(true);
        media.setConfirmedAt(LocalDateTime.now());

        // Set public URL
        String publicUrl = storageService.getPublicUrl(media.getStorageKey());
        media.setUrl(publicUrl);

        media = mediaRepository.save(media);

        log.info("Upload confirmed successfully for media ID: {}", mediaId);

        return mediaMapper.toResponse(media);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateDownloadUrl(Long mediaId, Long userId) {
        String userIdStr = String.valueOf(userId);
        log.info("Generating download URL for media ID: {}, user: {}", mediaId, userIdStr);

        // Find media
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(DomainCode.ADDRESS_NOT_FOUND,
                        "Media not found"));

        // Validate media is active
        if (media.getStatus() != Media.MediaStatus.ACTIVE) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                    "Media is not available");
        }

        // For external media, return the URL directly
        if (media.isExternal()) {
            return media.getUrl();
        }

        // For uploaded media, check permissions
        // Allow owner or if listing is public
        boolean hasPermission = media.isOwnedBy(userIdStr) ||
                (media.getListing() != null && isListingPublic(media.getListing()));

        if (!hasPermission) {
            throw new AppException(DomainCode.UNAUTHORIZED,
                    "You don't have permission to access this media");
        }

        // Generate pre-signed download URL
        R2StorageService.PresignedUrlResponse presignedUrl =
                storageService.generateDownloadUrl(media.getStorageKey());

        log.info("Download URL generated successfully for media ID: {}", mediaId);

        return presignedUrl.getUrl();
    }

    @Override
    @Transactional
    public void deleteMedia(Long mediaId, Long userId) {
        String userIdStr = String.valueOf(userId);
        log.info("Deleting media ID: {}, user: {}", mediaId, userIdStr);

        // Find media and validate ownership
        Media media = mediaRepository.findByMediaIdAndUserId(mediaId, userIdStr)
                .orElseThrow(() -> new AppException(DomainCode.UNAUTHORIZED,
                        "Media not found or you don't have permission"));

        // Update status to DELETED
        media.setStatus(Media.MediaStatus.DELETED);
        mediaRepository.save(media);

        // Delete from storage if uploaded file
        if (media.isUploaded() && media.getStorageKey() != null) {
            try {
                storageService.deleteObject(media.getStorageKey());
                log.info("File deleted from storage: {}", media.getStorageKey());
            } catch (Exception e) {
                log.error("Failed to delete file from storage: {}", media.getStorageKey(), e);
                // Don't fail the whole operation if storage deletion fails
            }
        }

        log.info("Media deleted successfully: {}", mediaId);
    }

    @Override
    @Transactional
    public MediaResponse saveExternalMedia(SaveExternalMediaRequest request, Long userId) {
        String userIdStr = String.valueOf(userId);
        log.info("Saving external media for user: {}, URL: {}", userIdStr, request.getUrl());

        // Validate user exists
        User user = userRepository.findById(userIdStr)
                .orElseThrow(() -> new AppException(DomainCode.USER_NOT_FOUND, "User not found"));

        // Validate listing if provided
        Listing listing = null;
        if (request.getListingId() != null) {
            listing = listingRepository.findById(request.getListingId())
                    .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND, "Listing not found"));

            // Validate user owns the listing
            if (!listing.getUserId().equals(userIdStr)) {
                throw new AppException(DomainCode.UNAUTHORIZED,
                        "You don't have permission to add media to this listing");
            }
        }

        // Determine source type from URL
        Media.MediaSourceType sourceType = detectSourceType(request.getUrl());

        // Create media entity
        Media media = Media.builder()
                .userId(userIdStr)
                .listing(listing)
                .mediaType(Media.MediaType.VIDEO) // External media are typically videos
                .sourceType(sourceType)
                .status(Media.MediaStatus.ACTIVE)
                .url(request.getUrl())
                .title(request.getTitle())
                .description(request.getDescription())
                .altText(request.getAltText())
                .isPrimary(request.getIsPrimary() != null && request.getIsPrimary())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .uploadConfirmed(true) // External media don't need upload confirmation
                .confirmedAt(LocalDateTime.now())
                .embedCode(generateEmbedCode(request.getUrl(), sourceType))
                .build();

        media = mediaRepository.save(media);

        log.info("External media saved successfully: {}", media.getMediaId());

        return mediaMapper.toResponse(media);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getListingMedia(Long listingId) {
        log.info("Fetching media for listing: {}", listingId);

        List<Media> mediaList = mediaRepository
                .findByListing_ListingIdAndStatusOrderBySortOrderAsc(listingId, Media.MediaStatus.ACTIVE);

        return mediaList.stream()
                .map(mediaMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getUserMedia(Long userId) {
        String userIdStr = String.valueOf(userId);
        log.info("Fetching media for user: {}", userIdStr);

        List<Media> mediaList = mediaRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userIdStr, Media.MediaStatus.ACTIVE);

        return mediaList.stream()
                .map(mediaMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse getMediaById(Long mediaId) {
        log.info("Fetching media by ID: {}", mediaId);

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(DomainCode.ADDRESS_NOT_FOUND,
                        "Media not found"));

        return mediaMapper.toResponse(media);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void cleanupExpiredPendingUploads() {
        log.info("Starting cleanup of expired pending uploads");

        // Find uploads pending for more than 2 hours
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(2);
        List<Media> expiredMedia = mediaRepository.findExpiredPendingUploads(expiryTime);

        if (expiredMedia.isEmpty()) {
            log.info("No expired pending uploads found");
            return;
        }

        log.info("Found {} expired pending uploads", expiredMedia.size());

        for (Media media : expiredMedia) {
            try {
                // Delete from storage if exists
                if (media.getStorageKey() != null) {
                    storageService.deleteObject(media.getStorageKey());
                }

                // Mark as deleted
                media.setStatus(Media.MediaStatus.DELETED);
                mediaRepository.save(media);

                log.info("Cleaned up expired pending upload: {}", media.getMediaId());
            } catch (Exception e) {
                log.error("Failed to cleanup media: {}", media.getMediaId(), e);
            }
        }

        log.info("Cleanup completed. Processed {} expired uploads", expiredMedia.size());
    }

    /**
     * Cleanup orphan media (ACTIVE media without listing after 24 hours)
     * This handles cases where user uploaded media but never created a listing
     * Runs every 6 hours
     */
    @Transactional
    @Scheduled(cron = "0 0 */6 * * *") // Run every 6 hours
    public void cleanupOrphanMedia() {
        log.info("Starting cleanup of orphan media (ACTIVE media without listing)");

        // Find ACTIVE media without listing, older than 24 hours
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(24);
        List<Media> orphanMedia = mediaRepository.findOrphanActiveMedia(expiryTime);

        if (orphanMedia.isEmpty()) {
            log.info("No orphan media found");
            return;
        }

        log.info("Found {} orphan media items", orphanMedia.size());

        for (Media media : orphanMedia) {
            try {
                // Delete from storage if exists
                if (media.isUploaded() && media.getStorageKey() != null) {
                    storageService.deleteObject(media.getStorageKey());
                    log.info("Deleted orphan media file from storage: {}", media.getStorageKey());
                }

                // Mark as deleted
                media.setStatus(Media.MediaStatus.DELETED);
                mediaRepository.save(media);

                log.info("Cleaned up orphan media: {} (created at: {}, user: {})",
                        media.getMediaId(), media.getCreatedAt(), media.getUserId());
            } catch (Exception e) {
                log.error("Failed to cleanup orphan media: {}", media.getMediaId(), e);
            }
        }

        log.info("Orphan media cleanup completed. Processed {} items", orphanMedia.size());
    }

    // Helper methods

    private Media.MediaSourceType detectSourceType(String url) {
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            return Media.MediaSourceType.YOUTUBE;
        } else if (url.contains("tiktok.com")) {
            return Media.MediaSourceType.TIKTOK;
        } else {
            return Media.MediaSourceType.EXTERNAL;
        }
    }

    private String generateEmbedCode(String url, Media.MediaSourceType sourceType) {
        switch (sourceType) {
            case YOUTUBE:
                String videoId = extractYouTubeVideoId(url);
                return String.format(
                        "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/%s\" " +
                        "frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; " +
                        "encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>",
                        videoId
                );
            case TIKTOK:
                return String.format(
                        "<blockquote class=\"tiktok-embed\" cite=\"%s\" data-video-id=\"\" " +
                        "style=\"max-width: 605px;min-width: 325px;\" >" +
                        "<section></section></blockquote>",
                        url
                );
            default:
                return null;
        }
    }

    private String extractYouTubeVideoId(String url) {
        // Extract video ID from various YouTube URL formats
        if (url.contains("youtu.be/")) {
            return url.substring(url.lastIndexOf("/") + 1).split("\\?")[0];
        } else if (url.contains("youtube.com/watch?v=")) {
            String[] parts = url.split("v=");
            if (parts.length > 1) {
                return parts[1].split("&")[0];
            }
        }
        return "";
    }

    private boolean isListingPublic(Listing listing) {
        // Check if listing is published/verified
        // Based on your Listing entity, it has 'verified' and 'expired' fields
        return listing.getVerified() != null && listing.getVerified() &&
               (listing.getExpired() == null || !listing.getExpired());
    }
}
