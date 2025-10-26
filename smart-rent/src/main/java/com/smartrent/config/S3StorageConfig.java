package com.smartrent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "open.storage.s3")
@Getter
@Setter
public class S3StorageConfig {
    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String publicUrl;
    private Integer maxFileSizeMB;
    private List<String> allowedImageTypes;
    private List<String> allowedVideoTypes;

    // Presigned URL TTL
    private Integer uploadUrlTtlMinutes = 30;
    private Integer downloadUrlTtlMinutes = 60;
}
