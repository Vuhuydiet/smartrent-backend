package com.smartrent.infra.storage;

import com.smartrent.config.S3StorageConfig;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

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
        if (s3Client != null) {
            s3Client.close();
            log.info("S3 Client closed");
        }
        if (presigner != null) {
            presigner.close();
            log.info("S3 Presigner closed");
        }
    }

    /**
     * Generate pre-signed upload URL
     */
    public PresignedUrlResponse generateUploadUrl(String key, String contentType) {
        try {
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

            log.info("Generated upload URL for key: {}, expires in {} minutes", key, config.getUploadUrlTtlMinutes());

            return PresignedUrlResponse.builder()
                    .url(presignedRequest.url().toString())
                    .expiresIn(config.getUploadUrlTtlMinutes() * 60)
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate upload URL for key: {}", key, e);
            throw new RuntimeException("Failed to generate upload URL", e);
        }
    }

    /**
     * Generate pre-signed download URL
     */
    public PresignedUrlResponse generateDownloadUrl(String key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(config.getDownloadUrlTtlMinutes()))
                    .getObjectRequest(getRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

            log.info("Generated download URL for key: {}, expires in {} minutes", key, config.getDownloadUrlTtlMinutes());

            return PresignedUrlResponse.builder()
                    .url(presignedRequest.url().toString())
                    .expiresIn(config.getDownloadUrlTtlMinutes() * 60)
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate download URL for key: {}", key, e);
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    /**
     * Upload file directly from backend to R2/S3
     * Use this when the backend receives the file and needs to upload it to cloud storage
     *
     * @param key Storage key (path in bucket)
     * @param fileData Byte array of file content
     * @param contentType MIME type of the file
     * @param fileSize Size of the file in bytes
     * @return PutObjectResponse with upload metadata
     */
    public PutObjectResponse uploadFile(String key, byte[] fileData, String contentType, long fileSize) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(fileSize)
                    .build();

            PutObjectResponse response = s3Client.putObject(
                    putRequest,
                    RequestBody.fromBytes(fileData)
            );

            log.info("Successfully uploaded file to storage: {}, size: {} bytes, ETag: {}",
                    key, fileSize, response.eTag());

            return response;
        } catch (Exception e) {
            log.error("Failed to upload file to storage: {}", key, e);
            throw new RuntimeException("Failed to upload file to cloud storage", e);
        }
    }

    /**
     * Upload file directly from backend using InputStream
     * More memory-efficient for large files
     *
     * @param key Storage key (path in bucket)
     * @param inputStream InputStream of file content
     * @param contentType MIME type of the file
     * @param fileSize Size of the file in bytes
     * @return PutObjectResponse with upload metadata
     */
    public PutObjectResponse uploadFile(String key, java.io.InputStream inputStream, String contentType, long fileSize) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(fileSize)
                    .build();

            PutObjectResponse response = s3Client.putObject(
                    putRequest,
                    RequestBody.fromInputStream(inputStream, fileSize)
            );

            log.info("Successfully uploaded file to storage: {}, size: {} bytes, ETag: {}",
                    key, fileSize, response.eTag());

            return response;
        } catch (Exception e) {
            log.error("Failed to upload file to storage: {}", key, e);
            throw new RuntimeException("Failed to upload file to cloud storage", e);
        }
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
            log.info("Successfully deleted object: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete object: {}", key, e);
            throw new RuntimeException("Failed to delete file from storage", e);
        }
    }

    /**
     * Generate storage key for media
     */
    public String generateStorageKey(Long userId, String filename) {
        // Sanitize filename: keep only alphanumeric, dots, underscores, and hyphens
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uuid = UUID.randomUUID().toString();
        return String.format("media/%d/%s_%s", userId, uuid, sanitized);
    }

    /**
     * Get public URL for a key (CDN URL)
     */
    public String getPublicUrl(String key) {
        if (config.getPublicUrl() != null && !config.getPublicUrl().isEmpty()) {
            return config.getPublicUrl() + "/" + key;
        }
        // Fallback to endpoint URL if no public URL configured
        return config.getEndpoint() + "/" + config.getBucketName() + "/" + key;
    }

    /**
     * Validate file size
     */
    public boolean isValidFileSize(Long fileSize) {
        long maxSizeBytes = config.getMaxFileSizeMB() * 1024L * 1024L;
        return fileSize > 0 && fileSize <= maxSizeBytes;
    }

    /**
     * Validate content type
     */
    public boolean isValidContentType(String contentType, boolean isImage) {
        if (contentType == null || contentType.isEmpty()) {
            return false;
        }

        if (isImage) {
            return config.getAllowedImageTypes() != null &&
                   config.getAllowedImageTypes().contains(contentType.toLowerCase());
        } else {
            return config.getAllowedVideoTypes() != null &&
                   config.getAllowedVideoTypes().contains(contentType.toLowerCase());
        }
    }

    @Data
    @Builder
    public static class PresignedUrlResponse {
        private String url;
        private int expiresIn; // seconds
    }
}
