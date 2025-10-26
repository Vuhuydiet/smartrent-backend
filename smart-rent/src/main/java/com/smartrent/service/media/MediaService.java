package com.smartrent.service.media;

import com.smartrent.dto.request.ConfirmUploadRequest;
import com.smartrent.dto.request.GenerateUploadUrlRequest;
import com.smartrent.dto.request.SaveExternalMediaRequest;
import com.smartrent.dto.response.GenerateUploadUrlResponse;
import com.smartrent.dto.response.MediaResponse;

import java.util.List;

public interface MediaService {

    /**
     * Generate pre-signed upload URL (Step 1 of upload flow)
     */
    GenerateUploadUrlResponse generateUploadUrl(GenerateUploadUrlRequest request, Long userId);

    /**
     * Confirm upload completion (Step 2 of upload flow)
     */
    MediaResponse confirmUpload(Long mediaId, ConfirmUploadRequest request, Long userId);

    /**
     * Generate pre-signed download URL
     */
    String generateDownloadUrl(Long mediaId, Long userId);

    /**
     * Delete media (soft delete with storage cleanup)
     */
    void deleteMedia(Long mediaId, Long userId);

    /**
     * Save external media (YouTube/TikTok)
     */
    MediaResponse saveExternalMedia(SaveExternalMediaRequest request, Long userId);

    /**
     * Get all media for a listing
     */
    List<MediaResponse> getListingMedia(Long listingId);

    /**
     * Get all media for a user
     */
    List<MediaResponse> getUserMedia(Long userId);

    /**
     * Get media by ID
     */
    MediaResponse getMediaById(Long mediaId);

    /**
     * Cleanup expired pending uploads (scheduled job)
     */
    void cleanupExpiredPendingUploads();
}
