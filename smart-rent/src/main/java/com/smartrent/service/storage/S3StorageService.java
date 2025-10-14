package com.smartrent.service.storage;

import com.smartrent.config.S3StorageProperties;
import com.smartrent.dto.request.PresignedUrlRequest;
import com.smartrent.dto.response.PresignedUrlResponse;
import com.smartrent.infra.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {
    private final S3StorageProperties properties;
    private S3Client s3Client;
    private S3Presigner s3Presigner;

    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(15);

    @PostConstruct
    public void init() {
        log.info("Initializing S3 storage client for bucket: {}", properties.getBucketName());

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey()
        );

        s3Client = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(java.net.URI.create(properties.getEndpoint()))
                .build();

        s3Presigner = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(java.net.URI.create(properties.getEndpoint()))
                .build();

        log.info("S3 client and presigner initialized successfully");
    }

    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            log.info("Closing S3 client connection");
            s3Client.close();
        }
        if (s3Presigner != null) {
            log.info("Closing S3 presigner");
            s3Presigner.close();
        }
    }

    @Override
    public String uploadImage(MultipartFile file) {
        validateFile(file, properties.getAllowedImageTypes());
        return upload(file, "images/");
    }

    @Override
    public String uploadVideo(MultipartFile file) {
        validateFile(file, properties.getAllowedVideoTypes());
        return upload(file, "videos/");
    }

    @Override
    public void deleteFile(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        try {
            log.info("Attempting to delete file: {}", key);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build());
            log.info("Successfully deleted file: {}", key);
        } catch (NoSuchKeyException e) {
            log.error("File not found for deletion: {}", key);
            throw new FileNotFoundException(key);
        } catch (S3Exception e) {
            log.error("S3 error while deleting file: {}", key, e);
            throw new FileUploadException("Failed to delete file: " + e.awsErrorDetails().errorMessage());
        }
    }

    private String upload(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String ext = extractFileExtension(originalFilename);
        String key = generateFileKey(folder, ext, originalFilename);

        try {
            log.info("Uploading file to S3: {}", key);
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            log.info("Successfully uploaded file: {}", key);
            return properties.getPublicUrl() + "/" + key;
        } catch (IOException e) {
            log.error("IO error while reading file: {}", originalFilename, e);
            throw new FileUploadException("Failed to read file: " + e.getMessage());
        } catch (S3Exception e) {
            log.error("S3 error while uploading file: {}", key, e);
            throw new FileUploadException("S3 upload failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    private void validateFile(MultipartFile file, List<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException();
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            String allowedTypesStr = allowedTypes.stream()
                    .collect(Collectors.joining(", "));
            throw new InvalidFileTypeException(allowedTypesStr);
        }

        long fileSizeInMB = file.getSize() / (1024 * 1024);
        if (fileSizeInMB > properties.getMaxFileSizeMB()) {
            throw new FileSizeLimitExceededException(properties.getMaxFileSizeMB());
        }
    }

    private String extractFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return "";
    }

    private String generateFileKey(String folder, String ext, String originalFilename) {
        String uniqueId = UUID.randomUUID().toString();
        if (ext.isEmpty()) {
            return folder + uniqueId;
        }
        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8);
        return folder + uniqueId + "-" + encodedFilename;
    }

    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(properties.getPublicUrl())) {
            throw new FileNotFoundException(fileUrl);
        }
        return fileUrl.substring(properties.getPublicUrl().length() + 1);
    }

    // ===== PRE-SIGNED URL METHODS =====

    @Override
    public PresignedUrlResponse generatePresignedImageUploadUrl(PresignedUrlRequest request) {
        validatePresignedRequest(request, properties.getAllowedImageTypes());
        return generatePresignedUploadUrl(request, "images/");
    }

    @Override
    public PresignedUrlResponse generatePresignedVideoUploadUrl(PresignedUrlRequest request) {
        validatePresignedRequest(request, properties.getAllowedVideoTypes());
        return generatePresignedUploadUrl(request, "videos/");
    }

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(PresignedUrlRequest request, String folder) {
        String originalFilename = request.getFilename();
        String ext = extractFileExtension(originalFilename);
        String key = generateFileKey(folder, ext, originalFilename);

        log.info("Generating pre-signed URL for upload: {} ({})", key, request.getContentType());

        try {
            // Create PutObjectRequest
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .contentType(request.getContentType())
                    .build();

            // Create presign request
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(PRESIGNED_URL_DURATION)
                    .putObjectRequest(putObjectRequest)
                    .build();

            // Generate pre-signed URL
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

            String uploadUrl = presignedRequest.url().toString();
            String fileUrl = properties.getPublicUrl() + "/" + key;
            LocalDateTime expiresAt = LocalDateTime.now().plus(PRESIGNED_URL_DURATION);

            log.info("Pre-signed URL generated successfully: {} (expires at: {})", key, expiresAt);

            return PresignedUrlResponse.builder()
                    .uploadUrl(uploadUrl)
                    .fileUrl(fileUrl)
                    .fileKey(key)
                    .expiresAt(expiresAt)
                    .requiredHeaders(PresignedUrlResponse.UploadHeaders.builder()
                            .contentType(request.getContentType())
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL for: {}", key, e);
            throw new FileUploadException("Failed to generate pre-signed URL: " + e.getMessage());
        }
    }

    private void validatePresignedRequest(PresignedUrlRequest request, List<String> allowedTypes) {
        if (request.getFilename() == null || request.getFilename().isBlank()) {
            throw new EmptyFileException();
        }

        String contentType = request.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            String allowedTypesStr = allowedTypes.stream()
                    .collect(Collectors.joining(", "));
            throw new InvalidFileTypeException(allowedTypesStr);
        }

        if (request.getFileSize() != null) {
            long fileSizeInMB = request.getFileSize() / (1024 * 1024);
            if (fileSizeInMB > properties.getMaxFileSizeMB()) {
                throw new FileSizeLimitExceededException(properties.getMaxFileSizeMB());
            }
        }
    }
}
