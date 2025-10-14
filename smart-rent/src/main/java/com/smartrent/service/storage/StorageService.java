package com.smartrent.service.storage;

import com.smartrent.dto.request.PresignedUrlRequest;
import com.smartrent.dto.response.PresignedUrlResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    // Legacy direct upload methods (still supported)
    String uploadImage(MultipartFile file);
    String uploadVideo(MultipartFile file);
    void deleteFile(String fileUrl);

    // New pre-signed URL methods
    PresignedUrlResponse generatePresignedUploadUrl(PresignedUrlRequest request, String folder);
    PresignedUrlResponse generatePresignedImageUploadUrl(PresignedUrlRequest request);
    PresignedUrlResponse generatePresignedVideoUploadUrl(PresignedUrlRequest request);
}
