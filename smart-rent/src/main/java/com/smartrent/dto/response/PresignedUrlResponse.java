package com.smartrent.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresignedUrlResponse {

    /**
     * Pre-signed URL for direct upload to S3
     */
    String uploadUrl;

    /**
     * The public URL where the file will be accessible after upload
     */
    String fileUrl;

    /**
     * Unique key/path for the file in S3
     */
    String fileKey;

    /**
     * Expiration time of the pre-signed URL
     */
    LocalDateTime expiresAt;

    /**
     * Required headers for the upload request
     */
    UploadHeaders requiredHeaders;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UploadHeaders {
        String contentType;
        // Add more headers if needed (e.g., Content-MD5, x-amz-meta-*)
    }
}
