package com.smartrent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "open.storage.s3")
public class S3StorageProperties {
    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String publicUrl;
    private long maxFileSizeMB;
    private List<String> allowedImageTypes;
    private List<String> allowedVideoTypes;
}
