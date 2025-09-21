package com.smartrent.service.storage;

import com.smartrent.config.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {
    private final S3StorageProperties properties;
    private S3Client s3Client;

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/quicktime"); // quicktime = mov

    @PostConstruct
    public void init() {
        s3Client = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .endpointOverride(java.net.URI.create(properties.getEndpoint()))
                .build();
    }

    @Override
    public String uploadImage(MultipartFile file) {
        validateFileType(file, IMAGE_TYPES);
        return upload(file, "images/");
    }

    @Override
    public String uploadVideo(MultipartFile file) {
        validateFileType(file, VIDEO_TYPES);
        return upload(file, "videos/");
    }

    private String upload(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
        String key = folder + UUID.randomUUID() + (ext.isEmpty() ? "" : ("-" + URLEncoder.encode(originalFilename, StandardCharsets.UTF_8)));
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .contentType(file.getContentType())
                    .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception e) {
            throw new RuntimeException("Failed to upload file", e);
        }
        return properties.getPublicUrl() + "/" + key;
    }

    private void validateFileType(MultipartFile file, Set<String> allowedTypes) {
        if (file == null || file.isEmpty() || file.getContentType() == null || !allowedTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type");
        }
    }
}
