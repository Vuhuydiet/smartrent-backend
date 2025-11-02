package com.smartrent.service.media;

import com.smartrent.dto.request.ConfirmUploadRequest;
import com.smartrent.dto.request.GenerateUploadUrlRequest;
import com.smartrent.dto.request.SaveExternalMediaRequest;
import com.smartrent.dto.response.GenerateUploadUrlResponse;
import com.smartrent.dto.response.MediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {

    /**
     * Generate pre-signed upload URL (Step 1 of upload flow)
     */
    GenerateUploadUrlResponse generateUploadUrl(GenerateUploadUrlRequest request, String userId);

    /**
     * Upload file directly from backend to cloud storage
     * This is a complete upload flow: validate → upload → save → return
     *
     * @param file MultipartFile from request
     * @param mediaType Type of media (IMAGE or VIDEO)
     * @param listingId Optional listing ID to associate with
     * @param title Optional title
     * @param description Optional description
     * @param altText Optional alt text for images
     * @param isPrimary Whether this is the primary media
     * @param sortOrder Display order
     * @param userId User ID (UUID String) performing the upload
     * @return MediaResponse with media details and public URL
     */
    MediaResponse uploadMedia(
            MultipartFile file,
            String mediaType,
            Long listingId,
            String title,
            String description,
            String altText,
            Boolean isPrimary,
            Integer sortOrder,
            String userId
    );

    /**
     * Confirm upload completion (Step 2 of upload flow)
     */
    MediaResponse confirmUpload(Long mediaId, ConfirmUploadRequest request, String userId);

    /**
     * Generate pre-signed download URL
     */
    String generateDownloadUrl(Long mediaId, String userId);

    /**
     * Delete media (soft delete with storage cleanup)
     */
    void deleteMedia(Long mediaId, String userId);

    /**
     * Save external media (YouTube/TikTok)
     */
    MediaResponse saveExternalMedia(SaveExternalMediaRequest request, String userId);

    /**
     * Get all media for a listing
     */
    List<MediaResponse> getListingMedia(Long listingId);

    /**
     * Get all media for a user
     */
    List<MediaResponse> getUserMedia(String userId);

    /**
     * Get media by ID
     */
    MediaResponse getMediaById(Long mediaId);

    /**
     * Cleanup expired pending uploads (scheduled job)
     */
    void cleanupExpiredPendingUploads();
}
