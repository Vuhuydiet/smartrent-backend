package com.smartrent.service.media;

import com.smartrent.dto.request.ConfirmUploadRequest;
import com.smartrent.dto.request.GenerateUploadUrlRequest;
import com.smartrent.dto.request.SaveExternalMediaRequest;
import com.smartrent.dto.response.GenerateUploadUrlResponse;
import com.smartrent.dto.response.MediaResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.ListingDraftRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ListingRepository listingRepository;
    private final ListingDraftRepository listingDraftRepository;
    private final R2StorageService storageService;
    private final MediaMapper mediaMapper;

    @Override
    @Transactional
    public GenerateUploadUrlResponse generateUploadUrl(GenerateUploadUrlRequest request, String userId) {
        log.info("Generating upload URL for user: {}, type: {}, purpose: {}",
                userId, request.getMediaType(), request.getPurpose());

        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(DomainCode.USER_NOT_FOUND, "User not found"));

        // Validate content type against media type
        boolean isImage = request.getMediaType() == GenerateUploadUrlRequest.MediaType.IMAGE;
        if (!storageService.isValidContentType(request.getContentType(), isImage)) {
            throw new AppException(DomainCode.INVALID_FILE_TYPE,
                    String.format("Invalid content type %s for %s", request.getContentType(), request.getMediaType()));
        }

        // Validate file size against per-type limit
        if (!storageService.isValidFileSize(request.getFileSize(), isImage)) {
            throw new AppException(DomainCode.FILE_TOO_LARGE,
                    String.format("File size exceeds maximum allowed for %s: %d MB",
                            request.getMediaType(), storageService.getMaxSizeMB(isImage)));
        }

        // Cross-validate purpose <-> listingId + mediaType constraints
        GenerateUploadUrlRequest.Purpose purpose = request.getPurpose();
        Listing listing = null;
        String storageKey;

        if (purpose == GenerateUploadUrlRequest.Purpose.LISTING) {
            // listingId is optional on LISTING uploads to support the create-post flow,
            // where the client uploads media before the listing record exists. When a
            // listingId is provided (edit flow), verify ownership and use a listing-scoped
            // storage key. Otherwise fall back to a generic user-scoped key; the media will
            // be associated to a listing later (via the listing create/update endpoint) and
            // the cleanupOrphanMedia job sweeps unassociated ACTIVE media after 24 hours.
            if (request.getListingId() != null) {
                listing = listingRepository.findById(request.getListingId())
                        .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND, "Listing not found"));

                if (!listing.getUserId().equals(userId)) {
                    throw new AppException(DomainCode.UNAUTHORIZED,
                            "You don't have permission to add media to this listing");
                }
                storageKey = storageService.generateListingStorageKey(
                        listing.getListingId(), userId, request.getFilename(), request.getContentType());
            } else {
                storageKey = storageService.generateGenericStorageKey(
                        userId, request.getFilename(), request.getContentType());
            }
        } else if (purpose == GenerateUploadUrlRequest.Purpose.AVATAR) {
            if (request.getListingId() != null) {
                throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                        "listingId must not be provided when purpose is AVATAR");
            }
            if (!isImage) {
                throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                        "Avatar upload requires mediaType=IMAGE");
            }
            storageKey = storageService.generateAvatarStorageKey(
                    userId, request.getFilename(), request.getContentType());
        } else {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR, "Unsupported purpose");
        }

        // Create media entity in PENDING status
        Media media = Media.builder()
                .userId(userId)
                .listing(listing)
                .mediaType(isImage ? Media.MediaType.IMAGE : Media.MediaType.VIDEO)
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

        // Generate pre-signed upload URL with content-type + content-length constraints
        R2StorageService.PresignedUrlResponse presignedUrl =
                storageService.generateUploadUrl(storageKey, request.getContentType(), request.getFileSize());

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
            String userId) {

        log.info("Starting direct upload for user: {}, filename: {}, type: {}",
                userId, file.getOriginalFilename(), mediaType);

        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(DomainCode.USER_NOT_FOUND, "User not found"));

        // Validate listing ownership if provided
        Listing listing = null;
        if (listingId != null) {
            listing = listingRepository.findById(listingId)
                    .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND, "Listing not found"));

            if (!listing.getUserId().equals(userId)) {
                throw new AppException(DomainCode.UNAUTHORIZED,
                        "You don't have permission to add media to this listing");
            }
        }

        return doUploadMedia(file, mediaType, listing, title, description, altText, isPrimary, sortOrder, userId);
    }

    @Override
    @Transactional
    public MediaResponse confirmUpload(Long mediaId, ConfirmUploadRequest request, String userId) {
        log.info("Confirming upload for media ID: {}, user: {}", mediaId, userId);

        // Find media and validate ownership
        Media media = mediaRepository.findByMediaIdAndUserId(mediaId, userId)
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

        // Verify the file actually exists on R2 and its metadata matches the PENDING record.
        // This guards against clients calling /confirm without uploading, and against
        // Content-Length abuse (since SigV4 does not hard-enforce contentLength on PUT).
        Optional<HeadObjectResponse> headOpt = storageService.headObject(media.getStorageKey());
        if (headOpt.isEmpty()) {
            log.warn("Confirm failed: file not found on storage. mediaId={}, key={}",
                    mediaId, media.getStorageKey());
            throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                    "Upload not found on storage. Please upload the file before confirming.");
        }
        HeadObjectResponse head = headOpt.get();

        // Content-Length must match what we recorded pre-sign
        long expectedSize = media.getFileSize() != null ? media.getFileSize() : -1L;
        long actualSize = head.contentLength() != null ? head.contentLength() : -1L;
        if (expectedSize > 0 && actualSize != expectedSize) {
            log.warn("Confirm failed: file size mismatch. mediaId={}, expected={}, actual={}",
                    mediaId, expectedSize, actualSize);
            // Cleanup: delete the rogue object + mark record DELETED
            try {
                storageService.deleteObject(media.getStorageKey());
            } catch (Exception e) {
                log.error("Failed to delete mismatched object: {}", media.getStorageKey(), e);
            }
            media.setStatus(Media.MediaStatus.DELETED);
            mediaRepository.save(media);
            throw new AppException(DomainCode.FILE_TOO_LARGE,
                    String.format("Uploaded file size (%d bytes) does not match declared size (%d bytes)",
                            actualSize, expectedSize));
        }

        // Content-Type sanity check (R2 already rejects PUT if mismatched, so this is a safety net)
        String actualContentType = head.contentType();
        String expectedContentType = media.getMimeType();
        if (actualContentType != null && expectedContentType != null
                && !actualContentType.equalsIgnoreCase(expectedContentType)) {
            log.warn("Content-Type mismatch on confirm. mediaId={}, expected={}, actual={}",
                    mediaId, expectedContentType, actualContentType);
        }

        // Reconcile metadata from R2 (in case anything drifted)
        if (actualContentType != null) {
            media.setMimeType(actualContentType);
        }
        if (actualSize > 0) {
            media.setFileSize(actualSize);
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
    public String generateDownloadUrl(Long mediaId, String userId) {
        log.info("Generating download URL for media ID: {}, user: {}", mediaId, userId);

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
        boolean hasPermission = media.isOwnedBy(userId) ||
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
    public void deleteMedia(Long mediaId, String userId) {
        log.info("Deleting media ID: {}, user: {}", mediaId, userId);

        // Find media and validate ownership
        Media media = mediaRepository.findByMediaIdAndUserId(mediaId, userId)
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
    public MediaResponse saveExternalMedia(SaveExternalMediaRequest request, String userId) {
        log.info("Saving external media for user: {}, URL: {}", userId, request.getUrl());

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(DomainCode.USER_NOT_FOUND, "User not found"));

        // Validate listing if provided
        Listing listing = null;
        if (request.getListingId() != null) {
            listing = listingRepository.findById(request.getListingId())
                    .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND, "Listing not found"));

            // Validate user owns the listing
            if (!listing.getUserId().equals(userId)) {
                throw new AppException(DomainCode.UNAUTHORIZED,
                        "You don't have permission to add media to this listing");
            }
        }

        // Determine source type from URL
        Media.MediaSourceType sourceType = detectSourceType(request.getUrl());

        // Create media entity
        Media media = Media.builder()
                .userId(userId)
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
    public List<MediaResponse> getUserMedia(String userId) {
        log.info("Fetching media for user: {}", userId);

        List<Media> mediaList = mediaRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, Media.MediaStatus.ACTIVE);

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

        // Find uploads pending for more than 1 hour
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(1);
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
     * Cleanup orphan media (ACTIVE media without listing after 24 hours).
     *
     * A media record is only a true orphan if it is neither attached to a listing nor
     * referenced by any draft. Drafts can be long-lived (users save a draft and come back
     * days or weeks later to publish), so media IDs that appear in any listing_drafts.media_ids
     * string must be preserved even if the media row itself has listing_id = NULL.
     */
    @Transactional
    @Scheduled(cron = "0 0 */6 * * *") // Run every 6 hours
    public void cleanupOrphanMedia() {
        log.info("Starting cleanup of orphan media (ACTIVE media without listing)");

        LocalDateTime expiryTime = LocalDateTime.now().minusHours(24);
        List<Media> candidates = mediaRepository.findOrphanActiveMedia(expiryTime);

        if (candidates.isEmpty()) {
            log.info("No orphan media found");
            return;
        }

        Set<Long> draftReferencedIds = loadDraftReferencedMediaIds();
        List<Media> orphanMedia = draftReferencedIds.isEmpty()
                ? candidates
                : candidates.stream()
                    .filter(m -> !draftReferencedIds.contains(m.getMediaId()))
                    .collect(Collectors.toList());

        int protectedByDraft = candidates.size() - orphanMedia.size();
        if (protectedByDraft > 0) {
            log.info("Protected {} orphan candidate(s) from cleanup because they are still referenced by drafts",
                    protectedByDraft);
        }

        if (orphanMedia.isEmpty()) {
            log.info("All orphan candidates are referenced by drafts; nothing to clean up");
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

    /**
     * Parse every listing_drafts.media_ids string into a single Set of media IDs. Entries are
     * stored as comma-separated strings, so this walks each row once and ignores malformed
     * tokens rather than failing the cron over a single bad draft.
     */
    private Set<Long> loadDraftReferencedMediaIds() {
        List<String> raw = listingDraftRepository.findAllDraftMediaIdsStrings();
        if (raw == null || raw.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        for (String csv : raw) {
            if (csv == null || csv.isBlank()) {
                continue;
            }
            for (String token : csv.split(",")) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    ids.add(Long.parseLong(trimmed));
                } catch (NumberFormatException e) {
                    log.warn("Ignoring malformed draft media id token: '{}'", trimmed);
                }
            }
        }
        return ids;
    }

    // ==================== Admin Methods ====================

    @Override
    @Transactional
    public MediaResponse adminUploadMedia(
            MultipartFile file,
            String mediaType,
            Long listingId,
            String title,
            String description,
            String altText,
            Boolean isPrimary,
            Integer sortOrder,
            String adminId) {

        log.info("Admin upload - admin: {}, filename: {}, type: {}",
                adminId, file.getOriginalFilename(), mediaType);

        // Validate admin exists
        adminRepository.findById(adminId)
                .orElseThrow(() -> new AppException(DomainCode.USER_NOT_FOUND, "Admin not found"));

        // Validate listing if provided (no ownership check for admin)
        Listing listing = null;
        if (listingId != null) {
            listing = listingRepository.findById(listingId)
                    .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND, "Listing not found"));
        }

        return doUploadMedia(file, mediaType, listing, title, description, altText, isPrimary, sortOrder, adminId);
    }

    @Override
    @Transactional
    public void adminDeleteMedia(Long mediaId) {
        log.info("Admin deleting media ID: {}", mediaId);

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(DomainCode.ADDRESS_NOT_FOUND, "Media not found"));

        media.setStatus(Media.MediaStatus.DELETED);
        mediaRepository.save(media);

        if (media.isUploaded() && media.getStorageKey() != null) {
            try {
                storageService.deleteObject(media.getStorageKey());
                log.info("File deleted from storage: {}", media.getStorageKey());
            } catch (Exception e) {
                log.error("Failed to delete file from storage: {}", media.getStorageKey(), e);
            }
        }

        log.info("Admin deleted media successfully: {}", mediaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> adminGetAllMedia(String status, int page, int size) {
        log.info("Admin fetching media - status: {}, page: {}, size: {}", status, page, size);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Media> mediaList;
        if (status != null && !status.isBlank()) {
            Media.MediaStatus mediaStatus = Media.MediaStatus.valueOf(status.toUpperCase());
            mediaList = mediaRepository.findByStatus(mediaStatus, pageRequest).getContent();
        } else {
            mediaList = mediaRepository.findAll(pageRequest).getContent();
        }

        return mediaList.stream()
                .map(mediaMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== Core Upload Logic ====================

    private MediaResponse doUploadMedia(
            MultipartFile file,
            String mediaType,
            Listing listing,
            String title,
            String description,
            String altText,
            Boolean isPrimary,
            Integer sortOrder,
            String ownerId) {

        // Validate file is not empty
        if (file.isEmpty()) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR, "File is empty");
        }

        // Determine if image or video
        boolean isImage = "IMAGE".equalsIgnoreCase(mediaType);
        Media.MediaType mediaTypeEnum = isImage ? Media.MediaType.IMAGE : Media.MediaType.VIDEO;

        // Validate file size against per-type limit
        if (!storageService.isValidFileSize(file.getSize(), isImage)) {
            throw new AppException(DomainCode.FILE_TOO_LARGE,
                    String.format("File size exceeds maximum allowed for %s: %d MB",
                            mediaType, storageService.getMaxSizeMB(isImage)));
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !storageService.isValidContentType(contentType, isImage)) {
            throw new AppException(DomainCode.INVALID_FILE_TYPE,
                    String.format("Invalid content type: %s for media type: %s", contentType, mediaType));
        }

        // Generate storage key: listing-scoped when we have a listing, otherwise generic
        String storageKey = (listing != null)
                ? storageService.generateListingStorageKey(
                        listing.getListingId(), ownerId, file.getOriginalFilename(), contentType)
                : storageService.generateGenericStorageKey(
                        ownerId, file.getOriginalFilename(), contentType);

        try {
            log.info("Uploading file to cloud storage: {}", storageKey);
            storageService.uploadFile(
                    storageKey,
                    file.getInputStream(),
                    contentType,
                    file.getSize()
            );
            log.info("File uploaded successfully to cloud storage: {}", storageKey);

            String publicUrl = storageService.getPublicUrl(storageKey);

            Media media = Media.builder()
                    .userId(ownerId)
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
            throw new AppException(DomainCode.FILE_READ_ERROR, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to upload file to cloud storage: {}", storageKey, e);
            throw new AppException(DomainCode.FILE_UPLOAD_ERROR, e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

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
